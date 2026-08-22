/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.settings.ConfigSection;

import java.util.List;

// 後方互換クラス - 古いコンフィグを新しい形式に変換する
public class SettingsBackwardsCompatibility {

    // バージョン4未満のコンフィグを変換する
    public static void configBelow4(ConfigSection config) {

        double configVersion = config.getDouble("configVersion");
        if (configVersion >= 4.0) {
            return;
        }

        int backupPeriod = config.getInt("backupPeriod", 24);
        backupPeriod *= 60;
        config.set("backupPeriod", backupPeriod);

        boolean fixedBackupTime = config.getBoolean("fixedBackupTime", false);
        if (!fixedBackupTime) {
            config.set("backupTime", -1);
        } else {
            config.set("backupTime", config.getInt("firstBackupTime"));
        }
    }

    // バージョン8未満のコンフィグを変換する
    public static void configBelow8(ConfigSection config) {

        double configVersion = config.getDouble("configVersion");
        if (configVersion >= 8.0) {
            return;
        }

        String localBackupsFolder = config.getString("backupsFolder", DefaultConfigs.backupsFolderPath());

        int localMaxBackupsNumber = config.getInt("maxBackupsNumber", 0);

        long localMaxBackupsWeight = config.getLong("maxBackupsWeight", 0L);

        boolean localZipArchive = config.getBoolean("zipArchive", true);

        boolean autoBackup = config.getBoolean("autoBackup", true);

        List<String> addDirectoryToBackup = config.getStringList("addDirectoryToBackup");

        List<String> excludeDirectoryFromBackup = config.getStringList("excludeDirectoryFromBackup");

        int backupPeriod = config.getInt("backupPeriod", 1440);

        int backupTime = config.getInt("backupTime", -1);

        boolean skipDuplicateBackup = config.getBoolean("skipDuplicateBackup", true);

        String afterBackup = config.getString("afterBackup", "NOTHING");

        boolean setWorldsReadOnly = config.getBoolean("setWorldsReadOnly", false);

        long alertTimeBeforeRestart = config.getLong("alertTimeBeforeRestart", 60L);

        boolean alertOnlyServerRestart = config.getBoolean("alertOnlyServerRestart", true);

        boolean betterLogging = config.getBoolean("betterLogging", false);

        config.set("local.backupsFolder", localBackupsFolder);
        config.set("local.maxBackupsNumber", localMaxBackupsNumber);
        config.set("local.maxBackupsWeight", localMaxBackupsWeight);
        config.set("local.zipArchive", localZipArchive);

        config.set("backup.autoBackup", autoBackup);
        config.set("backup.addDirectoryToBackup", addDirectoryToBackup);
        config.set("backup.excludeDirectoryFromBackup", excludeDirectoryFromBackup);
        config.set("backup.backupPeriod", backupPeriod);
        config.set("backup.backupTime", backupTime);
        config.set("backup.skipDuplicateBackup", skipDuplicateBackup);
        config.set("backup.afterBackup", afterBackup);
        config.set("backup.setWorldsReadOnly", setWorldsReadOnly);

        config.set("server.alertTimeBeforeRestart", alertTimeBeforeRestart);
        config.set("server.alertOnlyServerRestart", alertOnlyServerRestart);
        config.set("server.betterLogging", betterLogging);
    }

    // バージョン13未満のコンフィグを変換する
    public static void configBelow13(ConfigSection config) {

        double configVersion = config.getDouble("configVersion");
        if (configVersion >= 13.0) {
            return;
        }

        {

            int backupPeriod = config.getInt("backup.backupPeriod", 1440);

            int backupTime = config.getInt("backup.backupTime", -1);

            if (backupTime < -1 || backupTime > 23) {
                MakeBackup.getInstance().getLogManager().warn("Failed to load config value!");
                MakeBackup.getInstance().getLogManager().warn("backupTime must be >= -1, using default -1 value...");
                backupTime = -1;
            }

            if (backupPeriod <= 0 && backupPeriod != -1) {
                MakeBackup.getInstance().getLogManager().warn("Failed to load config value!");
                MakeBackup.getInstance().getLogManager().warn("backup.backupPeriod must be > 0, using default 1440 value...");
                backupPeriod = 1440;
            }

            String cron = "";
            if (backupTime != -1) {
                cron = "0 0 %s 1/1 * ? *".formatted(backupTime);
            }

            config.set("backup.autoBackupCron", cron);
            config.set("backup.autoBackupPeriod", backupPeriod);
        }

        {

            ConfigSection localSection = config.getConfigSection("local");
            try {
                localSection.set("type", "local");
            } catch (Exception e) {

            }
            ConfigSection ftpSection = config.getConfigSection("ftp");
            try {
                ftpSection.set("type", "ftp");
            } catch (Exception e) {

            }
            ConfigSection sftpSection = config.getConfigSection("sftp");
            try {
                sftpSection.set("type", "sftp");
            } catch (Exception e) {

            }
            ConfigSection googleDriveSection = config.getConfigSection("googleDrive");
            try {
                googleDriveSection.set("type", "googleDrive");
            } catch (Exception e) {

            }

            config.set("local", null);
            config.set("ftp", null);
            config.set("sftp", null);
            config.set("googleDrive", null);

            config.set("storages.local", localSection);
            config.set("storages.ftp", ftpSection);
            config.set("storages.sftp", sftpSection);
            config.set("storages.googleDrive", googleDriveSection);
        }
    }

    // バージョン14未満のコンフィグを変換する
    public static void configBelow14(ConfigSection config) {

        double configVersion = config.getDouble("configVersion");
        if (configVersion >= 14.0) {
            return;
        }

        for (String storageId : List.of("local", "ftp", "sftp", "googleDrive")) {
            ConfigSection storageSection = config.getConfigSection("storages.%s".formatted(storageId));
            if (storageSection != null && !storageSection.isSet("debug.protocolLogging")) {
                storageSection.set("debug.protocolLogging", true);
            }
        }
    }
}
