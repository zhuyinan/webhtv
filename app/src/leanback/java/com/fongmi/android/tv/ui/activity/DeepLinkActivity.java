package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.impl.Callback;

/**
 * External deep link into a vod's detail page. Invoked by the launcher's Continue Watching card with
 * string extra "data" = webhtv://detail?key=siteKey@@@vodId (URL-encoded). Loads the config first when the
 * app was started cold (HomeActivity normally does that), then opens the detail page; falls back to home.
 */
public class DeepLinkActivity extends Activity {

    private static final String TAG = "DeepLink";
    private static final long CONFIG_TIMEOUT = 15000;

    private final Handler timeout = new Handler(Looper.getMainLooper());
    private String site, id;
    private boolean opened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Uri uri = Uri.parse(getIntent().getStringExtra("data"));
            String[] parts = uri.getQueryParameter("key").split(AppDatabase.SYMBOL, -1);
            site = parts[0];
            id = parts.length > 1 ? parts[1] : "";
        } catch (Exception e) {
            Log.e(TAG, "bad intent: " + e);
            home();
            return;
        }
        Log.e(TAG, "site=" + site + " id=" + id + " sites=" + VodConfig.get().getSites().size());
        if (VodConfig.get().getSites().isEmpty()) {
            timeout.postDelayed(this::home, CONFIG_TIMEOUT);
            VodConfig.get().init().load(new Callback() {
                @Override
                public void success() {
                    open();
                }

                @Override
                public void error() {
                    home();
                }

                @Override
                public void error(String msg) {
                    home();
                }
            });
        } else {
            open();
        }
    }

    private void open() {
        if (opened) return;
        opened = true;
        timeout.removeCallbacksAndMessages(null);
        Log.e(TAG, "opening detail, sites=" + VodConfig.get().getSites().size()
                + " found=" + VodConfig.get().getSite(site).getKey());
        VideoActivity.start(this, site, id, null, null, null, null);
        finish();
    }

    private void home() {
        if (opened) return;
        opened = true;
        timeout.removeCallbacksAndMessages(null);
        Log.e(TAG, "config failed, falling back to home");
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
