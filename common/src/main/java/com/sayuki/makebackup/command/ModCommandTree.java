/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command;

import com.sayuki.makebackup.platform.ModCommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// コマンドツリークラス - コマンド構造を定義する
public class ModCommandTree {

    @FunctionalInterface
    // 実行インターフェース - コマンド実行処理を定義する
    public interface Executor {

        // 実行する
        void execute(ModCommandSender sender, CommandArgs args);
    }

    @FunctionalInterface
    // サジェストプロバイダインターフェース - 補完候補を提供する
    public interface SuggestionProvider {

        // サジェストを取得する
        List<String> getSuggestions(SuggestionContext context);
    }

    // サジェストコンテキストレコード - 補完時の情報を保持する
    public record SuggestionContext(ModCommandSender sender, Map<String, Object> previousArgs, String currentArg) {
    }

    // 引数タイプ列挙 - 引数の種類を定義する
    public enum ArgType {
        LITERAL,
        STRING,
        INTEGER,
        LONG,
        TEXT
    }

    // 引数クラス - コマンド引数を表現する
    public static class Arg {

        private final ArgType type;
        private final String name;
        private final List<Arg> children = new ArrayList<>();
        private String permission;
        private SuggestionProvider suggestionProvider;
        private Executor executor;

        // コンストラクタ - タイプと名前で初期化する
        Arg(ArgType type, String name) {
            this.type = type;
            this.name = name;
        }

        // 次を追加する - 子引数を追加する
        public Arg then(Arg child) {
            children.add(child);
            return this;
        }

        // 権限を設定する
        public Arg withPermission(String permission) {
            this.permission = permission;
            return this;
        }

        // サジェストを追加する
        public Arg includeSuggestions(SuggestionProvider provider) {
            this.suggestionProvider = provider;
            return this;
        }

        // 実行処理を設定する
        public Arg executes(Executor executor) {
            this.executor = executor;
            return this;
        }

        // タイプを取得する
        public ArgType getType() {
            return type;
        }

        // 名前を取得する
        public String getName() {
            return name;
        }

        // 子要素を取得する
        public List<Arg> getChildren() {
            return children;
        }

        // 権限を取得する
        public String getPermission() {
            return permission;
        }

        // サジェストプロバイダを取得する
        public SuggestionProvider getSuggestionProvider() {
            return suggestionProvider;
        }

        // 実行処理を取得する
        public Executor getExecutor() {
            return executor;
        }
    }

    private final String name;
    private String permission;
    private final List<Arg> children = new ArrayList<>();

    // コンストラクタ - 名前で初期化する
    public ModCommandTree(String name) {
        this.name = name;
    }

    // 権限を設定する
    public ModCommandTree withPermission(String permission) {
        this.permission = permission;
        return this;
    }

    // 次を追加する
    public ModCommandTree then(Arg child) {
        children.add(child);
        return this;
    }

    // 名前を取得する
    public String getName() {
        return name;
    }

    // 権限を取得する
    public String getPermission() {
        return permission;
    }

    // 子要素を取得する
    public List<Arg> getChildren() {
        return children;
    }

    // リテラル引数を作成する
    public static Arg literal(String name) {
        return new Arg(ArgType.LITERAL, name);
    }

    // 文字列引数を作成する
    public static Arg string(String name) {
        return new Arg(ArgType.STRING, name);
    }

    // 整数引数を作成する
    public static Arg integer(String name) {
        return new Arg(ArgType.INTEGER, name);
    }

    // ロング引数を作成する
    public static Arg longArg(String name) {
        return new Arg(ArgType.LONG, name);
    }

    // テキスト引数を作成する
    public static Arg text(String name) {
        return new Arg(ArgType.TEXT, name);
    }
}
