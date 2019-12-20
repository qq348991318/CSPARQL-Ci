package streamer;

import com.esotericsoftware.kryo.Kryo;
import org.apache.spark.serializer.KryoSerializer;
import spark.SparkSingleton;

/**
 * @author 杨帆玉
 * @date 2019/11/1 9:43 上午
 */
public class SparkSerializer {
    public static KryoSerializer kryoSerializer = new KryoSerializer(SparkSingleton.conf);
    Kryo kryo = kryoSerializer.newKryo();

    static ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
        protected Kryo initialValue() {
            return kryoSerializer.newKryo();
        }
    };
}
