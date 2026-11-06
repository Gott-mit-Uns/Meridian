package app.meridian.mobile
import org.junit.Assert.*
import org.junit.Test
class BackSceneGeometryTest{
 @Test fun invalidProgressCannotDistortTheScene(){for(p in listOf(Float.NaN,Float.POSITIVE_INFINITY,-2f)){val g=backSceneGeometry(p,1f,1000f,3f,true);assertEquals(1f,g.scale,0f);assertEquals(0f,g.translationX,0f)};assertEquals(1f,safeBackProgress(3f),0f)}
 @Test fun eitherEdgeRevealsParentWithVerticalSharedAxis(){val left=backSceneGeometry(1f,1f,1000f,3f,true);val right=backSceneGeometry(1f,-1f,1000f,3f,true);assertEquals(25f,left.translationX,0f);assertEquals(-left.translationX,right.translationX,0f);assertEquals(.985f,left.scale,.0001f);assertEquals(18f,left.radiusDp,0f);assertEquals(192f,left.translationY,0f);assertEquals(0f,left.alpha,0f)}
 @Test fun intermediateGestureDoesNotWaitUntilReleaseToRevealParent(){val mid=backSceneGeometry(.5f,1f,1000f,2f,true);assertEquals(.5f,mid.alpha,0f);assertEquals(64f,mid.translationY,0f);val cancel=backSceneGeometry(0f,1f,1000f,2f,true);assertEquals(1f,cancel.alpha,0f);assertEquals(0f,cancel.translationY,0f)}
 @Test fun cancelAndReducedMotionRestoreIdentity(){val reset=backSceneGeometry(0f,-1f,1000f,3f,true);val reduced=backSceneGeometry(.8f,1f,1000f,3f,false);assertEquals(BackSceneGeometry(1f,0f,0f),reduced);assertEquals(1f,reset.scale,0f);assertEquals(0f,reset.translationX,0f);assertEquals(0f,reset.radiusDp,0f)}
}
