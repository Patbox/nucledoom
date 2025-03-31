package eu.pb4.nucledoom;

import eu.pb4.nucledoom.game.DoomConfig;
import eu.pb4.nucledoom.game.DoomGameController;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NucleDoom implements ModInitializer {
	public static final String MOD_ID = "nucledoom";

	private static final Identifier GAME_ID = NucleDoom.identifier("doom");
	public static final GameType<DoomConfig> DOOM = GameType.register(GAME_ID, DoomConfig.CODEC, DoomGameController::open);

	public static final boolean IS_DEV = FabricLoader.getInstance().isDevelopmentEnvironment();

	public static final Map<Identifier, byte[]> WADS = new HashMap<>();

	@Override
	public void onInitialize() {
		System.setProperty("java.awt.headless", "true");
		ExtraFonts.load();
		ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
			for (var entry : server.getResourceManager().findResources("wads", (x) -> x.getPath().endsWith(".wad")).entrySet()) {
                try {
                    WADS.put(entry.getKey().withPath(entry.getKey().getPath().substring("wads/".length())), entry.getValue().getInputStream().readAllBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
		});

		ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
			WADS.clear();
		});
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
