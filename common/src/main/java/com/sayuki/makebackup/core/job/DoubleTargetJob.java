/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;

import com.sayuki.makebackup.core.target.Target;

// ダブルターゲットジョブインターフェース - 2つのストレージ間で処理するジョブ
public interface DoubleTargetJob extends Job {

    // 転送元ストレージを取得する
    Target getSourceStorage();

    // 転送先ストレージを取得する
    Target getTargetStorage();
}
