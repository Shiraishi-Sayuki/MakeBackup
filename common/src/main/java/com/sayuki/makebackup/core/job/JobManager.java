/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.helper.UiHelper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

// ジョブマネージャークラス - ジョブの実行と制御を管理する
public class JobManager {

    @Getter
    private Job currentTask;
    private List<String> currentTaskPermissions;
    boolean forceLock = false;

    // 開始する - 内部でタスクを実行する
    private Result start(Job task, ModCommandSender sender, List<String> permissions, Function<Runnable, CompletableFuture<Void>> taskExecutor) {
        if (isLocked()) {
            return Result.LOCKED.sendMessage(task, sender);
        }
        if (!hasPermissions(permissions, sender)) {
            return Result.NO_PERMISSION.sendMessage(task, sender);
        }
        currentTask = task;
        currentTaskPermissions = permissions;
        Result.STARTED.sendMessage(task, sender);
        CompletableFuture<Void> taskFuture = taskExecutor.apply(() -> {
            try {
                MakeBackup.getInstance().getTaskManager().startTaskRaw(currentTask, sender);
            } catch (Exception e) {
                MakeBackup.getInstance().getLogManager().warn("An error occurred while executing task %s".formatted(task.getTaskName()));
                MakeBackup.getInstance().getLogManager().warn(e);
            }
            this.currentTaskPermissions = null;
            this.currentTask = null;
            Result.COMPLETED.sendMessage(task, sender);
        });
        task.setTaskFuture(taskFuture);
        if (taskFuture.isDone()) {
            return Result.COMPLETED;
        } else {
            return Result.STARTED;
        }
    }

    // タスクを開始する - 同期で実行する
    public Result startTask(Job task, ModCommandSender sender, List<String> permissions) {
        return start(task, sender, permissions, (runnable) -> {
            runnable.run();
            return CompletableFuture.completedFuture(null);
        });
    }

    // タスクを非同期で開始する
    public Result startTaskAsync(Job task, ModCommandSender sender, List<String> permissions) {
        return start(task, sender, permissions, MakeBackup.getInstance().getScheduleManager()::runAsync);
    }

    // 生でタスクを開始する - 実際にrunを呼ぶ
    public void startTaskRaw(Job task, ModCommandSender sender) throws JobException {
        MakeBackup.getInstance().getLogManager().devLog("Job %s started".formatted(task.getTaskName()));
        task.start(sender);
        MakeBackup.getInstance().getLogManager().devLog("Job %s completed".formatted(task.getTaskName()));
    }

    // 生でタスクをキャンセルする
    public void cancelTaskRaw(Job task) {
        task.cancel();
        if (task.getPrepareTaskFuture() != null) {
            try {
                task.getPrepareTaskFuture().cancel(false);
                task.getPrepareTaskFuture().join();
            } catch (Exception e) {

            }
        }
        if (task.getTaskFuture() != null) {
            try {
                task.getTaskFuture().cancel(false);
                task.getTaskFuture().join();
            } catch (Exception e) {

            }
        }
    }

    // タスクを準備する - 非同期でprepareTaskを呼ぶ
    public void prepareTask(Job task, ModCommandSender sender) throws Throwable {
        CompletableFuture<Void> prepareTaskFuture = MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
            MakeBackup.getInstance().getLogManager().devLog("Preparing task %s".formatted(task.getTaskName()));
            try {
                task.prepareTask(sender);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            } finally {
                MakeBackup.getInstance().getLogManager().devLog("%s task preparation completed".formatted(task.getTaskName()));
            }
        });
        task.setPrepareTaskFuture(prepareTaskFuture);
        try {
            prepareTaskFuture.get();
        } catch (InterruptedException ignored) {

        } catch (ExecutionException e) {
            throw e.getCause().getCause();
        }
    }

    // 現在のタスクをキャンセルする
    public Result cancelCurrentTask(ModCommandSender sender) {
        if (currentTask == null) {
            return Result.NO_TASK_RUNNING.sendMessage(null, sender);
        }
        if (!hasPermissions(currentTaskPermissions, sender)) {
            return Result.NO_PERMISSION.sendMessage(currentTask, sender);
        }
        sendCancellingMessage(sender);
        cancelTaskRaw(currentTask);
        return Result.CANCELLED;
    }

    // ロック中か判定する
    public boolean isLocked() {
        return currentTask != null && !forceLock;
    }

    // 権限を持っているか確認する
    private boolean hasPermissions(List<String> permissions, ModCommandSender sender) {
        return permissions.stream().allMatch(sender::hasPermission);
    }

    // 結果列挙 - タスク実行結果を表す
    public enum Result {
        STARTED(""),
        COMPLETED(""),
        CANCELLED("%s task has been successfully cancelled"),
        NO_PERMISSION("You don't have enough permissions"),
        LOCKED("%s task is blocked by another running task"),
        NO_TASK_RUNNING("There are no running tasks");

        private final String message;

        // コンストラクタ - メッセージで初期化する
        Result(String message) {
            this.message = message;
        }

        // メッセージを取得する
        private Component getMessage(Job task, ModCommandSender sender) {
            if (STARTED.equals(this)) {
                return getTaskStartedMessage(task, sender);
            }
            if (COMPLETED.equals(this)) {
                return getTaskCompletedMessage(task, sender);
            }
            return Component.text(this.message.formatted(task.getTaskName()));
        }

        // メッセージを送信する
        public Result sendMessage(Job task, ModCommandSender sender) {
            MakeBackup.getInstance().getLogManager().log(getMessage(task, sender), sender);
            return this;
        }

        // 完了メッセージを取得する
        private Component getTaskCompletedMessage(Job task, ModCommandSender sender) {
            Component message = Component.empty();

            message = message
                    .append(Component.text("The "))
                    .append(Component.text(task.getTaskName())
                            .decorate(TextDecoration.BOLD)
                            .color(TextColor.color(task.isCancelled() ? 0xB02100 : 0x4974B)))
                    .append(Component.text(" task %s".formatted(task.isCancelled() ? "cancelled" : "completed")));

            if (!(sender.isConsole())) {
                return UiHelper.getFramedMessage(message, 15, sender);
            } else {
                return UiHelper.getFramedMessage(message, sender);
            }
        }

        // 開始メッセージを取得する
        private Component getTaskStartedMessage(Job task, ModCommandSender sender) {

            Component header = Component.empty();
            Component message = Component.empty();

            if (!(sender.isConsole())) {

                header = header
                        .append(Component.text("The "))
                        .append(Component.text(task.getTaskName())
                                .decorate(TextDecoration.BOLD)
                                .color(TextColor.color(0x4974B)))
                        .append(Component.text(" task has been started"));

                message = message
                        .append(Component.text("[STATUS]")
                                .clickEvent(ClickEvent.runCommand("/makebackup task status"))
                                .color(TextColor.color(17, 102, 212))
                                .decorate(TextDecoration.BOLD))
                        .append(Component.space())
                        .append(Component.text("[CANCEL]")
                                .decorate(TextDecoration.BOLD)
                                .color(TextColor.color(0xB02100))
                                .clickEvent(ClickEvent.runCommand("/makebackup task cancel")));
            } else {

                header = header
                        .append(Component.text("The "))
                        .append(Component.text(task.getTaskName())
                                .decorate(TextDecoration.BOLD)
                                .color(TextColor.color(0x4974B)))
                        .append(Component.text(" task has been started"));
                message = message
                        .append(Component.text("You can check the task status using command"))
                        .append(Component.newline())
                        .append(Component.text("/makebackup task status")
                                .decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.suggestCommand("/makebackup task status")))
                        .append(Component.newline())
                        .append(Component.text("You can cancel the task using command"))
                        .append(Component.newline())
                        .append(Component.text("/makebackup task cancel")
                                .decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.suggestCommand("/makebackup task cancel")));
            }

            if (!(sender.isConsole())) {
                return UiHelper.getFramedMessage(header, message, 15, sender);
            } else {
                return UiHelper.getFramedMessage(header, message, sender);
            }
        }
    }

    // キャンセル中メッセージを送信する
    private void sendCancellingMessage(ModCommandSender sender) {
        MakeBackup.getInstance().getLogManager().log("Cancelling %s task...".formatted(currentTask.getTaskName()), sender);
    }

    // 強制ロックする
    public void forceLock() {
        forceLock = true;
    }

    // 強制ロックを解除する
    public void forceUnlock() {
        forceLock = false;
    }
}
