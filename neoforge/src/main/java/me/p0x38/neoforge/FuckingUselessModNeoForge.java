package me.p0x38.neoforge;

import me.p0x38.fuckinguselessmod.ChatTransformer;
import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import me.p0x38.fuckinguselessmod.config.ConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = FuckingUselessMod.MOD_ID, dist = Dist.CLIENT)
public final class FuckingUselessModNeoForge {
    public FuckingUselessModNeoForge(
            ModContainer container,
            EventBus modBus
    ) {
        FuckingUselessMod.init();

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (ignoredContainer, parent) -> ConfigScreen.create(parent)
        );

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (ClientChatEvent event) ->
                        event.setMessage(
                                ChatTransformer.transform(event.getMessage())
                        )
        );
    }
}
