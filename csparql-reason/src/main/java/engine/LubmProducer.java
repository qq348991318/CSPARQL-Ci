package engine;

import eu.larkc.csparql.cep.esper.Producer;
import redis.JedisUnit;
import redis.clients.jedis.Jedis;

/**
 * @author 杨帆玉
 * @date 2019/11/11 3:53 下午
 */
public class LubmProducer {

    public static int streamRate;

    public LubmProducer(int streamRate) {
        this.streamRate = streamRate;
    }

    public static void main(String[] args) {
        streamRate = 10000;

        long count = 0L;

        int id = 1;

        boolean keeprunning = true;

        long startTime, endTime;

        startTime = System.currentTimeMillis();

        Jedis jedis = JedisUnit.getJedis();

        while (keeprunning && (count < streamRate)) {

            String rdfStr = jedis.get(String.valueOf(id));

            Producer.sendMessagesString(Producer.TO_TOPIC, String.valueOf(1), rdfStr, Producer.BROKER_LIST);
//            com.triple.common.RDFTriple rdfTriple = JSON.parseObject(rdfStr, com.triple.common.RDFTriple.class);
//            RdfQuadruple q = new RdfQuadruple(rdfTriple.getSubject(), rdfTriple.getPredicate(), rdfTriple.getObject(), System.currentTimeMillis());
//            this.put(q);
            count++;
            id++;

            try {
                if (count == streamRate) {
                    endTime = System.currentTimeMillis();
                    long time = 1000L - (endTime - startTime);
                    Thread.sleep(time > 0 ? time : 0);
                    System.out.println(count +
                            " triples have been completely updated, time spent: " + (endTime - startTime));
                    startTime = System.currentTimeMillis();
                    count = 0;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
