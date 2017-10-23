
import ml.neuralnet.Net;
import ml.neuralnet.models.Learnable;
import ml.neuralnet.models.Topology;
import ml.nlp.stanford.StanfordNLPHelper;
import models.Movie;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Test;
import persistence.elastic.ElasticHelper;
import persistence.elastic.client.ElasticSearchClient;
import persistence.elastic.ml.ScoreScript;
import persistence.elastic.ml.builders.ScoreScriptBuilder;
import persistence.elastic.utils.ElasticIndex;

import java.net.UnknownHostException;
import java.util.*;

public class TestNeuralNet {

    @Test
    public void test() throws Exception {
        Client client = null;
        try {
            client = ElasticHelper.getClient(ElasticHelper.Host.LOCALHOST);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        ElasticSearchClient<Movie> searchClient = new ElasticSearchClient<>(client, ElasticIndex.MOVIES, Movie.class);

//        QueryBuilder match = QueryBuilders.matchQuery(
//                "overview",
//                "James Bond 007"
//        );

        // The current date and time
        Date currentDate = new Date();

        String queryText = "James Bond 007";

        StanfordNLPHelper nlpHelper = new StanfordNLPHelper();
        // Perform a sentimental analysis on the query
        Map<String, String> sentimentMap = nlpHelper.getSentimentMap(queryText);

        // Get a numerical score of the sentiment
        double meanSentiment = nlpHelper.getMeanSentiment(queryText);
        System.out.println("Sentiment: " + sentimentMap.get(queryText) + " : " + meanSentiment);

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

//        System.out.println(qb.toString());

        SearchHits searchHits = searchClient.search(qb, SearchType.DFS_QUERY_THEN_FETCH);
        List<Movie> movies = searchClient.deserialize(searchHits);

        int nhits = movies.size();
        System.out.println(nhits);

        for (int i = 0; i < nhits; i++) {
            Movie movie = movies.get(i);
            System.out.println(movie.toString() + " Score: " + searchHits.getAt(i).getScore());
            System.out.println(movie.getPopularity() + " " + movie.getVoteCount() + " " + movie.getAverageVote() + " " + movie.getRevenue());
//            System.out.println(movies.get(i).getReleaseDate().toString());
            System.out.println();
        }

        int nIterations = 1000;
        Random random = new Random();
        Movie movie;

        // Init the neural net
        Topology topology = new Topology(Arrays.asList(5, 10, 2, 1));  // 5 input, 10 hidden, 1 output
        Net myNet = new Net(topology);

        while (nIterations > 0) {
            // Pick a hit at random we deem to be the most relvant hit
//            int i = random.nextInt(nhits);
            int i = random.nextInt(nhits);
            int ix = i+1;

            // Initial scoring is based on the order elasticsearch gave us (i.e from 0 to nhits-1)
            // New scoring: boost the hit the user chose to the top of the list (normalisedRank = 1), and retrain
            // the model using all hits in their new order

            double rank = (double) nhits-ix;
            double normalisedRank = Learnable.normalise(rank, 0, nhits-1);  // correct, do not change

//            System.out.println(nhits + " : " + ix + " : " + normalisedRank);

            SearchHit mostRelevantHit = searchHits.getAt(i);
            Movie mostRelevantMovie = movies.get(i);
            float score = mostRelevantHit.getScore();  // the score according to elasticsearch

            // Move this entry to the top of the list
            movies.remove(mostRelevantMovie);
            // Re-insert as the top entry
            movies.add(0, mostRelevantMovie);

            // Retrain the model based on the features
            for (int m = 0; m < movies.size() - 1; m++) {
                // m is the new rank
                double newNormalisedRank = Learnable.normalise(m, 0, nhits-1);
                // This is our targetVal
                // Perform a training step

                // Input vals
                movie = movies.get(m);
                score = searchHits.getAt(m).getScore();

                List<Double> inputVals = movie.getInputVals();
                // Add elasticsearch internal score to inputVals
//                inputVals.add(Double.valueOf(score));
                // Use the query sentiment as an input value
                inputVals.add(Double.valueOf(meanSentiment));

                // Target val is the normalised rank
                List<Double> targetVal = new LinkedList<>(Arrays.asList(Double.valueOf(newNormalisedRank)));

                // Do the training step
                List<Double> results = myNet.executeTrainingStep(inputVals, targetVal);

                // Print the inputs
                System.out.println("Inputs: " + inputVals);

                // Print the target
                System.out.println("Target: " + targetVal);

                // Print the results
                System.out.println("Results: " + results);
                System.out.println();

                // Error
                System.out.println("Error: " + myNet.getRecentAverageError());
                System.out.println();
            }

            // Perform a new random iteration
            nIterations--;
        }

        System.out.println("Error: " + myNet.getRecentAverageError());

        // Check feed-forward works
        for (int m = 0; m < movies.size(); m++) {
            // m is the new rank
            double newNormalisedRank = Learnable.normalise(m, 0, nhits-1);
            // This is our targetVal
            // Perform a training step

            // Input vals
            movie = movies.get(m);
            float score = searchHits.getAt(m).getScore();

            List<Double> inputVals = movie.getInputVals();
            // Add elasticsearch internal score to inputVals
//            inputVals.add(Double.valueOf(score));
            // Use the query sentiment as an input value
            inputVals.add(Double.valueOf(meanSentiment));

            // Do the training step
            myNet.feedForward(inputVals);
            List<Double> results = myNet.getResults();

            // Print the inputs
            System.out.println("Inputs: " + inputVals);

            // Print the results
            System.out.println("Results: " + results);
            System.out.println();

            // Error
            System.out.println("Error: " + myNet.getRecentAverageError());
            System.out.println();
        }

    }

    private static float normalise(float val, float max, float min) {
        float normalised = (val - min) / (max - min);  // [0-1]
        return (normalised - 0.5f) * 2.0f;  // [-1, 1]
    }

}
