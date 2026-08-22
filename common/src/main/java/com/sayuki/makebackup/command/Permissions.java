/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command;

import lombok.Getter;
import com.sayuki.makebackup.core.target.Target;

// 権限列挙 - 各操作に必要な権限を定義する
@Getter
public enum Permissions {

    BACKUPER("makebackup"),

    STOP("makebackup.backup.stop"),
    RESTART("makebackup.backup.restart"),
    ALERT("makebackup.backup_alert"),

    STORAGE("makebackup.%s"),
    BACKUP("makebackup.%s.backup"),
    TO_ZIP("makebackup.%s.list.tozip"),
    UNZIP("makebackup.%s.list.unzip"),
    DELETE("makebackup.%s.list.delete"),
    ACCOUNT("makebackup.%s.account"),

    CONFIG("makebackup.config"),
    CONFIG_RELOAD("makebackup.config.reload"),

    STATUS("makebackup.status");

    private final String permission;

    // コンストラクタ - 権限文字列で初期化する
    Permissions(String permission) {
        this.permission = permission;
    }

    // 権限を取得する - ストレージIDでフォーマットする
    public String getPermission(Target storage) {
        return this.permission.formatted(storage.getId());
    }
}
