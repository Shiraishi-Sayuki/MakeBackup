/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.platform;

import com.sayuki.makebackup.core.ModLogger;

import java.util.ServiceLoader;

// サービスクラス - ServiceLoaderでプラットフォーム実装を取得する
public class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    // サービスを読み込む - 指定クラスの実装を取得する
    public static <T> T load(Class<T> clazz) {

        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        ModLogger.getLogger().debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
