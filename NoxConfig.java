package com.example.noxvisuals;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Настройки мода. Хранятся в файле config/noxvisuals.properties. */
public class NoxConfig {
    // атмосфера и блок
    public static int cloudMode = 0;      // 0 стандарт, 1 свои, 2 выкл
    public static float cloudHeight = 192f;
    public static int cloudColor = 0;
    public static int fogMode = 0;        // 0 стандарт, 1 свой
    public static float fogDist = 1f;     // множитель дальности тумана
    public static int fogColor = 0;
    public static int atmoPreset = 0;
    public static int blockFx = 0;        // 0 выкл, 1 контур, 2 сетка, 3 пульс
    public static int blockColor = 0;
    public static final int[] ATMO = {0x6A2FC8, 0xE0407A, 0xFF9A5C, 0xDDE3F0, 0x5CCBF0, 0x4C9A6A};

    public static void preset(int v) {
        atmoPreset = v;
        if (v == 0) { cloudMode = 0; fogMode = 0; cloudHeight = 192f; fogDist = 1f; return; }
        cloudMode = 1; fogMode = 1;
        if (v == 1) { cloudColor = 0; cloudHeight = 170f; fogColor = 0; fogDist = 0.7f; }
        if (v == 2) { cloudColor = 2; cloudHeight = 200f; fogColor = 1; fogDist = 0.8f; }
        if (v == 3) { cloudColor = 3; cloudHeight = 150f; fogColor = 3; fogDist = 0.45f; }
    }

    static float fnum(Properties p, String key, float def, float min, float max) {
        try {
            return Math.max(min, Math.min(max, Float.parseFloat(p.getProperty(key, String.valueOf(def)))));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static boolean freelook = true, freelookThird = true, hudInventory = true, itemRadius = true;
    public static boolean colorSat = false;            // насыщенность цвета (как в Lunar Client)
    public static float satValue = 1f;                 // 0 серый, 1 обычный, 2 яркий
    public static boolean hitRing = true;              // круг под ногами при ударе
    public static int trailStyle = 0;                  // 0 стандарт, 1 спираль, 2 кольца, 3 пыль, 4 огонь
    public static boolean dmgNumbers = true, killFx = true, killStreak = true;
    public static boolean hudFps = true, hudPing = true, hudCoords = true, hudSpeed = true, hudTheme = true;
    public static boolean hudKeys = true, hudCps = true, hudArmor = true, hudEffects = true;
    public static int crossStyle = 0, crossSize = 5;   // 0 стандартный прицел
    public static boolean crossSpread = true, crossAcc = true;
    public static int sky = 0;                         // 0 выкл, 1 звёзды, 2 звёзды + сияние
    public static boolean trail = true, hitFx = true, hud = true, targetHud = true, targetEsp = true;
    public static int wings = 0, pack = 0, head = 0;   // косметика (0 = выкл)
    public static int gDesign = 0;                     // выбранное граффити
    public static boolean gShow = true;                // показывать граффити
    public static int espStyle = 1;                    // 0 кольцо, 1 рамка, 2 уголки
    public static boolean espMoon = true, espHp = false, espPulse = true;
    public static int theme = 0;       // 0 выкл, 1 зима, 2 весна
    public static int accent = 0;      // номер цвета в PALETTE
    public static float density = 1f;  // густота осадков, 0.25..3

    /** Акцентные цвета: фиолетовый, розовый, оранжевый, зелёный, голубой, синий. */
    public static final int[] PALETTE = {0x8A2BE2, 0xE0407A, 0xE0782C, 0x4C9A6A, 0x5CCBF0, 0x3B5BDB};

    public static int acc() {
        return PALETTE[accent];
    }

    /** Более светлая версия акцентного цвета (для текста). */
    public static int accLight() {
        int c = acc();
        int r = ((c >> 16) & 255), g = ((c >> 8) & 255), b = (c & 255);
        r += (255 - r) * 4 / 10;
        g += (255 - g) * 4 / 10;
        b += (255 - b) * 4 / 10;
        return (r << 16) | (g << 8) | b;
    }

    public static void reset() {
        trail = hitFx = hud = targetHud = targetEsp = true;
        hitRing = true;
        colorSat = false;
        satValue = 1f;
        freelook = freelookThird = hudInventory = itemRadius = true;
        trailStyle = 0;
        dmgNumbers = killFx = killStreak = true;
        hudKeys = hudCps = hudArmor = hudEffects = true;
        hudFps = hudPing = hudCoords = hudSpeed = hudTheme = true;
        crossStyle = 0;
        crossSize = 5;
        crossSpread = crossAcc = true;
        sky = 0;
        preset(0);
        cloudColor = fogColor = 0;
        blockFx = blockColor = 0;
        theme = 0;
        wings = pack = head = 0;
        gDesign = 0;
        gShow = true;
        espStyle = 1;
        espMoon = true;
        espHp = false;
        espPulse = true;
        accent = 0;
        density = 1f;
    }

    static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("noxvisuals.properties");
    }

    static int num(Properties p, String key, int def, int max) {
        try {
            int v = Integer.parseInt(p.getProperty(key, String.valueOf(def)));
            return Math.max(0, Math.min(max, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static void load() {
        try (InputStream in = Files.newInputStream(path())) {
            Properties p = new Properties();
            p.load(in);
            trail = Boolean.parseBoolean(p.getProperty("trail", "true"));
            hitFx = Boolean.parseBoolean(p.getProperty("hitFx", "true"));
            hud = Boolean.parseBoolean(p.getProperty("hud", "true"));
            targetHud = Boolean.parseBoolean(p.getProperty("targetHud", "true"));
            targetEsp = Boolean.parseBoolean(p.getProperty("targetEsp", "true"));
            hitRing = Boolean.parseBoolean(p.getProperty("hitRing", "true"));
            freelook = Boolean.parseBoolean(p.getProperty("freelook", "true"));
            freelookThird = Boolean.parseBoolean(p.getProperty("freelookThird", "true"));
            hudInventory = Boolean.parseBoolean(p.getProperty("hudInventory", "true"));
            itemRadius = Boolean.parseBoolean(p.getProperty("itemRadius", "true"));
            colorSat = Boolean.parseBoolean(p.getProperty("colorSat", "false"));
            satValue = fnum(p, "satValue", 1f, 0f, 2f);
            trailStyle = num(p, "trailStyle", 0, 4);
            dmgNumbers = Boolean.parseBoolean(p.getProperty("dmgNumbers", "true"));
            killFx = Boolean.parseBoolean(p.getProperty("killFx", "true"));
            killStreak = Boolean.parseBoolean(p.getProperty("killStreak", "true"));
            hudFps = Boolean.parseBoolean(p.getProperty("hudFps", "true"));
            hudPing = Boolean.parseBoolean(p.getProperty("hudPing", "true"));
            hudCoords = Boolean.parseBoolean(p.getProperty("hudCoords", "true"));
            hudSpeed = Boolean.parseBoolean(p.getProperty("hudSpeed", "true"));
            hudTheme = Boolean.parseBoolean(p.getProperty("hudTheme", "true"));
            hudKeys = Boolean.parseBoolean(p.getProperty("hudKeys", "true"));
            hudCps = Boolean.parseBoolean(p.getProperty("hudCps", "true"));
            hudArmor = Boolean.parseBoolean(p.getProperty("hudArmor", "true"));
            hudEffects = Boolean.parseBoolean(p.getProperty("hudEffects", "true"));
            crossStyle = num(p, "crossStyle", 0, 3);
            crossSize = Math.max(2, num(p, "crossSize", 5, 12));
            crossSpread = Boolean.parseBoolean(p.getProperty("crossSpread", "true"));
            crossAcc = Boolean.parseBoolean(p.getProperty("crossAcc", "true"));
            sky = num(p, "sky", 0, 2);
            cloudMode = num(p, "cloudMode", 0, 2);
            cloudHeight = fnum(p, "cloudHeight", 192f, 120f, 260f);
            cloudColor = num(p, "cloudColor", 0, ATMO.length - 1);
            fogMode = num(p, "fogMode", 0, 1);
            fogDist = fnum(p, "fogDist", 1f, 0.2f, 2f);
            fogColor = num(p, "fogColor", 0, ATMO.length - 1);
            atmoPreset = num(p, "atmoPreset", 0, 3);
            blockFx = num(p, "blockFx", 0, 3);
            blockColor = num(p, "blockColor", 0, PALETTE.length - 1);
            theme = num(p, "theme", 0, 2);
            wings = num(p, "wings", 0, 3);
            pack = num(p, "pack", 0, 2);
            head = num(p, "head", 0, 2);
            gDesign = num(p, "gDesign", 0, NoxGraffiti.ART.length - 1);
            gShow = Boolean.parseBoolean(p.getProperty("gShow", "true"));
            espStyle = num(p, "espStyle", 1, 2);
            espMoon = Boolean.parseBoolean(p.getProperty("espMoon", "true"));
            espHp = Boolean.parseBoolean(p.getProperty("espHp", "false"));
            espPulse = Boolean.parseBoolean(p.getProperty("espPulse", "true"));
            accent = num(p, "accent", 0, PALETTE.length - 1);
            try {
                density = Math.max(0.25f, Math.min(3f, Float.parseFloat(p.getProperty("density", "1"))));
            } catch (NumberFormatException e) {
                density = 1f;
            }
        } catch (IOException ignored) {
            // файла ещё нет: остаются значения по умолчанию
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("trail", String.valueOf(trail));
        p.setProperty("hitFx", String.valueOf(hitFx));
        p.setProperty("hud", String.valueOf(hud));
        p.setProperty("targetHud", String.valueOf(targetHud));
        p.setProperty("targetEsp", String.valueOf(targetEsp));
        p.setProperty("hitRing", String.valueOf(hitRing));
        p.setProperty("freelook", String.valueOf(freelook));
        p.setProperty("freelookThird", String.valueOf(freelookThird));
        p.setProperty("hudInventory", String.valueOf(hudInventory));
        p.setProperty("itemRadius", String.valueOf(itemRadius));
        p.setProperty("colorSat", String.valueOf(colorSat));
        p.setProperty("satValue", String.valueOf(satValue));
        p.setProperty("trailStyle", String.valueOf(trailStyle));
        p.setProperty("dmgNumbers", String.valueOf(dmgNumbers));
        p.setProperty("killFx", String.valueOf(killFx));
        p.setProperty("killStreak", String.valueOf(killStreak));
        p.setProperty("hudFps", String.valueOf(hudFps));
        p.setProperty("hudPing", String.valueOf(hudPing));
        p.setProperty("hudCoords", String.valueOf(hudCoords));
        p.setProperty("hudSpeed", String.valueOf(hudSpeed));
        p.setProperty("hudTheme", String.valueOf(hudTheme));
        p.setProperty("hudKeys", String.valueOf(hudKeys));
        p.setProperty("hudCps", String.valueOf(hudCps));
        p.setProperty("hudArmor", String.valueOf(hudArmor));
        p.setProperty("hudEffects", String.valueOf(hudEffects));
        p.setProperty("crossStyle", String.valueOf(crossStyle));
        p.setProperty("crossSize", String.valueOf(crossSize));
        p.setProperty("crossSpread", String.valueOf(crossSpread));
        p.setProperty("crossAcc", String.valueOf(crossAcc));
        p.setProperty("sky", String.valueOf(sky));
        p.setProperty("cloudMode", String.valueOf(cloudMode));
        p.setProperty("cloudHeight", String.valueOf(cloudHeight));
        p.setProperty("cloudColor", String.valueOf(cloudColor));
        p.setProperty("fogMode", String.valueOf(fogMode));
        p.setProperty("fogDist", String.valueOf(fogDist));
        p.setProperty("fogColor", String.valueOf(fogColor));
        p.setProperty("atmoPreset", String.valueOf(atmoPreset));
        p.setProperty("blockFx", String.valueOf(blockFx));
        p.setProperty("blockColor", String.valueOf(blockColor));
        p.setProperty("theme", String.valueOf(theme));
        p.setProperty("wings", String.valueOf(wings));
        p.setProperty("pack", String.valueOf(pack));
        p.setProperty("head", String.valueOf(head));
        p.setProperty("gDesign", String.valueOf(gDesign));
        p.setProperty("gShow", String.valueOf(gShow));
        p.setProperty("espStyle", String.valueOf(espStyle));
        p.setProperty("espMoon", String.valueOf(espMoon));
        p.setProperty("espHp", String.valueOf(espHp));
        p.setProperty("espPulse", String.valueOf(espPulse));
        p.setProperty("accent", String.valueOf(accent));
        p.setProperty("density", String.valueOf(density));
        try (OutputStream out = Files.newOutputStream(path())) {
            p.store(out, "NoxVisuals");
        } catch (IOException ignored) {
        }
    }
}
