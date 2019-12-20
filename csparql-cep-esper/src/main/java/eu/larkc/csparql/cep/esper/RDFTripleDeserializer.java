package eu.larkc.csparql.cep.esper;

import com.alibaba.fastjson.JSON;
import com.triple.common.RDFTriple;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author 杨帆玉
 * @date 2019/11/3 2:58 下午
 */
public class RDFTripleDeserializer implements Deserializer<RDFTriple> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public RDFTriple deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        if (data.length < 8) {
            throw new SerializationException("Size of data received by DemoDeserializer is shorter than expected!");
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int length = buffer.getInt();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        String str = new String(bytes, StandardCharsets.UTF_8);
        return JSON.parseObject(str, RDFTriple.class);
    }

    @Override
    public void close() {

    }
}
