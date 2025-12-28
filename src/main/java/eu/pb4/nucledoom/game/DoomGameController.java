package eu.pb4.nucledoom.game;

import eu.pb4.mapcanvas.api.utils.VirtualDisplay;
import eu.pb4.nucledoom.NBSPlayer;
import eu.pb4.nucledoom.NucleDoom;
import eu.pb4.nucledoom.PlayerSaveData;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;
import xyz.nucleoid.plasmid.api.game.*;
import xyz.nucleoid.plasmid.api.game.common.PlayerLimiter;
import xyz.nucleoid.plasmid.api.game.common.config.PlayerLimiterConfig;
import xyz.nucleoid.plasmid.api.game.config.GameConfig;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerC2SPacketEvent;
import xyz.nucleoid.stimuli.event.player.PlayerChatEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.List;
import java.util.Random;

public class DoomGameController implements GameHandler.PlayerInterface, GamePlayerEvents.Add, GameActivityEvents.Destroy, GameActivityEvents.Tick, GameActivityEvents.Enable, GamePlayerEvents.Remove, GamePlayerEvents.Accept, PlayerDamageEvent, PlayerDeathEvent, PlayerChatEvent, PlayerC2SPacketEvent {
    private final Thread thread;
    private final GameSpace gameSpace;
    private final ServerLevel world;
    private final DoomConfig config;
    protected final GameHandler handler;
    private final Entity cameraEntity;
    private final Display.ItemDisplay leftAudio;
    private final Display.ItemDisplay rightAudio;
    private VirtualDisplay display;
    private ServerPlayer player;
    private boolean hasStarted = false;

    public DoomGameController(GameSpace gameSpace, ServerLevel world, DoomConfig config, GameHandler handler) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.config = config;

        this.handler = handler;

        cameraEntity = EntityType.ITEM_DISPLAY.create(world, EntitySpawnReason.LOAD);
        assert cameraEntity != null;
        cameraEntity.setInvisible(true);
        world.addFreshEntity(cameraEntity);

        this.leftAudio = EntityType.ITEM_DISPLAY.create(world, EntitySpawnReason.LOAD);
        assert leftAudio != null;
        leftAudio.setInvisible(true);
        world.addFreshEntity(leftAudio);

        this.rightAudio = EntityType.ITEM_DISPLAY.create(world, EntitySpawnReason.LOAD);
        assert rightAudio != null;
        rightAudio.setInvisible(true);
        world.addFreshEntity(rightAudio);

        this.updateSetup();

        this.thread = new Thread(this::runThread);
        this.thread.setName("nucledoom_" + gameSpace.getMetadata().userId().toShortLanguageKey());
        this.thread.setDaemon(true);
        handler.setPlayerInterface(this);
    }

    public static void setRules(GameActivity activity) {
        activity.deny(GameRuleType.BLOCK_DROPS);
        activity.deny(GameRuleType.BREAK_BLOCKS);
        activity.deny(GameRuleType.CRAFTING);
        activity.deny(GameRuleType.DISMOUNT_VEHICLE);
        activity.deny(GameRuleType.STOP_SPECTATING_ENTITY);
        activity.deny(GameRuleType.FALL_DAMAGE);
        activity.deny(GameRuleType.FIRE_TICK);
        activity.deny(GameRuleType.FLUID_FLOW);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.ICE_MELT);
        activity.deny(GameRuleType.MODIFY_ARMOR);
        activity.deny(GameRuleType.MODIFY_INVENTORY);
        activity.deny(GameRuleType.PICKUP_ITEMS);
        activity.deny(GameRuleType.PLACE_BLOCKS);
        activity.deny(GameRuleType.PLAYER_PROJECTILE_KNOCKBACK);
        activity.deny(GameRuleType.PORTALS);
        activity.deny(GameRuleType.PVP);
        activity.deny(GameRuleType.SWAP_OFFHAND);
        activity.deny(GameRuleType.THROW_ITEMS);
        activity.deny(GameRuleType.TRIDENTS_LOYAL_IN_VOID);
        activity.deny(GameRuleType.UNSTABLE_TNT);
    }

    public static GameOpenProcedure open(GameOpenContext<DoomConfig> context) {
        DoomConfig config = context.config();

        if (!NucleDoom.WADS.containsKey(config.wadFile())) {
            throw new GameOpenException(Component.literal("Missing wad file! " + config.wadFile()));
        }

        for (var iwad : config.pwads()) {
            if (!NucleDoom.WADS.containsKey(iwad)) {
                throw new GameOpenException(Component.literal("Missing wad file! " + config.wadFile()));
            }
        }

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setDimensionType(BuiltinDimensionTypes.OVERWORLD_CAVES)
                .setGenerator(new VoidChunkGenerator(context.server()));


        return context.openWithWorld(worldConfig, (activity, world) -> {
            //noinspection unchecked
            var name = GameConfig.name((Holder<GameConfig<?>>) (Object) context.gameConfig()).getString();
            DoomGameController phase = new DoomGameController(
                    activity.getGameSpace(), world, config, new GameHandler(config, name, activity.getGameSpace().getServer()));

            phase.setupActivity(activity);
        });
    }

    private void setupActivity(GameActivity activity) {
        DoomGameController.setRules(activity);

        PlayerLimiter.addTo(activity, new PlayerLimiterConfig(1));
        // Listeners
        activity.listen(GamePlayerEvents.ADD, this);
        activity.listen(GameActivityEvents.ENABLE, this);
        activity.listen(GameActivityEvents.DESTROY, this);
        activity.listen(GameActivityEvents.TICK, this);
        activity.listen(GamePlayerEvents.ACCEPT, this);
        activity.listen(PlayerDamageEvent.EVENT, this);
        activity.listen(PlayerDeathEvent.EVENT, this);
        activity.listen(GamePlayerEvents.REMOVE, this);
        activity.listen(PlayerC2SPacketEvent.EVENT, this);
        activity.listen(PlayerChatEvent.EVENT, this);
    }

    public void updateSetup() {
        if (this.handler.getCanvas() != null && this.hasStarted) {
            world.setBlockAndUpdate(this.cameraEntity.blockPosition(), Blocks.AIR.defaultBlockState());
        }

        this.handler.updateCanvas(false, 1);

        var players = PlayerSet.EMPTY;
        if (this.display != null) {
            players = this.gameSpace.getPlayers();
            this.display.destroy();
        }

        this.display = VirtualDisplay.builder(this.handler.getCanvas().getCanvas(), this.handler.getCanvas().getDisplayPos(), Direction.SOUTH)
                .invisible()
                .build();

        players.forEach(this.display::addPlayer);
        players.forEach(this.display.getCanvas()::addPlayer);

        cameraEntity.setPos(handler.getSpawnPos());
        cameraEntity.setYRot(handler.getSpawnAngle());
        leftAudio.setPos(handler.getSpawnPos().add(2, 0, 0));
        rightAudio.setPos(handler.getSpawnPos().add(-2, 0, 0));
        if (this.hasStarted) {
            world.setBlockAndUpdate(this.cameraEntity.blockPosition(), Blocks.BARRIER.defaultBlockState());
        }
    }

    // Listeners
    @Override
    public void onAddPlayer(ServerPlayer player) {
        if (this.display != null) {
            this.display.addPlayer(player);
            this.display.getCanvas().addPlayer(player);
        }
        player.connection.send(new ClientboundSetCameraPacket(this.cameraEntity));

        player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
    }

    @Override
    public void onDestroy(GameCloseReason reason) {
        this.handler.destroy();
        this.display.destroy();
        this.display.getCanvas().destroy();
        this.thread.interrupt();
    }

    @Override
    public EventResult onPacket(ServerPlayer player, Packet<?> packet) {
        if (packet instanceof ServerboundSetCreativeModeSlotPacket) {
            return EventResult.DENY;
        }

        if (packet instanceof ServerboundPlayerLoadedPacket) {
            player.connection.send(new ClientboundSetCameraPacket(this.cameraEntity));
            player.connection.send(new ClientboundSetHeldSlotPacket(8));
            return EventResult.PASS;
        }

        if (this.player != player) {
            return EventResult.PASS;
        }

        if (packet instanceof ServerboundPlayerInputPacket(net.minecraft.world.entity.player.Input input)) {
            this.handler.updateKeyboard(input);
            return EventResult.DENY;
        } else if (packet instanceof ServerboundSetCarriedItemPacket updateSelectedSlotC2SPacket) {
            if (updateSelectedSlotC2SPacket.getSlot() == 7 && NucleDoom.IS_DEV) {
                this.updateSetup();
            }

            if (updateSelectedSlotC2SPacket.getSlot() != 8) {
                this.handler.selectSlot(updateSelectedSlotC2SPacket.getSlot());
            }
            player.connection.send(new ClientboundSetHeldSlotPacket(8));
            return EventResult.DENY;
        } else if (packet instanceof ServerboundPlayerCommandPacket clientCommandC2SPacket) {
            if (clientCommandC2SPacket.getAction() == ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY) {
                this.handler.pressE();
            }
            return EventResult.DENY;
        } else if (packet instanceof ServerboundUseItemOnPacket blockC2SPacket) {
            this.handler.pressMouseRight(true);
            return EventResult.DENY;
        } else if (packet instanceof ServerboundClientTickEndPacket clientTickEndC2SPacket) {
            this.handler.clientTick();
            return EventResult.PASS;
        } else if (packet instanceof ServerboundMovePlayerPacket.Rot playerMoveC2SPacket) {
            this.handler.updateMousePosition(playerMoveC2SPacket.getYRot(0), playerMoveC2SPacket.getXRot(0));
            return EventResult.DENY;
        } else if (packet instanceof ServerboundPlayerActionPacket playerActionC2SPacket) {
            switch (playerActionC2SPacket.getAction()) {
                case DROP_ITEM, DROP_ALL_ITEMS -> this.handler.pressQ();
                case SWAP_ITEM_WITH_OFFHAND -> this.handler.pressF();
                case START_DESTROY_BLOCK -> this.handler.pressMouseLeft(true);
                case ABORT_DESTROY_BLOCK -> this.handler.pressMouseLeft(false);
                //case RELEASE_USE_ITEM -> this.canvas.pressMouseRight(false);
            }
            return EventResult.DENY;
        }

        return EventResult.PASS;
    }

    @Override
    public void onTick() {
        if (this.player == null) {
            return;
        }

        if (!this.hasStarted) {
            this.thread.start();
            world.setBlockAndUpdate(this.cameraEntity.blockPosition(), Blocks.BARRIER.defaultBlockState());
            this.hasStarted = true;
        }

        for (var player : this.gameSpace.getPlayers()) {
            if (player.getCamera() != this.cameraEntity && this.cameraEntity.tickCount > 8) {
                player.setCamera(this.cameraEntity);
            }
        }
        this.handler.tick();
        this.player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.BLOCKS));
    }


    @Override
    public void onEnable() {

    }

    protected void runThread() {
        this.handler.start(DoomGame::create);
    }

    // /game open {type:"consolebox:console_box", game:"consolebox:cart"}
    @Override
    public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
        Vec3 spawnPos = this.handler.getSpawnPos().add(0, -EntityType.PLAYER.getHeight() + 0.2, 0);

        if (acceptor.intent().canPlay()) {
            return acceptor.teleport(this.world, spawnPos, this.handler.getSpawnAngle(), 0).thenRunForEach(player -> {
                this.player = player;
                this.spawnMount(spawnPos.add(0, 0, 1), player);
                this.initializePlayer(player, GameType.SURVIVAL);
            });
        }

        return acceptor.teleport(this.world, spawnPos, this.handler.getSpawnAngle(), 0).thenRunForEach(player -> {
            this.initializePlayer(player, GameType.SPECTATOR);
        });
    }

    @Override
    public EventResult onDamage(ServerPlayer player, DamageSource source, float damage) {
        return EventResult.DENY;
    }

    @Override
    public EventResult onDeath(ServerPlayer player, DamageSource source) {
        return EventResult.DENY;
    }

    @Override
    public void onRemovePlayer(ServerPlayer player) {
        this.display.removePlayer(player);
        this.display.getCanvas().removePlayer(player);

        if (player.getVehicle() != null) {
            player.getVehicle().discard();
        }

        if (player == this.player) {
            this.gameSpace.close(GameCloseReason.FINISHED);
        }
    }

    // Utilities
    private void spawnMount(Vec3 playerPos, ServerPlayer player) {
        var mount = EntityType.MULE.create(this.world, EntitySpawnReason.JOCKEY);
        mount.refreshDimensions();
        double y = playerPos.y() - 0.1f;
        mount.setPosRaw(playerPos.x(), y, playerPos.z());
        mount.setYRot(this.handler.getSpawnAngle());

        mount.setNoAi(true);
        mount.setNoGravity(true);
        mount.setSilent(true);
        mount.setPersistenceRequired();
        mount.setInvulnerable(true);
        mount.getAttribute(Attributes.SCALE).setBaseValue(0);

        // Prevent mount from being visible
        mount.addEffect(this.createInfiniteStatusEffect(MobEffects.INVISIBILITY));
        mount.setInvisible(true);

        // Remove mount hearts from HUD
        mount.getAttribute(Attributes.MAX_HEALTH).setBaseValue(0);

        this.world.addFreshEntity(mount);
        player.startRiding(mount, true, false);
        player.displayClientMessage(Component.empty(), true);
    }

    protected void initializePlayer(ServerPlayer player, GameType gameMode) {
        player.setGameMode(gameMode);
        player.setInvisible(true);
        player.setInvulnerable(true);
        player.addEffect(this.createInfiniteStatusEffect(MobEffects.NIGHT_VISION));
        player.addEffect(this.createInfiniteStatusEffect(MobEffects.INVISIBILITY));
        player.getAttributes().getInstance(Attributes.ATTACK_SPEED).setBaseValue(9999999);
        for (int i = 0; i < 9; i++) {
            var stack = Items.STICK.getDefaultInstance();
            stack.set(DataComponents.ITEM_MODEL, Identifier.parse("air"));
            stack.set(DataComponents.ITEM_NAME, Component.empty());
            stack.set(DataComponents.CONSUMABLE, new Consumable(Float.MAX_VALUE, ItemUseAnimation.NONE, Holder.direct(SoundEvents.EMPTY), false, List.of()));
            player.getInventory().setItem(i, stack);
        }
        player.getAbilities().instabuild = false;
        player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
        player.setYRot(this.handler.getSpawnAngle());
        player.setXRot(Float.MIN_VALUE);
        player.displayClientMessage(Component.empty(), true);
    }

    private MobEffectInstance createInfiniteStatusEffect(Holder<MobEffect> statusEffect) {
        return new MobEffectInstance(statusEffect, MobEffectInstance.INFINITE_DURATION, 0, true, false);
    }

    @Override
    public void playSound(SoundEvent soundEvent, float pitch, float volume, long seed) {
        this.gameSpace.getPlayers().sendPacket(new ClientboundSoundEntityPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent),
                SoundSource.MASTER,
                this.cameraEntity,
                volume,
                pitch,
                seed
        ));
    }

    @Override
    public void close() {
        this.gameSpace.close(GameCloseReason.FINISHED);
    }

    @Override
    public @Nullable PlayerSaveData getSaveData() {
        var config = this.gameSpace.getMetadata().sourceConfig();
        var saveId = this.config.saveName().or(() -> config.unwrapKey().map(ResourceKey::identifier)).orElse(NucleDoom.identifier("unknown"));
        return this.config.saves() ? new PlayerSaveData(this.gameSpace.getServer().getWorldPath(LevelResource.ROOT).resolve("nucledoom_playerdata").resolve(this.player.getStringUUID()).resolve(saveId.toDebugFileName())) : null;
    }

    public static GameOpenProcedure openNbs(GameOpenContext<DoomConfig> context) {
        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setDimensionType(BuiltinDimensionTypes.OVERWORLD_CAVES)
                .setGenerator(new VoidChunkGenerator(context.server()));


        return context.openWithWorld(worldConfig, (activity, world) -> {
            //noinspection unchecked
            var name = GameConfig.name((Holder<GameConfig<?>>) (Object) context.gameConfig()).getString();
            DoomGameController phase = new DoomGameController(
                    activity.getGameSpace(), world, context.config(), new GameHandler(context.config(), name, activity.getGameSpace().getServer())) {
                @Override
                protected void runThread() {
                    this.handler.start(((handler1, saveData, config1, resourceManager) -> new DoomGame.Open(new NBSPlayer(handler1, saveData, config1, resourceManager), null)));
                }

                @Override
                protected void initializePlayer(ServerPlayer player, GameType gameMode) {
                    super.initializePlayer(player, GameType.SPECTATOR);
                }
            };

            phase.setupActivity(activity);
        });
    }

    @Override
    public EventResult onSendChatMessage(ServerPlayer serverPlayer, PlayerChatMessage playerChatMessage, ChatType.Bound bound) {
        if (bound.chatType().is(ChatType.CHAT)) {
            return this.handler.onChat(playerChatMessage.signedContent()) ? EventResult.DENY : EventResult.PASS;
        }
        return EventResult.PASS;
    }
}