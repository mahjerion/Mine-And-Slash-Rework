package com.robertx22.mine_and_slash.mechanics.thresholds;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import java.util.List;

public interface SpendThresholdContributor {
    /** Return zero or more specs active for this unit (e.g., from allocated talents). */
    List<SpendThresholdSpec> getSpendThresholds(EntityData unit);
}
