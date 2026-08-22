/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import org.jetbrains.annotations.ApiStatus;
import com.sayuki.makebackup.MakeBackup;

import java.util.concurrent.CompletableFuture;

import static java.lang.Math.min;

// ベースジョブ抽象クラス - 共通のジョブ処理を提供する
public abstract class BaseJob implements Job {

    protected ModCommandSender sender = null;
    protected String taskName;

    protected long currentProgress = 0;
    protected long maxProgress = 0;
    protected boolean cancelled = false;

    protected CompletableFuture<Void> prepareTaskFuture = null;
    protected CompletableFuture<Void> taskFuture = null;

    // コンストラクタ - 初期化する
    protected BaseJob() {}

    // タスク名を取得する
    public String getTaskName() {
        return this.getClass().getSimpleName().replace("Job", "");
    }

    // 進捗率を取得する
    public long getTaskPercentProgress() {

        if (getTaskMaxProgress() == 0) {
            return 0;
        }
        return (long) min((((double) getTaskCurrentProgress()) / ((double) getTaskMaxProgress()) * 100.0), 100.0);
    }

    // 現在の進捗を取得する
    public long getTaskCurrentProgress() {
        return currentProgress;
    }

    // 最大進捗を取得する
    public long getTaskMaxProgress() {
        return maxProgress;
    }

    // 現在の進捗を増やす
    public synchronized void incrementCurrentProgress(long progress) {
        this.currentProgress += progress;
    }

    // キャンセル済みか判定する
    public boolean isCancelled() {
        return cancelled;
    }

    @ApiStatus.Internal
    // 実行する - サブクラスで実装する
    public abstract void run();

    @ApiStatus.Internal
    // タスクを準備する - サブクラスで実装する
    public abstract void prepareTask(ModCommandSender sender) throws Throwable;

    @ApiStatus.Internal
    // 開始する - 準備と実行を呼び出す
    public void start(ModCommandSender sender) throws JobException {
        if (!cancelled) {
            this.sender = sender;
        }
        if (!isTaskPrepared() && !cancelled) {
            try {
                MakeBackup.getInstance().getTaskManager().prepareTask(this, sender);
            } catch (Throwable e) {
                throw new JobException(this, e);
            }
        }
        if (!cancelled) {
            try {
                prepareTaskFuture.get();
            } catch (Exception e) {
                throw new JobException(this, e);
            }
        }
        if (!cancelled) {
            try {
                run();
            } catch (Exception e) {
                throw new JobException(this, e);
            }
        }
    }

    @ApiStatus.Internal
    // キャンセルする - サブクラスで実装する
    public abstract void cancel();

    @Override
    // 準備タスクのFutureを設定する
    public void setPrepareTaskFuture(CompletableFuture<Void> future) {
        this.prepareTaskFuture = future;
    }

    @Override
    // 準備タスクのFutureを取得する
    public CompletableFuture<Void> getPrepareTaskFuture() {
        return prepareTaskFuture;
    }

    @Override
    // タスクのFutureを設定する
    public void setTaskFuture(CompletableFuture<Void> future) {
        this.taskFuture = future;
    }

    @Override
    // タスクのFutureを取得する
    public CompletableFuture<Void> getTaskFuture() {
        return taskFuture;
    }

    // 警告を出力する
    protected void warn(String message) {
        MakeBackup.getInstance().getLogManager().warn(message);
    }

    // 警告を出力する - 送信者付き
    protected void warn(String message, ModCommandSender sender) {
        MakeBackup.getInstance().getLogManager().warn(message, sender);
    }

    // 警告を出力する - 例外版
    protected void warn(Exception e) {
        MakeBackup.getInstance().getLogManager().warn(e);
    }

    // 警告を出力する - JobException版
    protected void warn(JobException e) {
        MakeBackup.getInstance().getLogManager().warn(e);
    }

    // ログを出力する
    protected void log(String message) {
        MakeBackup.getInstance().getLogManager().log(message);
    }

    // ログを出力する - 送信者付き
    protected void log(String message, ModCommandSender sender) {
        MakeBackup.getInstance().getLogManager().log(message, sender);
    }

    // 開発用ログを出力する
    protected void devLog(String message) {
        MakeBackup.getInstance().getLogManager().devLog(message);
    }

    // 開発用警告を出力する
    protected void devWarn(String message) {
        MakeBackup.getInstance().getLogManager().devWarn(message);
    }

    // 開発用警告を出力する - 例外版
    protected void devWarn(Exception e) {
        MakeBackup.getInstance().getLogManager().devWarn(e);
    }

    // タスクが準備済みか判定する
    public boolean isTaskPrepared() {
        return prepareTaskFuture != null && prepareTaskFuture.isDone();
    }
}
