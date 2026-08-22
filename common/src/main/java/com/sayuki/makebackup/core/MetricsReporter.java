/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core;

import com.sayuki.makebackup.MakeBackup;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// メトリクスレポータークラス - bStatsにデータを送信する
public class MetricsReporter {

    private static final String SUBMIT_URL = "https://bstats.org/submitData/%s";

    private final HttpClient client = HttpClient.newBuilder().build();

    private List<Metrics.ChartSpec> chartSpecs = List.of();

    private final String serverUuid = UUID.randomUUID().toString();

    // bStatsを初期化する
    public void initBstats(List<Metrics.ChartSpec> chartSpecs) {
        this.chartSpecs = chartSpecs;
        submit();
    }

    // 破棄する
    public void destroy() {
    }

    // データを送信する - bStatsへ統計を送る
    private void submit() {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("pluginName", "MakeBackup");
            data.put("id", String.valueOf(17735));
            data.put("pluginVersion", com.sayuki.makebackup.core.helper.Helper.getProperty("version"));
            data.put("osName", System.getProperty("os.name"));
            data.put("osVersion", System.getProperty("os.version"));
            data.put("osArch", System.getProperty("os.arch"));
            data.put("javaVersion", System.getProperty("java.version"));
            data.put("coreCount", Runtime.getRuntime().availableProcessors());
            data.put("serverId", serverUuid);
            data.put("customCharts", chartSpecs.stream()
                    .map(spec -> Map.<String, Object>of("chartId", spec.id(), "data", Map.of("value", spec.value().get())))
                    .toList());

            StringBuilder json = new StringBuilder("{");
            json.append(toJson("pluginName", data.get("pluginName"))).append(",");
            json.append(toJson("id", data.get("id"))).append(",");
            json.append(toJson("pluginVersion", data.get("pluginVersion"))).append(",");
            json.append(toJson("osName", data.get("osName"))).append(",");
            json.append(toJson("osVersion", data.get("osVersion"))).append(",");
            json.append(toJson("osArch", data.get("osArch"))).append(",");
            json.append(toJson("javaVersion", data.get("javaVersion"))).append(",");
            json.append(toJson("coreCount", data.get("coreCount"))).append(",");
            json.append(toJson("serverId", data.get("serverId"))).append(",");
            json.append("\"customCharts\":[");
            for (int i = 0; i < chartSpecs.size(); i++) {
                Metrics.ChartSpec spec = chartSpecs.get(i);
                json.append("{\"chartId\":").append(toJson("", spec.id()).substring(1)).append(",")
                        .append("\"data\":{\"value\":").append(toJson("", spec.value().get()).substring(1)).append("}}");
                if (i < chartSpecs.size() - 1) json.append(",");
            }
            json.append("]}");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SUBMIT_URL.formatted(17735)))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "MC-Server/1.20.1")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().devWarn("Failed to submit bStats data: %s".formatted(e.getMessage()));
        }
    }

    // JSONに変換する
    private String toJson(String key, Object value) {

        String escaped = String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"");
        return key.isEmpty() ? "\"%s\"".formatted(escaped) : "\"%s\":\"%s\"".formatted(key, escaped);
    }
}
