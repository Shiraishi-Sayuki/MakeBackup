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

// ワールド読み取り専用設定ジョブクラス - ワールドを読み取り専用にする
public class SetWorldsReadOnlyJob extends BaseJob {

    private final boolean force;

    // コンストラクタ - 強制フラグで初期化する
    public SetWorldsReadOnlyJob(boolean force) {
        super();
        this.force = force;
    }

    // コンストラクタ - デフォルトで初期化する
    public SetWorldsReadOnlyJob() {
        super();
        this.force = false;
    }

    @Override
    // 実行する - ワールドを読み取り専用に設定する
    public void run() {

        if (!MakeBackup.getInstance().getConfigManager().getBackupConfig().isSetWorldsReadOnly() && !force) {
            return;
        }

        for (ModWorld world : Services.PLATFORM.getWorlds()) {
            if (!Helper.errorSetWritable) {
                Helper.isAutoSaveEnabled.put(world.getName(), world.isAutoSave());
            }

            world.setAutoSave(false);
            if (!world.getWorldFolder().setReadOnly()) {
                warn("Can not set folder read only!", sender);
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
