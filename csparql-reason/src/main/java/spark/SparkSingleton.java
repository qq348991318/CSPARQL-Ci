package spark;

import org.apache.spark.SparkConf;

/**
 * @author 杨帆玉
 * @date 2019/11/1 10:37 上午
 */
public class SparkSingleton {
    public static SparkConf conf = new SparkConf().
    setAppName("Big-SR").
    set("spark.ui.enabled", "true").
    set("spark.locality.wait", "1s").
    set("spark.some.config.option", "some-value").
    set("spark.ui.showConsoleProgress", "false").
    set("spark.storage.memoryFraction", "0.5").
    set("spark.scheduler.mode", "FAIR").
    set("spark.streaming.backpressure.enabled", "false").
    set("spark.streaming.ui.retainedBatches", "300").
    set("spark.executor.extraJavaOptions", "-XX:+UseConcMarkSweepGC").
    set("spark.driver.extraJavaOptions", "-XX:+UseConcMarkSweepGC").
    set("spark.kryo.registrationRequired", "true").
    set("spark.serializer", "org.apache.spark.serializer.KryoSerializer").
    set("spark.kryo.registrator", String.valueOf(SparkKryoRegistrator.class));
    int numPartition = 8;
    int shuffledPartition = 4;
    int checkpointThreshold = 10;

    public void init(Boolean localMode,
             int numPartition,
             int shuffledPartition,
             int concurrentJobs,
             int checkpointThreshold) {
        if (localMode)
            conf.setMaster("local[*]").
                    set("spark.driver.host", "localhost");
        conf.set("spark.default.parallelism", numPartition + "").
                set("spark. streaming.concurrentJobs", concurrentJobs + "");
        this.numPartition = numPartition;
        this.shuffledPartition = shuffledPartition;
        this.checkpointThreshold = checkpointThreshold;
    }
}
