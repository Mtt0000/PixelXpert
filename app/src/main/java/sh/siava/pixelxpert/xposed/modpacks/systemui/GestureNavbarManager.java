package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static android.view.MotionEvent.ACTION_DOWN;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.Context;
import android.graphics.Point;
import android.view.MotionEvent;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.KSUConfigReader;

@SuppressWarnings("RedundantThrows")
@SystemUIModPack
public class GestureNavbarManager extends XposedModPack {
	//region Back gesture
	private static float backGestureHeightFractionLeft = 0f; // % of screen height. can be anything between 0 to 1
	private static float backGestureHeightFractionRight = 0f; // % of screen height. can be anything between 0 to 1
	private static boolean leftEnabled = false;
	private static boolean rightEnabled = false;
	float initialBackX = 0;

	Object EdgeBackGestureHandler;
	//endregion

	public GestureNavbarManager(Context context) {
		super(context);
	}

	public void onPreferenceUpdated(String... Key) {
		boolean disableGestures = KSUConfigReader.getBoolean("back_gesture", true);
		if (disableGestures) {
			leftEnabled = false;
			rightEnabled = false;
			backGestureHeightFractionLeft = 0f;
			backGestureHeightFractionRight = 0f;
		}
	}

	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		onPreferenceUpdated(); // Initialize settings from KSU config

		boolean disableGestures = KSUConfigReader.getBoolean("back_gesture", true);
		if (!disableGestures) return;

		ReflectedClass EdgeBackGestureHandlerClass = ReflectedClass.ofIfPossible("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler");
		ReflectedClass NavigationBarEdgePanelClass = ReflectedClass.ofIfPossible("com.android.systemui.navigationbar.gestural.NavigationBarEdgePanel");
		ReflectedClass BackPanelControllerClass = ReflectedClass.of("com.android.systemui.navigationbar.gestural.BackPanelController");

		//region back gesture
		//A16 QPR2 - The class doesn't have a visible constructor anymore, thus replacement method
		EdgeBackGestureHandlerClass
				.before("updateIsEnabled")
				.run(param -> EdgeBackGestureHandler = param.thisObject);

		BackPanelControllerClass
				.before("onMotionEvent")
				.run(param -> {
					try {
						MotionEvent ev = (MotionEvent) param.args[0];

						if(ev.getActionMasked() == ACTION_DOWN) //down action is enough. once gesture is refused it won't accept further actions
						{
							if(notWithinInsets(ev.getX(),
									ev.getY(),
									(Point) getObjectField(EdgeBackGestureHandler, "mDisplaySize"),
									getFloatField(EdgeBackGestureHandler, "mBottomGestureHeight")))
							{
								setObjectField(EdgeBackGestureHandler, "mAllowGesture", false); //act like the gesture was not good enough
								param.setResult(null); //and stop the current method too
							}
						}
					} catch (Throwable t) {
						// Suppress crash on Android 17 if obfuscated / missing fields
					}
				});

		//Android 13
		NavigationBarEdgePanelClass
				.before("onMotionEvent")
				.run(param -> {
					try {
						MotionEvent event = (MotionEvent) param.args[0];
						if(event.getAction() == ACTION_DOWN)
						{
							initialBackX = event.getX();
						}
						if (notWithinInsets(initialBackX, event.getY(), (Point) getObjectField(param.thisObject, "mDisplaySize"), 0)) {
							//event.setAction(MotionEvent.ACTION_CANCEL);
							param.setResult(null);
						}
					} catch (Throwable t) {
						// Suppress crash on Android 17 if obfuscated / missing fields
					}
				});
		//endregion

	}

	//region Back gesture
	private boolean notWithinInsets(float x, float y, Point mDisplaySize, float mBottomGestureHeight) {
		boolean isLeftSide = x < (mDisplaySize.x / 3f);
		if ((isLeftSide && !leftEnabled)
				|| (!isLeftSide && !rightEnabled)) {
			return true;
		}

		int mEdgeHeight = isLeftSide ?
				Math.round(mDisplaySize.y * backGestureHeightFractionLeft) :
				Math.round(mDisplaySize.y * backGestureHeightFractionRight);

		return mEdgeHeight != 0
				&& y < (mDisplaySize.y
				- mBottomGestureHeight
				- mEdgeHeight);
	}
	//endregion
}
