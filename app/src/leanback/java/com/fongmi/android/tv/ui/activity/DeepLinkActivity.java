package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.fongmi.android.tv.db.AppDatabase;

/**
 * External deep link into a vod's detail page. Invoked by the launcher's Continue Watching card with
 * string extra "data" = webhtv://detail?key=siteKey@@@vodId (URL-encoded). Falls back to the app's home.
 */
public class DeepLinkActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Uri uri = Uri.parse(getIntent().getStringExtra("data"));
            String key = uri.getQueryParameter("key");
            String[] parts = key.split(AppDatabase.SYMBOL, -1);
            VideoActivity.start(this, parts[0], parts.length > 1 ? parts[1] : "", null, null, null, null);
        } catch (Exception e) {
            startActivity(new Intent(this, HomeActivity.class));
        }
        finish();
    }
}
