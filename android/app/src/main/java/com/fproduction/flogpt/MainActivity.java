package com.fproduction.flogpt;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "FloGPT";
    private static final String LOCAL_URL = "file:///android_asset/public/index.html";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Internet-Verbindung prüfen (direkt vom Android-System)
        boolean online = hasInternet();
        Log.i(TAG, "App-Start – online: " + online);

        if (!online) {
            // OFFLINE: Lokale Bootstrap laden statt Cloudflare
            Log.i(TAG, "Offline erkannt – lade lokale Bootstrap");
            loadLocalBootstrap();
        }
        // ONLINE: Capacitor lädt Cloudflare (Standard aus server.url)
    }

    private void loadLocalBootstrap() {
        try {
            final WebView webView = getBridge().getWebView();
            if (webView == null) {
                Log.w(TAG, "WebView nicht verfügbar");
                return;
            }
            // Auf UI-Thread ausführen
            webView.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Lade: " + LOCAL_URL);
                    webView.loadUrl(LOCAL_URL);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Laden der lokalen Bootstrap: " + e.getMessage());
        }
    }

    private boolean hasInternet() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            Network network = cm.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps == null) return false;

            // NET_CAPABILITY_INTERNET = hat Netzwerk
            // NET_CAPABILITY_VALIDATED = hat echtes Internet (nicht nur lokales WLAN)
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (Exception e) {
            Log.w(TAG, "Connectivity-Check Fehler: " + e.getMessage());
            return false;
        }
    }
}
