package com.example.miyu;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Cfg {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> KEY;
    public static final ModConfigSpec.ConfigValue<String> MODEL;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        KEY = b.comment("Anthropic API key (sk-ant-...). Не показывайте этот файл другим!").define("apiKey", "");
        MODEL = b.comment("Модель Claude").define("model", "claude-haiku-5-5");
        SPEC = b.build();
    }
}
