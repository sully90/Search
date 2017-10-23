import engine.SearchEngine;
import ml.neuralnet.Net;
import ml.neuralnet.models.Layer;
import ml.neuralnet.models.Learnable;
import ml.neuralnet.models.Topology;
import models.Movie;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.search.SearchHits;
import persistence.elastic.ml.ScoreScript;
import persistence.elastic.ml.builders.ScoreScriptBuilder;
import persistence.elastic.utils.ElasticIndex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.*;

public class App {

    private static Random random = new Random();

    public static void main(String[] args) {
        try {
            // Initialise an Artificial Neural Network and Topology
            Topology topology = new Topology(Arrays.asList(5, 50, 2, 10, 1));  // 5 input, 50 hidden, 2 hidden (search weights), 10 hidden, 1 output
            Net myNet = new Net(topology);

            SearchEngine<Movie> searchEngine = new SearchEngine<>(myNet, ElasticIndex.MOVIES, Movie.class);

            System.out.println("Take 2nd hidden layer outputs and search weights for ES (in script)");

            String queryText;

            Map<String, Double> fieldWeights = new HashMap<>();
            fieldWeights.put("popularity", 0.5);
            fieldWeights.put("averageVote", 0.5);
            while(true) {
                queryText = readInput();

                // Perform sentiment analysis
                double meanSentiment = searchEngine.meanSentimentOfQuery(queryText);

                // Build the query object

                QueryBuilder qb = buildQuery(queryText, fieldWeights);

                // Perform the search
                SearchHits searchHits = searchEngine.search(qb, SearchType.DFS_QUERY_THEN_FETCH);

                // Deserialize our Movie objects
                List<Movie> movies = searchEngine.deserialize(searchHits);
                List<Learnable> learnables = new LinkedList<>(movies);

                // Retrain the model based on the features and some additional inputs
                List<List<Double>> additionalInputVals = new LinkedList<>();

                for (int i = 0; i < movies.size(); i++) {
                    double normalisedRank = Learnable.normalise(i, 0, movies.size() - 1);
                    List<Double> inputVals = new LinkedList<>();
                    inputVals.add(Double.valueOf(normalisedRank));
                    inputVals.add(Double.valueOf(meanSentiment));

                    additionalInputVals.add(inputVals);
                }

                // Feed-forward to get the scores on which to order our results
                List<Double> relevance = searchEngine.feedForwardAndSort(learnables, additionalInputVals);
                System.out.println(relevance);

                // Choose a movie at random and move it to the top of the list
                int nhits = movies.size();
                int mostRelevantIndex = chooseAtRandom(nhits);

                System.out.println(String.format("Got %d hits.", nhits));

                Movie mostRelevantMovie = movies.get(mostRelevantIndex);
                movies.remove(mostRelevantMovie);
                movies.add(0, mostRelevantMovie);

                // Retrain the model based on the features and some additional inputs
                additionalInputVals = new LinkedList<>();

                for (int i = 0; i < movies.size(); i++) {
                    double normalisedRank = Learnable.normalise(i, 0, movies.size() - 1);
                    List<Double> inputVals = new LinkedList<>();
                    inputVals.add(Double.valueOf(normalisedRank));
                    inputVals.add(Double.valueOf(meanSentiment));

                    additionalInputVals.add(inputVals);
                }

                searchEngine.updateNeuralNet(learnables, additionalInputVals, 1000);
                List<Double> searchWeights = searchEngine.getLayerOutputs(2);
                System.out.println(searchWeights);

                Set<String> keySet = fieldWeights.keySet();
                Iterator<String> keySetIt = keySet.iterator();

                for (int i = 0; i < keySet.size(); i ++) {
                    String key = keySetIt.next();
                    fieldWeights.put(key, searchWeights.get(i));
                }

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

    public static QueryBuilder buildQuery(String queryText, Map<String, Double> fieldWeights) {
        QueryBuilder match = QueryBuilders.multiMatchQuery(
                queryText, "title", "overview", "tagLine"
        );

        ScoreScript<Movie> scoreScript = new ScoreScript<>(Movie.class);

        for (String key : fieldWeights.keySet()) {
            scoreScript.builder().add(key, fieldWeights.get(key), ScoreScriptBuilder.ScriptOperator.MULTIPLY, ScoreScriptBuilder.ScriptOperator.MULTIPLY);
        }

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
