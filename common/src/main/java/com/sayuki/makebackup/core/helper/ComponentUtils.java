/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.helper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

// コンポーネントユーティルクラス - Component操作を補助する
public class ComponentUtils {

    // コンストラクタ - インスタンス化を防ぐ
    private ComponentUtils() {
    }

    // プレーンテキストに変換する
    public static String toPlainText(Component component) {
        if (component == null) return "";
        StringBuilder builder = new StringBuilder();
        append(builder, component);
        return builder.toString();
    }

    // 追加する - Componentの内容をビルダーに詰める
    private static void append(StringBuilder builder, Component component) {
        if (component instanceof TextComponent textComponent) {
            builder.append(textComponent.content());
        }
        for (Component child : component.children()) {
            append(builder, child);
        }
    }
}
