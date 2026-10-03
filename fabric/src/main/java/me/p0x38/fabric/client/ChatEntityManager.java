package me.p0x38.fabric.client;

import me.p0x38.fabric.client.chatentity.ChatConnectionMode;
import me.p0x38.fabric.client.chatentity.ChatEntity;
import me.p0x38.fabric.client.chatentity.ChatEntityBrain;
import me.p0x38.fabric.client.chatentity.Memory;
import me.p0x38.fuckinguselessmod.util.DebugLogger;
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

    private static final Map<BlockPos, ChatEntity> ENTITIES = new HashMap<>();

    private static ClientLevel observedLevel;
    private static BlockPos observedOrigin;
    private static long hiddenSince = Long.MIN_VALUE;
    private static boolean entityActivatedWhileHidden;

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

        entity.observe(
                looking,
                gameTick,
                ChatConnectionMode.detect(client),
                player.getGameProfile().name()
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

    private static void think(ChatEntity entity, ClientLevel level) {
        String before = entity.currentMessage();

        ChatEntityBrain.think(entity, level);

        if (!java.util.Objects.equals(before, entity.currentMessage())) {
            showChatEntityMessage(
                    entity.currentMessage()
            );
        }
    }

    private static void showChatEntityMessage(String message) {
        String name = Component.translatable(
                "text.fuckinguselessmod.chat.entity.name"
        ).getString();

        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("<" + name + "> " + message)
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
    }
}
