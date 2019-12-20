package scparql_query;

import eu.larkc.csparql.common.utils.CsparqlUtils;
import eu.larkc.csparql.common.utils.ReasonerChainingType;
import eu.larkc.csparql.core.engine.ConsoleFormatter;
import eu.larkc.csparql.core.engine.CsparqlEngineImpl;
import eu.larkc.csparql.core.engine.CsparqlQueryResultProxy;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import streamer.FriendStreamer;
import streamer.LubmStreamer;

/**
 * @author 杨帆玉
 * @date 2019/11/11 4:50 下午
 */
public class Query3 {
    public static final Logger logger = LoggerFactory.getLogger(Query3.class);

    public static void main(String[] args) {

        try{

            //Configure log4j logger for the csparql engine
            PropertyConfigurator.configure("/Users/yangfanyu/IdeaProjects/CSPARQL-engine/csparql-reason/src/main/java/log4j_configuration/csparql_readyToGoPack_log4j.properties");

//            String queryBody = "REGISTER QUERY reasonging AS "
//                    + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
//                    + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#> "
//                    + "SELECT ?X"
//                    + "FROM STREAM <http://streamreasoning.org/streams/lubm> [RANGE 1s STEP 1s] "
//                    + "WHERE { "
//                    + "?X rdf:type ub:GraduateStudent ."
//                    + "?X ub:takesCourse http://www.Department0.University0.edu/GraduateCourse0"
//                    + "}";

            String queryBody = "REGISTER QUERY friend AS "
                    + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
                    + "PREFIX : <http://www.reasoning.org/ontologies#> "
                    + "CONSTRUCT { ?X rdfs:friendOf ?Y } "
                    + "FROM STREAM <http://www.reasoning.org/streams/friend> [RANGE 1s STEP 1s] "
                    + "WHERE { "
                    + "?X rdfs:friendOf ?Y ."
                    + "FILTER (?X != ?Y)"
                    + "}";

            FriendStreamer friendStreamer = new FriendStreamer("http://www.reasoning.org/streams/friend", "http://www.reasoning.org/ontologies#", 10000, 1);

            //Create csparql engine instance
            CsparqlEngineImpl engine = new CsparqlEngineImpl();

            //Initialize the engine instance
            //The initialization creates the static engine (SPARQL) and the stream engine (CEP)
            engine.initialize();

            //Register new stream in the engine
            engine.registerStream(friendStreamer);



            //Register new query in the engine
            CsparqlQueryResultProxy c = engine.registerQuery(queryBody, false, true);

            //Attach a result consumer to the query result proxy to print the results on the console
            c.addObserver(new ConsoleFormatter());

            //Start the thread that put the triples in the engine
            Thread postThread = new Thread(friendStreamer);
            postThread.start();

//            engine.updateReasoner(c.getSparqlQueryId(), CsparqlUtils.fileToString("/Users/yangfanyu/IdeaProjects/csparql_spark/csparql-reason/src/main/java/examples_files/friendRules.rules"), ReasonerChainingType.FORWARD, CsparqlUtils.serializeRDFFile("/Users/yangfanyu/IdeaProjects/csparql_spark/csparql-reason/src/main/java/examples_files/friend.rdf"));

        }catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
