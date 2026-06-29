package com.minyuwei.xhs.coffeeagent.user.infrastructure;

import com.minyuwei.xhs.coffeeagent.tasting.domain.CoffeeRecord;
import com.minyuwei.xhs.coffeeagent.user.domain.UserPreference;

import java.util.List;

public class PreferenceInferenceConsumer {
    public List<UserPreference> infer(CoffeeRecord record) {
        return record.flavorKeywords().stream()
                .limit(2)
                .map(flavor -> UserPreference.inferred(record.userId(), flavor, "多次归档或近期归档中出现该风味"))
                .toList();
    }
}
