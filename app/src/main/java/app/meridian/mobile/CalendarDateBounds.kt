package app.meridian.mobile
import java.time.LocalDate
internal fun boundedCalendarDate(date:LocalDate,latest:LocalDate,earliest:LocalDate?):LocalDate{
 require(earliest==null||earliest<=latest)
 return when{date>latest->latest;earliest!=null&&date<earliest->earliest;else->date}
}
