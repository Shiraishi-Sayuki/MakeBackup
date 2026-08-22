/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.forge;

import com.sayuki.makebackup.core.Metrics;
import com.sayuki.makebackup.core.MetricsReporter;
import com.sayuki.makebackup.core.MetricsService;

import java.util.List;

// ForgeBstatsサービスクラス - bStatsの初期化をする
public class ForgeBstatsService implements MetricsService {

    private final MetricsReporter reporter = new MetricsReporter();

    @Override
    // bStatsを初期化する
    public void initBstats(List<Metrics.ChartSpec> chartSpecs) {
        reporter.initBstats(chartSpecs);
    }

    @Override
    // 破棄する
    public void destroy() {
        reporter.destroy();
    }
}
