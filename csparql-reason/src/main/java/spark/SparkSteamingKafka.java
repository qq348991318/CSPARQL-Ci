package spark;

import com.triple.common.RDFTriple;
import eu.larkc.csparql.cep.esper.Producer;
import eu.larkc.csparql.cep.esper.RDFTripleDeserializer;
import eu.larkc.csparql.core.pojo.Consumer;
import eu.larkc.csparql.core.pojo.Rules;
import eu.larkc.csparql.core.pojo.SchemeTriple;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;

import java.util.*;

/**
 * @author 杨帆玉
 * @date 2019/11/4 9:51 上午
 */
public class SparkSteamingKafka {
    public static final int parallism = 3;

    public static void main(String[] args) throws InterruptedException {
        String brokers = "127.0.0.1:9092";
        String topics = "MsgRDFTriple";
        SparkConf conf = new SparkConf().setMaster("local[2]").setAppName("streaming")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
        JavaSparkContext sc = new JavaSparkContext(conf);
        sc.setLogLevel("WARN");
        JavaStreamingContext ssc = new JavaStreamingContext(sc, Durations.seconds(1));

        Collection<String> topicsSet = new HashSet<>(Arrays.asList(topics.split(",")));
        //kafka相关参数，必要！缺了会报错
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("metadata.broker.list", brokers);
        kafkaParams.put("bootstrap.servers", brokers);
        kafkaParams.put("group.id", "group1");
//            kafkaParams.put("partition.assignment.strategy", "1");
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        kafkaParams.put("value.deserializer", RDFTripleDeserializer.class);
        //Topic分区  也可以通过配置项实现
        //如果没有初始化偏移量或者当前的偏移量不存在任何服务器上，可以使用这个配置属性
        //earliest 当各分区下有已提交的offset时，从提交的offset开始消费；无提交的offset时，从头开始消费
        //latest 当各分区下有已提交的offset时，从提交的offset开始消费；无提交的offset时，消费新产生的该分区下的数据
        //none topic各分区都存在已提交的offset时，从offset后开始消费；只要有一个分区不存在已提交的offset，则抛出异常
        //kafkaParams.put("auto.offset.reset", "latest");
        //kafkaParams.put("enable.auto.commit",false);

        Map<TopicPartition, Long> offsets = new HashMap<>();
        offsets.put(new TopicPartition("MsgRDFTriple", 0), 2L);
        //通过KafkaUtils.createDirectStream(...)获得kafka数据，kafka相关参数由kafkaParams指定
        JavaInputDStream<ConsumerRecord<String, RDFTriple>> stream = KafkaUtils.createDirectStream(
                ssc, LocationStrategies.PreferConsistent(), ConsumerStrategies.Subscribe(topicsSet, kafkaParams, offsets)
        );
//            stream.mapToPair(record -> new Tuple2<>(record.key(), record.value().toString())).print();
        // 推理
//        JavaDStream<com.triple.common.RDFTriple> triples = stream.filter(t -> t.key().equals("1")).map(t -> t.value());
//        JavaDStream<com.triple.common.RDFTriple> schemes = stream.filter(t -> t.key().equals("2")).map(t -> t.value());
        JavaDStream<RDFTriple> triples = stream
                .map(t -> t.value())
                .window(Durations.seconds(1), Durations.seconds(1));

        HashSet<RDFTriple> subprops = transitiveClosure(SchemeTriple.getSubPropSet());
        HashSet<RDFTriple> subclasses = transitiveClosure(SchemeTriple.getSubClassSet());
//        HashSet<com.triple.common.RDFTriple> friendOf = transitiveClosure(SchemeTriple.getFriendSet());
        Broadcast<HashMap<String, String>> sp = sc.broadcast(toMap(subprops));
        Broadcast<HashMap<String, String>> cl = sc.broadcast(toMap(subclasses));
//        Broadcast<HashMap<String, String>> fo = sc.broadcast(toMap(friendOf));
        Broadcast<HashMap<String, String>> dm = sc.broadcast(toMap(SchemeTriple.getDomainSet()));
        Broadcast<HashMap<String, String>> rg = sc.broadcast(toMap(SchemeTriple.getRangeSet()));
//        System.out.println(subclasses.toString());
//        System.out.println(subprops.toString());
        //rule 7: s p o & p rdfs:subPropertyOf q => s q o
        JavaDStream<RDFTriple> r7_t = triples
                .filter(t -> sp.value().containsKey(t.predicate))
                .map(t -> new RDFTriple(t.subject, sp.value().get(t.predicate), t.object))
                .persist();
        JavaDStream<RDFTriple> r7_out = r7_t.union(triples);
        //rule 2:p rdfs:domain x & s p o => s rdf:type x
        JavaDStream<RDFTriple> r2_out = r7_out
                .filter(t -> dm.value().containsKey(t.predicate))
                .map(t -> new RDFTriple(t.subject, Rules.RDFS_TYPE, dm.value().get(t.predicate)));
        //rule 3:p rdfs:range x & s p o => o rdf:type x
        JavaDStream<RDFTriple> r3_out = r7_out
                .filter(t -> rg.value().containsKey(t.predicate))
                .map(t -> new RDFTriple(t.object, Rules.RDFS_TYPE, rg.value().get(t.predicate)));
        //the result of rule 2 and rule 3
        JavaDStream<RDFTriple> tp = triples
                .filter(t -> t.predicate.equals(Rules.RDFS_TYPE));
        JavaDStream<RDFTriple> out_23 = r2_out.union(r3_out).union(tp);
        //rule 9:s rdf:type x & x rdfs:subClassOf y => s rdf:type y
        JavaDStream<RDFTriple> r9_out = out_23
                .filter(t -> cl.value().containsKey(t.predicate))
                .map(t -> new RDFTriple(t.subject, Rules.RDFS_TYPE, cl.value().get(t.predicate)));
        // rule 11 (?a rdfs:friendOf ?b) -> (?b rdfs:friendOf ?a)
        JavaDStream<RDFTriple> friend = triples.filter(t -> t.predicate.equals(Rules.RDFS_FRIENDOF));
        JavaDStream<RDFTriple> r11_t = friend
                .map(t -> new RDFTriple(t.object, Rules.RDFS_FRIENDOF, t.subject));
        JavaDStream<RDFTriple> r11_out = friend.union(r11_t);

//         rule 12 (?a rdfs:friendOf ?b), (?b rdfs:friendOf ?c) -> (?a rdfs:friendOf ?c)
//        JavaPairDStream<String, String> subjects = r11_out.mapToPair(t -> {return new Tuple2<>(t.subject, t.object);});
//        JavaPairDStream<String, String> objects = r11_out.mapToPair(t -> {return new Tuple2<>(t.object, t.subject);});
//        r11_out.print();
//        subjects.print();
//        objects.print();

//        objects.join(objects);

//        JavaPairDStream<String, Tuple2<String, String>> r12_t_1 = objects.join(subjects);

//        JavaDStream<com.triple.common.RDFTriple> r12_out = r12_t_1.map(t -> new com.triple.common.RDFTriple(t._2._1, Rules.RDFS_FRIENDOF, t._2._2));

//        JavaDStream<com.triple.common.RDFTriple> r12_t =
        JavaDStream<RDFTriple> tpall = out_23
                .union(r9_out)
                .union(r7_t)
//                .union(r12_out)
                .union(triples);
        tpall.foreachRDD(rdd -> {
            long startTS = System.currentTimeMillis();
            rdd.foreach(triple -> {
                Producer.sendMessagesTriple(Consumer.BACK_TOPIC, String.valueOf(1), triple, Consumer.BROKER_LIST);
                System.out.println("放入: " + triple.toString());
            });
            long endTS = System.currentTimeMillis();
            System.out.println("Execution Time : " + (endTS - startTS));
//            System.out.println("放入 : " + rdd.count());
        });

        // 导入scheme三元组
//  可以打印所有信息，看下ConsumerRecord的结构
//      lines.foreachRDD(rdd -> {
//          rdd.foreach(x -> {
//            System.out.println(x);
//          });
//        });

        ssc.start();
        ssc.awaitTermination();
        ssc.close();
    }

    private static HashMap<String, String> toMap(HashSet<RDFTriple> set) {
        HashMap<String, String> map = new HashMap<>();
        for (RDFTriple rdf: set) {
            map.put(rdf.subject, rdf.object);
        }
        return map;
    }

    private static HashSet<RDFTriple> toSet(JavaDStream<RDFTriple> subprop) {
        HashSet<RDFTriple> set = new HashSet<>();
        subprop.filter(rdfTriple -> {
            set.add(rdfTriple);
            return true;
        });
        return set;
    }

    private static HashSet<RDFTriple> transitiveClosure(HashSet<RDFTriple> set) {
        HashSet<RDFTriple> t = addTransitive(set);
        if (t.size() == set.size()) {
            return t;
        } else {
            return transitiveClosure(t);
        }
    }

    private static HashSet<RDFTriple> addTransitive(HashSet<RDFTriple> set) {
        HashSet<RDFTriple> res = new HashSet<>();
        for (RDFTriple rdf: set) {
            for (RDFTriple rdf1: set) {
                if (rdf.object.equals(rdf1.subject)) {
                    res.add(new RDFTriple(rdf.subject, rdf.object, rdf1.object));
                }
            }
        }
        set.addAll(res);
        return set;
    }
}
