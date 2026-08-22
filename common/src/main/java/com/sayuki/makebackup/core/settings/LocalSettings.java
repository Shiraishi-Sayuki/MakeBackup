/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

import lombok.Getter;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.helper.Helper;

// ローカル設定クラス - ローカルストレージの設定を管理する
@Getter
public class LocalSettings implements PathTargetSettings {

    private String id;

    private boolean enabled;
    private boolean autoBackup;
    private String backupsFolder;
    private int backupsNumber;
    private long backupsWeight;
    private boolean zipArchive;
    private int zipCompressionLevel;
    private boolean protocolLogging;
    private String pathSeparatorSymbol = Helper.isWindows ? "\\" : "/";

    private ConfigSection config;

    // 読み込む - コンフィグからローカル設定をロードする
    public LocalSettings load(ConfigSection config, String name) {
        this.config = config;
        this.id = name;
        this.enabled = config.getBoolean("enabled");
        this.autoBackup = config.getBoolean("autoBackup");

        int backupsNumber = config.getInt("maxBackupsNumber");
        long backupsWeight = config.getLong("maxBackupsWeight") * 1_048_576L;
        this.zipArchive = config.getBoolean("zipArchive");
        this.backupsFolder = config.getString("backupsFolder");
        this.protocolLogging = config.getBoolean("debug.protocolLogging");

        int zipCompressionLevel = config.getInt("zipCompressionLevel");

        if (backupsNumber < 0) {
            MakeBackup.getInstance().getLogManager().warn("Failed to load config value!");
            MakeBackup.getInstance().getLogManager().warn("%s.maxBackupsNumber must be >= 0, using default 0 value...".formatted(config.getCurrentPath()));
            backupsNumber = 0;
        }
        this.backupsNumber = backupsNumber;

        if (backupsWeight < 0) {
            MakeBackup.getInstance().getLogManager().warn("Failed to load config value!");
            MakeBackup.getInstance().getLogManager().warn("%s.maxBackupsWeight must be >= 0, using default 0 value...".formatted(config.getCurrentPath()));
            backupsWeight = 0;
        }
        this.backupsWeight = backupsWeight;

        if (zipCompressionLevel > 9 || zipCompressionLevel < 0) {
            MakeBackup.getInstance().getLogManager().warn("Failed to load config value!");
            if (zipCompressionLevel < 0) {
                MakeBackup.getInstance().getLogManager().warn("%s.zipCompressionLevel must be >= 0, using 0 value...".formatted(config.getCurrentPath()));
                zipCompressionLevel = 0;
            }
            if (zipCompressionLevel > 9) {
                MakeBackup.getInstance().getLogManager().warn("%s.zipCompressionLevel must be <= 9, using 9 value...".formatted(config.getCurrentPath()));
                zipCompressionLevel = 9;
            }
        }
        this.zipCompressionLevel = zipCompressionLevel;
        return this;
    }

    @Override
    // デフォルトコンフィグを取得する
    public ConfigSection getDefaultConfig() {
        return DefaultConfigs.localConfig();
    }
}
