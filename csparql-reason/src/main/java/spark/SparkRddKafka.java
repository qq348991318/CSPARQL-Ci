package spark;

import com.triple.common.RDFTriple;
import eu.larkc.csparql.cep.esper.Producer;
import eu.larkc.csparql.cep.esper.RDFTripleDeserializer;
import eu.larkc.csparql.core.pojo.Consumer;
import eu.larkc.csparql.core.pojo.Rules;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.HashPartitioner;
import org.apache.spark.Partitioner;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import redis.clients.jedis.Jedis;
import scala.Tuple3;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 杨帆玉
 * @date 2019/11/13 10:55 上午
 */
public class SparkRddKafka {
    public static final int parallism = 10;
    public static StorageLevel storagelevel = StorageLevel.MEMORY_ONLY();
    public static final Partitioner partitioner = new HashPartitioner(parallism);
    public static Jedis jedis = new Jedis("localhost");

    public static AtomicInteger gIndex = new AtomicInteger(0);

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

        JavaDStream<RDFTriple> tpall = triples.transform((Function<JavaRDD<RDFTriple>, JavaRDD<RDFTriple>>) rdd -> {
            // rule11
            JavaRDD<Tuple3<String, String, String>> friends = rdd
                    .filter(t -> t.predicate.equals(Rules.RDFS_FRIENDOF))
                    .map(t -> new Tuple3<>(t.subject, t.predicate, t.object));

            JavaRDD<Tuple3<String, String, String>> r11_t = friends.map(t -> new Tuple3<>(t._3(), t._2(), t._1()));

            JavaRDD<Tuple3<String, String, String>> r11_out = friends.union(r11_t).distinct(parallism);

            // rule12
//            JavaRDD<Tuple3<String, String, String>> r12_out = transitive(r11_out);
            JavaRDD<Tuple3<String, String, String>> r12_out = transitiveRedis(r11_out);
            JavaRDD<Tuple3<String, String, String>> friendOut = r12_out.union(r11_out);
            return friendOut.distinct().map(t -> new RDFTriple(t._1(), t._2(), t._3()));
        });

        tpall.foreachRDD(rdd -> {
//            long startTS = System.currentTimeMillis();
//            rdd.foreach(triple -> {
//                Producer.sendMessagesTriple(Consumer.BACK_TOPIC, String.valueOf(1), triple, Consumer.BROKER_LIST);
////                System.out.println("放入: " + triple.toString());
//            });
//            long endTS = System.currentTimeMillis();
//            System.out.println("Execution Time : " + (endTS - startTS));
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

    private static JavaRDD<Tuple3<String, String, String>> transitiveRedis(JavaRDD<Tuple3<String, String, String>> input) {
        long startTS = System.currentTimeMillis();
        // 定义图的索引
        Set<String> keys = new HashSet<>();
        String graph = "friendOf_";

        input.foreach(t -> {
            System.out.println("****************");
            System.out.println("K => " + t._1() + "  V => " + jedis.get(t._1()));
            System.out.println("K => " + t._3() + "  V => " + jedis.get(t._3()));
            String sIndex = jedis.get(t._1());
            String oIndex = jedis.get(t._3());
            if (sIndex == null && oIndex == null) {
                jedis.set(t._1(), graph + gIndex.get());
                jedis.set(t._3(), graph + gIndex.get());
                jedis.sadd(graph + gIndex.get(), t._1());
                jedis.sadd(graph + gIndex.get(), t._3());
                keys.add(graph + gIndex.get());
                gIndex.incrementAndGet();
            } else {
                if (sIndex == null) {
                    jedis.set(t._1(), oIndex);
                    jedis.sadd(oIndex, t._1());
                } else if (oIndex == null) {
                    jedis.set(t._3(), sIndex);
                    jedis.sadd(sIndex, t._3());
                } else if (!oIndex.equals(sIndex)) {
                    // 获取最小图 index
                    // 将图中的所有元素全部设置为 currG
                    System.out.println(jedis.smembers(sIndex));
                    System.out.println(jedis.smembers(oIndex));
                    Set<String> sElements = jedis.smembers(sIndex);
                    Set<String> oElements = jedis.smembers(oIndex);
                    if (sElements.size() < oElements.size()) {
                        for (String element: sElements) {
                            jedis.set(element, oIndex);
                            jedis.sadd(oIndex, element);
//                            jedis.smove(sIndex, oIndex, element);
                        }
                        keys.remove(sIndex);
                    } else {
                        for (String element: oElements) {
                            jedis.set(element, sIndex);
                            jedis.sadd(sIndex, element);
//                            jedis.smove(oIndex, sIndex, element);
                        }
                        keys.remove(oIndex);
                    }
                }
            }
        });
        int count = 0;
        for (String key: keys) {
            Set<String> elements = jedis.smembers(key);
            System.out.println(key);
            for (String element: elements) {
                for (String element1: elements) {
                    RDFTriple rdfTriple = new RDFTriple(element, Rules.RDFS_FRIENDOF, element1);
                    System.out.println(rdfTriple.toString());
                    Producer.sendMessagesTriple(Consumer.BACK_TOPIC, String.valueOf(1), rdfTriple, Consumer.BROKER_LIST);
                    count++;
                }

            }
        }
        long endTS = System.currentTimeMillis();
        System.out.println("Execution Time : " + (endTS - startTS));
        System.out.println("放入 : " + count);
        return input;
    }

//    public static JavaRDD<Tuple3<String, String, String>> transitive(JavaRDD<Tuple3<String, String, String>> input) {
//        long nextcount;
//        JavaRDD<Tuple3<String, String, String>> p = input;
//        JavaRDD<Tuple3<String, String, String>> q = p;
//
//
//        JavaPairRDD<Tuple2<String, String>, String> l = q
//                .mapToPair(t -> new Tuple2<>(new Tuple2<>(t._2(), t._3()), t._1()))
//                .partitionBy(partitioner);
//        do {
//            JavaPairRDD<Tuple2<String, String>, String> r = q
//                    .mapToPair(t -> new Tuple2<>(new Tuple2<>(t._2(), t._1()), t._3()))
//                    .partitionBy(partitioner);
//            JavaRDD<Tuple3<String, String, String>> q1 = l.join(r).map(t -> new Tuple3<>(t._2._1, t._1._1, t._2._2));
//            q = q1.subtract(p, parallism).persist(storagelevel);
//            nextcount = q.count();
//
//            if (nextcount != 0L) {
//                l = q
//                    .mapToPair(t -> new Tuple2<>(new Tuple2<>(t._2(), t._3()), t._1()))
//                    .partitionBy(partitioner);
//                JavaPairRDD<Tuple2<String, String>, String> s = p
//                        .mapToPair(t -> new Tuple2<>(new Tuple2<>(t._2(), t._1()), t._3()))
//                        .partitionBy(partitioner);
//                JavaRDD<Tuple3<String, String, String>> p1 = s.join(l).map(t -> new Tuple3<>(t._2._2, t._1._1, t._2._1));
//                p = p1.union(p).union(q);
//            }
//        } while (nextcount != 0L);
//        return p;
//    }

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
