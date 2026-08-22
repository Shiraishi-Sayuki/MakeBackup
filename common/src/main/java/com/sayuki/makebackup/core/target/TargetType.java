/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;

// ターゲットタイプ列挙 - ストレージの種類を定義する
public enum TargetType {
    LOCAL,
    FTP,
    SFTP,
    GOOGLE_DRIVE,
    NULL
}
