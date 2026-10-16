package com.example.miyu;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Cfg {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> PROVIDER, BASE, KEY, MODEL, PERSONA;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        PROVIDER = b.comment("openai = формат OpenAI").define("provider", "openai");
        BASE = b.comment("Адрес API").define("baseUrl", "https://openrouter.ai/api/v1");
        KEY = b.comment("API-ключ (начинается с sk-or-v1-)").define("apiKey", "");
        MODEL = b.comment("Модель").define("model", "qwen/qwen-2.5-7b-instruct:free");
        PERSONA = b.comment("Характер").define("persona", "Всегда отвечай на русском языке.");
        SPEC = b.build();
    }
}
