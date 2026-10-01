package me.p0x38.fuckinguselessmod.entity;

import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<UselessEntity> USELESS_ENTITY = register(
            "useless",
            EntityType.Builder.<UselessEntity>of(
                    UselessEntity::new,
                    MobCategory.CREATURE
            )
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
    );

    private ModEntities() {}

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(
                        FuckingUselessMod.MOD_ID,
                        name
                )
        );

        return Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                key,
                builder.build(key)
        );
    }
}
