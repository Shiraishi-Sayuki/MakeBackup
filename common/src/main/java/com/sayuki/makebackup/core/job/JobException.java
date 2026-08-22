/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;

import lombok.Getter;

// ジョブ例外クラス - ジョブ実行時のエラーを扱う
public class JobException extends Exception {

    @Getter
    private final Job task;
    @Getter
    private final Throwable exception;

    // コンストラクタ - タスクと例外で初期化する
    public JobException(Job task, Throwable exception) {
        super("An error occurred while executing task %s".formatted(task.getTaskName()));
        this.task = task;
        this.exception = exception;
        this.setStackTrace(exception.getStackTrace());
    }
}
