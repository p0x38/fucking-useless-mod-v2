package me.p0x38.fabric.client;

import me.p0x38.fabric.client.chatentity.ChatConnectionMode;
import me.p0x38.fabric.client.chatentity.ChatEntity;
import me.p0x38.fabric.client.chatentity.ChatEntityBrain;
import me.p0x38.fabric.client.chatentity.Memory;
import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.util.DebugLogger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class ChatEntityManager {
    private static final int MIN_HIDDEN_TICKS = 10;
    private static final int MAX_HIDDEN_TICKS = 60;
    private static final double LOOK_DOT_THRESHOLD = 0.75;
    private static final double LOOK_DISTANCE = 6.0;

    private static final long IDLE_SHORT_TICKS = 15 * 20L;
    private static final long IDLE_MEDIUM_TICKS = 30 * 20L;
    private static final long IDLE_LONG_TICKS = 60 * 20L;
    private static final long IDLE_VERY_LONG_TICKS = 3 * 60 * 20L;
    private static final long IDLE_EXTREME_TICKS = 5 * 60 * 20L;
    private static final long IDLE_VERY_EXTREME_TICKS = 10 * 60 * 20L;

    private static final double IDLE_MOVEMENT_THRESHOLD_SQR = 0.0025;
    private static final float IDLE_ROTATION_THRESHOLD = 1.0F;

    private static final Map<BlockPos, ChatEntity> ENTITIES = new HashMap<>();

    private static ClientLevel observedLevel;
    private static BlockPos observedOrigin;
    private static long hiddenSince = Long.MIN_VALUE;
    private static boolean entityActivatedWhileHidden;

    private static long playerIdleSince = Long.MIN_VALUE;
    private static int lastIdleChatlineStage;
    private static boolean activityStateInitialized;
    private static double lastActivityX;
    private static double lastActivityY;
    private static double lastActivityZ;
    private static float lastActivityYaw;
    private static float lastActivityPitch;

    private ChatEntityManager() {}

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        Player player = client.player;

        if (level == null || player == null) {
            reset();
            return;
        }

        if (observedLevel != null && observedLevel != level) {
            DebugLogger.debug(
                    "[ChatEntityManager] world changed old={} new={}",
                    observedLevel.dimension(),
                    level.dimension()
            );
            reset();
        }

        observedLevel = level;

        BlockPos lookedAt = getLookedAtBlock(level, player, client);

        if (observedOrigin == null) {
            if (lookedAt == null) {
                return;
            }

            observedOrigin = lookedAt;
            hiddenSince = Long.MIN_VALUE;
            entityActivatedWhileHidden = false;

            DebugLogger.debug(
                    "[ChatEntityManager] observing origin={}",
                    observedOrigin
            );
        }

        ChatEntity entity = ENTITIES.computeIfAbsent(
                observedOrigin,
                ChatEntity::new
        );

        long gameTick = level.getGameTime();
        boolean looking =
                lookedAt != null
                        && observedOrigin.equals(lookedAt);

        updatePlayerActivity(
                player,
                gameTick
        );

        entity.observe(
                looking,
                gameTick,
                ChatConnectionMode.detect(client),
                player.getGameProfile().name()
        );

        entity.observeLocation(
                gameTick,
                level.dimension().identifier().toString(),
                player.blockPosition(),
                player.getX(),
                player.getY(),
                player.getZ()
        );

        processPendingResponses(
                entity,
                level,
                gameTick
        );

        processIdleChatline(
                entity,
                level,
                gameTick
        );

        if (looking) {
            hiddenSince = Long.MIN_VALUE;
            entityActivatedWhileHidden = false;
            return;
        }

        if (hiddenSince == Long.MIN_VALUE) {
            hiddenSince = gameTick;
            return;
        }

        long hiddenTicks = gameTick - hiddenSince;

        if (entityActivatedWhileHidden
                || hiddenTicks < MIN_HIDDEN_TICKS
                || hiddenTicks > MAX_HIDDEN_TICKS
                || ThreadLocalRandom.current().nextDouble() >= 0.03) {
            return;
        }

        entityActivatedWhileHidden = true;
        entity.activate();

        entity.remember(
                new Memory(
                        Memory.Type.WORLD_CHANGED,
                        gameTick,
                        0.85f,
                        entity.origin(),
                        Map.of("phase", "hidden")
                )
        );

        DebugLogger.debug(
                "[ChatEntityManager] entity activated id={} hiddenTicks={}",
                entity.id(),
                hiddenTicks
        );

        think(entity, level);
    }

    public static boolean talkToChatEntity(String message) {
        Minecraft client = Minecraft.getInstance();

        if (client.level == null
                || client.player == null
                || observedLevel != client.level
                || observedOrigin == null
                || message == null
                || message.isBlank()) {
            return false;
        }

        ChatEntity entity = ENTITIES.get(observedOrigin);

        if (entity == null || !entity.isActive()) {
            return false;
        }

        String username = client.player.getGameProfile().name();
        String trimmed = message.trim();
        long gameTick = client.level.getGameTime();

        resetPlayerIdleTimer(
                client.player,
                gameTick
        );

        client.gui.getChat().addMessage(
                Component.literal("<" + username + "> " + trimmed)
        );

        entity.interact(
                trimmed,
                client.level.getGameTime(),
                ChatConnectionMode.detect(client),
                username
        );

        think(entity, client.level);
        return true;
    }

    public static String getObservedChatEntityId() {
        ChatEntity entity = getObservedChatEntity();
        return entity == null ? null : entity.id();
    }

    public static String getObservedChatEntityInfo() {
        ChatEntity entity = getObservedChatEntity();

        if (entity == null) {
            return null;
        }

        return "Chat Entity ID: " + entity.id()
                + " | active: " + entity.isActive()
                + " | mode: " + entity.connectionMode().name().toLowerCase()
                + " | mood: " + entity.mood().name().toLowerCase()
                + " | awareness: " + entity.awareness()
                + " | suspicion: " + entity.suspicion()
                + " | curiosity: " + entity.curiosity()
                + " | trust: " + entity.trust()
                + " | irritation: " + entity.irritation()
                + " | interactions: " + entity.interactionCount()
                + " | memories: " + entity.memories().size();
    }

    private static ChatEntity getObservedChatEntity() {
        if (observedLevel == null || observedOrigin == null) {
            return null;
        }

        return ENTITIES.get(observedOrigin);
    }

    private static void think(
            ChatEntity entity,
            ClientLevel level
    ) {
        ChatEntityBrain.think(
                entity,
                level
        );
    }

    private static void processIdleChatline(
            ChatEntity entity,
            ClientLevel level,
            long gameTick
    ) {
        if (!entity.isActive()
                || entity.pendingResponseCount() > 0
                || playerIdleSince == Long.MIN_VALUE) {
            return;
        }

        long idleTicks =
                gameTick - playerIdleSince;

        int stage =
                getIdleChatlineStage(idleTicks);

        if (stage <= 0
                || stage <= lastIdleChatlineStage) {
            return;
        }

        Memory memory =
                new Memory(
                        Memory.Type.PLAYER_IDLE,
                        gameTick,
                        1.0f,
                        entity.origin(),
                        Map.of(
                                "idleTicks",
                                Long.toString(idleTicks),
                                "stage",
                                Integer.toString(stage)
                        )
                );

        entity.remember(memory);
        lastIdleChatlineStage = stage;

        DebugLogger.debug(
                "[ChatEntityManager] player idle id={} stage={} idleTicks={}",
                entity.id(),
                stage,
                idleTicks
        );

        think(
                entity,
                level
        );
    }

    private static int getIdleChatlineStage(long idleTicks) {
        if (idleTicks >= IDLE_VERY_EXTREME_TICKS) {
            return 6;
        }

        if (idleTicks >= IDLE_EXTREME_TICKS) {
            return 5;
        }

        if (idleTicks >= IDLE_VERY_LONG_TICKS) {
            return 4;
        }

        if (idleTicks >= IDLE_LONG_TICKS) {
            return 3;
        }

        if (idleTicks >= IDLE_MEDIUM_TICKS) {
            return 2;
        }

        if (idleTicks >= IDLE_SHORT_TICKS) {
            return 1;
        }

        return 0;
    }

    private static void updatePlayerActivity(
            Player player,
            long gameTick
    ) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        if (!activityStateInitialized) {
            activityStateInitialized = true;
            lastActivityX = x;
            lastActivityY = y;
            lastActivityZ = z;
            lastActivityYaw = yaw;
            lastActivityPitch = pitch;
            playerIdleSince = gameTick;
            lastIdleChatlineStage = 0;
            return;
        }

        double dx = x - lastActivityX;
        double dy = y - lastActivityY;
        double dz = z - lastActivityZ;

        boolean active =
                dx * dx + dy * dy + dz * dz
                        >= IDLE_MOVEMENT_THRESHOLD_SQR
                        || Math.abs(
                                net.minecraft.util.Mth.wrapDegrees(
                                        yaw - lastActivityYaw
                                )
                        ) >= IDLE_ROTATION_THRESHOLD
                        || Math.abs(pitch - lastActivityPitch)
                                >= IDLE_ROTATION_THRESHOLD
                        || player.isUsingItem()
                        || player.isCrouching()
                        || player.isSprinting();

        if (active) {
            playerIdleSince = gameTick;
            lastIdleChatlineStage = 0;
        }

        lastActivityX = x;
        lastActivityY = y;
        lastActivityZ = z;
        lastActivityYaw = yaw;
        lastActivityPitch = pitch;
    }

    private static void resetPlayerIdleTimer(
            Player player,
            long gameTick
    ) {
        activityStateInitialized = true;
        playerIdleSince = gameTick;
        lastIdleChatlineStage = 0;
        lastActivityX = player.getX();
        lastActivityY = player.getY();
        lastActivityZ = player.getZ();
        lastActivityYaw = player.getYRot();
        lastActivityPitch = player.getXRot();
    }

    private static void processPendingResponses(
            ChatEntity entity,
            ClientLevel level,
            long gameTick
    ) {
        ChatEntity.PendingResponse response =
                entity.pollDueResponse(gameTick);

        if (response == null) {
            return;
        }

        entity.deliverResponse(
                response,
                gameTick
        );

        showChatEntityMessage(
                response.message()
        );

        /*
         * The brain can now observe CHAT_ENTITY_SPOKE because the response
         * has actually been delivered to the player.
         */
        think(
                entity,
                level
        );
    }

    private static void showChatEntityMessage(String message) {

        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal(message).withStyle(
                        ChatFormatting.DARK_AQUA
                )
        );
    }

    private static BlockPos getLookedAtBlock(
            ClientLevel level,
            Player player,
            Minecraft client
    ) {
        Vec3 cameraPosition =
                client.gameRenderer.getMainCamera().position();

        var forwardVector =
                client.gameRenderer.getMainCamera().forwardVector();

        Vec3 forward = new Vec3(
                forwardVector.x(),
                forwardVector.y(),
                forwardVector.z()
        );

        BlockHitResult hit = level.clip(
                new ClipContext(
                        cameraPosition,
                        cameraPosition.add(forward.scale(LOOK_DISTANCE)),
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        player
                )
        );

        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        Vec3 toBlock =
                Vec3.atCenterOf(hit.getBlockPos())
                        .subtract(cameraPosition);

        if (toBlock.lengthSqr() <= 0.0001
                || forward.dot(toBlock.normalize()) < LOOK_DOT_THRESHOLD) {
            return null;
        }

        return hit.getBlockPos();
    }

    private static void reset() {
        ENTITIES.clear();
        observedLevel = null;
        observedOrigin = null;
        hiddenSince = Long.MIN_VALUE;
        entityActivatedWhileHidden = false;
        playerIdleSince = Long.MIN_VALUE;
        lastIdleChatlineStage = 0;
        activityStateInitialized = false;
        lastActivityX = 0.0;
        lastActivityY = 0.0;
        lastActivityZ = 0.0;
        lastActivityYaw = 0.0F;
        lastActivityPitch = 0.0F;
    }
}
