/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;
import com.sayuki.makebackup.platform.ModCommandSender;

import lombok.Setter;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.snapshot.SnapshotManager;
import com.sayuki.makebackup.core.settings.LocalSettings;
import com.sayuki.makebackup.core.target.error.TargetConnectionException;
import com.sayuki.makebackup.core.target.error.TargetLimitException;
import com.sayuki.makebackup.core.target.error.TargetMethodException;
import com.sayuki.makebackup.core.target.support.TransferProgressInputStream;
import com.sayuki.makebackup.core.target.support.TransferProgressListener;
import com.sayuki.makebackup.core.helper.Helper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

// ローカルターゲットクラス - ローカルストレージを操作する
public class LocalTarget implements PathTarget {

    @Setter
    private String id = null;
    private final SnapshotManager backupManager;
    private final LocalSettings config;
    private final TargetTraceLogger protocolLogger;

    private final int FILE_BUFFER_SIZE = 65536;

    // コンストラクタ - ローカル設定で初期化する
    public LocalTarget(LocalSettings config) {
        this.config = config;
        this.backupManager = new SnapshotManager(this);
        this.protocolLogger = config.isProtocolLogging() ? new TargetTraceLogger(config.getId()) : null;
    }

    @Override
    // IDを取得する
    public String getId() {
        return this.id;
    }

    @Override
    // タイプを取得する
    public TargetType getType() {
        return TargetType.LOCAL;
    }

    @Override
    // 設定を取得する
    public LocalSettings getConfig() {
        return config;
    }

    @Override
    // バックアップマネージャーを取得する
    public SnapshotManager getBackupManager() {
        return backupManager;
    }

    @Override
    // 接続をチェックする
    public boolean checkConnection() {
        return checkConnection(null);
    }

    @Override
    // 接続をチェックする（送信者付き）
    public boolean checkConnection(ModCommandSender sender) {
        try {
            if (!config.isEnabled()) {
                MakeBackup.getInstance().getLogManager().warn("Local storage is disabled in config.yml", sender);
                return false;
            }

            File folder = new File(config.getBackupsFolder());
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    MakeBackup.getInstance().getLogManager().warn("Failed to create local backups folder: %s".formatted(config.getBackupsFolder()), sender);
                    return false;
                }
            }

            if (!folder.isDirectory()) {
                MakeBackup.getInstance().getLogManager().warn("Local backups folder is not a directory: %s".formatted(config.getBackupsFolder()), sender);
                return false;
            }

            if (!folder.canRead() || !folder.canWrite()) {
                MakeBackup.getInstance().getLogManager().warn("Local backups folder is not accessible (read/write permissions): %s".formatted(config.getBackupsFolder()), sender);
                return false;
            }

            return true;
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to check local storage connection", sender);
            MakeBackup.getInstance().getLogManager().warn(e);
            return false;
        }
    }

    @Override
    // 一覧を取得する
    public List<String> ls(String path) throws TargetMethodException, TargetConnectionException {
        try {
            File directory = new File(path);
            logOperation("LS", path);
            if (!directory.exists() || !directory.isDirectory()) {
                throw new TargetMethodException(this, "Directory does not exist or is not a directory: %s".formatted(path));
            }

            File[] files = directory.listFiles();
            if (files == null) {
                throw new TargetMethodException(this, "Failed to list files in directory: %s".formatted(path));
            }

            List<String> fileNames = new ArrayList<>();
            for (File file : files) {
                fileNames.add(file.getName());
            }
            return fileNames;
        } catch (Exception e) {
            throw new TargetMethodException(this, "Failed to get file list from dir \"%s\" using local storage".formatted(path), e);
        }
    }

    @Override
    // 存在を確認する
    public boolean exists(String path) throws TargetMethodException, TargetConnectionException {
        logOperation("EXISTS", path);
        return new File(path).exists();
    }

    @Override
    // ファイルか確認する
    public boolean isFile(String path) throws TargetMethodException, TargetConnectionException {
        File file = new File(path);
        logOperation("STAT", path);
        if (!file.exists()) {
            throw new TargetMethodException(this, "File \"%s\" does not exist".formatted(path));
        }
        return file.isFile();
    }

    @Override
    // ディレクトリサイズを取得する
    public long getDirByteSize(String path) throws TargetMethodException, TargetConnectionException {
        try {
            File file = new File(path);
            logOperation("SIZE", path);
            if (!file.exists()) {
                throw new TargetMethodException(this, "File or directory does not exist: %s".formatted(path));
            }

            return Helper.getFileFolderByteSize(file);
        } catch (Exception e) {
            throw new TargetMethodException(this, "Failed to get \"%s\" dir size using local storage".formatted(path), e);
        }
    }

    @Override
    // ディレクトリを作成する
    public void createDir(String newDirName, String parentDir) throws TargetMethodException, TargetConnectionException {
        try {
            File folder = new File(resolve(parentDir, newDirName));
            logOperation("MKDIR", folder.getAbsolutePath());
            if (folder.exists()) {
                if (!folder.isDirectory()) {
                    throw new TargetMethodException(this, "Path exists but is not a directory: %s".formatted(parentDir));
                }
                return;
            }

            if (!folder.mkdirs()) {
                throw new TargetMethodException(this, "Failed to create directory: %s".formatted(parentDir));
            }
            if (!folder.exists() || !folder.isDirectory()) {
                throw new TargetMethodException(this, "Directory creation verification failed: %s".formatted(folder.getAbsolutePath()));
            }
        } catch (Exception e) {
            throw new TargetMethodException(this, "Failed to create dir \"%s\" using local storage".formatted(parentDir), e);
        }
    }

    @Override
    // ファイルをアップロードする
    public void uploadFile(InputStream sourceStream, String newFileName, String targetParentDir, TransferProgressListener progressListener) throws TargetLimitException, TargetMethodException, TargetConnectionException {
        File target = new File(resolve(targetParentDir, newFileName));
        logOperation("UPLOAD", target.getAbsolutePath());

        try (OutputStream targetStream = new FileOutputStream(target)) {
            byte[] buffer = new byte[FILE_BUFFER_SIZE];
            int read;
            while ((read = sourceStream.read(buffer)) != -1) {
                targetStream.write(buffer, 0, read);
                progressListener.incrementProgress(read);
            }
        } catch (IOException e) {
            throw new TargetMethodException(this, "Failed to copy stream to \"%s\" in %s storage".formatted(target.getAbsolutePath(), id), e);
        }

        if (!target.exists() || !target.isFile()) {
            throw new TargetMethodException(this, "Upload verification failed: %s".formatted(target.getAbsolutePath()));
        }
    }

    @Override
    // ファイルをダウンロードする
    public InputStream downloadFile(String sourcePath, TransferProgressListener progressListener) throws TargetMethodException, TargetConnectionException {
        File file = new File(sourcePath);
        logOperation("DOWNLOAD", sourcePath);
        if (!file.exists()) {
            throw new TargetMethodException(this, "Source file \"%s\" does not exist".formatted(sourcePath));
        }

        try {
            return new TransferProgressInputStream(new FileInputStream(file), progressListener);
        } catch (IOException e) {
            throw new TargetMethodException(this, "Failed to get file's \"%s\" input stream from \"%s\" storage".formatted(sourcePath, id), e);
        }
    }

    @Override
    // 削除する
    public void delete(String path) throws TargetMethodException, TargetConnectionException {
        File file = new File(path);
        logOperation("DELETE", path);
        if (!file.delete()) {
            throw new TargetMethodException(this, "Failed to delete \"%s\" file/dir from local storage".formatted(path));
        }
        if (file.exists()) {
            throw new TargetMethodException(this, "Delete verification failed: %s".formatted(path));
        }
    }

    @Override
    // ファイル名を変更する
    public void renameFile(String path, String newFileName) throws TargetMethodException, TargetConnectionException {
        File sourceFile = new File(path);
        File targetFile = new File(resolve(getParentPath(path), newFileName));
        logOperation("RENAME", "%s -> %s".formatted(sourceFile.getAbsolutePath(), targetFile.getAbsolutePath()));

        if (!sourceFile.exists())
            throw new TargetMethodException(this, "Source file does not exist: %s".formatted(path));
        if (targetFile.exists())
            throw new TargetMethodException(this, "Target file already exists: %s".formatted(newFileName));

        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new TargetMethodException(this, "Failed to create parent directory for: %s".formatted(newFileName));
            }
        }

        for (int i = 0; i < 1000000; i++) {
            try {
                if (!sourceFile.renameTo(targetFile)) {

                    Files.move(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                if (!targetFile.exists() || sourceFile.exists()) {
                    throw new TargetMethodException(this, "Rename verification failed from \"%s\" to \"%s\" using local storage".formatted(path, newFileName));
                }
                break;
            } catch (Exception e) {
                if (i == 999999) throw new TargetMethodException(this, "Failed to rename file \"%s\" to \"%s\" using local storage".formatted(path, newFileName), e);
            }
        }
    }

    @Override
    // 速度係数を取得する
    public int getStorageSpeedMultiplier() {
        return 1;
    }

    @Override
    // 破棄する
    public void destroy() {
        if (protocolLogger != null) {
            protocolLogger.close();
        }
    }

    @Override
    // ダウンロード完了を処理する
    public void downloadCompleted() throws TargetMethodException, TargetConnectionException {

    }

    // 操作をログ出力する
    private void logOperation(String operation, String message) {
        if (protocolLogger != null) {
            protocolLogger.logOperation(operation, message);
        }
    }
}
