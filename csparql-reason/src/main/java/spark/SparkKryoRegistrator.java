package spark;

import com.esotericsoftware.kryo.Kryo;
import com.triple.common.RDFTriple;
import org.apache.spark.serializer.KryoRegistrator;

/**
 * @author 杨帆玉
 * @date 2019/11/1 11:20 上午
 */
public class SparkKryoRegistrator implements KryoRegistrator {
    public void registerClasses(Kryo kryo) {
        kryo.register(RDFTriple.class);
        kryo.register(String.class);
    }
}
