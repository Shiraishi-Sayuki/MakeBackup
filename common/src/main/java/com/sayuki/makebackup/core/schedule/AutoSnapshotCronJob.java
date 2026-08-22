/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.schedule;

import org.quartz.JobExecutionContext;
import com.sayuki.makebackup.MakeBackup;

// 自動スナップショットCronジョブクラス - Cronトリガーで実行されるジョブ
public class AutoSnapshotCronJob implements org.quartz.Job {

    @Override

    // 実行する - バックアップと次回アラートを処理する
    public void execute(JobExecutionContext jobExecutionContext) {
        MakeBackup.getInstance().getAutoBackupScheduleManager().getAutoBackupJobScheduler().executeBackupAndScheduleNextAlert();
    }
}
