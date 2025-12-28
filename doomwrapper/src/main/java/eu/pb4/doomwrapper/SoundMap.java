package eu.pb4.doomwrapper;

import data.sounds;
import eu.pb4.nucledoom.NucleDoom;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SoundMap {
    static Map<String, List<Sound>> MAP = new HashMap<>();
    static Map<String, SoundEvent> DOOM_MAP = new HashMap<>();

    
    static void put(String name, SoundEvent... event) {
        MAP.put(name, List.of(event).stream().map(x -> new Sound(x, 1, 1)).toList());
    }

    static void put(String name, Sound... event) {
        MAP.put(name, List.of(event));
    }

    static void updateSoundMap() {
        MAP.clear();
        for (var sfx : sounds.S_sfx) {
            DOOM_MAP.put(sfx.name, new SoundEvent(Identifier.fromNamespaceAndPath(NucleDoom.MOD_ID, "sfx." + sfx.name), Optional.empty()));
        }
        put("punch", SoundEvents.PLAYER_ATTACK_STRONG);
        put("pistol", SoundEvents.CROSSBOW_SHOOT);
        put("shotgn", new Sound(SoundEvents.DRAGON_FIREBALL_EXPLODE, 1, 0.8f));
        put("sgcock", SoundEvents.CROSSBOW_LOADING_START.value());
        put("sawup", SoundEvents.UI_STONECUTTER_TAKE_RESULT);
        put("sawidl", SoundEvents.UI_STONECUTTER_TAKE_RESULT);
        put("sawful", SoundEvents.UI_STONECUTTER_TAKE_RESULT);
        put("sawhit", SoundEvents.UI_STONECUTTER_TAKE_RESULT);
        put("rlaunc", SoundEvents.BLAZE_SHOOT);
        put("rxplod", SoundEvents.DRAGON_FIREBALL_EXPLODE);
        put("barexp", SoundEvents.GENERIC_EXPLODE.value());
        put("firsht", SoundEvents.BREEZE_WIND_CHARGE_BURST.value());
        put("firxpl", SoundEvents.GENERIC_EXPLODE.value());
        put("pstart", SoundEvents.PISTON_EXTEND);
        put("pstop", SoundEvents.PISTON_CONTRACT);
        put("doropn", SoundEvents.PISTON_EXTEND);
        put("dorcls", SoundEvents.PISTON_CONTRACT);
        put("stnmov", SoundEvents.STONE_STEP);
        put("swtchn", SoundEvents.LEVER_CLICK);
        put("swtchx", SoundEvents.STONE_BUTTON_CLICK_OFF);
        put("plpain", SoundEvents.ZOMBIE_VILLAGER_HURT);
        put("dmpain", SoundEvents.ZOMBIFIED_PIGLIN_HURT);
        put("popain", SoundEvents.ZOMBIE_HURT);
        put("slop", SoundEvents.HONEY_BLOCK_BREAK);
        put("itemup", SoundEvents.ITEM_PICKUP);
        put("wpnup", new Sound(SoundEvents.ITEM_PICKUP, 1, 0.8f));
        put("oof", SoundEvents.PLAYER_BIG_FALL);
        put("telept", SoundEvents.CHORUS_FRUIT_TELEPORT);
        put("posit1", SoundEvents.ZOMBIE_AMBIENT);
        put("posit2", SoundEvents.ZOMBIE_AMBIENT);
        put("posit3", SoundEvents.ZOMBIE_AMBIENT);
        put("bgsit1", SoundEvents.PIGLIN_AMBIENT);
        put("bgsit2", SoundEvents.PIGLIN_AMBIENT);
        put("sgtsit", SoundEvents.HOGLIN_AMBIENT);
        put("brssit", SoundEvents.ZOGLIN_AMBIENT);
        put("sgtatk", SoundEvents.ZOGLIN_ATTACK);
        put("claw", SoundEvents.ZOGLIN_ATTACK);
        put("pldeth", SoundEvents.ZOMBIE_VILLAGER_DEATH);
        put("podth1", SoundEvents.ZOMBIE_DEATH);
        put("podth2", SoundEvents.ZOMBIE_DEATH);
        put("podth3", SoundEvents.ZOMBIE_DEATH);
        put("bgdth1", SoundEvents.PIGLIN_DEATH);
        put("bgdth2", SoundEvents.PIGLIN_DEATH);
        put("sgtdth", SoundEvents.ZOGLIN_DEATH);
        put("btsdth", SoundEvents.ZOGLIN_DEATH);
        put("posact", SoundEvents.ZOMBIE_AMBIENT);
        put("bgact", SoundEvents.PIGLIN_AMBIENT);
        put("dmact", SoundEvents.HOGLIN_AMBIENT);
        put("noway", SoundEvents.ZOMBIE_VILLAGER_STEP);
        put("bdopn", SoundEvents.PISTON_EXTEND);
        put("bdcls", SoundEvents.PISTON_CONTRACT);
        put("getpow", SoundEvents.ENCHANTMENT_TABLE_USE);
        put("plasma", SoundEvents.WIND_CHARGE_THROW);
        put("bfg", SoundEvents.BEACON_ACTIVATE);
        put("cacsit", SoundEvents.GHAST_AMBIENT);
        put("cybsit", SoundEvents.RAVAGER_AMBIENT);
        put("spisit", SoundEvents.RAVAGER_AMBIENT);
        put("sklatk", SoundEvents.VEX_CHARGE);
        put("cacdth", SoundEvents.GHAST_DEATH);
        put("cybdth", SoundEvents.RAVAGER_DEATH);
        put("spidth", SoundEvents.RAVAGER_DEATH);
        put("shoof", SoundEvents.RAVAGER_STEP);
        put("metal", SoundEvents.IRON_GOLEM_STEP);
        put("dshtgn", new Sound(SoundEvents.DRAGON_FIREBALL_EXPLODE, 1, 0.65f));
        put("dbopn", SoundEvents.CROSSBOW_LOADING_START.value());
        put("dbcls", SoundEvents.CROSSBOW_LOADING_END.value());
        put("dbload", SoundEvents.CROSSBOW_LOADING_MIDDLE.value());
        put("vipain", SoundEvents.BLAZE_HURT);
        put("mnpain", SoundEvents.WARDEN_HURT);
        put("pepain", SoundEvents.GHAST_HURT);
        put("bspsit", SoundEvents.RAVAGER_AMBIENT);
        put("kntsit", SoundEvents.ZOGLIN_AMBIENT);
        put("vilsit", SoundEvents.BLAZE_AMBIENT);
        put("mansit", SoundEvents.WARDEN_AMBIENT);
        put("pesit", SoundEvents.GHAST_AMBIENT);
        put("skepch", SoundEvents.SKELETON_HORSE_AMBIENT);
        put("vilatk", SoundEvents.BLAZE_BURN);
        put("skeswh", SoundEvents.PLAYER_ATTACK_CRIT);
        put("bspdth", SoundEvents.IRON_GOLEM_DEATH);
        put("vildth", SoundEvents.BLAZE_DEATH);
        put("kntdth", SoundEvents.ZOGLIN_DEATH);
        put("pedth", SoundEvents.GHAST_DEATH);
        put("skedth", SoundEvents.SKELETON_DEATH);
        put("bspact", SoundEvents.IRON_GOLEM_STEP);
        put("bspwlk", SoundEvents.IRON_GOLEM_STEP);
        put("flame", SoundEvents.PLAYER_HURT_ON_FIRE);
        put("flamest", SoundEvents.FIRE_AMBIENT);
        put("bospit", SoundEvents.BLAZE_SHOOT);
        put("boscub", SoundEvents.BLAZE_AMBIENT);
        put("bossit", SoundEvents.ENDER_DRAGON_GROWL);
        put("bospn", SoundEvents.ENDER_DRAGON_HURT);
        put("bosdth", SoundEvents.ENDER_DRAGON_DEATH);
        put("manatk", SoundEvents.WARDEN_ATTACK_IMPACT);
        put("mandth", SoundEvents.WARDEN_DEATH);
        put("sssit", SoundEvents.VINDICATOR_AMBIENT);
        put("sssdth", SoundEvents.VINDICATOR_DEATH);
        put("keenpn", SoundEvents.PLAYER_HURT);
        put("keendt", SoundEvents.PLAYER_DEATH);
        put("skeact", SoundEvents.WITHER_AMBIENT);
        put("skesit", SoundEvents.WITHER_AMBIENT);
        put("skeatk", SoundEvents.BLAZE_SHOOT);
    }
    
    public record Sound(SoundEvent event, float volume, float pitch) {};
}
