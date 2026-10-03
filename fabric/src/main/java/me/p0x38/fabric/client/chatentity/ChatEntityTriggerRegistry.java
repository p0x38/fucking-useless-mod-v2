package me.p0x38.fabric.client.chatentity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.p0x38.fuckinguselessmod.util.DebugLogger;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.BufferedReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ChatEntityTriggerRegistry
        implements IdentifiableResourceReloadListener, ResourceManagerReloadListener {
    private static final Identifier RESOURCE_ID =
            Identifier.fromNamespaceAndPath(
                    "fuckinguselessmod",
                    "chat_entity/triggers.json"
            );

    public static final ChatEntityTriggerRegistry INSTANCE =
            new ChatEntityTriggerRegistry();

    private volatile Map<String, Pattern> patterns =
            Collections.emptyMap();

    private ChatEntityTriggerRegistry() {
    }

    public static boolean matches(
            String category,
            String message
    ) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        Pattern pattern = INSTANCE.patterns.get(category);

        return pattern != null
                && pattern.matcher(message).find();
    }

    @Override
    public Identifier getFabricId() {
        return RESOURCE_ID;
    }

    @Override
    public void onResourceManagerReload(
            ResourceManager resourceManager
    ) {
        Map<String, Pattern> loaded = new HashMap<>();

        Optional<Resource> resource =
                resourceManager.getResource(RESOURCE_ID);

        if (resource.isEmpty()) {
            DebugLogger.debug(
                    "[ChatEntityTriggers] missing resource {}",
                    RESOURCE_ID
            );
            patterns = Collections.emptyMap();
            return;
        }

        try (BufferedReader reader =
                     resource.get().openAsReader()) {
            JsonElement root =
                    JsonParser.parseReader(reader);

            if (!root.isJsonObject()) {
                throw new IllegalArgumentException(
                        "root must be an object"
                );
            }

            JsonElement categoriesElement =
                    root.getAsJsonObject().get("categories");

            if (categoriesElement == null
                    || !categoriesElement.isJsonObject()) {
                throw new IllegalArgumentException(
                        "missing categories object"
                );
            }

            JsonObject categories =
                    categoriesElement.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry :
                    categories.entrySet()) {
                JsonElement value = entry.getValue();

                if (!value.isJsonArray()) {
                    DebugLogger.debug(
                            "[ChatEntityTriggers] ignoring non-array category {}",
                            entry.getKey()
                    );
                    continue;
                }

                Pattern compiled =
                        compileCategory(
                                entry.getKey(),
                                value.getAsJsonArray()
                        );

                if (compiled != null) {
                    loaded.put(entry.getKey(), compiled);
                }
            }

            patterns =
                    Collections.unmodifiableMap(loaded);

            DebugLogger.debug(
                    "[ChatEntityTriggers] loaded {} trigger categories",
                    loaded.size()
            );
        } catch (Exception exception) {
            DebugLogger.debug(
                    "[ChatEntityTriggers] failed to load {}: {}",
                    RESOURCE_ID,
                    exception.getMessage()
            );

            patterns = Collections.emptyMap();
        }
    }

    private static Pattern compileCategory(
            String category,
            JsonArray entries
    ) {
        if (entries.isEmpty()) {
            return null;
        }

        StringBuilder combined =
                new StringBuilder(entries.size() * 32);
        int validPatterns = 0;

        for (JsonElement entry : entries) {
            if (!entry.isJsonPrimitive()
                    || !entry.getAsJsonPrimitive().isString()) {
                continue;
            }

            String regex =
                    entry.getAsString().strip();

            if (regex.isEmpty()) {
                continue;
            }

            if (validPatterns++ > 0) {
                combined.append('|');
            }

            combined.append("(?:")
                    .append(regex)
                    .append(')');
        }

        if (validPatterns == 0) {
            return null;
        }

        try {
            return Pattern.compile(
                    combined.toString(),
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            );
        } catch (PatternSyntaxException exception) {
            DebugLogger.debug(
                    "[ChatEntityTriggers] invalid regex category={} error={}",
                    category,
                    exception.getMessage()
            );
            return null;
        }
    }
}
