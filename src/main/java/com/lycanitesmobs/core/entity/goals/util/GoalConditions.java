package com.lycanitesmobs.core.entity.goals.util;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;

public class GoalConditions {
    protected boolean rareVariantOnly = false;
    protected int battlePhase = -1;

    public GoalConditions setRareVariantOnly(boolean rareVariantOnly) {
        this.rareVariantOnly = rareVariantOnly;
        return this;
    }

    public GoalConditions setBattlePhase(int battlePhase) {
        this.battlePhase = battlePhase;
        return this;
    }

    public boolean isMet(BaseCreatureEntity creatureEntity) {
        if (this.rareVariantOnly && !creatureEntity.isRareVariant()) {
            return false;
        }

        if (this.battlePhase >= 0 && creatureEntity.getBattlePhase() != this.battlePhase) {
            return false;
        }

        return true;
    }
}
