import engine.SearchEngine;
import ml.neuralnet.Net;
import ml.neuralnet.models.Learnable;
import ml.neuralnet.models.Topology;
import models.Movie;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import persistence.elastic.ml.ScoreScript;
import persistence.elastic.ml.builders.ScoreScriptBuilder;
import persistence.elastic.utils.ElasticIndices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.*;

public class App {

    private static Random random = new Random();

    public static void main(String[] args) {
        try {
            SearchEngine<Movie> searchEngine = new SearchEngine<Movie>(ElasticIndices.MOVIES, Movie.class);

            // Initialise an Artificial Neural Network and Topology
            Topology topology = new Topology(Arrays.asList(5, 10, 1));  // 5 input, 10 hidden, 1 output
            Net myNet = new Net(topology);

            String queryText;
            while(true) {
                queryText = readInput();

                // Perform sentiment analysis
                double meanSentiment = searchEngine.meanSentimentOfQuery(queryText);

                // Build the query object
                QueryBuilder qb = buildQuery(queryText);

                // Perform the search
                SearchHits searchHits = searchEngine.search(qb, SearchType.DFS_QUERY_THEN_FETCH);

                // Deserialize our Movie objects
                List<Movie> movies = searchEngine.deserialize(searchHits);

                // Choose a movie at random and move it to the top of the list
                int nhits = movies.size();
                int mostRelevantIndex = chooseAtRandom(nhits);

                System.out.println(String.format("Got %d hits.", nhits));

                Movie mostRelevantMovie = movies.get(mostRelevantIndex);
                movies.remove(mostRelevantMovie);
                movies.add(0, mostRelevantMovie);

                // Retrain the model based on the features and some additional inputs
                List<List<Double>> additionalInputVals = new LinkedList<>();

                for (SearchHit hit : searchHits) {
                    List<Double> inputVals = new LinkedList<>();
                    inputVals.add(Double.valueOf(hit.getScore()));
                    inputVals.add(Double.valueOf(meanSentiment));

                    additionalInputVals.add(inputVals);
                }

                List<Learnable> learnables = new LinkedList<>(movies);
                searchEngine.updateNeuralNet(myNet, learnables, additionalInputVals, 1000);

            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int chooseAtRandom(int nhits) {
        return random.nextInt(nhits);
    }

    public static QueryBuilder buildQuery(String queryText) {
        QueryBuilder match = QueryBuilders.multiMatchQuery(
                queryText, "title", "overview", "tagLine"
        );

        ScoreScript<Movie> scoreScript = new ScoreScript<>(Movie.class);
        scoreScript.builder()
                .add("popularity", 0.5, ScoreScriptBuilder.ScriptOperator.MULTIPLY, ScoreScriptBuilder.ScriptOperator.MULTIPLY)
//                .add("voteCount", 10000, ScoreScriptBuilder.ScriptOperator.MULTIPLY, ScoreScriptBuilder.ScriptOperator.DIVIDE)
                .add("averageVote", 0.5, ScoreScriptBuilder.ScriptOperator.MULTIPLY, ScoreScriptBuilder.ScriptOperator.MULTIPLY);
//                .add("revenue", 1000000, ScoreScriptBuilder.ScriptOperator.MULTIPLY, ScoreScriptBuilder.ScriptOperator.DIVIDE);

//        QueryBuilder qb = QueryBuilders.functionScoreQuery(match, ScoreFunctionBuilders.scriptFunction("_score * doc['popularity'].value"));

        ScriptScoreFunctionBuilder scriptScoreFunctionBuilder = scoreScript.getScript();
        QueryBuilder qb = QueryBuilders.functionScoreQuery(match, scriptScoreFunctionBuilder)
                .scoreMode(FiltersFunctionScoreQuery.ScoreMode.MAX);

        return qb;
    }

    public static String readInput() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter String:");
        String s = br.readLine();
        return s;
    }

}
