package com.sully90.elasticutils.persistence.mongo.util;

import org.bson.types.ObjectId;
import org.jongo.MongoCollection;
import com.sully90.elasticutils.persistence.mongo.MongoHelper;

import java.util.Collection;

public class ObjectFinder<T> {

    protected MongoCollection collection;

    protected Class<T> returnClass;

    public ObjectFinder(MongoCollection collection, Class<T> returnClass) {
        this.collection = collection;
        this.returnClass = returnClass;
    }

    public ObjectFinder(DatabaseType databaseType, CollectionNames collection, Class<T> returnClass) {
        this.collection = MongoHelper.getDatabase(databaseType).getCollection(collection);
        this.returnClass = returnClass;
    }

    public ObjectFinder(CollectionNames collection, Class<T> returnClass) {
        this.collection = MongoHelper.getDatabase(DatabaseType.LOCAL).getCollection(collection);
        this.returnClass = returnClass;
    }

    public T find(ObjectId id) {
        return findOne(id);
    }

    public Iterable<T> find(Collection<ObjectId> ids) {
        return collection.find("{ _id : { $in : #} }", ids).as(returnClass);
    }

    public Iterable<T> find() {
        return find("{}");
    }

    public Iterable<T> find(String query) {
        return find(query, "{}");
    }

    public Iterable<T> find(String query, String sort) {
        return find(query, sort, 0);
    }

    public Iterable<T> find(String query, int limit) {
        return find(query, "{}", 0, limit);
    }

    public Iterable<T> find(String query, String sort, int skip) {
        return find(query, sort, skip, 0);
    }

    public Iterable<T> find(String query, String sort, int skip, int limit) {
        return collection.find(query).sort(sort).skip(skip).limit(limit).as(returnClass);
    }

    public T findOne() {
        return findOne("{}");
    }

    public T findOne(String query) {
        return collection.findOne(query).as(returnClass);
    }

    public T findOne(ObjectId id) {
        return collection.findOne(id).as(returnClass);
    }

    public long count() {
        return count("{}");
    }

    public long count(String query) {
        return collection.count(query);
    }

    public MongoCollection getCollection() {
        return collection;
    }

}
