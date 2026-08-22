/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

// ジョブインターフェース - 全ジョブの共通操作を定義する
public interface Job {

    // タスク名を取得する
    String getTaskName();

    // 進捗率を取得する
    long getTaskPercentProgress();

    // 現在の進捗を取得する
    long getTaskCurrentProgress();

    // 最大進捗を取得する
    long getTaskMaxProgress();

    // 現在の進捗を増やす
    void incrementCurrentProgress(long progress);

    // キャンセル済みか判定する
    boolean isCancelled();

    // 実行する
    void run() throws IOException, JSchException, SftpException;

    // タスクを準備する
    void prepareTask(ModCommandSender sender) throws Throwable;

    // 開始する
    void start(ModCommandSender sender) throws JobException;

    // キャンセルする
    void cancel();

    // 準備タスクのFutureを設定する
    void setPrepareTaskFuture(CompletableFuture<Void> future);

    // 準備タスクのFutureを取得する
    CompletableFuture<Void> getPrepareTaskFuture();

    // タスクのFutureを設定する
    void setTaskFuture(CompletableFuture<Void> future);

    // タスクのFutureを取得する
    CompletableFuture<Void> getTaskFuture();

    // タスクが準備済みか判定する
    boolean isTaskPrepared();
}
