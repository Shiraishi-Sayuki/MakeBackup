/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

import lombok.Getter;
import lombok.Setter;
import org.quartz.CronExpression;
import com.sayuki.makebackup.MakeBackup;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// スナップショット設定クラス - バックアップの動作設定を管理する
@Getter
public class SnapshotSettings implements Settings {

    private boolean autoBackup;
    private long autoBackupPeriod;
    private CronExpression autoBackupCron;
    private String backupFileNameFormat;
    private List<String> addDirectoryToBackup;
    private List<String> excludeDirectoryFromBackup;
    private boolean deleteBrokenBackups;
    private boolean skipDuplicateBackup;
    private String afterBackup;
    private boolean setWorldsReadOnly;

    private DateTimeFormatter dateTimeFormatter;

    @Setter
    private ConfigSection config;

    // 読み込む - コンフィグから設定をロードする
    public SnapshotSettings load(ConfigSection config, String name) {
        this.config = config;

        boolean autoBackup = config.getBoolean("autoBackup");
        this.autoBackupPeriod = config.getLong("autoBackupPeriod");
        CronExpression autoBackupCron = null;
        if (autoBackup && !config.getString("autoBackupCron").isEmpty()) {
            try {
                autoBackupCron = new CronExpression(config.getString("autoBackupCron"));
            } catch (ParseException e) {
                MakeBackup.getInstance().getLogManager().warn("Failed to parse backup.autoBackupCron! Using autoBackupPeriod instead");
                MakeBackup.getInstance().getLogManager().warn(e);
            }
        }
        this.autoBackup = autoBackup;
        this.autoBackupCron = autoBackupCron;

        String backupFileNameFormat = config.getString("backupFileNameFormat");
        this.addDirectoryToBackup = config.getStringList("addDirectoryToBackup");
        this.excludeDirectoryFromBackup = config.getStringList("excludeDirectoryFromBackup");
        this.deleteBrokenBackups = config.getBoolean("deleteBrokenBackups");
        this.skipDuplicateBackup = config.getBoolean("skipDuplicateBackup");
        this.afterBackup = config.getString("afterBackup").toUpperCase();
        this.setWorldsReadOnly = config.getBoolean("setWorldsReadOnly");

        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(backupFileNameFormat);
            LocalDateTime localDateTime = LocalDateTime.parse(LocalDateTime.now().format(dateTimeFormatter), dateTimeFormatter);
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Wrong backupFileNameFormat format: \"%s\", using default \"dd-MM-yyyy HH-mm-ss\" value...".formatted(backupFileNameFormat));
            MakeBackup.getInstance().getLogManager().warn(e);
            backupFileNameFormat = "dd-MM-yyyy HH-mm-ss";
        }
        this.backupFileNameFormat = backupFileNameFormat;
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(backupFileNameFormat);
        return this;
    }

    @Override
    // デフォルトコンフィグを取得する
    public ConfigSection getDefaultConfig() {
        return DefaultConfigs.backupConfig();
    }
}
