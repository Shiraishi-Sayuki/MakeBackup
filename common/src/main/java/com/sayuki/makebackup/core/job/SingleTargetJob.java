/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;

import com.sayuki.makebackup.core.target.Target;

// シングルターゲットジョブインターフェース - 単一ストレージを扱うジョブ
public interface SingleTargetJob extends Job {
    // ストレージを取得する
    Target getStorage();
}
