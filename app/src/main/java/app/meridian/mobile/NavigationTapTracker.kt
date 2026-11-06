package app.meridian.mobile

internal class NavigationTapTracker {
 private var previous=-1
 private var time=Long.MIN_VALUE
 fun tap(target:Int,current:Int,now:Long):Boolean {
  val double=target==current && previous==target && time!=Long.MIN_VALUE && now-time in 0..320
  previous=if(double)-1 else target;time=if(double)Long.MIN_VALUE else now
  return double
 }
}
