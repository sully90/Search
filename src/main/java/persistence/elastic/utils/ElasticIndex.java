package persistence.elastic.utils;

public enum ElasticIndex {

    MOVIES("movies");

    private String indexName;

    ElasticIndex(String indexName) {
        this.indexName = indexName;
    }

    public String getIndexName() {
        return this.indexName;
    }

}
