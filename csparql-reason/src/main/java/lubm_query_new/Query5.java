package lubm_query_new;

import eu.larkc.csparql.core.engine.ConsoleFormatter;
import eu.larkc.csparql.core.engine.CsparqlEngineImpl;
import eu.larkc.csparql.core.engine.CsparqlQueryResultProxy;
import eu.larkc.csparql.core.reasoner.Index;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import streamer.LubmStreamer;

import java.util.ArrayList;

/**
 * @author 杨帆玉
 * @date 2019/11/11 4:50 下午
 */
public class Query5 {
    public static final Logger logger = LoggerFactory.getLogger(Query5.class);

    public static void main(String[] args) {

        try{

            //Configure log4j logger for the csparql engine
            PropertyConfigurator.configure("/Users/yangfanyu/IdeaProjects/CSPARQL-engine/csparql-reason/src/main/java/log4j_configuration/csparql_readyToGoPack_log4j.properties");

//            <http://www.Department13.University20.edu>
            /*
            Q5: This query goes further than Q4 by mixing Q2 and Q3, i.e., it requires reasoning over the Professor and Student concept hierarchies and the memberOf property hierarchy.
            * */
            String queryBody = "REGISTER QUERY friend AS "
                    + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
                    + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                    + "PREFIX lubm: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#> "
                    + "select ?s ?x ?o "
                    + "FROM STREAM <http://www.reasoning.org/streams/lubm> [RANGE 1s STEP 1s] "
                    + "WHERE { "
                    + "?x rdf:type lubm:Professor ."
                    + "?x rdfs:memberOf ?o ."
                    + "?s lubm:advisor ?x ."
                    + "?s rdf:type lubm:Student ."
                    + "}";
            ArrayList<Index> indexList = new ArrayList<>();
            indexList.add(new Index(Index._PO, "", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" , "http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#Professor", 1, 0, 0));
            indexList.add(new Index(Index._P_, "", "http://www.w3.org/2000/01/rdf-schema#memberOf" , "", 1, 0,2));
            indexList.add(new Index(Index._P_, "", "http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#advisor" , "", 3, 0,1));
            indexList.add(new Index(Index._PO, "", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" , "http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#Student", 3, 0,0));
            LubmStreamer lubmStreamer = new LubmStreamer("http://www.reasoning.org/streams/lubm", "http://www.reasoning.org/ontologies#", 10000);

            //Create csparql engine instance
            CsparqlEngineImpl engine = new CsparqlEngineImpl();

            //Initialize the engine instance
            //The initialization creates the static engine (SPARQL) and the stream engine (CEP)
            engine.initialize();

            engine.registerCount(20);

            engine.registerIndex(indexList);
            //Register new stream in the engine
            engine.registerStream(lubmStreamer);

            //Register new query in the engine
            CsparqlQueryResultProxy c = engine.registerQuery(queryBody, true, true);

            //Attach a result consumer to the query result proxy to print the results on the console
            c.addObserver(new ConsoleFormatter());

            //Start the thread that put the triples in the engine
            Thread postThread = new Thread(lubmStreamer);
            postThread.start();

//            engine.updateReasoner(c.getSparqlQueryId(), CsparqlUtils.fileToString("/Users/yangfanyu/IdeaProjects/csparql_spark/csparql-reason/src/main/java/examples_files/friendRules.rules"), ReasonerChainingType.FORWARD, CsparqlUtils.serializeRDFFile("/Users/yangfanyu/IdeaProjects/csparql_spark/csparql-reason/src/main/java/examples_files/univ-bench.nt"));

        }catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
