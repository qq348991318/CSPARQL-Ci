package eu.larkc.csparql.cep.esper;

import com.triple.common.RDFTriple;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * @author 杨帆玉
 * @date 2019/11/1 6:19 下午
 */
public class Producer {
    public static final String TO_TOPIC = "MsgRDFTriple";
    public static final String BROKER_LIST = "127.0.0.1:9092";

    public static Properties getProducerConfigTriple(String brokerAddr) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddr);//kafka地址，多个地址用逗号分割
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RDFTripleSerializer.class);
        return props;
    }
    public static Properties getProducerConfigString(String brokerAddr) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddr);//kafka地址，多个地址用逗号分割
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    public static KafkaProducer<String, RDFTriple> producerTriple = new KafkaProducer<>(getProducerConfigTriple(BROKER_LIST));

    public static KafkaProducer<String, String> producerString = new KafkaProducer<>(getProducerConfigString(BROKER_LIST));


    public static void sendMessagesTriple(String topic, String key, RDFTriple messages, String brokerAddr) {
        ProducerRecord<String, RDFTriple> record = new ProducerRecord<>(topic, key, messages);
        producerTriple.send(record);
    }

    public static void sendMessagesString(String topic, String key, String messages, String brokerAddr) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, messages);
        producerString.send(record);
    }

    public static void main(String[] args) throws InterruptedException {
        RDFTriple rdfTriple = new RDFTriple("subject", "predicate", "object");
        while (true) {
            System.out.println(rdfTriple.toString());
            sendMessagesTriple(TO_TOPIC, "key", rdfTriple, BROKER_LIST);
            Thread.sleep(100);
        }
    }
}
