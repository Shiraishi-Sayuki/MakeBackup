/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

// 設定インターフェース - 各設定の共通操作を定義する
public interface Settings {

    // 読み込む - コンフィグから設定をロードする
    Settings load(ConfigSection config, String name);

    // デフォルトコンフィグを取得する
    ConfigSection getDefaultConfig();

    // コンフィグを取得する
    ConfigSection getConfig();

    // 修復する - デフォルト値で欠損を補う
    default ConfigSection repair(ConfigSection config) {
        ConfigSection defaultConfig = getDefaultConfig();
        for (String key : defaultConfig.getKeys(true)) {
            if (config.contains(key) && !(config.get(key) instanceof java.util.Map)) {
                defaultConfig.set(key, config.get(key));
            }
        }
        for (String key : config.getKeys(true)) {
            if (!defaultConfig.contains(key)) {
                defaultConfig.set(key, config.get(key));
            }
        }
        return defaultConfig;
    }

    // 修復してから読み込む
    default Settings repairThenLoad(ConfigSection config) {
        ConfigSection repairedConfig = repair(config);
        return load(repairedConfig, config.getName());
    }
}
