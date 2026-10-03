package me.p0x38.fabric.client.unknownentity;

import net.minecraft.client.Minecraft;

public enum ChatConnectionMode {
    SINGLEPLAYER, LAN, MULTIPLAYER, DISCONNECTED;

    public static ChatConnectionMode detect(Minecraft client) {
        if (client.level == null || client.player == null) return DISCONNECTED;
        if (!client.hasSingleplayerServer()) return MULTIPLAYER;
        return client.level.players().size() > 1 ? LAN : SINGLEPLAYER;
    }
}
