package com.sully90.elasticutils.persistence.mongo.util;

public enum DatabaseType {

    LOCAL("local");


    String label;

    DatabaseType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String toString() {
        return label;
    }
}
