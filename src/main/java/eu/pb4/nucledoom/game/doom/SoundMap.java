package eu.pb4.nucledoom.game.doom;

import data.sounds;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SoundMap {
    static Map<String, List<Sound>> MAP = new HashMap<>();

    
    static void put(String name, SoundEvent... event) {
        MAP.put(name, List.of(event).stream().map(x -> new Sound(x, 1, 1)).toList());
    }

    static void put(String name, Sound... event) {
        MAP.put(name, List.of(event));
    }

    static void updateSoundMap() {
        MAP.clear();
        put("pistol", SoundEvents.ITEM_CROSSBOW_SHOOT);
        put("shotgn", new Sound(SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 0.8f));
        put("sgcock", SoundEvents.ITEM_CROSSBOW_LOADING_START.value());
        put("sawup", SoundEvents.UI_STONECUTTER_TAKE_RESULT);
        put("sawidl", SoundEvents.UI_STONECUTTER_TAKE_RESULT);
        put("sawful", SoundEvents.UI_STONECUTTER_TAKE_RESULT);
        put("sawhit", SoundEvents.UI_STONECUTTER_TAKE_RESULT);
        put("rlaunc", SoundEvents.ENTITY_BLAZE_SHOOT);
        put("rxplod", SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE);
        put("barexp", SoundEvents.ENTITY_GENERIC_EXPLODE.value());
        put("firsht", SoundEvents.ENTITY_BREEZE_WIND_BURST.value());
        put("firxpl", SoundEvents.ENTITY_GENERIC_EXPLODE.value());
        put("pstart", SoundEvents.BLOCK_PISTON_EXTEND);
        put("pstop", SoundEvents.BLOCK_PISTON_CONTRACT);
        put("doropn", SoundEvents.BLOCK_PISTON_EXTEND);
        put("dorcls", SoundEvents.BLOCK_PISTON_CONTRACT);
        put("stnmov", SoundEvents.BLOCK_GRINDSTONE_USE);
        put("swtchn", SoundEvents.BLOCK_LEVER_CLICK);
        put("swtchx", SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF);
        put("plpain", SoundEvents.ENTITY_ZOMBIE_VILLAGER_HURT);
        put("dmpain", SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_HURT);
        put("popain", SoundEvents.ENTITY_ZOMBIE_HURT);
        put("slop", SoundEvents.BLOCK_HONEY_BLOCK_BREAK);
        put("itemup", SoundEvents.ENTITY_ITEM_PICKUP);
        put("wpnup", new Sound(SoundEvents.ENTITY_ITEM_PICKUP, 1, 0.8f));
        put("oof", SoundEvents.ENTITY_PLAYER_BIG_FALL);
        put("telept", SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT);
        put("posit1", SoundEvents.ENTITY_ZOMBIE_AMBIENT);
        put("posit2", SoundEvents.ENTITY_ZOMBIE_AMBIENT);
        put("posit3", SoundEvents.ENTITY_ZOMBIE_AMBIENT);
        put("bgsit1", SoundEvents.ENTITY_PIGLIN_AMBIENT);
        put("bgsit2", SoundEvents.ENTITY_PIGLIN_AMBIENT);
        put("sgtsit", SoundEvents.ENTITY_HOGLIN_AMBIENT);
        put("brssit", SoundEvents.ENTITY_ZOGLIN_AMBIENT);
        put("sgtatk", SoundEvents.ENTITY_ZOGLIN_ATTACK);
        put("claw", SoundEvents.ENTITY_ZOGLIN_ATTACK);
        put("pldeth", SoundEvents.ENTITY_ZOMBIE_VILLAGER_DEATH);
        put("podth1", SoundEvents.ENTITY_ZOMBIE_DEATH);
        put("podth2", SoundEvents.ENTITY_ZOMBIE_DEATH);
        put("podth3", SoundEvents.ENTITY_ZOMBIE_DEATH);
        put("bgdth1", SoundEvents.ENTITY_PIGLIN_DEATH);
        put("bgdth2", SoundEvents.ENTITY_PIGLIN_DEATH);
        put("sgtdth", SoundEvents.ENTITY_ZOGLIN_DEATH);
        put("btsdth", SoundEvents.ENTITY_ZOGLIN_DEATH);
        put("posact", SoundEvents.ENTITY_ZOMBIE_AMBIENT);
        put("bgact", SoundEvents.ENTITY_PIGLIN_AMBIENT);
        put("dmact", SoundEvents.ENTITY_HOGLIN_AMBIENT);
        put("noway", SoundEvents.ENTITY_ZOMBIE_VILLAGER_STEP);
        put("bdopn", SoundEvents.BLOCK_PISTON_EXTEND);
        put("bdcls", SoundEvents.BLOCK_PISTON_CONTRACT);
        put("getpow", SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE);
        put("plasma", SoundEvents.ENTITY_WIND_CHARGE_THROW);
        put("bfg", SoundEvents.BLOCK_BEACON_ACTIVATE);
        put("cacsit", SoundEvents.ENTITY_GHAST_AMBIENT);
        put("cybsit", SoundEvents.ENTITY_RAVAGER_AMBIENT);
        put("spisit", SoundEvents.ENTITY_RAVAGER_AMBIENT);
        put("sklatk", SoundEvents.ENTITY_VEX_CHARGE);
        put("cacdth", SoundEvents.ENTITY_GHAST_DEATH);
        put("cybdth", SoundEvents.ENTITY_RAVAGER_DEATH);
        put("spidth", SoundEvents.ENTITY_RAVAGER_DEATH);
        put("shoof", SoundEvents.ENTITY_RAVAGER_STEP);
        put("metal", SoundEvents.ENTITY_IRON_GOLEM_STEP);
        put("dshtgn", new Sound(SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 0.65f));
        put("dbopn", SoundEvents.ITEM_CROSSBOW_LOADING_START.value());
        put("dbcls", SoundEvents.ITEM_CROSSBOW_LOADING_END.value());
        put("dbload", SoundEvents.ITEM_CROSSBOW_LOADING_MIDDLE.value());
        put("vipain", SoundEvents.ENTITY_BLAZE_HURT);
        put("mnpain", SoundEvents.ENTITY_WARDEN_HURT);
        put("pepain", SoundEvents.ENTITY_GHAST_HURT);
        put("bspsit", SoundEvents.ENTITY_RAVAGER_AMBIENT);
        put("kntsit", SoundEvents.ENTITY_ZOGLIN_AMBIENT);
        put("vilsit", SoundEvents.ENTITY_BLAZE_AMBIENT);
        put("mansit", SoundEvents.ENTITY_WARDEN_AMBIENT);
        put("pesit", SoundEvents.ENTITY_GHAST_AMBIENT);
        put("skepch", SoundEvents.ENTITY_SKELETON_HORSE_AMBIENT);
        put("vilatk", SoundEvents.ENTITY_BLAZE_BURN);
        put("skeswh", SoundEvents.ENTITY_PLAYER_ATTACK_CRIT);
        put("bspdth", SoundEvents.ENTITY_IRON_GOLEM_DEATH);
        put("vildth", SoundEvents.ENTITY_BLAZE_DEATH);
        put("kntdth", SoundEvents.ENTITY_ZOGLIN_DEATH);
        put("pedth", SoundEvents.ENTITY_GHAST_DEATH);
        put("skedth", SoundEvents.ENTITY_SKELETON_DEATH);
        put("bspact", SoundEvents.ENTITY_IRON_GOLEM_STEP);
        put("bspwlk", SoundEvents.ENTITY_IRON_GOLEM_STEP);
        put("flame", SoundEvents.ENTITY_PLAYER_HURT_ON_FIRE);
        put("flamest", SoundEvents.BLOCK_FIRE_AMBIENT);
        put("bospit", SoundEvents.ENTITY_BLAZE_SHOOT);
        put("boscub", SoundEvents.ENTITY_BLAZE_AMBIENT);
        put("bossit", SoundEvents.ENTITY_ENDER_DRAGON_GROWL);
        put("bospn", SoundEvents.ENTITY_ENDER_DRAGON_HURT);
        put("bosdth", SoundEvents.ENTITY_ENDER_DRAGON_DEATH);
        put("manatk", SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT);
        put("mandth", SoundEvents.ENTITY_WARDEN_DEATH);
        put("sssit", SoundEvents.ENTITY_VINDICATOR_AMBIENT);
        put("sssdth", SoundEvents.ENTITY_VINDICATOR_DEATH);
        put("keenpn", SoundEvents.ENTITY_PLAYER_HURT);
        put("keendt", SoundEvents.ENTITY_PLAYER_DEATH);
        put("skeact", SoundEvents.ENTITY_WITHER_AMBIENT);
        put("skesit", SoundEvents.ENTITY_WITHER_AMBIENT);
        put("skeatk", SoundEvents.ENTITY_BLAZE_SHOOT);
    }
    
    public record Sound(SoundEvent event, float volume, float pitch) {};
}
