package com.sully90.elasticutils.persistence.mongo.util;

import org.jongo.Jongo;
import org.jongo.MongoCollection;

public class DatabaseConnection {

    private Jongo jongo;

    public DatabaseConnection(Jongo jongo) {
        this.jongo = jongo;
    }

    public Jongo getJongo() {
        return jongo;
    }

    public void setJongo(Jongo jongo) {
        this.jongo = jongo;
    }

    @Deprecated
    public MongoCollection getCollection(String collection) {
        return jongo.getCollection(collection);
    }

    public MongoCollection getCollection(CollectionNames collection) {
        return jongo.getCollection(collection.getName());
    }
}
