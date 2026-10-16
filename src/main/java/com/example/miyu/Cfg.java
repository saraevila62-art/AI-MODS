package com.example.miyu;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Cfg {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> PROVIDER, BASE, KEY, MODEL, PERSONA;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        PROVIDER = b.comment("openai = формат OpenAI (Grok от xAI, OpenAI и др.); anthropic = Claude").define("provider", "openai");
        BASE = b.comment("Адрес API без /chat/completions. Для Grok: https://api.x.ai/v1").define("baseUrl", "https://api.x.ai/v1");
        KEY = b.comment("API-ключ (для Grok начинается с xai-). Не показывайте этот файл другим!").define("apiKey", "");
        MODEL = b.comment("Имя модели. Для Grok смотрите актуальные на docs.x.ai, например grok-4.3").define("model", "grok-4.3");
        PERSONA = b.comment("Свои дополнения к характеру Мию (по-русски, без сексуального контента). Можно оставить пустым").define("persona", "");
        SPEC = b.build();
    }
}
