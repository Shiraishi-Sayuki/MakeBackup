/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.helper;

import com.sayuki.makebackup.platform.ModCommandSender;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.target.LocalTarget;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

// ヘルパークラス - 汎用的な便利処理をまとめる
public class Helper {

    public static final Properties properties = new Properties();

    public static boolean errorSetWritable = false;
    public static volatile HashMap<String, Boolean> isAutoSaveEnabled = new HashMap<>();

    static {
        try {
            properties.load(Helper.class.getClassLoader().getResourceAsStream("project.properties"));
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().devWarn("Failed to load properties!");
            MakeBackup.getInstance().getLogManager().warn(e);
        }
    }

    public static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    public static boolean isFolia = false;

    // プロパティを取得する
    public static String getProperty(String property) {
        return properties.getProperty(property);
    }

    // ファイル/フォルダのバイトサイズを取得する
    public static long getFileFolderByteSize(File path) {

        if (!path.exists()) {
            return 0;
        }

        if (!path.isDirectory()) {
            try {
                return Files.size(path.toPath());
            } catch (Exception e) {
                MakeBackup.getInstance().getLogManager().warn("Something went wrong while trying to calculate file size!");
                MakeBackup.getInstance().getLogManager().warn(e);
                return 0;
            }
        }

        long size = 0;

        if (path.isDirectory()) {
            for (File file : Objects.requireNonNull(path.listFiles())) {
                size += getFileFolderByteSize(file);
            }
        }

        return size;
    }

    // 除外対象を除いたバイトサイズを取得する
    public static long getFileFolderByteSizeExceptExcluded(File path) {
        if (!path.exists() || Helper.isExcludedDirectory(path, null)) return 0;

        if (!path.isDirectory()) {
            try {
                return Files.size(path.toPath());
            } catch (Exception e) {
                MakeBackup.getInstance().getLogManager().warn("Something went wrong while trying to calculate backup size!");
                MakeBackup.getInstance().getLogManager().warn(e);
                return 0;
            }
        }

        long size = 0;
        if (path.isDirectory()) {
            for (File file : Objects.requireNonNull(path.listFiles())) {
                size += getFileFolderByteSizeExceptExcluded(file);
            }
        }
        return size;
    }

    // 除外ディレクトリかどうか判定する
    public static boolean isExcludedDirectory(File path, ModCommandSender sender) {
        if (!path.exists()) return true;

        boolean isExcludedDirectory = false;
        try {
            Path normalizedPath =  path.toPath().toAbsolutePath().normalize();
            List<LocalTarget> localStorages = MakeBackup.getInstance().getStorageManager().getStorages().stream()
                    .filter(storage -> storage instanceof LocalTarget).map(storage -> (LocalTarget) storage).toList();
            List<Path> normalizedBackupFolderPaths = localStorages.stream()
                    .map(storage -> new File(storage.getConfig().getBackupsFolder()).toPath().toAbsolutePath().normalize())
                    .toList();

            if (localStorages.stream().anyMatch(storage -> path.toPath().startsWith(new File(storage.getConfig().getBackupsFolder()).toPath())) ||
                    normalizedBackupFolderPaths.stream().anyMatch(normalizedPath::startsWith) ||
                    localStorages.stream().anyMatch(storage -> !Helper.isWindows && path.toPath().startsWith(new File("./%s".formatted(storage.getConfig().getBackupsFolder())).toPath())) ||
                    localStorages.stream().anyMatch(storage -> Helper.isWindows && path.toPath().startsWith(new File(storage.getConfig().getBackupsFolder()).toPath())) ||
                    localStorages.stream().anyMatch(storage -> Helper.isWindows && storage.getConfig().getBackupsFolder().charAt(1) != ':' &&
                            path.toPath().startsWith(new File(".\\%s".formatted(storage.getConfig().getBackupsFolder())).toPath()))) {
                return true;
            }
        } catch (SecurityException e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to copy file \"%s\", no access".formatted(path.getAbsolutePath()), sender);
            MakeBackup.getInstance().getLogManager().warn(e);
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Something went wrong while trying to copy file \"%s\"".formatted(path.getAbsolutePath()), sender);
            MakeBackup.getInstance().getLogManager().warn(e);
        }

        for (String excludeDirectoryFromBackup : MakeBackup.getInstance().getConfigManager().getBackupConfig().getExcludeDirectoryFromBackup()) {
            try {

                File excludeDirectoryFromBackupFile = Paths.get(excludeDirectoryFromBackup).toFile().getCanonicalFile();
                if (path.getCanonicalFile().toPath().startsWith(excludeDirectoryFromBackupFile.toPath())) {
                    isExcludedDirectory = true;
                }
            } catch (SecurityException e) {
                MakeBackup.getInstance().getLogManager().warn("Failed to copy file \"%s\", no access".formatted(path.getAbsolutePath()), sender);
                MakeBackup.getInstance().getLogManager().warn(e);
                return true;
            } catch (Exception e) {
                MakeBackup.getInstance().getLogManager().warn("Something went wrong while trying to copy file \"%s\"".formatted(path.getAbsolutePath()), sender);
                MakeBackup.getInstance().getLogManager().warn(e);
                return true;
            }
        }

        return isExcludedDirectory;
    }
}
