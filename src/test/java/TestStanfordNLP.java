import ml.nlp.opennlp.StanfordNLPHelper;
import org.junit.Test;

import java.util.Map;

public class TestStanfordNLP {

    @Test
    public void test() {
        String[] sentences = new String[]{
            "The World is an amazing place.",
            "The World is an ok place.",
            "I hate everything",
            "James Bond 007"
        };

        StanfordNLPHelper nlpHelper = new StanfordNLPHelper();

        for(String text : sentences) {
            Map<String, String> sentimentMap = nlpHelper.getSentimentMap(text);
            String sentiment = sentimentMap.get(text);

            System.out.println(text + ": " + sentiment);
        }
    }

}
