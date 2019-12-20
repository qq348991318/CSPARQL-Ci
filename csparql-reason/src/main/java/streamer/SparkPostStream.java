package streamer;

import com.triple.common.RDFTriple;
import eu.larkc.csparql.cep.api.RdfQuadruple;
import eu.larkc.csparql.cep.api.RdfStream;
import eu.larkc.csparql.core.pojo.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.Arrays;

/**
 * @author 杨帆玉
 * @date 2019/11/6 4:05 下午
 */
public class SparkPostStream extends RdfStream implements Runnable {

    private int streamRate;
    private String baseUri;

    public SparkPostStream(String iri, String baseUri, int streamRate) {
        super(iri);
        this.baseUri = baseUri;
        this.streamRate = streamRate;
    }

    public SparkPostStream(String iri) {
        super(iri);
    }

    public void run() {

        Consumer.consumer.subscribe(Arrays.asList(Consumer.BACK_TOPIC));

        try {
            while (true) {
                ConsumerRecords<String, RDFTriple> records = Consumer.consumer.poll(0);
                for (ConsumerRecord<String, RDFTriple> record : records) {
                    RdfQuadruple rdfQuadruple = new RdfQuadruple(record.value().getSubject(), record.value().getPredicate(), record.value().getObject(), System.currentTimeMillis());
                    this.put(rdfQuadruple);
//                    System.out.println(record.value().toString());
                }
            }
        } finally {
            Consumer.consumer.close();
        }
    }
}

