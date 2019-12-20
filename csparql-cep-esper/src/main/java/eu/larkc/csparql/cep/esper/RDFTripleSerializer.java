package eu.larkc.csparql.cep.esper;


import com.alibaba.fastjson.JSON;
import com.triple.common.RDFTriple;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author 杨帆玉
 * @date 2019/11/1 8:50 上午
 */
public class RDFTripleSerializer implements Serializer<RDFTriple> {

    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    public byte[] serialize(String topic, RDFTriple data) {
        if (data == null) {
            return null;
        }
//        Long start = System.currentTimeMillis();
        byte[] bytes;
        String json = JSON.toJSONString(data);
        bytes = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + bytes.length);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
//        Long end = System.currentTimeMillis();
//        System.out.println(end-start);
        return buffer.array();
    }

    public void close() {

    }
}
