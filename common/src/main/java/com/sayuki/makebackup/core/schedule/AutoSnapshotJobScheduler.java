/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.schedule;

import com.sayuki.makebackup.MakeBackup;

import static java.lang.Math.max;
import static java.lang.Math.min;

// 自動スナップショットジョブスケジューラーインターフェース - 共通スケジュール処理を定義する
public interface AutoSnapshotJobScheduler {

    // 初期化する
    void init();

    // 次回バックアップまでの遅延秒数を取得する
    long getNextBackupDelaySeconds();

    // 次回アラートまでの遅延秒数を取得する
    default long getNextAlertDelaySeconds() {

        long delay = getNextBackupDelaySeconds();
        return max(delay - MakeBackup.getInstance().getConfigManager().getServerConfig().getAlertTimeBeforeRestart(), 1L);
    }

    // 次回アラートのメッセージ用秒数を取得する
    default long getNextAlertMessageSeconds() {
        return min(MakeBackup.getInstance().getConfigManager().getServerConfig().getAlertTimeBeforeRestart(), getNextBackupDelaySeconds());
    }

    // 次回バックアップのアラートをスケジュールする
    default void scheduleNextBackupAlert() {
        MakeBackup.getInstance().getScheduleManager().runGlobalRegionDelayed(() -> {
            getAutoBackupScheduleManager().getAutoBackupJob().executeAlert(getNextAlertMessageSeconds(), MakeBackup.getInstance().getConfigManager().getBackupConfig().getAfterBackup());
        }, getNextAlertDelaySeconds() * 20L);
    }

    // バックアップを実行して次回アラートをスケジュールする
    default void executeBackupAndScheduleNextAlert() {
        getAutoBackupScheduleManager().getAutoBackupJob().executeBackup();
        scheduleNextBackupAlert();
    }

    // スケジュールマネージャーを取得する
    AutoSnapshotScheduleManager getAutoBackupScheduleManager();
}
