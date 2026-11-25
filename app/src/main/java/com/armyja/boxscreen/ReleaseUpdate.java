package com.armyja.boxscreen;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class ReleaseUpdate {
    static final String RELEASES = "https://github.com/armyja/boxscreen/releases";
    private static final String API = "https://api.github.com/repos/armyja/boxscreen/releases/latest";

    final String version;
    final String notes;
    final String downloadUrl;
    final String pageUrl;

    private ReleaseUpdate(String version, String notes, String downloadUrl, String pageUrl) {
        this.version = version;
        this.notes = notes;
        this.downloadUrl = downloadUrl;
        this.pageUrl = pageUrl;
    }

    static ReleaseUpdate fetch() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(API).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "BoxScreen");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Release API unavailable");
            StringBuilder json = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8)) {
                char[] buffer = new char[4096];
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    if (json.length() + count > 512 * 1024) throw new IOException("Release response too large");
                    json.append(buffer, 0, count);
                }
            }

            JSONObject release = new JSONObject(json.toString());
            String tag = release.getString("tag_name");
            String version = tag.startsWith("v") ? tag.substring(1) : tag;
            if (!isVersion(version) || release.optBoolean("draft") || release.optBoolean("prerelease")) {
                throw new IOException("Unrecognized release");
            }

            String download = null;
            int matchCount = 0;
            JSONArray assets = release.getJSONArray("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.optString("name");
                String prefix = "BoxScreen-v" + version;
                String url = asset.optString("browser_download_url");
                if ((name.equals(prefix + ".apk") || (name.startsWith(prefix + "-") && name.endsWith(".apk")))
                        && "uploaded".equals(asset.optString("state"))
                        && url.equals(RELEASES + "/download/" + tag + "/" + Uri.encode(name))) {
                    download = url;
                    matchCount++;
                }
            }
            return new ReleaseUpdate(version, release.optString("body", ""),
                    matchCount == 1 ? download : null, RELEASES + "/tag/" + tag);
        } finally {
            connection.disconnect();
        }
    }

    static boolean isVersion(String version) {
        return version != null && version.matches("[0-9]{1,9}\\.[0-9]{1,9}\\.[0-9]{1,9}");
    }

    boolean isNewerThan(String current) {
        String[] latestParts = version.split("\\.");
        String[] currentParts = current.split("\\.");
        for (int i = 0; i < latestParts.length; i++) {
            int comparison = Integer.compare(Integer.parseInt(latestParts[i]), Integer.parseInt(currentParts[i]));
            if (comparison != 0) return comparison > 0;
        }
        return false;
    }
}
