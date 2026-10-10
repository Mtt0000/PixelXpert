package sh.siava.pixelxpert.ui.activities;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;

import sh.siava.pixelxpert.PixelXpert;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // Check if root is available and inject su execution wrapper if needed,
        // though KernelSU usually handles this automatically in its webroot environment
        // if opened through the KernelSU app. We will just load the file.
        // It requires the module to be installed.
        File webrootFile = new File("/data/adb/modules/PixelXpert-Minimal/webroot/index.html");

        if (PixelXpert.get().hasRootAccess()) {
             // For testing, we can load it directly via su if it's not readable by the app,
             // or we can assume it's readable if permissions are set correctly.
             // Usually, KSU modules have webroot readable by system/shell.
             webView.loadUrl("file:///data/adb/modules/PixelXpert-Minimal/webroot/index.html");
        } else {
             Toast.makeText(this, "Root access not available. Settings might not apply.", Toast.LENGTH_LONG).show();
             webView.loadUrl("file:///data/adb/modules/PixelXpert-Minimal/webroot/index.html");
        }
    }
}
