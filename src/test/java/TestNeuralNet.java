
import models.Movie;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Test;
import persistence.elastic.ElasticHelper;
import persistence.elastic.client.ElasticSearchClient;
import persistence.elastic.ml.ScoreScript;
import persistence.elastic.ml.builders.ScoreScriptBuilder;
import persistence.elastic.utils.ElasticIndices;

import java.net.UnknownHostException;
import java.util.List;

public class TestNeuralNet {

    @Test
    public void test() {
        Client client = null;
        try {
            client = ElasticHelper.getClient(ElasticHelper.Host.LOCALHOST);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        ElasticSearchClient<Movie> searchClient = new ElasticSearchClient<>(client, ElasticIndices.MOVIES, Movie.class);

//        QueryBuilder match = QueryBuilders.matchQuery(
//                "overview",
//                "James Bond 007"
//        );

        QueryBuilder match = QueryBuilders.multiMatchQuery(
                "James Bond 007", "title", "overview", "tagLine"
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

        System.out.println(qb.toString());

        SearchHits searchHits = searchClient.search(qb, SearchType.DFS_QUERY_THEN_FETCH);
        List<Movie> movies = searchClient.deserialize(searchHits);

        int nhits = movies.size();
        System.out.println(nhits);

        for (int i = 0; i < nhits; i++) {
            Movie movie = movies.get(i);
            System.out.println(movie.toString() + ": " + searchHits.getAt(i).getScore());
            System.out.println(movie.getPopularity() + " " + movie.getVoteCount() + " " + movie.getAverageVote() + " " + movie.getRevenue());
//            System.out.println(movies.get(i).getReleaseDate().toString());
            System.out.println();
        }

    }

}
