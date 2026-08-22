/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup;

import lombok.Getter;
import com.sayuki.makebackup.core.Metrics;
import com.sayuki.makebackup.core.ModLogger;
import com.sayuki.makebackup.core.SchedulerService;
import com.sayuki.makebackup.core.schedule.AutoSnapshotScheduleManager;
import com.sayuki.makebackup.core.settings.SettingsManager;
import com.sayuki.makebackup.core.target.TargetManager;
import com.sayuki.makebackup.core.job.SetWorldsWritableJob;
import com.sayuki.makebackup.core.job.Job;
import com.sayuki.makebackup.core.job.JobManager;
import com.sayuki.makebackup.core.helper.Helper;
import com.sayuki.makebackup.command.CommandProcessor;
import com.sayuki.makebackup.platform.Services;

import java.io.File;
import java.io.InputStream;

// MakeBackupクラス - MOD全体を管理するメインクラス
@Getter
public class MakeBackup {

    JobManager taskManager;
    ModLogger logManager;
    SettingsManager configManager;
    SchedulerService scheduleManager;
    TargetManager storageManager;
    CommandProcessor commandManager;
    AutoSnapshotScheduleManager autoBackupScheduleManager;
    Metrics bstats;

    private boolean enabled = false;

    @Getter
    private static MakeBackup instance;

    public static boolean restarting = false;

    // 有効化する - MODが有効になった時に初期化と登録をする
    public void onEnable() {
        instance = this;
        init();
        commandManager.init();
        Services.PLATFORM.registerCommands(commandManager.getTrees());
        scheduleManager.runAsync(() -> {
            storageManager.indexStorages();
        });

        MakeBackup.getInstance().getLogManager().log("MakeBackup mod has been enabled!");
        this.enabled = true;
    }

    // 初期化する - 各マネージャーを生成して設定を読み込む
    public void init() {
        this.configManager = new SettingsManager();
        this.logManager = new ModLogger();
        this.taskManager = new JobManager();
        this.scheduleManager = new SchedulerService();
        this.taskManager.forceLock();
        this.storageManager = new TargetManager();
        this.commandManager = new CommandProcessor();
        this.autoBackupScheduleManager = new AutoSnapshotScheduleManager();
        this.bstats = new Metrics();

        File configFile = getConfigFile();

        File configDir = configFile.getParentFile();
        if (!configDir.exists() && !configDir.mkdirs()) MakeBackup.getInstance().getLogManager().warn("Can not create config dir!");

        configManager.load(configFile, Services.PLATFORM.getConsoleSender());
        scheduleManager.init();
        scheduleManager.runAsync(() -> {
            storageManager.loadSizeCache();
            storageManager.checkStoragesConnection();
        });
        bstats.init();
        taskManager.forceUnlock();
        autoBackupScheduleManager.init();
    }

    // シャットダウンする - 終了処理をしてリソースを解放する
    public void shutdown() {
        taskManager.forceLock();
        storageManager.saveSizeCache();
        Job setWorldsWritableTask = new SetWorldsWritableJob();
        try {
            getTaskManager().startTaskRaw(setWorldsWritableTask, Services.PLATFORM.getConsoleSender());
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn(e);
        }
        MakeBackup.getInstance().getScheduleManager().destroy();
        configManager.setConfigField("lastBackup", configManager.getLastBackup());
        configManager.setConfigField("lastChange", configManager.getLastChange());
        storageManager.destroy();
        autoBackupScheduleManager.destroy();
        scheduleManager.destroy();
        bstats.destroy();
        this.enabled = false;
    }

    // MODディレクトリを取得する
    public File getModDir() {
        return Services.PLATFORM.getConfigDir().resolve("makebackup").toFile();
    }

    // コンフィグファイルを取得する
    public File getConfigFile() {
        return new File(getModDir(), "config.json");
    }

    // リソースを取得する - 指定した名前のリソースを読み込む
    public InputStream getResource(String name) {
        return MakeBackup.class.getClassLoader().getResourceAsStream(name);
    }
}
