package sh.siava.pixelxpert.xposed.modpacks.launcher;

import static android.view.View.GONE;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.xposed.XPLauncher;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.LauncherModPack;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;

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
		RecentClearAllReposition = true;
	}

	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		ReflectedClass OverviewActionsViewClass = ReflectedClass.of("com.android.quickstep.views.OverviewActionsView");
		ReflectedClass RecentsViewClass = ReflectedClass.of("com.android.quickstep.views.RecentsView");
		Method dismissAllTasksMethod = findMethodBestMatch(RecentsViewClass.getClazz(), "dismissAllTasks", View.class);

		RecentsViewClass
				.afterConstruction()
				.run(param -> recentView = param.thisObject);

		RecentsViewClass
				.after("setColorTint")
				.run(param -> {
					if (!RecentClearAllReposition) return;
					if (clearAllButton != null) {
						clearAllButton.setTextColor(getThemedColor(mContext));
						clearAllButton.setCompoundDrawableTintList(getThemedColor(mContext));
					}
				});

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
						int actionButtonsId = mContext.getResources().getIdentifier("action_buttons", "id", mContext.getPackageName());
						if (actionButtonsId == 0) return;

						ViewGroup actionButtonsView = parent.findViewById(actionButtonsId);
						if (actionButtonsView == null) return;

						int clearAllResId = mContext.getResources().getIdentifier("recents_clear_all", "string", mContext.getPackageName());
						CharSequence clearAllText = clearAllResId != 0 ? mContext.getResources().getString(clearAllResId) : "Clear all";

						int layoutId = mContext.getResources().getIdentifier("clear_all_button", "layout", mContext.getPackageName());
						if (layoutId != 0) {
							clearAllButton = (android.widget.Button) android.view.LayoutInflater.from(mContext).inflate(layoutId, actionButtonsView, false);
							clearAllButton.setText(clearAllText);
						} else {
							clearAllButton = new android.widget.Button(mContext, null, android.R.attr.borderlessButtonStyle);
							clearAllButton.setText(clearAllText);
							clearAllButton.setTextColor(getThemedColor(mContext));
							clearAllButton.setAllCaps(false);

							android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
									ViewGroup.LayoutParams.WRAP_CONTENT,
									ViewGroup.LayoutParams.WRAP_CONTENT
							);
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

	public static ColorStateList getThemedColor(Context context) {
		return getSystemAttrColor(context, android.R.attr.textColorPrimary);
	}

	public static ColorStateList getSystemAttrColor(Context context, int attr) {
		try(TypedArray a = context.obtainStyledAttributes(new int[]{attr}))
		{
			return a.getColorStateList(a.getIndex(0));
		}
	}
}
