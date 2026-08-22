/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target.support;

import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;

// 基本転送進捗リスナークラス - 転送進捗を記録する
public class BasicTransferProgressListener implements TransferProgressListener {

    final AtomicLong progress = new AtomicLong(0);
    @Setter
    long maxProgress = 0;

    @Override
    // 現在の進捗を取得する
    public long getCurrentProgress() {
        return progress.get();
    }

    @Override
    // 最大進捗を取得する
    public long getMaxProgress() {
        return maxProgress;
    }

    @Override
    // 進捗を増やす
    public void incrementProgress(long value) {
        progress.addAndGet(value);
    }
}
