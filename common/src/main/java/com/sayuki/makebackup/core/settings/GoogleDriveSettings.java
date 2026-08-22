/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

import lombok.Getter;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.GoogleDriveTarget;

import java.io.File;
import java.util.HashMap;

// GoogleDrive設定クラス - GoogleDriveストレージの設定を管理する
@Getter
public class GoogleDriveSettings implements TargetSettings {

    private String id;

    private boolean enabled;
    private boolean autoBackup;
    private File tokenFolder;
    private String backupsFolderId;
    private String authServiceUrl;
    private boolean createBackuperFolder;
    private int backupsNumber;
    private long backupsWeight;
    private boolean zipArchive;
    private int zipCompressionLevel;
    private boolean protocolLogging;

    private ConfigSection config;

    // 読み込む - コンフィグからGoogleDrive設定をロードする
    public GoogleDriveSettings load(ConfigSection config, String name) {
        this.config = config;
        this.id = name;
        this.enabled = config.getBoolean("enabled");
        this.autoBackup = config.getBoolean("autoBackup");
        this.backupsFolderId = config.getString("backupsFolderId");

        String googleDriveTokenFolder = config.getString("auth.tokenFolderPath");
        this.tokenFolder = new File(googleDriveTokenFolder);
        this.authServiceUrl = config.getString("auth.authServiceUrl", "https://auth.backuper-mc.com");
        this.createBackuperFolder = config.getBoolean("createBackuperFolder");
        this.backupsNumber = config.getInt("maxBackupsNumber");
        this.backupsWeight = config.getLong("maxBackupsWeight") * 1_048_576L;
        this.zipArchive = config.getBoolean("zipArchive");
        this.protocolLogging = config.getBoolean("debug.protocolLogging");

        int zipCompressionLevel = config.getInt("zipCompressionLevel");

        if (zipCompressionLevel > 9 || zipCompressionLevel < 0) {
            MakeBackup.getInstance().getLogManager().warn("Failed to load config value!");
            if (zipCompressionLevel < 0) {
                MakeBackup.getInstance().getLogManager().warn("zipCompressionLevel must be >= 0, using 0 value...");
                zipCompressionLevel = 0;
            }
            if (zipCompressionLevel > 9) {
                MakeBackup.getInstance().getLogManager().warn("zipCompressionLevel must be <= 9, using 9 value...");
                zipCompressionLevel = 9;
            }
        }
        this.zipCompressionLevel = zipCompressionLevel;
        return this;
    }

    // 生のバックアップフォルダIDを取得する
    public String getRawBackupFolderId() {
        return backupsFolderId;
    }

    // バックアップフォルダIDを取得する - 必要ならフォルダを作成する
    public String getBackupsFolderId() {
        if (!createBackuperFolder) {
            return backupsFolderId;
        }

        GoogleDriveTarget storage = (GoogleDriveTarget) MakeBackup.getInstance().getStorageManager().getStorage(id);
        if (storage == null) {
            throw new RuntimeException("Tried to get backupsFolder from unregistered \"%s\" GoogleDrive storage".formatted(id));
        }

        for (com.google.api.services.drive.model.File driveFile : storage.ls(backupsFolderId, "appProperties has { key='root' and value='true' }")) {
            if (driveFile.getName().equals("MakeBackup")) {
                return driveFile.getId();
            }
        }
        HashMap<String, String> properties = new HashMap<>();
        properties.put("root", "true");
        storage.createDir("MakeBackup", backupsFolderId, properties);
        return storage.getFileByName("MakeBackup", backupsFolderId).getId();
    }

    @Override
    // バックアップフォルダを取得する
    public String getBackupsFolder() {
        return getBackupsFolderId();
    }

    @Override
    // IDを取得する
    public String getId() {
        return id;
    }

    @Override
    // デフォルトコンフィグを取得する
    public ConfigSection getDefaultConfig() {
        return DefaultConfigs.googleDriveConfig();
    }
}
