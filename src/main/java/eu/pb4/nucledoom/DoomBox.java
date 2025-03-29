package eu.pb4.nucledoom;

import eu.pb4.nucledoom.game.DoomConfig;
import eu.pb4.nucledoom.game.DoomGameController;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;

public class DoomBox implements ModInitializer {
	public static final String MOD_ID = "nucledoom";

	private static final Identifier GAME_ID = DoomBox.identifier("doom");
	public static final GameType<DoomConfig> DOOM = GameType.register(GAME_ID, DoomConfig.CODEC, DoomGameController::open);

	@Override
	public void onInitialize() {
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
