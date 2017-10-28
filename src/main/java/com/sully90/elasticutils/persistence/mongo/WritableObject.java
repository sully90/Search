package com.sully90.elasticutils.persistence.mongo;

import com.sully90.elasticutils.persistence.mongo.util.FindableObject;
import com.sully90.elasticutils.persistence.mongo.util.ObjectWriter;

public interface WritableObject extends FindableObject {

    ObjectWriter writer();
}
