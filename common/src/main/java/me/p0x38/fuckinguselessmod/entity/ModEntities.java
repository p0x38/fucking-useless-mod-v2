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
    public static final EntityType<ChatEntity> CHAT_ENTITY = register(
            "chat_entity",
            EntityType.Builder.of(ChatEntity::new, MobCategory.MISC)
                    .sized(.6f, 1.8f)
    );

    private ModEntities() {}

    // Keep this stuff empty unless we need to add stuff here
    public static void initialize() {}

    private static <T extends Entity> EntityType<T> register(
            String id,
            EntityType.Builder<T> builder
    ) {
        Identifier location = Identifier.fromNamespaceAndPath(FuckingUselessMod.MOD_ID, id);

        return Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                location,
                builder.build(
                        ResourceKey.create(Registries.ENTITY_TYPE, location)
                )
        );
    }
}
