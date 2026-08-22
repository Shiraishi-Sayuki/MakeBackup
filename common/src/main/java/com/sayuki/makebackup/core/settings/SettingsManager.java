/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

import lombok.Getter;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.*;
import com.sayuki.makebackup.platform.ModCommandSender;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

// 設定マネージャークラス - コンフィグの読み込みと保存を管理する
public class SettingsManager {

    private File configFile;

    @Getter
    private long lastBackup;
    @Getter
    private long lastChange;

    @Getter
    private SnapshotSettings backupConfig;
    @Getter
    private ServerSettings serverConfig;

    // コンフィグのフィールドを設定する
    public synchronized void setConfigField(String path, Object value) {

        ConfigSection config = ConfigSection.loadFromFile(configFile);
        config.set(path, value);
        try {
            config.save(configFile);
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to save config.json file");
        }
    }

    // 最終変更時刻を更新する
    public void updateLastChange() {
        this.lastChange = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    }

    // 最終バックアップ時刻を更新する
    public void updateLastBackup() {
        this.lastBackup = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    }

    // 設定を読み込む - ファイルからコンフィグをロードする
    public void load(File configFile, ModCommandSender sender) {

        if (!configFile.exists()) {

            File parentFile = configFile.getParentFile();
            if (!parentFile.exists() && !parentFile.mkdirs()) {
                MakeBackup.getInstance().getLogManager().warn("Can not create config dir!");
            }
            try {
                DefaultConfigs.masterConfig().save(configFile);
                MakeBackup.getInstance().getLogManager().log("Default config.json has been generated", sender);
            } catch (Exception e) {
                MakeBackup.getInstance().getLogManager().warn("Failed to generate default config.json", sender);
                MakeBackup.getInstance().getLogManager().warn(e);
            }
        }

        MakeBackup.getInstance().getLogManager().log("loading config...", sender);

        this.configFile = configFile;

        ConfigSection config = ConfigSection.loadFromFile(configFile);

        SettingsBackwardsCompatibility.configBelow4(config);
        SettingsBackwardsCompatibility.configBelow8(config);
        SettingsBackwardsCompatibility.configBelow13(config);
        SettingsBackwardsCompatibility.configBelow14(config);

        loadBackupConfig(config);
        loadStorages(config);
        loadServerConfig(config);

        this.lastBackup = config.getLong("lastBackup", 0);
        this.lastChange = config.getLong("lastChange", 0);

        config.set("configVersion", DefaultConfigs.CONFIG_VERSION);
        try {
            config.save(configFile);
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to save repaired config.json file", sender);
        }

        MakeBackup.getInstance().getLogManager().log("Settings has been loaded", sender);
    }

    // バックアップ設定を読み込む
    private void loadBackupConfig(ConfigSection config) {
        ConfigSection backupSection = config.getConfigurationSection("backup");
        this.backupConfig = (SnapshotSettings) new SnapshotSettings().repairThenLoad(backupSection);
        config.set("backup", backupConfig.getConfig());
    }

    // ストレージ設定を読み込む - 各保存先を登録する
    private void loadStorages(ConfigSection config) {
        ConfigSection storagesSection = config.getConfigurationSection("storages");
        storagesSection.getKeys(false).forEach(key -> {
            String storageId = key;
            ConfigSection storageSection = storagesSection.getConfigurationSection(storageId);

            String storageType = storageSection.getString("type", "");
            Target storage = switch (storageType) {
                case "local" -> new LocalTarget((LocalSettings) new LocalSettings().repairThenLoad(storageSection));
                case "ftp" -> new FtpTarget((FtpSettings) new FtpSettings().repairThenLoad(storageSection));
                case "sftp" -> new SftpTarget((SftpSettings) new SftpSettings().repairThenLoad(storageSection));
                case "googleDrive" -> new GoogleDriveTarget((GoogleDriveSettings) new GoogleDriveSettings().repairThenLoad(storageSection));
                default -> {
                    MakeBackup.getInstance().getLogManager().warn("Wrong storage type \"%s\" in \"%s\" storage in config.json. Skipping this storage...".formatted(storageType, storageId));
                    yield null;
                }
            };
            if (storage != null) storagesSection.set(storageId, storage.getConfig().getConfig());
            if (storage != null && storage.getConfig().isEnabled()) MakeBackup.getInstance().getStorageManager().registerStorage(storageId, storage);
        });
        config.set("storages", storagesSection);
    }

    // サーバー設定を読み込む
    private void loadServerConfig(ConfigSection config) {
        ConfigSection serverSection = config.getConfigurationSection("server");
        this.serverConfig = (ServerSettings) new ServerSettings().repairThenLoad(serverSection);
        config.set("server", serverConfig.getConfig());
    }
}
