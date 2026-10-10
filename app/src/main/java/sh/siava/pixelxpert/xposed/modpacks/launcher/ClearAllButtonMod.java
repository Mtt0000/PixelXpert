package sh.siava.pixelxpert.xposed.modpacks.launcher;

import static android.view.View.GONE;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.LauncherModPack;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.KSUConfigReader;

@SuppressWarnings("RedundantThrows")
@LauncherModPack
public class ClearAllButtonMod extends XposedModPack {
	private Object recentView;
	private static boolean RecentClearAllReposition = true;
	private android.widget.Button clearAllButton;

	public ClearAllButtonMod(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		RecentClearAllReposition = KSUConfigReader.getBoolean("clear_all", true);
	}

	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		RecentClearAllReposition = KSUConfigReader.getBoolean("clear_all", true);

		ReflectedClass OverviewActionsViewClass = ReflectedClass.of("com.android.quickstep.views.OverviewActionsView");
		ReflectedClass RecentsViewClass = ReflectedClass.of("com.android.quickstep.views.RecentsView");
		Method dismissAllTasksMethod = findMethodBestMatch(RecentsViewClass.getClazz(), "dismissAllTasks", View.class);

		RecentsViewClass
				.afterConstruction()
				.run(param -> recentView = param.thisObject);

		RecentsViewClass
				.after("setVisibility")
				.run(param -> {
					if (clearAllButton == null) return;
					clearAllButton.setVisibility((Integer) param.args[0]);
				});

		OverviewActionsViewClass
				.after("onFinishInflate")
				.run(param -> {
					if (!RecentClearAllReposition) return;

					try {
						FrameLayout parent = (FrameLayout) param.thisObject;

						ViewGroup actionButtonsView = null;
						// Find the action buttons container dynamically (usually a LinearLayout containing Screenshot/Select)
						for (int i = 0; i < parent.getChildCount(); i++) {
							View child = parent.getChildAt(i);
							if (child instanceof android.widget.LinearLayout) {
								actionButtonsView = (ViewGroup) child;
								break;
							}
						}

						if (actionButtonsView == null) {
							// Fallback if not wrapped in LinearLayout
							actionButtonsView = parent;
						}

						int clearAllResId = mContext.getResources().getIdentifier("recents_clear_all", "string", mContext.getPackageName());
						CharSequence clearAllText = clearAllResId != 0 ? mContext.getResources().getString(clearAllResId) : "Clear all";

						int layoutId = mContext.getResources().getIdentifier("clear_all_button", "layout", mContext.getPackageName());
						if (layoutId != 0) {
							clearAllButton = (android.widget.Button) android.view.LayoutInflater.from(mContext).inflate(layoutId, actionButtonsView, false);
							clearAllButton.setText(clearAllText);
						} else {
							clearAllButton = new android.widget.Button(mContext, null, android.R.attr.borderlessButtonStyle);
							clearAllButton.setText(clearAllText);
							clearAllButton.setTextColor(Color.WHITE);
							clearAllButton.setAllCaps(false);

							// Set the background as a semi-transparent black pill
							GradientDrawable background = new GradientDrawable();
							background.setColor(0x80000000); // Semi-transparent black
							background.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, mContext.getResources().getDisplayMetrics()));
							clearAllButton.setBackground(background);

							// Set padding to match other buttons (Screenshot/Select)
							int paddingHorizontal = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, mContext.getResources().getDisplayMetrics());
							int paddingVertical = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics());
							clearAllButton.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

							android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
									ViewGroup.LayoutParams.WRAP_CONTENT,
									ViewGroup.LayoutParams.WRAP_CONTENT
							);

							// Add margin right
							params.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics());
							clearAllButton.setLayoutParams(params);
						}

						clearAllButton.setOnClickListener(v -> {
							if (recentView != null) {
								try {
									dismissAllTasksMethod.invoke(recentView, v);
								} catch (Throwable ignored) {}
							}
						});

						actionButtonsView.addView(clearAllButton, 0);
						clearAllButton.setVisibility(GONE);
					} catch (Throwable ignored) {}
				});
	}
}
