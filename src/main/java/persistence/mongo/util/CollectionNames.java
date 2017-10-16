package persistence.mongo.util;

public enum CollectionNames {
    TEST("test"),
    MOVIES("movies"),
    NET("neuralnets");

    // Cannot be null
    private String name;

    CollectionNames(String inName) {
        name = inName;
    }

    public String getName() {
        return name;
    }
}
