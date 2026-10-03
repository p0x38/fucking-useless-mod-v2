package me.p0x38.fabric.client.unknownentity;

import net.minecraft.client.Minecraft;

/**
 * Describes the player's current Minecraft chat connection context.
 */
public enum ChatConnectionMode {
    /** A local singleplayer world. */
    SINGLEPLAYER,

    /** A local world opened to other players through LAN. */
    LAN,

    /** A connection to a multiplayer server. */
    MULTIPLAYER,

    /** No active world or player connection. */
    DISCONNECTED;

    /**
     * Determines the current connection mode from the client state.
     *
     * @param client the Minecraft client instance
     * @return the detected connection mode
     */
    public static ChatConnectionMode detect(Minecraft client) {
        if (client.level == null || client.player == null) return DISCONNECTED;
        if (!client.hasSingleplayerServer()) return MULTIPLAYER;
        return client.level.players().size() > 1 ? LAN : SINGLEPLAYER;
    }
}