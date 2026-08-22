/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.settings;

// ターゲット設定インターフェース - ストレージ共通の設定を定義する
public interface TargetSettings extends Settings {

    // IDを取得する
    String getId();

    // 有効かどうか判定する
    boolean isEnabled();

    // 自動バックアップするか判定する
    boolean isAutoBackup();

    // ZIP圧縮レベルを取得する
    int getZipCompressionLevel();

    // ZIPアーカイブするか判定する
    boolean isZipArchive();

    // バックアップフォルダを取得する
    String getBackupsFolder();

    // バックアップ数を取得する
    int getBackupsNumber();

    // バックアップ容量を取得する
    long getBackupsWeight();
}
