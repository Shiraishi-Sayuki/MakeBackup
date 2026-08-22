/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.command;

import java.util.HashMap;
import java.util.Map;

// コマンド引数クラス - コマンドの引数を保持する
public class CommandArgs {

    private final Map<String, Object> args;
    private final String fullInput;

    // コンストラクタ - 空で初期化する
    public CommandArgs() {
        this(new HashMap<>(), "");
    }

    // コンストラクタ - 引数マップと入力文字列で初期化する
    public CommandArgs(Map<String, Object> args, String fullInput) {
        this.args = args;
        this.fullInput = fullInput;
    }

    // 取得する - キーで値を取る
    public Object get(String key) {
        return args.get(key);
    }

    // 取得する - デフォルト値付き
    public Object getOrDefault(String key, Object defaultValue) {
        return args.getOrDefault(key, defaultValue);
    }

    // 含むか判定する
    public boolean containsKey(String key) {
        return args.containsKey(key);
    }

    // 引数マップを取得する
    public Map<String, Object> getArgs() {
        return args;
    }

    // フル入力を取得する
    public String getFullInput() {
        return fullInput;
    }
}
