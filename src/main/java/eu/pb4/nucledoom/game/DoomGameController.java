package eu.pb4.nucledoom.game;

import eu.pb4.mapcanvas.api.utils.VirtualDisplay;
import eu.pb4.nucledoom.NucleDoom;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.dimension.DimensionTypes;
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
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerC2SPacketEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class DoomGameController implements GameCanvas.PlayerInterface, GamePlayerEvents.Add, GameActivityEvents.Destroy, GameActivityEvents.Tick, GameActivityEvents.Enable, GamePlayerEvents.Remove, GamePlayerEvents.Accept, PlayerDamageEvent, PlayerDeathEvent, PlayerC2SPacketEvent {
    private final Thread thread;
    private final GameSpace gameSpace;
    private final ServerWorld world;
    private final DoomConfig config;
    private final GameCanvas canvas;
    private final VirtualDisplay display;
    private final Entity cameraEntity;
    private ServerPlayerEntity player;
    private boolean hasStarted = false;

    public DoomGameController(GameSpace gameSpace, ServerWorld world, DoomConfig config, GameCanvas canvas, Entity cameraEntity, VirtualDisplay display) {
        this.gameSpace = gameSpace;
        this.world = world;
        this.config = config;

        this.cameraEntity = cameraEntity;
        this.canvas = canvas;
        this.display = display;
        this.thread = new Thread(this::runThread);
        this.thread.setDaemon(true);
        canvas.setPlayerInterface(this);
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
            throw new GameOpenException(Text.literal("Missing wad file! " + config.wadFile()));
        }

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setDimensionType(DimensionTypes.OVERWORLD_CAVES)
                .setGenerator(new VoidChunkGenerator(context.server()));



        return context.openWithWorld(worldConfig, (activity, world) -> {
            //noinspection unchecked
            GameCanvas canvas = new GameCanvas(config,
                    GameConfig.name((RegistryEntry<GameConfig<?>>) (Object) context.gameConfig()).getString(),
                    activity.getGameSpace().getServer());

            VirtualDisplay display = VirtualDisplay.builder(canvas.getCanvas(), canvas.getDisplayPos(), Direction.SOUTH)
                    .invisible()
                    .build();


            var camera = EntityType.ITEM_DISPLAY.create(world, SpawnReason.LOAD);
            assert camera != null;
            camera.setInvisible(true);
            camera.setPosition(canvas.getSpawnPos());
            camera.setYaw(canvas.getSpawnAngle());
            world.spawnEntity(camera);

            var leftAudio = EntityType.ITEM_DISPLAY.create(world, SpawnReason.LOAD);
            assert leftAudio != null;
            leftAudio.setInvisible(true);
            leftAudio.setPosition(canvas.getSpawnPos().add(2, 0, 0));
            world.spawnEntity(leftAudio);

            var rightAudio = EntityType.ITEM_DISPLAY.create(world, SpawnReason.LOAD);
            assert rightAudio != null;
            rightAudio.setInvisible(true);
            rightAudio.setPosition(canvas.getSpawnPos().add(-2, 0, 0));
            world.spawnEntity(rightAudio);

            DoomGameController phase = new DoomGameController(activity.getGameSpace(), world, config, canvas, camera, display);
            //audioController.setOutput(camera, leftAudio, rightAudio, activity.getGameSpace().getPlayers()::sendPacket);
            DoomGameController.setRules(activity);


            PlayerLimiter.addTo(activity, new PlayerLimiterConfig(1));

            // Listeners
            activity.listen(GamePlayerEvents.ADD, phase);
            activity.listen(GameActivityEvents.ENABLE, phase);
            activity.listen(GameActivityEvents.DESTROY, phase);
            activity.listen(GameActivityEvents.TICK, phase);
            activity.listen(GamePlayerEvents.ACCEPT, phase);
            activity.listen(PlayerDamageEvent.EVENT, phase);
            activity.listen(PlayerDeathEvent.EVENT, phase);
            activity.listen(GamePlayerEvents.REMOVE, phase);
            activity.listen(PlayerC2SPacketEvent.EVENT, phase);
        });
    }

    // Listeners
    @Override
    public void onAddPlayer(ServerPlayerEntity player) {
        this.display.addPlayer(player);
        this.display.getCanvas().addPlayer(player);
        player.networkHandler.sendPacket(new SetCameraEntityS2CPacket(this.cameraEntity));

        player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));
    }

    @Override
    public void onDestroy(GameCloseReason reason) {
        this.canvas.destroy();
        this.display.destroy();
        this.display.getCanvas().destroy();
        this.thread.interrupt();
    }

    @Override
    public EventResult onPacket(ServerPlayerEntity player, Packet<?> packet) {
        if (packet instanceof CreativeInventoryActionC2SPacket) {
            return EventResult.DENY;
        }

        if (this.player != player) {
            return EventResult.PASS;
        }

        if (packet instanceof PlayerInputC2SPacket playerInputC2SPacket) {
            this.canvas.updateKeyboard(playerInputC2SPacket.input());
            return EventResult.DENY;
        } else if (packet instanceof UpdateSelectedSlotC2SPacket updateSelectedSlotC2SPacket) {
            if (updateSelectedSlotC2SPacket.getSelectedSlot() != 8) {
                this.canvas.selectSlot(updateSelectedSlotC2SPacket.getSelectedSlot());
            }
            player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(9));
            return EventResult.DENY;
        } else if (packet instanceof ClientCommandC2SPacket clientCommandC2SPacket) {
            if (clientCommandC2SPacket.getMode() == ClientCommandC2SPacket.Mode.OPEN_INVENTORY) {
                this.canvas.pressE();
            }
            return EventResult.DENY;
        } else if (packet instanceof PlayerInteractItemC2SPacket blockC2SPacket) {
            this.canvas.pressMouseRight(true);
            return EventResult.DENY;
        }  else if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround playerMoveC2SPacket) {
            this.canvas.updateMousePosition(playerMoveC2SPacket.getYaw(0), playerMoveC2SPacket.getPitch(0));
            return EventResult.DENY;
        } else if (packet instanceof PlayerActionC2SPacket playerActionC2SPacket) {
            switch (playerActionC2SPacket.getAction()) {
                case DROP_ITEM, DROP_ALL_ITEMS -> this.canvas.pressQ();
                case SWAP_ITEM_WITH_OFFHAND -> this.canvas.pressF();
                case START_DESTROY_BLOCK -> this.canvas.pressMouseLeft(true);
                case ABORT_DESTROY_BLOCK -> this.canvas.pressMouseLeft(false);
                case RELEASE_USE_ITEM -> this.canvas.pressMouseRight(false);
            }
            return EventResult.DENY;
        } else if (packet instanceof PlayerLoadedC2SPacket) {
            player.networkHandler.sendPacket(new SetCameraEntityS2CPacket(this.cameraEntity));
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
            world.setBlockState(this.cameraEntity.getBlockPos(), Blocks.BARRIER.getDefaultState());
            this.hasStarted = true;
        }

        for (var player : this.gameSpace.getPlayers()) {
            if (player.getCameraEntity() != this.cameraEntity) {
                player.setCameraEntity(this.cameraEntity);
            }
        }
        this.canvas.tick();
        this.player.networkHandler.sendPacket(new StopSoundS2CPacket(null, SoundCategory.BLOCKS));
    }


    @Override
    public void onEnable() {

    }

    private void runThread() {
        this.canvas.start();
    }

    // /game open {type:"consolebox:console_box", game:"consolebox:cart"}
    @Override
    public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
        Vec3d spawnPos = this.canvas.getSpawnPos().add(0, -EntityType.PLAYER.getHeight() + 0.2, 0);

        if (acceptor.intent().canPlay()) {
            return acceptor.teleport(this.world, spawnPos, this.canvas.getSpawnAngle(), 0).thenRunForEach(player -> {
                this.player = player;
                this.spawnMount(spawnPos.add(0, 0, 1), player);
                this.initializePlayer(player, GameMode.SURVIVAL);
            });
        }

        return acceptor.teleport(this.world, spawnPos, this.canvas.getSpawnAngle(), 0).thenRunForEach(player -> {
            this.initializePlayer(player, GameMode.SPECTATOR);
        });
    }

    @Override
    public EventResult onDamage(ServerPlayerEntity player, DamageSource source, float damage) {
        return EventResult.DENY;
    }

    @Override
    public EventResult onDeath(ServerPlayerEntity player, DamageSource source) {
        return EventResult.DENY;
    }

    @Override
    public void onRemovePlayer(ServerPlayerEntity player) {
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
    private void spawnMount(Vec3d playerPos, ServerPlayerEntity player) {
        var mount = EntityType.MULE.create(this.world, SpawnReason.JOCKEY);
        mount.calculateDimensions();
        double y = playerPos.getY() - 2.25f;
        mount.setPos(playerPos.getX(), y, playerPos.getZ());
        mount.setYaw(this.canvas.getSpawnAngle());

        mount.setAiDisabled(true);
        mount.setNoGravity(true);
        mount.setSilent(true);
        mount.setPersistent();
        mount.setInvulnerable(true);
        mount.getAttributeInstance(EntityAttributes.SCALE).setBaseValue(0);

        // Prevent mount from being visible
        mount.addStatusEffect(this.createInfiniteStatusEffect(StatusEffects.INVISIBILITY));
        mount.setInvisible(true);

        // Remove mount hearts from HUD
        mount.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(0);

        this.world.spawnEntity(mount);
        player.startRiding(mount, true);
        player.sendMessage(Text.empty(), true);
    }

    private void initializePlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.changeGameMode(gameMode);
        player.setInvisible(true);
        player.setInvulnerable(true);
        player.addStatusEffect(this.createInfiniteStatusEffect(StatusEffects.NIGHT_VISION));
        player.addStatusEffect(this.createInfiniteStatusEffect(StatusEffects.INVISIBILITY));
        player.getAttributes().getCustomInstance(EntityAttributes.ATTACK_SPEED).setBaseValue(9999999);
        for (int i = 0; i < 9; i++) {
            var stack = Items.STICK.getDefaultStack();
            stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("air"));
            stack.set(DataComponentTypes.ITEM_NAME, Text.empty());
            stack.set(DataComponentTypes.CONSUMABLE, new ConsumableComponent(Float.MAX_VALUE, UseAction.NONE, RegistryEntry.of(SoundEvents.INTENTIONALLY_EMPTY), false, List.of()));
            player.getInventory().main.set(i, stack);
        }
        player.getAbilities().creativeMode = false;
        player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));
        player.setYaw(this.canvas.getSpawnAngle());
        player.setPitch(Float.MIN_VALUE);
        player.sendMessage(Text.empty(), true);
    }

    private StatusEffectInstance createInfiniteStatusEffect(RegistryEntry<StatusEffect> statusEffect) {
        return new StatusEffectInstance(statusEffect, StatusEffectInstance.INFINITE, 0, true, false);
    }

    @Override
    public void playSound(SoundEvent soundEvent, float pitch, float volume) {
        this.gameSpace.getPlayers().sendPacket(new PlaySoundFromEntityS2CPacket(
                Registries.SOUND_EVENT.getEntry(soundEvent),
                SoundCategory.MASTER,
                this.cameraEntity,
                volume,
                pitch,
                new Random().nextLong()
        ));
    }

    @Override
    public void close() {
        this.gameSpace.close(GameCloseReason.FINISHED);
    }
}