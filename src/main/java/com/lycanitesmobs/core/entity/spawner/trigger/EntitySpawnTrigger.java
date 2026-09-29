package com.lycanitesmobs.core.entity.spawner.trigger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lycanitesmobs.core.entity.spawner.Spawner;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class EntitySpawnTrigger extends SpawnTrigger {
	/**
	 * A list of creature attributes that match this trigger. 1.21 removed MobType: undead/arthropod/water/illager
	 * are now entity type tags, and "undefined" (official MobType.UNDEFINED) means none of those tags.
	 **/
	protected List<String> creatureAttributes = new ArrayList<>();

	protected static final List<TagKey<EntityType<?>>> ATTRIBUTE_TAGS = List.of(EntityTypeTags.UNDEAD, EntityTypeTags.ARTHROPOD, EntityTypeTags.AQUATIC, EntityTypeTags.ILLAGER);

	/** Determines if the entity types list is a blacklist or whitelist. **/
	protected String entityTypesListType = "whitelist";

	/** A list of entity ids that match this trigger. **/
	protected List<String> entityIds = new ArrayList<>();

	/** Determines if the entity ids list is a blacklist or whitelist. **/
	protected String entityIdsListType = "blacklist";


	/** Constructor **/
	public EntitySpawnTrigger(Spawner spawner) {
		super(spawner);
	}


	@Override
	public void loadFromJSON(JsonObject json) {
		if(json.has("entityTypes")) {
			JsonArray jsonArray = json.get("entityTypes").getAsJsonArray();
			Iterator<JsonElement> jsonIterator = jsonArray.iterator();
			while (jsonIterator.hasNext()) {
				String creatureAttributeName = jsonIterator.next().getAsString();
				if (List.of("undead", "arthropod", "water", "illager", "undefined").contains(creatureAttributeName)) {
					this.creatureAttributes.add(creatureAttributeName);
				}
			}
		}

		if(json.has("entityTypesListType"))
			this.entityTypesListType = json.get("entityTypesListType").getAsString();

		if(json.has("entityIds")) {
			JsonArray jsonArray = json.get("entityIds").getAsJsonArray();
			Iterator<JsonElement> jsonIterator = jsonArray.iterator();
			while (jsonIterator.hasNext()) {
				String entityId = jsonIterator.next().getAsString();
				if(entityId != null) {
					entityIds.add(entityId);
				}
			}
		}

		if(json.has("entityIdsListType"))
			this.entityIdsListType = json.get("entityIdsListType").getAsString();

		super.loadFromJSON(json);
	}


	/** The official MobType name of the entity, derived from its entity type tags. **/
	public static String getCreatureAttribute(LivingEntity entity) {
		EntityType<?> type = entity.getType();
		if (type.is(EntityTypeTags.UNDEAD)) return "undead";
		if (type.is(EntityTypeTags.ARTHROPOD)) return "arthropod";
		if (type.is(EntityTypeTags.AQUATIC)) return "water";
		if (type.is(EntityTypeTags.ILLAGER)) return "illager";
		return "undefined";
	}

	/** Returns true if the provided entity should trigger this Spawn Trigger. **/
	public boolean isMatchingEntity(LivingEntity killedEntity) {

		// Check Entity Type:
		if(!this.creatureAttributes.isEmpty()) {
			if (this.creatureAttributes.contains(getCreatureAttribute(killedEntity))) {
				if ("blacklist".equalsIgnoreCase(this.entityTypesListType)) {
					return false;
				}
			}
			else {
				if ("whitelist".equalsIgnoreCase(this.entityTypesListType)) {
					return false;
				}
			}
		}

		// Check Entity Id:
		if(!this.entityIds.isEmpty()) {
			ResourceLocation entityResourceLocation = LMHelperClass.convertToResourceLocation(killedEntity.getType(), killedEntity.level().registryAccess());
			if(entityResourceLocation == null) {
				return false;
			}
			String entityId = entityResourceLocation.toString();
			if (this.entityIds.contains(entityId)) {
				if ("blacklist".equalsIgnoreCase(this.entityIdsListType)) {
					return false;
				}
			}
			else {
				if ("whitelist".equalsIgnoreCase(this.entityIdsListType)) {
					return false;
				}
			}
		}

		return true;
	}
}
