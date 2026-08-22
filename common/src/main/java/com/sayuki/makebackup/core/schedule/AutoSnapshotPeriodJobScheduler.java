/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.schedule;

import lombok.Getter;
import com.sayuki.makebackup.MakeBackup;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static java.lang.Long.max;

// 周期実行ジョブスケジューラークラス - 固定間隔でバックアップを実行する
public class AutoSnapshotPeriodJobScheduler implements AutoSnapshotJobScheduler {

    @Getter
    private final AutoSnapshotScheduleManager autoBackupScheduleManager;

    // コンストラクタ - スケジュールマネージャーを設定する
    public AutoSnapshotPeriodJobScheduler(AutoSnapshotScheduleManager autoBackupScheduleManager) {
        this.autoBackupScheduleManager = autoBackupScheduleManager;
    }

    @Override

    // 初期化する - 周期タスクを登録する
    public void init() {
        MakeBackup.getInstance().getScheduleManager().runGlobalRegionRepeatingTask(() -> {
            MakeBackup.getInstance().getScheduleManager().runAsync(this::executeBackupAndScheduleNextAlert);
        }, getNextBackupDelaySeconds() * 20L, MakeBackup.getInstance().getConfigManager().getBackupConfig().getAutoBackupPeriod() * 60L * 20L);
    }

    @Override

    // 次回バックアップまでの遅延秒数を取得する
    public long getNextBackupDelaySeconds() {
        return max(MakeBackup.getInstance().getConfigManager().getBackupConfig().getAutoBackupPeriod() * 60L -
                (LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - MakeBackup.getInstance().getConfigManager().getLastBackup()),
                1L);
    }
}
