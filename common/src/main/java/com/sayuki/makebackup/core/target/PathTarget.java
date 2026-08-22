/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;

import com.sayuki.makebackup.core.settings.PathTargetSettings;
import com.sayuki.makebackup.core.target.error.TargetConnectionException;
import com.sayuki.makebackup.core.target.error.TargetMethodException;

// パスターゲットインターフェース - パス操作を定義する
public interface PathTarget extends Target {

    // 設定を取得する
    PathTargetSettings getConfig();

    @Override
    // パスからファイル名を取得する
    default String getFileNameFromPath(String path) throws TargetMethodException, TargetConnectionException {
        return path.substring(path.lastIndexOf(getConfig().getPathSeparatorSymbol()) + 1);
    }

    @Override
    // 親パスを取得する
    default String getParentPath(String path) throws TargetMethodException, TargetConnectionException {
        return path.substring(0, path.lastIndexOf(getConfig().getPathSeparatorSymbol()) == -1 ? 0 : path.lastIndexOf(getConfig().getPathSeparatorSymbol()));
    }

    @Override
    // パスを解決する
    default String resolve(String path, String fileName) {
        if (!path.endsWith(getConfig().getPathSeparatorSymbol())) {
            path = "%s%s".formatted(path, getConfig().getPathSeparatorSymbol());
        }
        return "%s%s".formatted(path, fileName);
    }
}
