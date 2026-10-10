package sh.siava.pixelxpert.xposed.modpacks.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.LauncherModPack;
import sh.siava.pixelxpert.xposed.utils.KSUConfigReader;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.toolkit.Logger;

@LauncherModPack
public class KillAppMod extends XposedModPack {
    private boolean enableKillApp = true;

    public KillAppMod(Context context) {
        super(context);
    }

    @Override
    public void onPreferenceUpdated(String... Key) {
        enableKillApp = KSUConfigReader.getBoolean("kill_app", true);
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
        enableKillApp = KSUConfigReader.getBoolean("kill_app", true);

        ReflectedClass TaskMenuViewClass = ReflectedClass.of("com.android.quickstep.views.TaskMenuView");

        TaskMenuViewClass.after("populateAndLayoutMenu").run(param -> {
            if (!enableKillApp) return;

            try {
                ViewGroup taskMenuView = (ViewGroup) param.thisObject;

                LinearLayout optionContainer = null;
                for (int i = 0; i < taskMenuView.getChildCount(); i++) {
                    View child = taskMenuView.getChildAt(i);
                    if (child.getClass().getName().contains("LinearLayout") || child instanceof LinearLayout) {
                        optionContainer = (LinearLayout) child;
                        break;
                    }
                }

                if (optionContainer == null) {
                    // Fallback to searching all children of the view hierarchy
                    for (int i = 0; i < taskMenuView.getChildCount(); i++) {
                         if (taskMenuView.getChildAt(i) instanceof ViewGroup) {
                             ViewGroup vg = (ViewGroup)taskMenuView.getChildAt(i);
                             for (int j = 0; j < vg.getChildCount(); j++) {
                                 if (vg.getChildAt(j) instanceof LinearLayout) {
                                     optionContainer = (LinearLayout) vg.getChildAt(j);
                                     break;
                                 }
                             }
                         }
                    }
                }

                if (optionContainer == null) return;

                View firstChild = optionContainer.getChildCount() > 0 ? optionContainer.getChildAt(0) : null;

                LinearLayout newOptionLayout = new LinearLayout(mContext);
                newOptionLayout.setOrientation(LinearLayout.HORIZONTAL);
                newOptionLayout.setClickable(true);
                newOptionLayout.setFocusable(true);
                newOptionLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

                if (firstChild != null) {
                    newOptionLayout.setPadding(firstChild.getPaddingLeft(), firstChild.getPaddingTop(), firstChild.getPaddingRight(), firstChild.getPaddingBottom());
                    newOptionLayout.setBackground(firstChild.getBackground());
                    newOptionLayout.setLayoutParams(firstChild.getLayoutParams());
                } else {
                    newOptionLayout.setPadding(32, 24, 32, 24);
                }

                ImageView iconView = new ImageView(mContext);
                int closeIconId = mContext.getResources().getIdentifier("ic_close", "drawable", mContext.getPackageName());
                if (closeIconId != 0) {
                    iconView.setImageResource(closeIconId);
                } else {
                    // Fallback icon
                    iconView.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                }

                // Color filter for icon
                iconView.setColorFilter(Color.WHITE);

                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                        (int) (24 * mContext.getResources().getDisplayMetrics().density),
                        (int) (24 * mContext.getResources().getDisplayMetrics().density)
                );
                iconParams.rightMargin = (int) (16 * mContext.getResources().getDisplayMetrics().density);
                iconView.setLayoutParams(iconParams);

                TextView textView = new TextView(mContext);
                textView.setText("Arresto forzato"); // Localization later
                textView.setTextSize(16);
                textView.setTextColor(Color.WHITE);

                newOptionLayout.addView(iconView);
                newOptionLayout.addView(textView);

                // Fetch package name from TaskView
                Object taskView = de.robv.android.xposed.XposedHelpers.callMethod(param.thisObject, "getTaskView");
                if (taskView == null) return;

                Object task = de.robv.android.xposed.XposedHelpers.callMethod(taskView, "getTask");
                if (task == null) return;

                Object taskKey = task.getClass().getField("key").get(task);
                Object componentName = de.robv.android.xposed.XposedHelpers.callMethod(taskKey, "getComponent");
                String packageName = (String) de.robv.android.xposed.XposedHelpers.callMethod(componentName, "getPackageName");

                newOptionLayout.setOnClickListener(v -> {
                    killApp(packageName);
                    try {
                        de.robv.android.xposed.XposedHelpers.callMethod(param.thisObject, "close", true);
                    } catch (Throwable ignored) {}
                });

                if (optionContainer.getChildCount() > 1) {
                    optionContainer.addView(newOptionLayout, 1);
                } else {
                    optionContainer.addView(newOptionLayout);
                }

            } catch (Throwable t) {
                Logger.log("KillAppMod error: " + t.getMessage());
            }
        });
    }

    private void killApp(String packageName) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("am force-stop " + packageName + "\n");
            os.writeBytes("exit\n");
            os.flush();
            Toast.makeText(mContext, "App Kill: " + packageName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(mContext, "Failed to kill app", Toast.LENGTH_SHORT).show();
        }
    }
}
