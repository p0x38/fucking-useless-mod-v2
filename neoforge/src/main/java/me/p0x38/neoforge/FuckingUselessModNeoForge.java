package me.p0x38.neoforge;

import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import me.p0x38.fuckinguselessmod.config.ConfigScreen;
import me.p0x38.fuckinguselessmod.entity.ModEntities;
import me.p0x38.fuckinguselessmod.entity.UselessEntity;
import me.p0x38.fuckinguselessmod.transformers.ChatTransformer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(value = FuckingUselessMod.MOD_ID, dist = Dist.CLIENT)
public final class FuckingUselessModNeoForge {
    public FuckingUselessModNeoForge(
            ModContainer container,
            IEventBus modBus
    ) {
        FuckingUselessMod.init();

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (ignoredContainer, parent) -> ConfigScreen.create(parent)
        );

        modBus.addListener(
                FuckingUselessModNeoForge::registerAttributes
        );

        modBus.addListener(
                FuckingUselessModNeoForge::registerRenderers
        );


        NeoForge.EVENT_BUS.addListener(
                (ClientChatEvent event) ->
                        event.setMessage(
                                ChatTransformer.transform(event.getMessage())
                        )
        );
    }

    private static void registerAttributes(
            EntityAttributeCreationEvent event
    ) {
        event.put(
                ModEntities.USELESS_ENTITY,
                UselessEntity.createAttributes().build()
        );
    }

    private static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                ModEntities.USELESS_ENTITY,
                UselessEntityRenderer::new
        );
    }

    private static final class UselessEntityRenderer extends HumanoidMobRenderer<
            UselessEntity,
            HumanoidRenderState,
            HumanoidModel<HumanoidRenderState>
            > {

        private static final Identifier TEXTURE =
                Identifier.fromNamespaceAndPath(
                        FuckingUselessMod.MOD_ID,
                        "textures/entity/useless.png"
                );

        private UselessEntityRenderer(
                EntityRendererProvider.Context context
        ) {
            super(
                    context,
                    new HumanoidModel<>(
                            context.bakeLayer(ModelLayers.PLAYER)
                    ),
                    0.5F
            );
        }

        @Override
        public HumanoidRenderState createRenderState() {
            return new HumanoidRenderState();
        }

        @Override
        public Identifier getTextureLocation(
                HumanoidRenderState state
        ) {
            return TEXTURE;
        }
    }
}
