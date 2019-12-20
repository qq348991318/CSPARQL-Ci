package eu.larkc.csparql.core.pojo;

import com.triple.common.RDFTriple;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.Arrays;
import java.util.Observable;

/**
 * @author 杨帆玉
 * @date 2019/11/6 10:39 上午
 */
public class TripleBackList extends Observable {

//    List<>

    public static void main(String[] args) {
        Consumer.consumer.subscribe(Arrays.asList(Consumer.BACK_TOPIC));
        try {
            while (true) {
                ConsumerRecords<String, RDFTriple> records = Consumer.consumer.poll(0);
                for (ConsumerRecord<String, RDFTriple> record : records) {
                    System.out.println(record.key() + " : " + record.value().toString());
                }
            }
        } finally {
            Consumer.consumer.close();
        }

    }
}
