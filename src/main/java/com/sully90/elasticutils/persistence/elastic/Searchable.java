package com.sully90.elasticutils.persistence.elastic;

import java.util.HashMap;
import java.util.Map;

public abstract class Searchable {

    // Hashmap to store results from the OpenNLP plugin
    private Map<String, String[]> entities;

    protected Searchable() {
        this.entities = new HashMap<>();
    }

    public Map<String, String[]> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, String[]> entities) {
        this.entities = entities;
    }
}
