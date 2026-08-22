/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// コンフィグセクションクラス - JSONコンフィグの読み書きを扱う
public class ConfigSection {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final String COMMENT_KEY = "_comment";

    private final Map<String, Object> data;
    private final String name;

    // コンストラクタ - データと名前で初期化する
    public ConfigSection(Map<String, Object> data, String name) {
        this.data = data != null ? data : new LinkedHashMap<>();
        this.name = name;
    }

    // ルートを作成する
    public static ConfigSection root() {
        return new ConfigSection(new LinkedHashMap<>(), null);
    }

    // ファイルから読み込む
    public static ConfigSection loadFromFile(File file) {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return loadFromReader(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file %s".formatted(file.getPath()), e);
        }
    }

    // ストリームから読み込む
    public static ConfigSection loadFromStream(InputStream inputStream) {
        if (inputStream == null) {
            return root();
        }
        return loadFromReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    // リーダーから読み込む
    private static ConfigSection loadFromReader(Reader reader) {
        try {
            Map<String, Object> map = GSON.fromJson(reader, new TypeToken<LinkedHashMap<String, Object>>() {}.getType());
            return map != null ? new ConfigSection(map, null) : root();
        } catch (Exception e) {
            return root();
        }
    }

    // 保存する - ファイルに書き出す
    public void save(File file) {
        try {
            Files.writeString(file.toPath(), GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config file %s".formatted(file.getPath()), e);
        }
    }

    // 名前を取得する
    public String getName() {
        return name;
    }

    // 現在のパスを取得する
    public String getCurrentPath() {
        return name != null ? name : "";
    }

    // コンフィグセクションを取得する - 旧互換
    public ConfigSection getConfigSection(String path) {
        return getConfigurationSection(path);
    }

    // コンフィグセクションを取得する
    public ConfigSection getConfigurationSection(String path) {
        Object value = get(path);
        if (value instanceof ConfigSection section) {
            return section;
        }
        if (value instanceof Map<?, ?> map) {
            return new ConfigSection((Map<String, Object>) map, lastKey(path));
        }
        if (value == null) {
            return new ConfigSection(new LinkedHashMap<>(), lastKey(path));
        }
        return null;
    }

    // キー一覧を取得する
    public Set<String> getKeys(boolean deep) {
        Set<String> keys = new LinkedHashSet<>();
        collectKeys(data, "", deep, keys);
        return keys;
    }

    // キーを収集する - 再帰的に集める
    private void collectKeys(Map<String, Object> map, String prefix, boolean deep, Set<String> keys) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (COMMENT_KEY.equals(entry.getKey())) {
                continue;
            }

            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            keys.add(key);
            if (deep && entry.getValue() instanceof Map<?, ?> nested) {
                collectKeys((Map<String, Object>) nested, key, true, keys);
            }
        }
    }

    // 含むか判定する
    public boolean contains(String path) {
        return get(path) != null;
    }

    // 設定済みか判定する
    public boolean isSet(String path) {
        return get(path) != null;
    }

    // 取得する - パスで値を取る
    public Object get(String path) {
        return resolve(path);
    }

    // 解決する - パスを辿って値を取得する
    private Object resolve(String path) {

        String[] keys = path.split("\\.");
        Map<String, Object> current = data;
        Object value = null;
        for (int i = 0; i < keys.length; i++) {
            value = current.get(keys[i]);
            if (value instanceof Map<?, ?> nested) {
                current = (Map<String, Object>) nested;
            } else if (i < keys.length - 1) {
                return null;
            }
        }
        return value;
    }

    // 設定する - パスに値をセットする
    public void set(String path, Object value) {

        String[] keys = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < keys.length - 1; i++) {
            Object next = current.get(keys[i]);
            if (next instanceof Map<?, ?> nested) {
                current = (Map<String, Object>) nested;
            } else {
                Map<String, Object> newMap = new LinkedHashMap<>();
                current.put(keys[i], newMap);
                current = newMap;
            }
        }
        if (value == null) {
            current.remove(keys[keys.length - 1]);
        } else if (value instanceof ConfigSection section) {
            current.put(keys[keys.length - 1], section.data);
        } else {
            current.put(keys[keys.length - 1], value);
        }
    }

    // 文字列を取得する
    public String getString(String path) {
        Object value = get(path);
        return value == null ? null : value.toString();
    }

    // 文字列を取得する - デフォルト値付き
    public String getString(String path, String defaultValue) {
        Object value = get(path);
        return value == null ? defaultValue : value.toString();
    }

    // 整数を取得する
    public int getInt(String path) {
        return getInt(path, 0);
    }

    // 整数を取得する - デフォルト値付き
    public int getInt(String path, int defaultValue) {
        Object value = get(path);
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    // ロングを取得する
    public long getLong(String path) {
        return getLong(path, 0L);
    }

    // ロングを取得する - デフォルト値付き
    public long getLong(String path, long defaultValue) {
        Object value = get(path);
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    // ダブルを取得する
    public double getDouble(String path) {
        return getDouble(path, 0);
    }

    // ダブルを取得する - デフォルト値付き
    public double getDouble(String path, double defaultValue) {
        Object value = get(path);
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String string) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    // 真偽値を取得する
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    // 真偽値を取得する - デフォルト値付き
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = get(path);
        if (value instanceof Boolean bool) return bool;
        if (value instanceof String string) return Boolean.parseBoolean(string);
        return defaultValue;
    }

    // 文字列リストを取得する
    public List<String> getStringList(String path) {
        Object value = get(path);
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                result.add(item == null ? null : item.toString());
            }
        }
        return result;
    }

    // データを取得する
    public Map<String, Object> getData() {
        return data;
    }

    // 最後のキーを取得する
    private static String lastKey(String path) {

        int lastDot = path.lastIndexOf('.');
        return lastDot == -1 ? path : path.substring(lastDot + 1);
    }
}
