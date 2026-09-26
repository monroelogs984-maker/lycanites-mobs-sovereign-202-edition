package com.lycanitesmobs.core.item.equipment;

import com.lycanitesmobs.core.data.info.creature.CreatureType;
import com.lycanitesmobs.core.item.base.CreatureTypeItem;

/**
 * Saddle for a creature type's mounts. The item only identifies the creature type - equipping it is handled by the
 * mount's inventory. TODO(port): mounts (RideableCreatureEntity) and CreatureInventory aren't ported yet, so saddles
 * can't be equipped yet.
 */
public class CreatureSaddleItem extends CreatureTypeItem {

    public CreatureSaddleItem(Properties properties, CreatureType creatureType) {
        super(properties, creatureType.getSaddleName(), creatureType);
        this.setup();
    }
}
