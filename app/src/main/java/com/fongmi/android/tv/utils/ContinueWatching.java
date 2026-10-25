package com.fongmi.android.tv.utils;

import android.os.Environment;
import android.util.Log;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.Constant;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Exports the most recent watch history as JSON for the CleanYourTV launcher's global Continue Watching row.
 * Written to the launcher's external files dir; any failure is logged and swallowed: history saving must never break.
 */
public final class ContinueWatching {

    private static final String TAG = "ContinueWatching";
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor();
    private static final String LAUNCHER_PACKAGE = "com.gathin.tvhome";
    private static final int MAX_ITEMS = 8;

    private ContinueWatching() {
    }

    public static void export() {
        POOL.execute(ContinueWatching::write);
    }

    private static void write() {
        try {
            List<History> list = AppDatabase.get().getHistoryDao()
                    .find(VodConfig.getCid(), System.currentTimeMillis() - Constant.HISTORY_TIME);
            JSONArray arr = new JSONArray();
            for (History h : list) {
                if (arr.length() >= MAX_ITEMS) break;
                if (h.getVodName() == null || h.getVodName().trim().isEmpty()) continue;   // auto-source noise
                JSONObject o = new JSONObject();
                o.put("title", title(h));
                o.put("name", h.getVodName() == null ? "" : h.getVodName());
                if (notEmpty(h.getVodPic())) o.put("poster", h.getVodPic());
                o.put("progress", progress(h));
                o.put("key", h.getKey());
                o.put("time", h.getUpdateTime() > 0 ? h.getUpdateTime() : h.getCreateTime());
                arr.put(o);
            }
            JSONObject root = new JSONObject();
            root.put("items", arr);
            File dir = new File(Environment.getExternalStorageDirectory(), "Android/data/" + LAUNCHER_PACKAGE + "/files");
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            File tmp = new File(dir, "webhtv-history.json.tmp");
            try (FileWriter w = new FileWriter(tmp)) {
                w.write(root.toString());
            }
            File dst = new File(dir, "webhtv-history.json");
            if (!tmp.renameTo(dst)) tmp.delete();
            Log.d(TAG, "exported " + arr.length() + " items");
        } catch (Exception e) {
            Log.w(TAG, "export failed: " + e);
        }
    }

    private static String title(History h) {
        String name = h.getVodName() == null ? "" : h.getVodName();
        String remark = h.getVodRemarks() == null ? "" : h.getVodRemarks().trim();
        if (remark.isEmpty() || remark.equals(name)) return name;
        if (remark.matches("\\d+")) return name + "·第" + remark + "集";   // bare episode number
        if (remark.length() > 16 || remark.contains("[")) return name;      // file-name remarks: name only
        return name + "·" + remark;
    }

    private static int progress(History h) {
        if (h.getDuration() <= 0 || h.getPosition() <= 0) return -1;
        return (int) Math.max(1, Math.min(100, h.getPosition() * 100 / h.getDuration()));
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
