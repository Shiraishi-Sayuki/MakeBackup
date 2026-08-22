/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.sayuki.makebackup.core.snapshot.SnapshotManager;
import com.sayuki.makebackup.core.settings.TargetSettings;
import com.sayuki.makebackup.core.target.error.TargetConnectionException;
import com.sayuki.makebackup.core.target.error.TargetLimitException;
import com.sayuki.makebackup.core.target.error.TargetMethodException;
import com.sayuki.makebackup.core.target.support.BasicTransferProgressListener;
import com.sayuki.makebackup.core.target.support.TransferProgressListener;

import java.io.InputStream;
import java.util.List;

// ターゲットインターフェース - ストレージ操作を定義する
public interface Target {

    // IDを設定する
    void setId(String id);

    // IDを取得する
    String getId();

    // タイプを取得する
    TargetType getType();

    // コンフィグを取得する
    TargetSettings getConfig();

    // バックアップマネージャーを取得する
    SnapshotManager getBackupManager();

    // 接続をチェックする
    boolean checkConnection();

    // 接続をチェックする - 送信者付き
    boolean checkConnection(ModCommandSender sender);

    // 一覧を取得する
    List<String> ls(String path) throws TargetMethodException, TargetConnectionException;

    // パスを解決する
    String resolve(String path, String fileName) throws TargetMethodException;

    // 存在するか判定する
    boolean exists(String path) throws TargetMethodException, TargetConnectionException;

    // ファイルか判定する
    boolean isFile(String path) throws TargetMethodException, TargetConnectionException;

    // ディレクトリか判定する
    default boolean isDir(String path) throws TargetMethodException, TargetConnectionException {
        return !isFile(path);
    }

    // パスからファイル名を取得する
    String getFileNameFromPath(String path) throws TargetMethodException, TargetConnectionException;

    // 親パスを取得する
    String getParentPath(String path) throws TargetMethodException, TargetConnectionException;

    // ディレクトリのバイトサイズを取得する
    long getDirByteSize(String path) throws TargetMethodException, TargetConnectionException;

    // ディレクトリを作成する
    void createDir(String newDirName, String parentDir) throws TargetLimitException, TargetMethodException, TargetConnectionException;

    // ファイルをアップロードする - 進捗リスナー付き
    void uploadFile(InputStream sourceStream, String newFileName, String targetParentDir, TransferProgressListener progressListener) throws TargetLimitException, TargetMethodException, TargetConnectionException;

    // ファイルをアップロードする - シンプル版
    default void uploadFile(InputStream sourceStream, String newFileName, String targetParentDir) throws TargetLimitException, TargetMethodException, TargetConnectionException {
        uploadFile(sourceStream, newFileName, targetParentDir, new BasicTransferProgressListener());
    }

    // ファイルをダウンロードする - 進捗リスナー付き
    InputStream downloadFile(String sourcePath, TransferProgressListener progressListener) throws TargetMethodException, TargetConnectionException;

    // ファイルをダウンロードする - シンプル版
    default InputStream downloadFile(String sourcePath) throws TargetMethodException, TargetConnectionException {
        return downloadFile(sourcePath, new BasicTransferProgressListener());
    }

    // ダウンロード完了を通知する
    void downloadCompleted() throws TargetMethodException, TargetConnectionException;

    // 削除する
    void delete(String path) throws TargetMethodException, TargetConnectionException;

    // ファイル名を変更する
    void renameFile(String path, String newFileName) throws TargetMethodException, TargetConnectionException;

    // ストレージ速度倍率を取得する
    int getStorageSpeedMultiplier();

    // 削除進捗倍率を取得する
    default int getDeleteProgressMultiplier() {
        return getStorageSpeedMultiplier();
    }

    // 転送進捗倍率を取得する
    default int getTransferProgressMultiplier() {
        return getStorageSpeedMultiplier() * 5;
    }

    // ZIP進捗倍率を取得する
    default int getZipProgressMultiplier() {
        return getStorageSpeedMultiplier() * 10;
    }

    // 破棄する
    void destroy();

}
