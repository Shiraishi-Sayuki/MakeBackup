/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.FtpTarget;
import com.sayuki.makebackup.core.target.GoogleDriveTarget;
import com.sayuki.makebackup.core.target.LocalTarget;
import com.sayuki.makebackup.core.target.SftpTarget;

import java.util.List;
import java.util.function.Supplier;

// メトリクスクラス - bStatsのチャートを管理する
public class Metrics {

    private final int bStatsId = 17735;

    // チャート仕様レコード - IDと値を保持する
    public record ChartSpec(String id, Supplier<String> value) {
    }

    // 初期化する - bStatsをセットアップする
    public void init() {
        MakeBackup.getInstance().getLogManager().log("Initializing BStats...");
        MetricsService.INSTANCE.initBstats(getChartSpecs());
        MakeBackup.getInstance().getLogManager().log("BStats initialization completed");
    }

    // 破棄する - リソースを解放する
    public void destroy() {
        MetricsService.INSTANCE.destroy();
    }

    // チャート仕様を取得する - 各ストレージタイプの数を集計する
    public List<ChartSpec> getChartSpecs() {
        return List.of(
                new ChartSpec("local_storages_amount", () -> String.valueOf(MakeBackup.getInstance().getStorageManager().getStorages().stream().filter(storage -> storage instanceof LocalTarget).count())),
                new ChartSpec("ftp_storages_amount", () -> String.valueOf(MakeBackup.getInstance().getStorageManager().getStorages().stream().filter(storage -> storage instanceof FtpTarget).count())),
                new ChartSpec("sftp_storages_amount", () -> String.valueOf(MakeBackup.getInstance().getStorageManager().getStorages().stream().filter(storage -> storage instanceof SftpTarget).count())),
                new ChartSpec("google_drive_storages_amount", () -> String.valueOf(MakeBackup.getInstance().getStorageManager().getStorages().stream().filter(storage -> storage instanceof GoogleDriveTarget).count()))
        );
    }
}
