package main

import (
	"github.com/gin-gonic/gin"
	"net/http/httptest"
	"testing"
)

func TestMeridianInvalidInputsNeverReachDatabase(t *testing.T) {
	gin.SetMode(gin.TestMode)
	for _, path := range []string{"/cars/0/meridian/charges", "/cars/nope/meridian/drives", "/cars/1/meridian/history?startDate=invalid", "/cars/1/meridian/history?startDate=2020-01-01T00:00:00Z&endDate=2026-01-01T00:00:00Z"} {
		router := gin.New()
		registerMeridian(router.Group(""))
		response := httptest.NewRecorder()
		request := httptest.NewRequest("GET", path, nil)
		router.ServeHTTP(response, request)
		if response.Code != 400 {
			t.Fatalf("invalid request status=%d", response.Code)
		}
	}
}
func TestMeridianExtensionHasNoMutationRoutes(t *testing.T) {
	router := gin.New()
	registerMeridian(router.Group(""))
	for _, route := range router.Routes() {
		if route.Method != "GET" {
			t.Fatal("extension registered a mutation route")
		}
	}
}
func TestMeridianPageBounds(t *testing.T) {
	for _, query := range []string{"page=0&show=0", "page=-1&show=999999"} {
		c, _ := gin.CreateTestContext(httptest.NewRecorder())
		c.Request = httptest.NewRequest("GET", "/?"+query, nil)
		n, offset := meridianPage(c)
		if n != 500 || offset != 0 {
			t.Fatal("invalid pagination was not bounded")
		}
	}
}
