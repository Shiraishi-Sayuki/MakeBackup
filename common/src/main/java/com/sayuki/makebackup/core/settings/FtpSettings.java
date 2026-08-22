/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

import lombok.Getter;
import com.sayuki.makebackup.MakeBackup;

// FTP設定クラス - FTPストレージの設定を管理する
@Getter
public class FtpSettings implements PathTargetSettings {

    private String id;

    private boolean enabled;
    private boolean autoBackup;
    private String backupsFolder;
    private String username;
    private String address;
    private String password;
    private String pathSeparatorSymbol;
    private int backupsNumber;
    private long backupsWeight;
    private int port;
    private boolean zipArchive;
    private int zipCompressionLevel;
    private boolean protocolLogging;

    private ConfigSection config;

    // 読み込む - コンフィグからFTP設定をロードする
    public FtpSettings load(ConfigSection config, String name) {
        this.config = config;
        this.id = name;
        this.enabled = config.getBoolean("enabled");
        this.autoBackup = config.getBoolean("autoBackup");
        this.backupsFolder = config.getString("backupsFolder");
        this.pathSeparatorSymbol = config.getString("pathSeparatorSymbol");
        this.backupsNumber = config.getInt("maxBackupsNumber");
        this.backupsWeight = config.getLong("maxBackupsWeight") * 1_048_576L;
        this.zipArchive = config.getBoolean("zipArchive");

        int zipCompressionLevel = config.getInt("zipCompressionLevel");
        this.address = config.getString("auth.address");
        this.port = config.getInt("auth.port");
        this.username = config.getString("auth.username");
        this.password = config.getString("auth.password");
        this.protocolLogging = config.getBoolean("debug.protocolLogging");

        if (zipCompressionLevel > 9 || zipCompressionLevel < 0) {
            MakeBackup.getInstance().getLogManager().warn("Failed to load config value!");
            if (zipCompressionLevel < 0) {
                MakeBackup.getInstance().getLogManager().warn("%s.zipCompressionLevel must be >= 0, using 0 value...".formatted(config.getCurrentPath()));
                zipCompressionLevel = 0;
            }
            if (zipCompressionLevel > 9) {
                MakeBackup.getInstance().getLogManager().warn("ftp.zipCompressionLevel must be <= 9, using 9 value...".formatted(config.getCurrentPath()));
                zipCompressionLevel = 9;
            }
        }
        this.zipCompressionLevel = zipCompressionLevel;
        return this;
    }

    @Override
    // デフォルトコンフィグを取得する
    public ConfigSection getDefaultConfig() {
        return DefaultConfigs.ftpConfig();
    }
}
