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
public class Query8 {
    public static final Logger logger = LoggerFactory.getLogger(Query8.class);

    public static void main(String[] args) {

        try{

            //Configure log4j logger for the csparql engine
            PropertyConfigurator.configure("/Users/yangfanyu/IdeaProjects/CSPARQL-engine/csparql-reason/src/main/java/log4j_configuration/csparql_readyToGoPack_log4j.properties");

//            <http://www.Department13.University20.edu>
            /*
            Q8: The most complex query of our evaluation with two inferences over concept hierarchies (Faculty and Student),
            with the former containing sameAs individual cliques, and inferences over the memberOf property hierarchy.
            * */
            String queryBody = "REGISTER QUERY friend AS "
                    + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
                    + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                    + "PREFIX lubm: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#> "
                    + "select ?x ?s "
                    + "FROM STREAM <http://www.reasoning.org/streams/lubm> [RANGE 10s STEP 1s] "
                    + "WHERE { "
                    + "?x rdf:type lubm:Faculty ."
                    + "?x lubm:name ?nx ."
                    + "?x lubm:memberOf ?o ."
                    + "?s lubm:advisor ?x ."
                    + "?s rdf:type lubm:Student ."
                    + "?s lubm:name ?ns ."
                    + "}";

            LubmStreamer lubmStreamer = new LubmStreamer("http://www.reasoning.org/streams/lubm", "http://www.reasoning.org/ontologies#", 1000);

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

            engine.updateReasoner(c.getSparqlQueryId(), CsparqlUtils.fileToString("/Users/yangfanyu/IdeaProjects/csparql_spark/csparql-reason/src/main/java/examples_files/rdfs.rules"), ReasonerChainingType.FORWARD, CsparqlUtils.serializeRDFFile("/Users/yangfanyu/IdeaProjects/csparql_spark/csparql-reason/src/main/java/examples_files/univ-bench161.nt"));

        }catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
