package com.example.miyu;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Cfg {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> PROVIDER, BASE, KEY, MODEL, PERSONA, GENDER, NAME;
    public static final ModConfigSpec.BooleanValue SLIM;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        PROVIDER = b.comment("openai = формат OpenAI (Grok, OpenRouter, OpenAI и др.); anthropic = Claude").define("provider", "openai");
        BASE = b.comment("Адрес API без /chat/completions. OpenRouter: https://openrouter.ai/api/v1  Grok: https://api.x.ai/v1").define("baseUrl", "https://openrouter.ai/api/v1");
        KEY = b.comment("API-ключ. Не показывайте этот файл другим!").define("apiKey", "");
        MODEL = b.comment("Имя модели. OpenRouter бесплатно: meta-llama/llama-3.3-70b-instruct:free, openai/gpt-oss-120b:free или openrouter/free").define("model", "meta-llama/llama-3.3-70b-instruct:free");
        PERSONA = b.comment("Свои дополнения к характеру (по-русски, без сексуального контента). Можно пусто").define("persona", "");
        GENDER = b.comment("Пол персонажа: female или male (влияет на род в репликах и в промпте ИИ)").define("gender", "female");
        NAME = b.comment("Имя персонажа в промпте ИИ").define("name", "Юно Гасай");
        SLIM = b.comment("true = тонкие руки (скин Alex/Slim), false = обычные (скин Steve/Classic)").define("slimArms", false);
        SPEC = b.build();
    }
    public static boolean male() { try { return GENDER.get().trim().equalsIgnoreCase("male"); } catch (Exception e) { return false; } }
    public static String name() { try { String n = NAME.get().trim(); return n.isEmpty() ? "Юно Гасай" : n; } catch (Exception e) { return "Юно Гасай"; } }
    public static boolean slim() { try { return SLIM.get(); } catch (Exception e) { return false; } }
}
