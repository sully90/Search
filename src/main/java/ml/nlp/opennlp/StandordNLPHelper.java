package ml.nlp.opennlp;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class StandordNLPHelper {

    private static Properties getDefaultProperties() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment");
        return props;
    }

    private static Annotation getDefaultAnnotation(String text) {
        return getPipeline().process(text);
    }

    public static StanfordCoreNLP getPipeline() {
        return getPipeline(getDefaultProperties());
    }

    public static StanfordCoreNLP getPipeline(Properties props) {
        return new StanfordCoreNLP(props);
    }

    public static Map<String, String> getSentimentMap(String text) {
        // Performs a sentimental analysis on each sentence in text

        Annotation annotation = getDefaultAnnotation(text);

        Map<String, String> sentimentMap = new LinkedHashMap<>();

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sentence : sentences) {
            String sentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
            sentimentMap.put(sentence.toString(), sentiment);
        }

        return sentimentMap;
    }

    public static double getMeanSentiment(String text) throws Exception {
        // Computes the mean sentiment for a block of text, by individually
        // analysing each sentence
        Map<String, String> sentimentMap = getSentimentMap(text);
        double meanSentiment = 0;

        for(String key : sentimentMap.keySet()) {
            meanSentiment += getSentimentCategory(sentimentMap.get(key));
        }

        return meanSentiment / (double) sentimentMap.size();
    }

    public static double getSentimentCategory(String sentiment) throws Exception {
        double category;
        switch (sentiment.toLowerCase()) {
            case "very positive":
                category = 1.0d;
                break;
            case "positive":
                category = 0.5d;
                break;
            case "neutral":
                category = 0.0d;
                break;
            case "negative":
                category = -0.5d;
                break;
            case "very negative":
                category = -1.0d;
                break;
            default:
                throw new Exception(String.format("Unknown sentiment: %s", sentiment));
        }

        return category;
    }

}
