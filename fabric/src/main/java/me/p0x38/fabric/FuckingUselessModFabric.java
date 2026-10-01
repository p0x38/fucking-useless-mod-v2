package me.p0x38.fabric;

import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import me.p0x38.fuckinguselessmod.entity.ModEntities;
import me.p0x38.fuckinguselessmod.entity.UselessEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public final class FuckingUselessModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FuckingUselessMod.init();

        FabricDefaultAttributeRegistry.register(
                ModEntities.USELESS_ENTITY,
                UselessEntity.createAttributes()
        );
    }
}
