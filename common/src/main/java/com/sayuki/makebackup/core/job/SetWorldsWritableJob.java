/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.job;
import com.sayuki.makebackup.platform.ModCommandSender;

import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.helper.Helper;
import com.sayuki.makebackup.platform.ModWorld;
import com.sayuki.makebackup.platform.Services;

// ワールド書き込み可能設定ジョブクラス - ワールドを書き込み可能に戻す
public class SetWorldsWritableJob extends BaseJob {

    private final boolean force;

    // コンストラクタ - 強制フラグで初期化する
    public SetWorldsWritableJob(boolean force) {
        super();
        this.force = force;
    }

    // コンストラクタ - デフォルトで初期化する
    public SetWorldsWritableJob() {
        super();
        this.force = false;
    }

    @Override
    // 実行する - ワールドを書き込み可能に設定する
    public void run() {

        if (!MakeBackup.getInstance().getConfigManager().getBackupConfig().isSetWorldsReadOnly() && !force) {
            return;
        }

        Helper.errorSetWritable = false;

        for (ModWorld world : Services.PLATFORM.getWorlds()) {

            if (!world.getWorldFolder().setWritable(true)) {
                warn("Can not set %s writable!".formatted(world.getWorldFolder().getPath()), sender);
                Helper.errorSetWritable = true;
            }

            if (Helper.isAutoSaveEnabled.containsKey(world.getName())) {
                world.setAutoSave(force || Helper.isAutoSaveEnabled.get(world.getName()));
            }
        }
    }

    @Override
    // タスクを準備する - 特に何もしない
    public void prepareTask(ModCommandSender sender) {
    }

    @Override
    // キャンセルする
    public void cancel() {
        cancelled = true;
    }
}
