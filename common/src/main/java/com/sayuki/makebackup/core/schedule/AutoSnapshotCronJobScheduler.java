/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.schedule;

import lombok.Getter;
import org.quartz.CronTrigger;
import com.sayuki.makebackup.MakeBackup;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

// Cronジョブスケジューラークラス - Cron式でバックアップをスケジュールする
public class AutoSnapshotCronJobScheduler implements AutoSnapshotJobScheduler {

    @Getter
    private final AutoSnapshotScheduleManager autoBackupScheduleManager;
    private CronTrigger cronTrigger;
    private boolean firstAlert = true;

    // コンストラクタ - スケジュールマネージャーを設定する
    public AutoSnapshotCronJobScheduler(AutoSnapshotScheduleManager autoBackupScheduleManager) {
        this.autoBackupScheduleManager = autoBackupScheduleManager;
    }

    // 初期化する - Cronジョブを登録する
    public void init() {
        this.cronTrigger = MakeBackup.getInstance().getScheduleManager()
                .runCronScheduledJob(AutoSnapshotCronJob.class, "backup", "auto", MakeBackup.getInstance().getConfigManager().getBackupConfig().getAutoBackupCron());
    }

    @Override

    // 次回バックアップまでの遅延秒数を取得する
    public long getNextBackupDelaySeconds() {
        Date nextFireTime = firstAlert ? cronTrigger.getNextFireTime() : cronTrigger.getFireTimeAfter(cronTrigger.getNextFireTime());
        return nextFireTime.getTime() / 1000 - LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(nextFireTime.getTimezoneOffset() / 60 * -1));
    }

    @Override

    // 次回バックアップのアラートをスケジュールする
    public void scheduleNextBackupAlert() {
        MakeBackup.getInstance().getScheduleManager().runGlobalRegionDelayed(() -> {
            getAutoBackupScheduleManager().getAutoBackupJob().executeAlert(getNextAlertMessageSeconds(), MakeBackup.getInstance().getConfigManager().getBackupConfig().getAfterBackup());
            firstAlert = false;
        }, getNextAlertDelaySeconds() * 20L);
    }
}
