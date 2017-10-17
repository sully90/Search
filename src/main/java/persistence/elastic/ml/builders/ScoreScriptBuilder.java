package persistence.elastic.ml.builders;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;

import java.util.*;

// "_score * doc['popularity'].value"

public class ScoreScriptBuilder<T> implements ScriptBuilder {

    @JsonIgnore
    private static final String baseString = "_score";

    @JsonIgnore
    private static final String WHITESPACE = " ";

    private StringBuilder builder;

    private Map<String, Double> fieldWeightMap;
    private List<ScriptOperator> scoreOperators;
    private List<ScriptOperator> weightOperators;
    private Class<T> forClass;

    public ScoreScriptBuilder(Class<T> forClass) {
        this.builder = new StringBuilder(this.baseString);

        this.fieldWeightMap = new LinkedHashMap<>();
        this.scoreOperators = new LinkedList<>();
        this.weightOperators = new LinkedList<>();
        this.forClass = forClass;
    }

    private ScoreScriptBuilder() {
        // For Jackson
    }

    public ScoreScriptBuilder add(String field, double weight, ScriptOperator scoreOperator, ScriptOperator weightOperator) {

        // First, add to the internal map for storage
        this.fieldWeightMap.put(field, weight);
        this.scoreOperators.add(scoreOperator);
        this.weightOperators.add(weightOperator);

        StringBuilder command = new StringBuilder();
        command.append(scoreOperator.getOperation()).append(WHITESPACE)
                .append("(").append("doc['").append(field).append("']").append(".value").append(WHITESPACE)
                .append(weightOperator.getOperation()).append(WHITESPACE).append(weight).append(")");

        this.builder.append(WHITESPACE).append(command.toString()).append(WHITESPACE);
        return this;
    }

    public ScoreScriptBuilder reset() {
        this.builder = new StringBuilder(this.baseString);
        return this;
    }

    @Override
    public ScriptScoreFunctionBuilder getScript() {
        return ScoreFunctionBuilders.scriptFunction(this.builder.toString());
    }

    public enum ScriptOperator {

        MULTIPLY("*"),
        DIVIDE("/"),
        ADD("+"),
        SUBTRACT("-");

        private String operation;

        ScriptOperator(String operation) {
            this.operation = operation;
        }

        public String getOperation() {
            return this.operation;
        }
    }

}
