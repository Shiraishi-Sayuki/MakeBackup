/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

import lombok.Getter;
import com.sayuki.makebackup.MakeBackup;

import java.io.File;

// サーバー設定クラス - サーバー関連の設定を管理する
@Getter
public class ServerSettings implements Settings {

    private long alertTimeBeforeRestart;
    private boolean betterLogging;
    private boolean alertOnlyServerRestart;
    private File sizeCacheFile;
    private String alertBackupMessage;
    private String alertBackupRestartMessage;
    private int threadNumber;

    private ConfigSection config;

    @Override
    // 読み込む - コンフィグから設定をロードする
    public Settings load(ConfigSection config, String name) {
        this.config = config;
        this.alertBackupMessage = config.getString("alertBackupMessage");
        this.alertBackupRestartMessage = config.getString("alertBackupRestartMessage");
        this.sizeCacheFile = new File(config.getString("sizeCacheFile"));
        this.threadNumber = config.getInt("threadNumber");
        this.betterLogging = config.getBoolean("betterLogging");
        this.alertTimeBeforeRestart = config.getLong("alertTimeBeforeRestart");
        this.alertOnlyServerRestart = config.getBoolean("alertOnlyServerRestart");
        return this;
    }

    @Override
    // デフォルトコンフィグを取得する
    public ConfigSection getDefaultConfig() {
        return DefaultConfigs.serverConfig();
    }
}
