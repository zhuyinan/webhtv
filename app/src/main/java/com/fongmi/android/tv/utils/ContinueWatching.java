package com.fongmi.android.tv.utils;

import android.text.TextUtils;
import android.util.Log;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.db.AppDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Exports the user's most recent watch history as a small JSON the launcher reads to render its
 * Continue Watching shelf. One file, one thread, all failures swallowed: never disturb playback.
 */
public class ContinueWatching {

    private static final String TAG = "ContinueWatching";
    private static final String LAUNCHER_PACKAGE = "com.gathin.tvhome";
    private static final int MAX_ITEMS = 8;
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor();

    public static void export() {
        POOL.execute(() -> {
            try {
                write();
            } catch (Exception e) {
                Log.e(TAG, "export failed", e);
            }
        });
    }

    private static void write() throws Exception {
        List<History> list = AppDatabase.get().getHistoryDao()
                .find(VodConfig.getCid(), System.currentTimeMillis() - Constant.HISTORY_TIME);
        JSONArray arr = new JSONArray();
        Set<String> names = new HashSet<>();   // one card per show, whatever the source
        for (History h : list) {
            if (arr.length() >= MAX_ITEMS) break;
            if (h.getVodName() == null || h.getVodName().trim().isEmpty()) continue;   // auto-source noise
            try {   // same-named music albums are not the show: keep them off the shelf
                String sn = VodConfig.get().getSite(h.getSiteKey()).getName();
                if (sn != null && (sn.contains("\u97f3\u4e50") || sn.toLowerCase().contains("music"))) continue;
            } catch (Exception ignored) {
            }
            if (!names.add(h.getVodName().trim())) continue;   // newest entry of the same show wins (list is newest-first)
            JSONObject o = new JSONObject();
            o.put("title", title(h));
            o.put("name", h.getVodName());
            if (!TextUtils.isEmpty(h.getVodPic())) o.put("poster", h.getVodPic());
            long dur = h.getDuration();
            long pos = h.getPosition();
            o.put("progress", dur > 0 && pos > 0 ? Math.max(1, (int) (pos * 100 / dur)) : -1);
            o.put("key", h.getKey());
            o.put("time", h.getCreateTime());
            arr.put(o);
        }
        JSONObject root = new JSONObject();
        root.put("items", arr);
        File dir = new File("/sdcard/Android/data/" + LAUNCHER_PACKAGE + "/files");
        if (!dir.isDirectory() && !dir.mkdirs()) return;
        File tmp = new File(dir, "webhtv-history.json.tmp");
        File out = new File(dir, "webhtv-history.json");
        try (FileWriter w = new FileWriter(tmp)) {
            w.write(root.toString());
        }
        if (!tmp.renameTo(out)) tmp.delete();
    }

    private static String title(History h) {
        String name = h.getVodName();
        String remark = h.getVodRemarks();
        if (TextUtils.isEmpty(remark) || remark.equals(name)) return name;
        if (isNumber(remark)) return name + "\u00b7\u7b2c" + remark + "\u96c6";
        if (remark.length() > 16 || remark.contains("[")) return name;
        return name + "\u00b7" + remark;
    }

    private static boolean isNumber(String text) {
        for (char c : text.toCharArray()) if (!Character.isDigit(c)) return false;
        return text.length() > 0;
    }
}
