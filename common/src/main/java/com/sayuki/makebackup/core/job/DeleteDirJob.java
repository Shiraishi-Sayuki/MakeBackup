/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.jcraft.jsch.SftpException;
import com.sayuki.makebackup.core.target.Target;

// ディレクトリ削除ジョブクラス - ディレクトリを再帰的に削除する
public class DeleteDirJob extends BaseJob implements SingleTargetJob {

    private final Target storage;
    private final String path;

    // コンストラクタ - ストレージとパスで初期化する
    public DeleteDirJob(Target storage, String path) {
        this.storage = storage;
        this.path = path;
    }

    @Override
    // 実行する
    public void run() {
        if (!cancelled) {
            deleteDir(path);
        }
    }

    @Override
    // タスクを準備する - サイズを計算する
    public void prepareTask(ModCommandSender sender) throws SftpException {
        if (maxProgress != 0) return;
        maxProgress = storage.getDirByteSize(path);
    }

    // ディレクトリを削除する - 再帰的に処理する
    private void deleteDir(String currentPath) {

        if (cancelled) return;
        try {
            if (storage.isDir(currentPath)) {
                for (String file : storage.ls(currentPath)) {
                    deleteDir(storage.resolve(currentPath, file));
                }
                storage.delete(currentPath);
            } else {

                long fileSize = storage.getDirByteSize(currentPath);
                storage.delete(currentPath);
                incrementCurrentProgress(fileSize);
            }
        } catch (Exception e) {
            warn("Something went while trying to delete %s directory from %s storage".formatted(currentPath, storage.getId()), sender);
            warn(e);
        }
    }

    @Override
    // キャンセルする
    public void cancel() {
        cancelled = true;
    }

    @Override
    // ストレージを取得する
    public Target getStorage() {
        return storage;
    }
}
