import ml.neuralnet.Net;
import org.junit.Test;

public class TestNeuralNet {

    @Test
    public void testMongoFind() {
        Net myNet = Net.finder().findOne();
        System.out.println(myNet.getObjectId().toString());
    }

}
