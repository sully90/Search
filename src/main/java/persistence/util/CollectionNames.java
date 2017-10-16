package persistence.util;

public enum CollectionNames {
    TEST("test"),
    MOVIES("movies");

    // Cannot be null
    private String name;

    CollectionNames(String inName) {
        name = inName;
    }

    public String getName() {
        return name;
    }
}
