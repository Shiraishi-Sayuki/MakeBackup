/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.fabric.platform;

import com.sayuki.makebackup.command.ModCommandTree;
import com.sayuki.makebackup.platform.IPlatformHelper;
import com.sayuki.makebackup.platform.ModCommandSender;
import com.sayuki.makebackup.platform.ModPlayer;
import com.sayuki.makebackup.platform.ModWorld;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Fabricプラットフォームヘルパークラス - Fabric固有の処理を提供する
public class FabricPlatformHelper implements IPlatformHelper {

    public static final FabricPlatformHelper INSTANCE = new FabricPlatformHelper();

    private volatile MinecraftServer server;
    private final List<ModCommandTree> trees = new ArrayList<>();

    // サーバーを設定する
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    // サーバーを設定する
    public void setServer(Object server) {
        this.server = (MinecraftServer) server;
    }

    // サーバーを取得する
    public MinecraftServer getServer() {
        return server;
    }

    // コマンドツリーを取得する
    public List<ModCommandTree> getTrees() {
        return trees;
    }

    @Override
    // コマンドツリーを取得する
    public List<ModCommandTree> getCommandTrees() {
        return trees;
    }

    @Override
    // プラットフォーム名を取得する
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    // Modが読み込まれているか確認する
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    // 開発環境か確認する
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    // コンソール送信者を取得する
    public ModCommandSender getConsoleSender() {
        return FabricSender.console();
    }

    @Override
    // コンフィグディレクトリを取得する
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    // ワールド一覧を取得する
    public List<ModWorld> getWorlds() {
        MinecraftServer s = server;
        if (s == null) return List.of();
        List<ModWorld> worlds = new ArrayList<>();
        for (ServerLevel level : s.getAllLevels()) {
            worlds.add(new FabricWorld(level));
        }
        return worlds;
    }

    @Override
    // レベルディレクトリを取得する
    public Path getLevelDirectory() {
        MinecraftServer s = server;
        if (s == null) return Path.of(".");
        return s.getWorldPath(LevelResource.ROOT);
    }

    @Override
    // オンラインプレイヤーを取得する
    public List<ModPlayer> getOnlinePlayers() {
        MinecraftServer s = server;
        if (s == null) return List.of();
        List<ModPlayer> players = new ArrayList<>();
        for (var player : s.getPlayerList().getPlayers()) {
            players.add(new FabricPlayer(player));
        }
        return players;
    }

    @Override
    // サーバーを停止する
    public void stopServer(boolean restart) {
        MinecraftServer s = server;
        if (s == null) return;
        if (restart) {
            com.sayuki.makebackup.core.ModLogger.getLogger().warn("Server restart is not supported by the mod loader, stopping the server instead. Use a wrapper/manager to restart automatically.");
        }
        s.halt(false);
    }

    @Override
    // コマンドを登録する
    public void registerCommands(List<ModCommandTree> trees) {
        this.trees.clear();
        this.trees.addAll(trees);
    }
}
