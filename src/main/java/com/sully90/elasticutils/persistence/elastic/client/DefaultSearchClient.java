package com.sully90.elasticutils.persistence.elastic.client;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public interface DefaultSearchClient<T> extends AutoCloseable {

    void index(T entity);

    void index(List<T> entities);

    void index(Stream<T> entities);

    void flush();

    boolean awaitClose(long timeout, TimeUnit unit) throws InterruptedException;

}
