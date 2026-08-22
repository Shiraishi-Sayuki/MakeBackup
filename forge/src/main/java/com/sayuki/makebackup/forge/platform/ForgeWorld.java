/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.forge.platform;

import com.sayuki.makebackup.platform.ModWorld;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.nio.file.Path;

// Forgeワールドクラス - Forgeのワールドをラップする
public class ForgeWorld implements ModWorld {

    private final ServerLevel level;

    // 初期化する
    public ForgeWorld(ServerLevel level) {
        this.level = level;
    }

    @Override
    // ワールド名を取得する
    public String getName() {
        return level.dimension().location().getPath();
    }

    @Override
    // ワールドフォルダを取得する
    public File getWorldFolder() {
        MinecraftServer server = level.getServer();
        ResourceKey<Level> dimension = level.dimension();
        Path root = server.getWorldPath(LevelResource.ROOT);
        if (dimension == Level.OVERWORLD) return root.toFile();
        if (dimension == Level.NETHER) return root.resolve("DIM-1").toFile();
        if (dimension == Level.END) return root.resolve("DIM1").toFile();
        return root.resolve("dimensions").resolve(dimension.location().getNamespace()).resolve(dimension.location().getPath()).toFile();
    }

    @Override
    // 自動保存が有効か取得する
    public boolean isAutoSave() {

        return true;
    }

    @Override
    // 自動保存を設定する
    public void setAutoSave(boolean autoSave) {

    }
}
