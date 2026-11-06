// Meridian read-only extensions for tobiasehlert/teslamateapi 1.25.0.
package main

import (
	"context"
	"database/sql"
	"fmt"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
)

func registerMeridian(r *gin.RouterGroup) {
	r.GET("/cars/:CarID/meridian/features", func(c *gin.Context) {
		c.JSON(200, gin.H{"data": gin.H{"schema_version": 1, "read_only": true, "features": []string{"cost_provenance", "drive_places", "history", "tracks"}}})
	})
	r.GET("/cars/:CarID/meridian/charges", meridianCharges)
	r.GET("/cars/:CarID/meridian/drives", meridianDrives)
	r.GET("/cars/:CarID/meridian/history", meridianHistory)
	r.GET("/cars/:CarID/meridian/tracks", meridianTracks)
}

func meridianCar(c *gin.Context) (int, bool) {
	id, err := strconv.Atoi(c.Param("CarID"))
	if err != nil || id <= 0 {
		c.JSON(400, gin.H{"error": "invalid car id"})
		return 0, false
	}
	return id, true
}
func meridianPage(c *gin.Context) (int, int) {
	p, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	n, _ := strconv.Atoi(c.DefaultQuery("show", "500"))
	if p < 1 {
		p = 1
	}
	if n < 1 || n > 500 {
		n = 500
	}
	return n, (p - 1) * n
}
func meridianTimes(c *gin.Context, days int) (time.Time, time.Time, bool) {
	end := time.Now().UTC()
	start := end.AddDate(0, 0, -days)
	for _, field := range []string{"startDate", "endDate"} {
		if raw := c.Query(field); raw != "" {
			t, err := time.Parse(time.RFC3339, raw)
			if err != nil {
				c.JSON(400, gin.H{"error": "use RFC3339 dates"})
				return start, end, false
			}
			if field == "startDate" {
				start = t
			} else {
				end = t
			}
		}
	}
	if !end.After(start) {
		c.JSON(400, gin.H{"error": "invalid date range"})
		return start, end, false
	}
	return start.UTC(), end.UTC(), true
}
func meridianRows(c *gin.Context, query string, args ...any) ([]gin.H, bool) {
	ctx, cancel := context.WithTimeout(c.Request.Context(), 15*time.Second)
	defer cancel()
	rows, err := db.QueryContext(ctx, query, args...)
	if err != nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "read-only data unavailable"})
		return nil, false
	}
	defer rows.Close()
	cols, err := rows.Columns()
	if err != nil {
		c.JSON(503, gin.H{"error": "read unavailable"})
		return nil, false
	}
	result := []gin.H{}
	for rows.Next() {
		vals := make([]any, len(cols))
		ptr := make([]any, len(cols))
		for i := range vals {
			ptr[i] = &vals[i]
		}
		if rows.Scan(ptr...) != nil {
			c.JSON(503, gin.H{"error": "read unavailable"})
			return nil, false
		}
		item := gin.H{}
		for i, k := range cols {
			v := vals[i]
			switch x := v.(type) {
			case []byte:
				v = string(x)
			case time.Time:
				v = x.UTC().Format(time.RFC3339)
			}
			item[k] = v
		}
		result = append(result, item)
	}
	if rows.Err() != nil {
		c.JSON(503, gin.H{"error": "read incomplete"})
		return nil, false
	}
	return result, true
}
func meridianHas(table string) bool {
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	var name sql.NullString
	return db.QueryRowContext(ctx, "SELECT to_regclass($1)::text", table).Scan(&name) == nil && name.Valid
}
func meridianFunction(signature string) bool {
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	var name sql.NullString
	return db.QueryRowContext(ctx, "SELECT to_regprocedure($1)::text", signature).Scan(&name) == nil && name.Valid
}
func meridianCharges(c *gin.Context) {
	car, ok := meridianCar(c)
	if !ok {
		return
	}
	n, offset := meridianPage(c)
	manualCols := "NULL::numeric AS cost_manual,NULL::numeric AS manual_rate,NULL::text AS override_source,NULL::numeric AS override_original_cost"
	manualJoin := ""
	manualValue := "NULL::numeric"
	manualSource := "NULL::text"
	if meridianHas("public.charging_process_cost_overrides") {
		manualCols = "CASE WHEN o.source='manual' THEN o.cost END AS cost_manual,o.rate AS manual_rate,o.source AS override_source,o.original_cost AS override_original_cost"
		manualJoin = " LEFT JOIN charging_process_cost_overrides o ON o.charging_process_id=cp.id "
		manualValue = "CASE WHEN o.source='manual' THEN o.cost END"
		manualSource = "CASE WHEN o.source='manual' THEN CASE WHEN o.rate IS NOT NULL THEN '人工估算' ELSE '人工覆盖' END END"
	}
	touCols := "NULL::numeric AS cost_tou"
	touJoin := ""
	touValue := "NULL::numeric"
	if meridianHas("public.charging_processes_tou_cost") {
		touCols = "t.cost_tou"
		touValue = "t.cost_tou"
		touJoin = " LEFT JOIN charging_processes_tou_cost t ON t.charging_process_id=cp.id "
	}
	effective := "COALESCE(" + manualValue + ",cp.cost," + touValue + ")"
	defaultSource := "'未知'"
	if meridianFunction("public.effective_cost(integer,numeric)") {
		effective = "effective_cost(cp.id,cp.cost)"
		defaultSource = "'默认估算'"
	}
	source := "CASE WHEN (" + manualValue + ") IS NOT NULL THEN (" + manualSource + ") WHEN cp.cost IS NOT NULL THEN '原始记录' WHEN (" + touValue + ") IS NOT NULL THEN '分时计算' ELSE " + defaultSource + " END"
	query := fmt.Sprintf(`SELECT cp.id AS charge_id,cp.cost AS cost_recorded,cp.charge_energy_used AS energy_used_raw,cp.charge_energy_added AS energy_added_raw,%s,%s,%s AS cost_effective,%s AS cost_source
 FROM charging_processes cp %s %s WHERE cp.car_id=$1 AND cp.end_date IS NOT NULL ORDER BY cp.id LIMIT $2 OFFSET $3`, manualCols, touCols, effective, source, manualJoin, touJoin)
	if rows, ok := meridianRows(c, query, car, n, offset); ok {
		c.JSON(200, gin.H{"data": gin.H{"car_id": car, "records": rows}})
	}
}
func meridianDrives(c *gin.Context) {
	car, ok := meridianCar(c)
	if !ok {
		return
	}
	n, offset := meridianPage(c)
	query := `SELECT d.id AS drive_id,COALESCE(sa.city,sa.county,'') AS start_city,COALESCE(sa.state,'') AS start_province,
 COALESCE(sa.country,'') AS start_country,COALESCE(sa.state_district,sa.county,'') AS start_district,COALESCE(ea.city,ea.county,'') AS end_city,COALESCE(ea.state,'') AS end_province,COALESCE(ea.country,'') AS end_country,COALESCE(ea.state_district,ea.county,'') AS end_district,
 sp.latitude AS start_latitude,sp.longitude AS start_longitude,ep.latitude AS end_latitude,ep.longitude AS end_longitude,
 sp.battery_level AS start_battery,ep.battery_level AS end_battery
 FROM drives d LEFT JOIN addresses sa ON d.start_address_id=sa.id LEFT JOIN addresses ea ON d.end_address_id=ea.id
 LEFT JOIN positions sp ON d.start_position_id=sp.id LEFT JOIN positions ep ON d.end_position_id=ep.id
 WHERE d.car_id=$1 AND d.end_date IS NOT NULL ORDER BY d.id LIMIT $2 OFFSET $3`
	if rows, ok := meridianRows(c, query, car, n, offset); ok {
		c.JSON(200, gin.H{"data": gin.H{"car_id": car, "records": rows}})
	}
}
func meridianHistory(c *gin.Context) {
	car, ok := meridianCar(c)
	if !ok {
		return
	}
	start, end, ok := meridianTimes(c, 30)
	if !ok {
		return
	}
	if end.Sub(start) > 367*24*time.Hour {
		c.JSON(400, gin.H{"error": "history range exceeds one year"})
		return
	}
	query := `SELECT DISTINCT ON (date_trunc('hour',date)) date AT TIME ZONE 'UTC' AS time,battery_level,rated_battery_range_km AS remaining_range,
 inside_temp,outside_temp,speed,odometer FROM positions WHERE car_id=$1 AND date >= $2 AND date < $3
 ORDER BY date_trunc('hour',date),date DESC,id DESC`
	samples, ok := meridianRows(c, query, car, start.Format(dbTimestampFormat), end.Format(dbTimestampFormat))
	if !ok {
		return
	}
	events, ok := meridianRows(c, `SELECT id,state,start_date AT TIME ZONE 'UTC' AS start_time,end_date AT TIME ZONE 'UTC' AS end_time FROM states WHERE car_id=$1 AND start_date<$3 AND (end_date IS NULL OR end_date>=$2) ORDER BY start_date DESC,id DESC LIMIT 1000`, car, start.Format(dbTimestampFormat), end.Format(dbTimestampFormat))
	if !ok {
		return
	}
	c.JSON(200, gin.H{"data": gin.H{"car_id": car, "samples": samples, "states": events, "sampling": "one_last_position_per_hour", "state_limit": 1000}})
}
func meridianTracks(c *gin.Context) {
	car, ok := meridianCar(c)
	if !ok {
		return
	}
	start, end, ok := meridianTimes(c, 30)
	if !ok {
		return
	}
	n, offset := meridianPage(c)
	if n > 100 {
		offset = offset / n * 100
		n = 100
	}
	query := `WITH selected AS (SELECT id FROM drives WHERE car_id=$1 AND start_date >= $2 AND start_date < $3 AND end_date IS NOT NULL ORDER BY start_date DESC,id DESC LIMIT $4 OFFSET $5),
 ranked AS (SELECT p.drive_id,p.date,p.latitude,p.longitude,p.id,row_number() OVER (PARTITION BY p.drive_id ORDER BY p.date,p.id) AS rn,count(*) OVER (PARTITION BY p.drive_id) AS cnt FROM positions p JOIN selected s ON p.drive_id=s.id)
 SELECT drive_id,date AT TIME ZONE 'UTC' AS time,latitude,longitude FROM ranked WHERE rn=1 OR rn=cnt OR (rn-1) % GREATEST(1,CEIL(cnt/120.0)::integer)=0 ORDER BY drive_id,date,id`
	if rows, ok := meridianRows(c, query, car, start.Format(dbTimestampFormat), end.Format(dbTimestampFormat), n, offset); ok {
		c.JSON(200, gin.H{"data": gin.H{"car_id": car, "points": rows, "trip_page_size": n}})
	}
}
