package sh.siava.pixelxpert.xposed.modpacks.launcher;

import static de.robv.android.xposed.XposedHelpers.callMethod;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataOutputStream;

import io.github.libxposed.api.XposedModuleInterface;
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

        ReflectedClass TaskViewClass = ReflectedClass.of("com.android.quickstep.views.TaskView");

        TaskViewClass.after("onFinishInflate").run(param -> {
            if (!enableKillApp) return;

            try {
                FrameLayout taskView = (FrameLayout) param.thisObject;

                ImageView killButton = new ImageView(mContext);
                int closeIconId = mContext.getResources().getIdentifier("ic_close", "drawable", mContext.getPackageName());
                if (closeIconId != 0) {
                    killButton.setImageResource(closeIconId);
                } else {
                    killButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Fallback
                }

                killButton.setColorFilter(Color.WHITE);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0x80000000);
                bg.setShape(GradientDrawable.OVAL);
                killButton.setBackground(bg);

                int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics());
                killButton.setPadding(padding, padding, padding, padding);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, mContext.getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, mContext.getResources().getDisplayMetrics())
                );
                // Position top right
                lp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
                lp.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, mContext.getResources().getDisplayMetrics());
                lp.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, mContext.getResources().getDisplayMetrics());
                killButton.setLayoutParams(lp);

                killButton.setOnClickListener(v -> {
                    try {
                        Object task = callMethod(taskView, "getTask");
                        if (task != null) {
                            Object taskKey = task.getClass().getField("key").get(task);
                            Object componentName = callMethod(taskKey, "getComponent");
                            String packageName = (String) callMethod(componentName, "getPackageName");

                            killApp(packageName);

                            // Remove task from RecentsView visually
                            Object recentsView = callMethod(taskView, "getRecentsView");
                            if (recentsView != null) {
                                callMethod(recentsView, "removeView", taskView);
                            }
                        }
                    } catch (Throwable t) {
                        Logger.log("KillAppMod error killing app: " + t.getMessage());
                    }
                });

                taskView.addView(killButton);
            } catch (Throwable t) {
                Logger.log("KillAppMod error in TaskView: " + t.getMessage());
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
