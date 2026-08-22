/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.schedule;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.Target;
import com.sayuki.makebackup.core.job.SnapshotJob;
import com.sayuki.makebackup.core.job.Job;
import com.sayuki.makebackup.core.job.JobManager;
import com.sayuki.makebackup.core.helper.UiHelper;
import com.sayuki.makebackup.command.Permissions;
import com.sayuki.makebackup.platform.ModPlayer;
import com.sayuki.makebackup.platform.Services;

import java.util.ArrayList;
import java.util.List;

// 自動スナップショットジョブクラス - バックアップとアラートを実行する
public class AutoSnapshotJob {

    // バックアップを実行する
    public void executeBackup() {
        if (!MakeBackup.getInstance().getConfigManager().getBackupConfig().isAutoBackup()) return;

        MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
            try {

                List<Target> autoBackupStorages = MakeBackup.getInstance().getStorageManager().getStorages().stream().filter(storage -> storage.getConfig().isAutoBackup()).toList();
                Job backupTask = new SnapshotJob(autoBackupStorages, MakeBackup.getInstance().getConfigManager().getBackupConfig().getAfterBackup(), true);

                List<String> permissions = new ArrayList<>(){};
                permissions.addAll(autoBackupStorages.stream().map(Permissions.BACKUP::getPermission).toList());
                if ("RESTART".equals(MakeBackup.getInstance().getConfigManager().getBackupConfig().getAfterBackup())) {
                    permissions.add(Permissions.RESTART.getPermission());
                }
                if ("STOP".equals(MakeBackup.getInstance().getConfigManager().getBackupConfig().getAfterBackup())) {
                    permissions.add(Permissions.STOP.getPermission());
                }

                if (JobManager.Result.LOCKED.equals(MakeBackup.getInstance().getTaskManager().startTask(backupTask, Services.PLATFORM.getConsoleSender(), permissions))) {
                    MakeBackup.getInstance().getLogManager().warn("Failed to start an Auto Snapshot task. Blocked by another operation", Services.PLATFORM.getConsoleSender());
                }
            } catch (Exception e) {
                MakeBackup.getInstance().getLogManager().warn("An error occurred while starting an Auto Snapshot task");
                MakeBackup.getInstance().getLogManager().warn(e);
            }
        });
    }

    // アラートを実行する
    public void executeAlert(long timeSeconds, String afterBackup) {
        if (MakeBackup.getInstance().getConfigManager().getServerConfig().getAlertTimeBeforeRestart() == -1) return;
        boolean restart = false;

        if (afterBackup.equals("STOP")) {
            MakeBackup.getInstance().getLogManager().log(MakeBackup.getInstance().getConfigManager().getServerConfig().getAlertBackupRestartMessage().formatted(timeSeconds));
            restart = true;
        }
        if (afterBackup.equals("RESTART")) {
            MakeBackup.getInstance().getLogManager().log(MakeBackup.getInstance().getConfigManager().getServerConfig().getAlertBackupRestartMessage().formatted(timeSeconds));
            restart = true;
        }
        if (afterBackup.equals("NOTHING")) {
            MakeBackup.getInstance().getLogManager().log(MakeBackup.getInstance().getConfigManager().getServerConfig().getAlertBackupMessage().formatted(timeSeconds));
        }

        for (ModPlayer player : Services.PLATFORM.getOnlinePlayers()) {

            if (!player.hasPermission(Permissions.ALERT.getPermission())) {
                continue;
            }

            if (restart || !MakeBackup.getInstance().getConfigManager().getServerConfig().isAlertOnlyServerRestart()) {

                Component header = Component.empty();

                header = header
                        .append(Component.text("Snapshot Alert")
                                .decorate(TextDecoration.BOLD));

                Component message = Component.empty();

                message = message
                        .append(Component.text((restart ? MakeBackup.getInstance().getConfigManager().getServerConfig().getAlertBackupRestartMessage() :
                                MakeBackup.getInstance().getConfigManager().getServerConfig().getAlertBackupMessage()).formatted(timeSeconds)));

                UiHelper.sendFramedMessage(header, message, 15, player);
                UiHelper.notificationSound(player);
            }
        }
    }
}
