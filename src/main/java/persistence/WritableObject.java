package persistence;

import persistence.util.CollectionNames;
import persistence.util.FindableObject;
import persistence.util.ObjectWriter;

public interface WritableObject extends FindableObject {

    ObjectWriter writer();
}
