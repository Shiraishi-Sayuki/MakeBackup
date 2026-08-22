/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.schedule;

import lombok.Getter;
import com.sayuki.makebackup.MakeBackup;

@Getter

// 自動スナップショットスケジュールマネージャークラス - 定期実行を管理する
public class AutoSnapshotScheduleManager {

    private AutoSnapshotJob autoBackupJob;
    private AutoSnapshotJobScheduler autoBackupJobScheduler;

    // 初期化する
    public void init() {
        this.autoBackupJob = new AutoSnapshotJob();
        if (MakeBackup.getInstance().getConfigManager().getBackupConfig().getAutoBackupCron() != null) {
            this.autoBackupJobScheduler = new AutoSnapshotCronJobScheduler(this);
        } else {
            this.autoBackupJobScheduler = new AutoSnapshotPeriodJobScheduler(this);
        }

        if (!MakeBackup.getInstance().getConfigManager().getBackupConfig().isAutoBackup()) return;

        MakeBackup.getInstance().getLogManager().log("Initializing auto backup...");

        autoBackupJobScheduler.init();
        autoBackupJobScheduler.scheduleNextBackupAlert();

        MakeBackup.getInstance().getLogManager().log("Auto backup initialization completed");
    }

    // 破棄する
    public void destroy() {
        autoBackupJob = null;
        autoBackupJobScheduler = null;
    }
}
