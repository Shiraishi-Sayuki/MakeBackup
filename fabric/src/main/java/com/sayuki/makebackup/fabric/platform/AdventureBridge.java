/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.fabric.platform;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

// Adventureブリッジクラス - AdventureとMinecraftのコンポーネントを変換する
public class AdventureBridge {

    // インスタンス化を禁止する
    private AdventureBridge() {
    }

    // Minecraftコンポーネントに変換する
    public static net.minecraft.network.chat.Component toMinecraft(Component component) {
        return net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(component));
    }

    // Adventureコンポーネントに変換する
    public static Component toAdventure(net.minecraft.network.chat.Component component) {
        return GsonComponentSerializer.gson().deserializeFromTree(net.minecraft.network.chat.Component.Serializer.toJsonTree(component));
    }

    // プレーンテキストに変換する
    public static String toPlain(Component component) {
        return com.sayuki.makebackup.core.helper.ComponentUtils.toPlainText(component);
    }
}
