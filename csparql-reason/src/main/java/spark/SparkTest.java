package spark;

import com.triple.common.RDFTriple;
import eu.larkc.csparql.core.pojo.Rules;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple3;

import java.util.Arrays;

/**
 * @author 杨帆玉
 * @date 2019/11/13 1:24 下午
 */
public class SparkTest {

    public static RDFTriple[] source = {
            new RDFTriple("a", Rules.RDFS_FRIENDOF, "b"),
            new RDFTriple("b", Rules.RDFS_FRIENDOF, "c"),
            new RDFTriple("c", Rules.RDFS_FRIENDOF, "d")};

    public static void main(String[] args) {
        SparkConf conf = new SparkConf().setMaster("local[2]").setAppName("streaming")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
        JavaSparkContext sc = new JavaSparkContext(conf);
        JavaRDD<RDFTriple> rdfTriples = sc.parallelize(Arrays.asList(source));
        JavaRDD<Tuple3<String, String, String>> input = rdfTriples.map(t -> new Tuple3<>(t.subject, t.predicate, t.object));
//        JavaRDD<Tuple3<String, String, String>> output = SparkRddKafka.transitive(input);
//        output.foreach(rdd -> {
//            System.out.println(rdd.toString());
//        });
    }
}
