/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target.support;

// 転送進捗リスナーインターフェース - 進捗通知を定義する
public interface TransferProgressListener {

    // 現在の進捗を取得する
    long getCurrentProgress();

    // 最大進捗を取得する
    long getMaxProgress();

    // 進捗を増やす
    void incrementProgress(long value);
}
