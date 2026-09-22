package com.lycanitesmobs.core.util.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.joml.Vector3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JSONHelper {
    private static final Map<List<String>, List<String>> biomeTagCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    public static Vec3i getVector3i(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            JsonArray jsonArray = json.get(memberName).getAsJsonArray();
            Iterator<JsonElement> jsonIterator = jsonArray.iterator();
            int[] coords = new int[3];
            int i = 0;
            while (jsonIterator.hasNext() && i < coords.length) {
                coords[i] = jsonIterator.next().getAsInt();
                i++;
            }
            return new Vec3i(coords[0], coords[1], coords[2]);
        }
        return new Vec3i(0, 0, 0);
    }

    public static Vector3d getVector3d(JsonObject json, String memberName, Vector3d defaultVec) {
        if (json.has(memberName)) {
            JsonArray jsonArray = json.get(memberName).getAsJsonArray();
            Iterator<JsonElement> jsonIterator = jsonArray.iterator();
            double[] coords = new double[3];
            int i = 0;
            while (jsonIterator.hasNext() && i < coords.length) {
                coords[i] = jsonIterator.next().getAsDouble();
                i++;
            }
            return new Vector3d(coords[0], coords[1], coords[2]);
        }
        return defaultVec;
    }

    public static List<String> getJsonStrings(JsonArray jsonArray) {
        List<String> strings = new ArrayList<>();
        for (JsonElement jsonElement : jsonArray) {
            String string = jsonElement.getAsString();
            strings.add(string);
        }
        return strings;
    }

    public static List<Block> getJsonBlocks(JsonObject json) {
        List<Block> blocks = new ArrayList<>();
        if (json.has("blocks")) {
            blocks = getJsonBlocks(json.get("blocks").getAsJsonArray());
        }
        return blocks;
    }

    public static List<Block> getJsonBlocks(JsonArray jsonArray) {
        List<Block> blocks = new ArrayList<>();
        Iterator<JsonElement> jsonIterator = jsonArray.iterator();
        while (jsonIterator.hasNext()) {
            // BuiltInRegistries.BLOCK is a DefaultedRegistry (falls back to AIR on a miss), so a
            // plain .get() would never be null and silently turn typos into air blocks - use
            // getOptional() to preserve the original "skip unknown entries" behavior.
            Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(jsonIterator.next().getAsString())).orElse(null);
            if (block != null) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    public static List<Item> getJsonItems(JsonArray jsonArray) {
        List<Item> items = new ArrayList<>();
        Iterator<JsonElement> jsonIterator = jsonArray.iterator();
        while (jsonIterator.hasNext()) {
            Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(jsonIterator.next().getAsString())).orElse(null);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    // NOTE: getJsonMaterials(JsonObject) was dropped here during the NeoForge 1.21.1 port - it
    // depends on com.lycanitesmobs.core.block.Material, which is Phase 4 (Items & Blocks) and
    // doesn't exist yet. Port it back in alongside Material.

    public static List<String> getBiomesFromTags(Level world, List<String> biomeTags) {
        List<String> cachedResult = biomeTagCache.get(biomeTags);
        if (cachedResult != null) {
            return new ArrayList<>(cachedResult);
        }

        LinkedHashSet<String> biomeSet = new LinkedHashSet<>();

        var registryAccess = world.registryAccess();
        var biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);

        for (String rawEntry : biomeTags) {
            boolean additive;
            String entry = rawEntry;

            if (!entry.isEmpty()) {
                char c = entry.charAt(0);
                if (c == '+' || c == '-') {
                    additive = c != '-';
                    entry = entry.substring(1);
                } else {
                    additive = true;
                }
            } else {
                additive = true;
            }

            if ("ALL".equalsIgnoreCase(entry)) {
                biomeRegistry.registryKeySet().forEach(key -> {
                    String id = key.location().toString();
                    if (additive) {
                        biomeSet.add(id);
                    } else {
                        biomeSet.remove(id);
                    }
                });
                continue;
            }

            if ("NONE".equalsIgnoreCase(entry)) {
                continue;
            }

            if (!entry.contains(":")) {
                LMHelperClass.logWarning(
                        "Dungeon",
                        "Non-namespaced biome tag entry '" + entry + "' is not supported, skipping"
                );
                continue;
            }

            ResourceLocation tagLoc = ResourceLocation.parse(entry);
            List<String> selectedBiomeIds = new ArrayList<>();

            TagKey<Biome> vanillaKey =
                    TagKey.create(
                            Registries.BIOME,
                            tagLoc
                    );

            var vanillaSetOpt = biomeRegistry.getTag(vanillaKey);
            if (vanillaSetOpt.isPresent()) {
                vanillaSetOpt.get().forEach(holder ->
                        holder.unwrapKey().ifPresent(key -> {
                            var id = key.location();
                            if (id != null) {
                                selectedBiomeIds.add(id.toString());
                            }
                        })
                );
            }
            // NOTE: the original Forge version had a fallback here through
            // ForgeRegistries.BIOMES.tags() for Forge-registered biome tags outside the vanilla
            // dynamic registry. NeoForge has no separate biome tag manager - biome tags are only
            // ever resolved through the vanilla registry above, so that fallback is gone, not
            // ported-and-broken.

            if (selectedBiomeIds.isEmpty()) {
                LMHelperClass.logWarning(
                        "Dungeon",
                        "Biome tag '" + entry + "' is empty or missing"
                );
            } else {
                if (additive) {
                    biomeSet.addAll(selectedBiomeIds);
                } else {
                    biomeSet.removeAll(selectedBiomeIds);
                }
            }
        }

        List<String> biomeList = new ArrayList<>(biomeSet);
        LMHelperClass.logDebug("Dungeon", "Resolved biome tags " + biomeTags + " -> " + biomeList.size() + " biomes");

        cacheBiomeTags(biomeTags, biomeList);

        return biomeList;
    }

    private static void cacheBiomeTags(List<String> originalTags, List<String> resolvedBiomes) {
        if (biomeTagCache.size() >= MAX_CACHE_SIZE) {
            Iterator<Map.Entry<List<String>, List<String>>> iterator = biomeTagCache.entrySet().iterator();
            int entriesToRemove = MAX_CACHE_SIZE / 4;
            for (int i = 0; i < entriesToRemove && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
        }

        biomeTagCache.put(new ArrayList<>(originalTags), new ArrayList<>(resolvedBiomes));
    }

    public static void clearBiomeTagCache() {
        biomeTagCache.clear();
    }

    public static int getBiomeTagCacheSize() {
        return biomeTagCache.size();
    }

    // NOTE: getBiomes(List<String>) was dropped here - it resolved biomes with no RegistryAccess
    // or Level context at all (ForgeRegistries.BIOMES.getValue(...) as a static lookup), which
    // has no equivalent in 1.21+: Biome is a dynamic (datapack) registry with no static access.
    // Nothing in Phases 0-3 calls it. If Phase 7 (worldgen) needs it, it'll need a
    // RegistryAccess/Level parameter added - use getBiomesFromTags above as the pattern.
}
