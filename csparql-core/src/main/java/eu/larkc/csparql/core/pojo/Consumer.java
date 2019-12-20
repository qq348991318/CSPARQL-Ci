package eu.larkc.csparql.core.pojo;

import com.triple.common.RDFTriple;
import eu.larkc.csparql.cep.esper.RDFTripleDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Arrays;
import java.util.Properties;

/**
 * @author 杨帆玉
 * @date 2019/11/3 2:53 下午
 */
public class Consumer {
    public static final String BACK_TOPIC = "back";
    public static final String TO_TOPIC = "MsgRDFTriple";
    public static final String BROKER_LIST = "127.0.0.1:9096";
    public static Properties properties() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", BROKER_LIST);
        properties.put("group.id", "test");
        properties.put("enable.auto.commit", "true");
        properties.put("auto.commit.interval.ms", "1000");
        properties.put("session.timeout.ms", 10000);
        properties.put("key.deserializer", StringDeserializer.class);
        properties.put("value.deserializer", RDFTripleDeserializer.class);
        return properties;
    }

    public static final KafkaConsumer<String, RDFTriple> consumer = new KafkaConsumer<String, RDFTriple>(properties());

    public static void main(String[] args) {
        consumer.subscribe(Arrays.asList(BACK_TOPIC));
        try {
            while (true) {
                ConsumerRecords<String, RDFTriple> records = consumer.poll(0);
                for (ConsumerRecord<String, RDFTriple> record : records) {
                    System.out.println(record.value().toString());
                }
            }
        } finally {
            consumer.close();
        }

    }
}
