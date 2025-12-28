package eu.pb4.nucledoom;

import com.mojang.serialization.MapCodec;
import eu.pb4.nucledoom.game.DoomConfig;
import eu.pb4.nucledoom.game.DoomGameController;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NucleDoom implements ModInitializer {
	public static final String MOD_ID = "nucledoom";

	private static final Identifier GAME_ID = NucleDoom.identifier("doom");
	private static final Identifier NBS_GAME_ID = NucleDoom.identifier("nbs_player");
	public static final GameType<DoomConfig> DOOM = GameType.register(GAME_ID, DoomConfig.CODEC, DoomGameController::open);
	public static final GameType<DoomConfig> NBS_PLAYER = GameType.register(NBS_GAME_ID, MapCodec.unit(
			new DoomConfig(Identifier.fromNamespaceAndPath("nbs", "player"), "NBS Player", List.of(), List.of(), Map.of(), false, Optional.empty())
	), DoomGameController::openNbs);

	public static final boolean IS_DEV = FabricLoader.getInstance().isDevelopmentEnvironment();

	public static final Map<Identifier, byte[]> WADS = new HashMap<>();
	//public static final Map<Identifier, byte[]> WAD2SFX = new HashMap<>();

	@Override
	public void onInitialize() {
		System.setProperty("java.awt.headless", "true");
		ExtraFonts.load();
		SoundDecoder.load();

		ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
			for (var entry : server.getResourceManager().listResources("wads", (x) -> x.getPath().endsWith(".wad")).entrySet()) {
                try {
					var id = entry.getKey().withPath(entry.getKey().getPath().substring("wads/".length()));
                    WADS.put(id, entry.getValue().open().readAllBytes());
				} catch (IOException e) {
                    e.printStackTrace();
                }
            }

			/*for (var gc : server.getRegistryManager().getOrThrow(GameConfigs.REGISTRY_KEY)) {
				if (gc.config() instanceof DoomConfig doomConfig && !WAD2SFX.containsKey(doomConfig.wadFile())) {
					var generator = PolymerResourcePackUtils.createBuilder(FabricLoader.getInstance().getGameDir().resolve(("sfx_" + doomConfig.wadFile().toShortTranslationKey())));
					var soundAsset = SoundsAsset.builder();

                    try {
                        var game = DoomGame.create(null, doomConfig, server.getResourceManager(), 1);
						game.game().extractAudio((name, data) -> {
							soundAsset.add("sfx." + name, SoundEntry.builder().sound(Identifier.of(MOD_ID, "sfx." + name), (b) -> {}));

							var out = new ByteArrayOutputStream();

							var oggFile = new OggFile(out);
							oggFile.getPacketWriter().bufferPacket(new OggStreamAudioData());

						});
						game.game().clear();
						game.loader().close();
                    } catch (Throwable e) {
                        // Ignored
                    }
					generator.addData("assets/nucledoom/sounds.json", soundAsset.build());


                }
			}*/
		});

		ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
			WADS.clear();
		});
	}

	public static Identifier identifier(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
