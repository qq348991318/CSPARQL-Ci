package lubm_query_csparql;

import eu.larkc.csparql.common.utils.CsparqlUtils;
import eu.larkc.csparql.common.utils.ReasonerChainingType;
import eu.larkc.csparql.core.engine.ConsoleFormatter;
import eu.larkc.csparql.core.engine.CsparqlEngineImpl;
import eu.larkc.csparql.core.engine.CsparqlQueryResultProxy;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import streamer.LubmStreamer;

/**
 * @author 杨帆玉
 * @date 2019/11/11 4:50 下午
 */
public class Query9 {
    public static final Logger logger = LoggerFactory.getLogger(Query9.class);

    public static void main(String[] args) {

        try{

            //Configure log4j logger for the csparql engine
            PropertyConfigurator.configure("/Users/yangfanyu/IdeaProjects/CSPARQL-engine/csparql-reason/src/main/java/log4j_configuration/csparql_readyToGoPack_log4j.properties");

//            <http://www.Department13.University20.edu>
            /*
            Q8: 考虑一种情况
            a memberOf org, b memberOf org => a schoolMate b , b schoolMate a ,
            如果 a 和 b 都是同属于一个组织, 就说 a 和 b 互为校友, 此时数量是 2 (a,b) (2*1)
            那么当有 3 个人的时候, ab,ac,bc,ba,ca,cb = 6 (3*2)
            那么当有 4 个人的时候, ab,ac,ad,ba,bc,bd,ca,cb,cd,da,db,dc = 12 (4*3)
            那么当有 n 个人的时候, n*(n-1) 个数量级推理得到的数据就很胖大了
            * */
            String queryBody = "REGISTER QUERY friend AS "
                    + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
                    + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                    + "PREFIX lubm: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#> "
                    + "select ?x ?y "
                    + "FROM STREAM <http://www.reasoning.org/streams/lubm> [RANGE 40s STEP 1s] "
                    + "WHERE { "
//                    + "?x rdfs:classMate ?y ."
//                    + "?x rdfs:schoolMate ?y ."
                    + "?x rdfs:memberOf ?o ."
                    + "?y rdfs:memberOf ?o ."
//                    + "?x rdfs:takesCourse ?y ."
//                    + "FILTER (?x != ?y)"
                    + "}";

            LubmStreamer lubmStreamer = new LubmStreamer("http://www.reasoning.org/streams/lubm", "http://www.reasoning.org/ontologies#", 800);

            //Create csparql engine instance
            CsparqlEngineImpl engine = new CsparqlEngineImpl();

            //Initialize the engine instance
            //The initialization creates the static engine (SPARQL) and the stream engine (CEP)
            engine.initialize();

            //Register new stream in the engine
            engine.registerStream(lubmStreamer);

            //Register new query in the engine
            CsparqlQueryResultProxy c = engine.registerQuery(queryBody, true, false);

            //Attach a result consumer to the query result proxy to print the results on the console
            c.addObserver(new ConsoleFormatter());

            //Start the thread that put the triples in the engine
            Thread postThread = new Thread(lubmStreamer);
            postThread.start();

            engine.updateReasoner(c.getSparqlQueryId(), CsparqlUtils.fileToString("/Users/yangfanyu/IdeaProjects/csparql_spark/csparql-reason/src/main/java/examples_files/friendRules.rules"), ReasonerChainingType.FORWARD, CsparqlUtils.serializeRDFFile("/Users/yangfanyu/IdeaProjects/csparql_spark/csparql-reason/src/main/java/examples_files/univ-bench163.nt"));

        }catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
