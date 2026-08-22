/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core;

import java.util.List;

// メトリクスサービスインターフェース - bStats連携を抽象化する
public interface MetricsService {

    MetricsService INSTANCE = com.sayuki.makebackup.platform.Services.load(MetricsService.class);

    // bStatsを初期化する
    void initBstats(List<Metrics.ChartSpec> chartSpecs);

    // 破棄する
    void destroy();
}
