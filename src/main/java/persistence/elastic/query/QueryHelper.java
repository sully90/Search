package persistence.elastic.query;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class QueryHelper {
    public static QueryBuilder matchField(String key, String value) {
        return QueryBuilders.matchQuery(key, value);
    }

    public enum Fields {
        MONGOID("mongoId");

        String fieldName;

        Fields(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return this.fieldName;
        }
    }
}
