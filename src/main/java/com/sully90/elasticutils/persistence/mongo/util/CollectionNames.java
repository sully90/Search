package com.sully90.elasticutils.persistence.mongo.util;

public enum CollectionNames {
    TEST("test"),
    MOVIES("movies"),
    NET("neuralnets"),
    SCORE_SCRIPTS("scorescripts");

    // Cannot be null
    private String name;

    CollectionNames(String inName) {
        name = inName;
    }

    public String getName() {
        return name;
    }
}
