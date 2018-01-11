package buildergenerator;

import buildergenerator.internal.AbstractBuilder;

import java.util.HashMap;
import java.util.Map;

public class BuilderGenerator {

    private final Map<Class, AbstractBuilder> builderMap;

    public BuilderGenerator() {
        this.builderMap = new HashMap<>();
    }

    public void addNewEntry(Class clazz, AbstractBuilder abstractBuilder) {
        builderMap.put(clazz, abstractBuilder);
    }

    public AbstractBuilder getBuilder(Class clazz) {
        return builderMap.get(clazz);
    }

}