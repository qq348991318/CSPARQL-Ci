package scparql_query;

import eu.larkc.csparql.common.utils.CsparqlUtils;
import eu.larkc.csparql.common.utils.ReasonerChainingType;
import eu.larkc.csparql.core.engine.ConsoleFormatter;
import eu.larkc.csparql.core.engine.CsparqlEngineImpl;
import eu.larkc.csparql.core.engine.CsparqlQueryResultProxy;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import streamer.PostStream;

/**
 * @author 杨帆玉
 * @date 2019/10/30 8:03 下午
 */
public class Query2 {
    public static final Logger logger = LoggerFactory.getLogger(Query2.class);

    public static void main(String[] args) {

        try{

            //Configure log4j logger for the csparql engine
            PropertyConfigurator.configure("/Users/yangfanyu/IdeaProjects/CSPARQL-engine/csparql-reason/src/main/java/log4j_configuration/csparql_readyToGoPack_log4j.properties");

            String queryBody = "REGISTER STREAM TYPE AS "
                    + "PREFIX : <http://www.streamreasoning.org/ontologies/sr4ld2014-onto#> "
                    + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                    + "SELECT ?person ?receiver ?room "
                    + "FROM STREAM <http://streamreasoning.org/streams/post> [RANGE 1s STEP 1s] "
                    + "WHERE { "
                    + "?person :observes [ :to ?receiver; :where ?room]"
                    + "}";

            PostStream postStream = new PostStream("http://streamreasoning.org/streams/post", "http://www.streamreasoning.org/ontologies/sr4ld2014-onto#", 12);

            //Create csparql engine instance
            CsparqlEngineImpl engine = new CsparqlEngineImpl();

            //Initialize the engine instance
            //The initialization creates the static engine (SPARQL) and the stream engine (CEP)
            engine.initialize();

            //Register new stream in the engine
            engine.registerStream(postStream);



            //Register new query in the engine
            CsparqlQueryResultProxy c = engine.registerQuery(queryBody, false, true);

            //Attach a result consumer to the query result proxy to print the results on the console
            c.addObserver(new ConsoleFormatter());

            //Start the thread that put the triples in the engine
            for (int i = 0; i < 1; i++) {
                Thread postThread = new Thread(postStream);
                postThread.start();
            }

//            engine.updateReasoner(c.getSparqlQueryId(), CsparqlUtils.fileToString("/Users/yangfanyu/IdeaProjects/CSPARQL-engine/csparql-reason/src/main/java/examples_files/rdfs.rules"), ReasonerChainingType.FORWARD, CsparqlUtils.serializeRDFFile("/Users/yangfanyu/IdeaProjects/CSPARQL-engine/csparql-reason/src/main/java/examples_files/tbox.rdf"));

        }catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
