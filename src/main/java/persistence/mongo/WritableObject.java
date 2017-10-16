package persistence.mongo;

import persistence.mongo.util.FindableObject;
import persistence.mongo.util.ObjectWriter;

public interface WritableObject extends FindableObject {

    ObjectWriter writer();
}
