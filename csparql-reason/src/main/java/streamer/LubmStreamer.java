package streamer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.triple.common.RDFTriple;
import eu.larkc.csparql.cep.api.RdfQuadruple;
import eu.larkc.csparql.cep.api.RdfStream;

import java.io.InputStream;

/**
 * @author 杨帆玉
 * @date 2019/10/29 10:03 上午
 */
public class LubmStreamer extends RdfStream implements Runnable {

    public static String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static String rdfs = "http://www.w3.org/2000/01/rdf-schema#";

    private int streamRate;
    private String baseUri;

    public LubmStreamer(String iri, String baseUri, int streamRate) {
        super(iri);
        this.baseUri = baseUri;
        this.streamRate = streamRate;
    }

    public void run() {

        long start  = System.currentTimeMillis();
        String inputFileName = "/Users/yangfanyu/IdeaProjects/csparql_spark/csparql-reason/src/main/java/dataset/lubm1000000.nt";
//        String inputFileName = "/Users/yangfanyu/IdeaProjects/uba1.7/University0-49-clean2.nt";
//        String inputFileName = "/Users/yangfanyu/IdeaProjects/Sparkweave/spark-dist/tests/deprecated/bsbm/lubm10000.nt";

        Model model = ModelFactory.createDefaultModel();

        InputStream in = FileManager.get().open(inputFileName);

        if (in == null) throw new IllegalArgumentException("File: " + inputFileName + " not found");

        model.read(in, "", "N3");
        long end = System.currentTimeMillis();
        System.out.println("开始 : 文件解析时间为: " + (end - start));


        StmtIterator iter = model.listStatements();

        long count = 0L;

        int id = 1;

        long startTime, endTime;

        startTime = System.currentTimeMillis();

//        Jedis jedis = JedisUnit.getJedis();


        while (iter.hasNext() && (count < streamRate)) {
            Statement stmt = iter.nextStatement(); // get next statement
            RDFTriple rdfTriple = new RDFTriple();
            //RDFNode object = stmt.getObject(); // get the object
            String subject = stmt.getSubject().toString(); // get the subject
            rdfTriple.setSubject(subject);
            String predicate = stmt.getPredicate().toString(); // get the predicate
            if (predicate.equals("http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#memberOf")) {
                predicate = "http://www.w3.org/2000/01/rdf-schema#memberOf";
//                System.out.println(subject);
            }
            if (predicate.equals("http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#takesCourse")) {
                predicate = "http://www.w3.org/2000/01/rdf-schema#takesCourse";
//                System.out.println(subject);
            }
            rdfTriple.setPredicate(predicate);
            String object = stmt.getObject().toString(); // get the object
            rdfTriple.setObject(object);

            RdfQuadruple q = new RdfQuadruple(rdfTriple.getSubject(), rdfTriple.getPredicate(), rdfTriple.getObject(), System.currentTimeMillis());
            this.put(q);

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
