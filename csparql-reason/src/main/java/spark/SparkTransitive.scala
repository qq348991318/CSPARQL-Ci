package spark

import com.triple.common.RDFTriple
import eu.larkc.csparql.core.pojo.Rules
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}

/**
 * @author 杨帆玉
 * @date 2019/11/13 2:37 下午
 */
object SparkTransitive {

  val parallism = 3
  var storagelevel = StorageLevel.MEMORY_ONLY
  val partitioner = new HashPartitioner(parallism)

  def transitive(input: RDD[(String, String, String)]) = {
    var nextcount = 1L
    var p =input
    var q = p
    var l = q.map(t => ((t._2,t._3),t._1)).partitionBy(partitioner)
    do{
      val r = q.map(t => ((t._2,t._1),t._3)).partitionBy(partitioner)
      val q1 = l.join(r).map(t => (t._2._1,t._1._1,t._2._2))
      q = q1.subtract(p,parallism).persist(storagelevel)
      nextcount= q.count
      if(nextcount!=0){
        l = q.map(t => ((t._2,t._3),t._1)).partitionBy(partitioner)
        val s = p.map(t => ((t._2,t._1),t._3)).partitionBy(partitioner)
        val p1=s.join(l).map(t => (t._2._2,t._1._1,t._2._1)).persist(storagelevel)
        p = p1.union(q).union(p)
      }
    }while(nextcount != 0)
    p
  }

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setMaster("local[2]")
      .setAppName("reasoning")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    val sc = new SparkContext(conf)
    val source = new Array[RDFTriple](10)
    for (i <- 1 to 10) {
      source(i-1) = new RDFTriple("person" + i, Rules.RDFS_FRIENDOF, "person" + (i+1))
    }
    val rdfTriples = sc.parallelize(source)
//    println(rdfTriples.toString())
    val input = rdfTriples
      .map(t => (t.subject, t.predicate, t.`object`))
    val startTime = System.currentTimeMillis()
    val output = transitive(input)
    val endTime = System.currentTimeMillis()
    println("****************" + (endTime - startTime))
  }
}
