package sh.siava.pixelxpert.xposed.modpacks.launcher;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.LauncherModPack;
import sh.siava.pixelxpert.utils.ExtendedSharedPreferences;
import sh.siava.pixelxpert.xposed.utils.KSUConfigReader;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.toolkit.Logger;

@LauncherModPack
public class HideAppMod extends XposedModPack {
    private boolean enableHideApp = true;
    private android.content.SharedPreferences prefs;

    public HideAppMod(Context context) {
        super(context);
        prefs = mContext.getSharedPreferences("hidden_apps_prefs", Context.MODE_PRIVATE);
    }

    @Override
    public void onPreferenceUpdated(String... Key) {
        enableHideApp = KSUConfigReader.getBoolean("hide_app", true);
    }

    private List<String> getHiddenApps() {
        String hiddenStr = prefs.getString("hidden_apps", "");
        if (hiddenStr == null || hiddenStr.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(hiddenStr.split(",")));
    }

    private void addHiddenApp(String pkg) {
        List<String> hidden = getHiddenApps();
        if (!hidden.contains(pkg)) {
            hidden.add(pkg);
            prefs.edit().putString("hidden_apps", String.join(",", hidden)).apply();
        }
    }

    private void removeHiddenApp(String pkg) {
        List<String> hidden = getHiddenApps();
        if (hidden.contains(pkg)) {
            hidden.remove(pkg);
            prefs.edit().putString("hidden_apps", String.join(",", hidden)).apply();
        }
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
        enableHideApp = KSUConfigReader.getBoolean("hide_app", true);

        ReflectedClass TaskViewClass = ReflectedClass.of("com.android.quickstep.views.TaskView");
        ReflectedClass RecentsViewClass = ReflectedClass.of("com.android.quickstep.views.RecentsView");
        ReflectedClass OverviewActionsViewClass = ReflectedClass.of("com.android.quickstep.views.OverviewActionsView");

        // 1. Hide Button on each TaskView
        TaskViewClass.after("onFinishInflate").run(param -> {
            if (!enableHideApp) return;
            try {
                FrameLayout taskView = (FrameLayout) param.thisObject;

                ImageView hideButton = new ImageView(mContext);
                int hideIconId = mContext.getResources().getIdentifier("ic_collapse", "drawable", mContext.getPackageName());
                if (hideIconId != 0) {
                    hideButton.setImageResource(hideIconId);
                } else {
                    hideButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Fallback
                }

                hideButton.setColorFilter(Color.WHITE);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0x80000000);
                bg.setShape(GradientDrawable.OVAL);
                hideButton.setBackground(bg);

                int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics());
                hideButton.setPadding(padding, padding, padding, padding);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, mContext.getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, mContext.getResources().getDisplayMetrics())
                );
                lp.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
                lp.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, mContext.getResources().getDisplayMetrics());
                hideButton.setLayoutParams(lp);

                hideButton.setOnClickListener(v -> {
                    try {
                        Object task = callMethod(taskView, "getTask");
                        if (task != null) {
                            Object taskKey = task.getClass().getField("key").get(task);
                            Object componentName = callMethod(taskKey, "getComponent");
                            String packageName = (String) callMethod(componentName, "getPackageName");
                            addHiddenApp(packageName);

                            // Remove task from RecentsView
                            Object recentsView = callMethod(taskView, "getRecentsView");
                            if (recentsView != null) {
                                callMethod(recentsView, "removeView", taskView);
                            }
                        }
                    } catch (Throwable t) {
                        Logger.log("HideAppMod error hiding app: " + t.getMessage());
                    }
                });

                taskView.addView(hideButton);
            } catch (Throwable t) {
                Logger.log("HideAppMod error in TaskView: " + t.getMessage());
            }
        });

        // 2. Hide tasks in RecentsView that are in the hidden list
        // Depending on launcher version, we hook updateTaskSize or bind
        TaskViewClass.after("bind").run(param -> {
            if (!enableHideApp) return;
            try {
                Object task = param.args[0];
                if (task != null) {
                    Object taskKey = task.getClass().getField("key").get(task);
                    Object componentName = callMethod(taskKey, "getComponent");
                    String packageName = (String) callMethod(componentName, "getPackageName");

                    FrameLayout taskView = (FrameLayout) param.thisObject;
                    if (getHiddenApps().contains(packageName)) {
                        taskView.setVisibility(GONE);
                        // Also try to remove its layout params weight/size if needed to avoid gap
                        ViewGroup.LayoutParams lp = taskView.getLayoutParams();
                        lp.width = 0;
                        lp.height = 0;
                        taskView.setLayoutParams(lp);
                    } else {
                        taskView.setVisibility(VISIBLE);
                    }
                }
            } catch (Throwable t) {
                // Ignore
            }
        });

        // 3. Hidden Apps Manager Button in OverviewActionsView
        OverviewActionsViewClass.after("onFinishInflate").run(param -> {
            if (!enableHideApp) return;
            try {
                FrameLayout actionsView = (FrameLayout) param.thisObject;

                Button manageHiddenBtn = new Button(mContext, null, android.R.attr.borderlessButtonStyle);
                manageHiddenBtn.setText("App Nascoste");
                manageHiddenBtn.setTextColor(Color.WHITE);
                manageHiddenBtn.setAllCaps(false);

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0x80000000);
                bg.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, mContext.getResources().getDisplayMetrics()));
                manageHiddenBtn.setBackground(bg);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
                lp.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, mContext.getResources().getDisplayMetrics());
                lp.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, mContext.getResources().getDisplayMetrics());
                manageHiddenBtn.setLayoutParams(lp);

                manageHiddenBtn.setOnClickListener(v -> {
                    showHiddenAppsDialog(actionsView.getContext());
                });

                actionsView.addView(manageHiddenBtn);
            } catch (Throwable t) {
                Logger.log("HideAppMod error in OverviewActionsView: " + t.getMessage());
            }
        });
    }

    private void showHiddenAppsDialog(Context context) {
        List<String> hiddenApps = getHiddenApps();
        if (hiddenApps.isEmpty()) {
            new AlertDialog.Builder(context)
                .setTitle("App Nascoste")
                .setMessage("Nessuna app nascosta.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, hiddenApps);

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle("App Nascoste (Tocca per ripristinare)")
            .setAdapter(adapter, null)
            .setPositiveButton("Chiudi", null)
            .create();

        dialog.getListView().setOnItemClickListener((parent, view, position, id) -> {
            String pkg = hiddenApps.get(position);
            removeHiddenApp(pkg);
            hiddenApps.remove(position);
            adapter.notifyDataSetChanged();
            if (hiddenApps.isEmpty()) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
