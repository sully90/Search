package com.sully90.elasticutils.persistence.elastic.ml;

import com.sully90.elasticutils.persistence.elastic.ml.builders.ScoreScriptBuilder;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.script.Script;

public class ScoreScript<T> {

    private Class<T> returnClass;

    private ScoreScriptBuilder<T> builder;
    private Script script;

    public ScoreScript(Class<T> returnClass) {
        this.returnClass = returnClass;
        this.builder = new ScoreScriptBuilder<>(this.returnClass);
    }

    private ScoreScript() {
        // For Jackson
    }

    public ScoreScriptBuilder builder() {
        return builder;
    }

    public ScriptScoreFunctionBuilder getScript() {
        return this.builder().getScript();
    }

    public Class<T> getReturnClass() {
        return returnClass;
    }
}
