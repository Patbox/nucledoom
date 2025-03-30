package eu.pb4.nucledoom;

import eu.pb4.nucledoom.game.DoomConfig;
import eu.pb4.nucledoom.game.DoomGameController;
import eu.pb4.nucledoom.game.doom.SoundMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;

public class NucleDoom implements ModInitializer {
	public static final String MOD_ID = "nucledoom";

	private static final Identifier GAME_ID = NucleDoom.identifier("doom");
	public static final GameType<DoomConfig> DOOM = GameType.register(GAME_ID, DoomConfig.CODEC, DoomGameController::open);

	public static final boolean IS_DEV = FabricLoader.getInstance().isDevelopmentEnvironment();

	@Override
	public void onInitialize() {
		System.setProperty("java.awt.headless", "true");
		SoundMap.updateSoundMap();
		ExtraFonts.load();
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
