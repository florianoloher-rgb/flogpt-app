package com.fproduction.flogpt;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "FloGPT";
    private boolean offlineFallbackLoaded = false;

    // Capacitor Preferences speichert in SharedPreferences "CapacitorStorage"
    // mit Key-Prefix "_cap_"
    private static final String PREF_NAME = "CapacitorStorage";
    private static final String[] POSSIBLE_KEYS = {
        "_cap_flogpt_offline_html",
        "flogpt_offline_html",
        "_cap_flogpt_offline_meta"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = getBridge().getWebView();
        if (webView == null) {
            Log.e(TAG, "WebView nicht verfügbar");
            return;
        }

        // Debug: alle vorhandenen Keys loggen (nur zur Info)
        logAllPreferences();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);

                if (!request.isForMainFrame()) return;
                if (offlineFallbackLoaded) return;

                String desc = error.getDescription() != null ? error.getDescription().toString() : "?";
                String url = request.getUrl().toString();
                Log.w(TAG, "WebView Fehler: " + desc + " bei " + url);

                offlineFallbackLoaded = true;
                loadOfflineFallback(view);
            }
        });
    }

    private void logAllPreferences() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            Log.i(TAG, "=== Verfügbare Preferences in " + PREF_NAME + " ===");
            for (String key : prefs.getAll().keySet()) {
                Log.i(TAG, "  • " + key);
            }
            Log.i(TAG, "=== Ende ===");
        } catch (Exception e) {
            Log.e(TAG, "Log-Fehler: " + e.getMessage());
        }
    }

    private String findOfflineHtml() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

            // 1. Versuche bekannte Keys
            for (String key : POSSIBLE_KEYS) {
                String val = prefs.getString(key, null);
                if (val != null && !val.isEmpty()) {
                    Log.i(TAG, "Gefunden via Key: " + key + " (" + val.length() + " Zeichen)");
                    return val;
                }
            }

            // 2. Suche dynamisch nach jedem Key der "offline_html" enthält
            for (String key : prefs.getAll().keySet()) {
                if (key.contains("offline_html")) {
                    Object val = prefs.getAll().get(key);
                    if (val instanceof String) {
                        String s = (String) val;
                        if (!s.isEmpty()) {
                            Log.i(TAG, "Gefunden via Suche: " + key + " (" + s.length() + " Zeichen)");
                            return s;
                        }
                    }
                }
            }

            Log.w(TAG, "Kein Offline-HTML gefunden");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Suchen: " + e.getMessage());
            return null;
        }
    }

    private void loadOfflineFallback(final WebView webView) {
        try {
            String html = findOfflineHtml();

            final String content;
            if (html == null || html.isEmpty()) {
                Log.w(TAG, "Keine Offline-Version gespeichert");
                content = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                    "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                    "<style>body{background:#0a0a0a;color:#fff;text-align:center;padding:60px 20px;" +
                    "font-family:-apple-system,sans-serif;margin:0;}h1{color:#00ff41;font-size:1.5rem;}" +
                    "p{color:#888;max-width:340px;margin:20px auto;line-height:1.6;}</style></head>" +
                    "<body><div style='font-size:4rem;'>📴</div>" +
                    "<h1>Keine Offline-Version</h1>" +
                    "<p>Bitte gehe online und tippe in den Einstellungen auf " +
                    "<strong style='color:#00ff41;'>FloGPT Offline → Neues Update installieren</strong>.</p>" +
                    "</body></html>";
            } else {
                content = html;
            }

            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.loadDataWithBaseURL("https://localhost/", content, "text/html", "UTF-8", null);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Laden: " + e.getMessage(), e);
        }
    }
}
