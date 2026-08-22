/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

import lombok.Getter;
import com.sayuki.makebackup.MakeBackup;

// SFTP設定クラス - SFTPストレージの設定を管理する
@Getter
public class SftpSettings implements PathTargetSettings {

    private String id;

    private boolean enabled;
    private boolean autoBackup;
    private String sshConfigFilePath;
    private String backupsFolder;
    private String authType;
    private String username;
    private String address;
    private String password;
    private String knownHostsFilePath;
    private String useKnownHostsFile;
    private String keyFilePath;
    private String pathSeparatorSymbol;
    private int backupsNumber;
    private long backupsWeight;
    private boolean zipArchive;
    private int zipCompressionLevel;
    private boolean protocolLogging;
    private int port;

    private ConfigSection config;

    // 読み込む - コンフィグからSFTP設定をロードする
    public SftpSettings load(ConfigSection config, String name) {
        this.config = config;
        this.id = name;
        this.enabled = config.getBoolean("enabled");
        this.autoBackup = config.getBoolean("autoBackup");
        this.backupsFolder = config.getString("backupsFolder");
        this.pathSeparatorSymbol = config.getString("pathSeparatorSymbol");

        int backupsNumber = config.getInt("maxBackupsNumber");
        long backupsWeight = config.getLong("maxBackupsWeight") * 1_048_576L;
        this.zipArchive = config.getBoolean("zipArchive");

        int zipCompressionLevel = config.getInt("zipCompressionLevel");
        this.keyFilePath = config.getString("auth.keyFilePath");
        this.authType = config.getString("auth.authType");
        this.username = config.getString("auth.username");
        this.password = config.getString("auth.password");
        this.address = config.getString("auth.address");
        this.port = config.getInt("auth.port");
        this.protocolLogging = config.getBoolean("debug.protocolLogging");
        this.useKnownHostsFile = config.getString("auth.useKnownHostsFile");
        this.knownHostsFilePath = config.getString("auth.knownHostsFilePath");
        this.sshConfigFilePath = config.getString("auth.sshConfigFilePath");

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
        return DefaultConfigs.sftpConfig();
    }
}
