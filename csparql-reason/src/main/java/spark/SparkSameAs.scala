package spark

import com.triple.common.RDFTriple
import eu.larkc.csparql.core.pojo.Rules
import org.apache.spark.rdd.{RDD, UnionRDD}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}

/**
 * @author 杨帆玉
 * @date 2019/11/13 2:37 下午
 */
object SparkSameAs {

  val parallism = 3
  var storagelevel = StorageLevel.MEMORY_ONLY
  val partitioner = new HashPartitioner(parallism)

  def sameAsTable(input:RDD[(String,String)]) = {
    //<groupid -personid>
    var owlsame = input
    var it = 0L
    var newit =owlsame.count
    do{
      val owlsameshuffle = owlsame.union(owlsame.map(t => (t._2,t._1)))
        val owltemp = owlsameshuffle.groupByKey(parallism)
          .flatMap(
        t => {
          val min= t._2.min(
            new Ordering[String] {
              def compare(a:String,b:String) =
                a.hashCode compare b.hashCode
            })
          if(min.hashCode < t._1.hashCode){
            for(arg <- t._2) yield (min,arg)
          } else for(arg <- t._2) yield (t._1,arg)
        }).filter(t => !t._1.equals(t._2))
      owlsame = owltemp.persist(storagelevel)
      it = newit
      newit = owlsame.count()
    }while(it != newit)
    owlsame
  }

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setMaster("local[2]")
      .setAppName("reasoning")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    val sc = new SparkContext(conf)
    val source = new Array[RDFTriple](3)
    for (i <- 1 to 3) {
      source(i-1) = new RDFTriple("person" + i, Rules.RDFS_FRIENDOF, "person" + (i+1))
    }
    val rdfTriples = sc.parallelize(source)
//    println(rdfTriples.toString())
    val input = rdfTriples
      .map(t => (t.subject, t.`object`))
    val startTime = System.currentTimeMillis()
    val in = new UnionRDD(sc,List(input)).persist(storagelevel)
    val output = sameAsTable(in).map(t => (t._2,t._1))
    val endTime = System.currentTimeMillis()
    output.foreach(t => println(t.toString()))
    println("****************" + (endTime - startTime))
  }
}
