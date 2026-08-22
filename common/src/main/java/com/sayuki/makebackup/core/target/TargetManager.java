/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.target;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.sayuki.makebackup.MakeBackup;
import com.sayuki.makebackup.core.settings.LocalSettings;
import com.sayuki.makebackup.core.helper.UiHelper;
import com.sayuki.makebackup.command.CommandArgs;
import com.sayuki.makebackup.command.browse.BrowseCommand;
import com.sayuki.makebackup.platform.ModCommandSender;
import com.sayuki.makebackup.platform.ModPlayer;
import com.sayuki.makebackup.platform.Services;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

// ターゲットマネージャークラス - ストレージを管理する
public class TargetManager {

    private final HashMap<String, Target> storages = new HashMap<>();

    private final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    // コンストラクタ - 初期化する
    public TargetManager() {

        LocalSettings config = new LocalSettings();
        LocalTarget storage = new LocalTarget(config.load(config.getDefaultConfig(), "makebackup"));
        registerStorage("makebackup", storage);
    }

    // ストレージを登録する
    public void registerStorage(String id, Target storage) throws RuntimeException {
        if (storages.containsKey(id)) {
            throw new RuntimeException("\"%s\" id is already used for some storage".formatted(id));
        }
        storage.setId(id);
        storages.put(id, storage);
    }

    // ストレージを取得する
    public Target getStorage(String id) {
        return storages.get(id);
    }

    // ストレージ一覧を取得する
    public List<Target> getStorages() {
        return new ArrayList<>(storages.values().stream().filter(storage -> !storage.getId().equals("makebackup")).toList());
    }

    // サイズキャッシュを保存する
    public void saveSizeCache() {
        try {

            File sizeCachceFile = MakeBackup.getInstance().getConfigManager().getServerConfig().getSizeCacheFile();

            FileWriter writer = new FileWriter(sizeCachceFile);
            HashMap<String, ConcurrentMap<String, Long>> jsonedCache = new HashMap<>();
            for (Target storage : storages.values()) {
                jsonedCache.put(storage.getId(), storage.getBackupManager().getSizeCache());
            }

            String json = gson.toJson(jsonedCache);
            writer.write(json);
            writer.close();
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to save size cache to disk!");
            MakeBackup.getInstance().getLogManager().warn(e);
        }
    }

    // サイズキャッシュを読み込む
    public void loadSizeCache() {

        try {

            File sizeCacheFile = MakeBackup.getInstance().getConfigManager().getServerConfig().getSizeCacheFile();
            try {
                if (!sizeCacheFile.exists() && !sizeCacheFile.createNewFile()) {
                    MakeBackup.getInstance().getLogManager().warn("Unable to create %s file!".formatted(sizeCacheFile.getPath()));
                }
            } catch (Exception e) {
                MakeBackup.getInstance().getLogManager().warn("Unable to create %s file!".formatted(sizeCacheFile.getPath()));
            }

            FileReader reader = new FileReader(sizeCacheFile);
            StringBuilder json = new StringBuilder();
            char[] buffer = new char[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                json.append(new String(buffer, 0, length));
            }
            reader.close();

            String jsonString = json.toString();
            if (jsonString.isEmpty()) return;

            Type typeToken = new TypeToken<HashMap<String, HashMap<String, Long>>>() {}.getType();

            HashMap<String, HashMap<String, Long>> jsonedCache = gson.fromJson(jsonString, typeToken);

            for (Target storage : storages.values()) {
                if (!jsonedCache.containsKey(storage.getId())) {
                    continue;
                }
                storage.getBackupManager().loadSizeCache(jsonedCache.get(storage.getId()));
            }
        } catch (Exception e) {
            MakeBackup.getInstance().getLogManager().warn("Failed to load backups size cache");
            MakeBackup.getInstance().getLogManager().warn(e);
        }
    }

    // ストレージをインデックスする
    public void indexStorages() {
        for (Target storage : getStorages()) {
            MakeBackup.getInstance().getScheduleManager().runAsync(() -> {
                try {
                    MakeBackup.getInstance().getLogManager().devLog("Indexing %s storage...".formatted(storage.getId()));
                    new BrowseCommand(false, Services.PLATFORM.getConsoleSender(), new CommandArgs(
                            new HashMap<>(){{put("storage", storage.getId());}},
                            "/makebackup list %s".formatted(storage.getId())))
                            .execute();
                    MakeBackup.getInstance().getLogManager().devLog("%s storage indexing completed".formatted(storage.getId()));
                } catch (Exception e) {
                    MakeBackup.getInstance().getLogManager().warn("Failed to index storage %s".formatted(storage.getId()));
                    MakeBackup.getInstance().getLogManager().warn(e);
                }
            });
        }
    }

    // ストレージ接続をチェックする
    public void checkStoragesConnection() {
        for (Target storage : storages.values()) {
            if (storage.checkConnection()){
                MakeBackup.getInstance().getLogManager().log("Connection to %s storage established successfully".formatted(storage.getId()));
            } else {
                MakeBackup.getInstance().getLogManager().warn("Failed to establish connection to %s storage".formatted(storage.getId()));
            }
        }
        sendUserAuthStoragesCheckResult(Services.PLATFORM.getConsoleSender());
    }

    // 破棄する
    public void destroy() {
        for (Target storage : storages.values()) {
            storage.destroy();
        }
    }

    // 認証ストレージのチェック結果を送信する
    private void sendUserAuthStoragesCheckResult(ModCommandSender sender) {
        for (Target storage : MakeBackup.getInstance().getStorageManager().getStorages()) {
            if (!(storage instanceof UserAuthTarget)) continue;

            if (sender.isOp() && !storage.checkConnection()) {
                Component header = Component.empty();
                header = header
                        .append(Component.text("%s storage account".formatted(storage.getId()))
                                .decorate(TextDecoration.BOLD)
                                .color(NamedTextColor.RED));

                Component message = Component.empty();
                message = message
                        .append(Component.text("%s storage is enabled, but account is not linked!".formatted(storage.getId()))
                                .decorate(TextDecoration.BOLD)
                                .color(NamedTextColor.RED))
                        .append(Component.newline())
                        .append(Component.text("Use ")
                                .decorate(TextDecoration.BOLD)
                                .color(NamedTextColor.RED))
                        .append(Component.text("/makebackup account %s link".formatted(storage.getId()))
                                .decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.suggestCommand("/makebackup account %s link".formatted(storage.getId()))));

                UiHelper.sendFramedMessage(header, message, sender);
            }
        }
    }

    // プレイヤー参加時に呼ばれる - 認証チェックを送る
    public void onPlayerJoin(ModPlayer player) {
        sendUserAuthStoragesCheckResult(player);
    }
}
