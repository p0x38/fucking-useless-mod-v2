package me.p0x38.fabric.client.renderers.noise;

import java.util.EnumSet;

public final class NoiseRenderMask {
    public enum Layer {
        BODY,
        ARMOR
    }

    private final EnumSet<Layer> enabledLayers = EnumSet.noneOf(Layer.class);

    public boolean isEnabled(Layer layer) {
        return enabledLayers.contains(layer);
    }

    public void setEnabled(Layer layer, boolean enabled) {
        if (enabled) {
            enabledLayers.add(layer);
        } else {
            enabledLayers.remove(layer);
        }
    }

    public void enable(Layer layer) {
        enabledLayers.add(layer);
    }

    public void disable(Layer layer) {
        enabledLayers.remove(layer);
    }

    public void toggle(Layer layer) {
        if (enabledLayers.contains(layer)) {
            enabledLayers.remove(layer);
        } else {
            enabledLayers.add(layer);
        }
    }

    public void enableAll() {
        enabledLayers.addAll(EnumSet.allOf(Layer.class));
    }

    public void disableAll() {
        enabledLayers.clear();
    }
}
