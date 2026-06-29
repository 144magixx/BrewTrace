package com.minyuwei.xhs.coffeeagent.memory.infrastructure;

import java.util.ArrayList;
import java.util.List;

public class EmbeddingModelGateway {
    public List<Double> embed(String text) {
        List<Double> vector = new ArrayList<>();
        vector.add(text.contains("甜橙") ? 1.0 : 0.1);
        vector.add(text.contains("红茶") ? 1.0 : 0.1);
        vector.add(text.contains("水洗") ? 0.8 : 0.2);
        return vector;
    }
}
