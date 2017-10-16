package persistence.elastic.utils;

public enum ElasticIndices {

    MOVIES("movies");

    private String indexName;

    ElasticIndices(String indexName) {
        this.indexName = indexName;
    }

    public String getIndexName() {
        return this.indexName;
    }

}
