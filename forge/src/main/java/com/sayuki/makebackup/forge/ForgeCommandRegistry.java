/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.sayuki.makebackup.forge.platform.ForgeSender;
import com.sayuki.makebackup.command.CommandArgs;
import com.sayuki.makebackup.command.ModCommandTree;
import com.sayuki.makebackup.command.ModCommandTree.Arg;
import com.sayuki.makebackup.platform.ModCommandSender;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// コマンド登録クラス - Forgeのコマンドを登録する
public class ForgeCommandRegistry {

    // インスタンス化を禁止する
    private ForgeCommandRegistry() {
    }

    // コマンドを登録する
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, List<ModCommandTree> trees) {
        for (ModCommandTree tree : trees) {
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(tree.getName())
                    .requires(source -> hasPermission(source, tree.getPermission()));
            for (Arg child : tree.getChildren()) {
                root.then(build(child));
            }
            dispatcher.register(root);
        }
    }

    // 引数を構築する
    private static ArgumentBuilder<CommandSourceStack, ?> build(Arg arg) {
        ArgumentBuilder<CommandSourceStack, ?> builder;
        switch (arg.getType()) {
            case LITERAL -> builder = Commands.literal(arg.getName());
            case STRING -> builder = Commands.argument(arg.getName(), StringArgumentType.word());
            case INTEGER -> builder = Commands.argument(arg.getName(), IntegerArgumentType.integer());
            case LONG -> builder = Commands.argument(arg.getName(), LongArgumentType.longArg());
            case TEXT -> builder = Commands.argument(arg.getName(), StringArgumentType.string());
            default -> throw new IllegalStateException("Unknown argument type " + arg.getType());
        }

        if (arg.getPermission() != null) {
            builder.requires(source -> hasPermission(source, arg.getPermission()));
        }
        if (arg.getSuggestionProvider() != null) {
            ((RequiredArgumentBuilder<CommandSourceStack, ?>) builder).suggests((context, suggestionBuilder) -> {
                List<String> suggestions = arg.getSuggestionProvider().getSuggestions(
                        new ModCommandTree.SuggestionContext(wrapSender(context.getSource()), previousArgs(context), suggestionBuilder.getRemaining()));
                for (String suggestion : suggestions) {
                    suggestionBuilder.suggest(suggestion);
                }
                return suggestionBuilder.buildFuture();
            });
        }
        if (arg.getExecutor() != null) {
            builder.executes(context -> {
                arg.getExecutor().execute(wrapSender(context.getSource()), argsFromContext(context));
                return 1;
            });
        }
        for (Arg child : arg.getChildren()) {
            builder.then(build(child));
        }
        return builder;
    }

    // 引数の値を取得する
    private static Map<String, Object> argumentValues(CommandContext<CommandSourceStack> context) {
        Map<String, Object> args = new HashMap<>();
        for (com.mojang.brigadier.context.ParsedCommandNode<CommandSourceStack> node : context.getNodes()) {
            if (node.getNode() instanceof com.mojang.brigadier.tree.ArgumentCommandNode<?, ?>) {

                String name = node.getNode().getName();
                args.put(name, unwrap(context.getArgument(name, Object.class)));
            }
        }
        return args;
    }

    // 以前の引数を取得する
    private static Map<String, Object> previousArgs(CommandContext<CommandSourceStack> context) {
        Map<String, Object> previousArgs = new HashMap<>();
        for (Map.Entry<String, Object> entry : argumentValues(context).entrySet()) {
            if (entry.getValue() == null) continue;
            previousArgs.put(entry.getKey(), entry.getValue());
        }
        return previousArgs;
    }

    // コンテキストから引数を変換する
    private static CommandArgs argsFromContext(CommandContext<CommandSourceStack> context) {
        return new CommandArgs(argumentValues(context), context.getInput());
    }

    // クォートを除去する
    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    // 値をアンラップする
    private static Object unwrap(Object value) {
        if (value instanceof String s) return stripQuotes(s);
        if (value instanceof Integer i) return i;
        if (value instanceof Long l) return l;
        return value;
    }

    // 権限をチェックする
    private static boolean hasPermission(CommandSourceStack source, String permission) {
        if (permission == null) return true;
        ServerPlayer player = source.getPlayer();
        return player == null ? true : player.hasPermissions(2);
    }

    // 送信者をラップする
    private static ModCommandSender wrapSender(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player != null ? ForgeSender.player(player) : ForgeSender.source(source);
    }
}
