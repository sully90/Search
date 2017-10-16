package persistence.mongo.util;

import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import org.jongo.MongoCollection;
import persistence.mongo.MongoHelper;

public class ObjectWriter {

    protected MongoCollection collection;

    protected FindableObject object;

    public ObjectWriter(MongoCollection collection, FindableObject object) {
        this.collection = collection;
        this.object = object;
    }

    public ObjectWriter(DatabaseType databaseType, CollectionNames collection, FindableObject object) {
        this(MongoHelper.getDatabase(databaseType).getCollection(collection), object);
    }

    public ObjectWriter(CollectionNames collection, FindableObject object) {
        this(MongoHelper.getDatabase(DatabaseType.LOCAL).getCollection(collection), object);
    }

    public void save(WriteConcern writeConcern) throws RuntimeException {
        WriteResult writeResult = collection.withWriteConcern(writeConcern).save(object);
    }

    public void save() throws RuntimeException {
        this.save(WriteConcern.SAFE);
    }

    public void delete() {
        collection.remove(object.getObjectId());
    }

}
