/**
 * Copyright 2011-2015 DEIB - Politecnico di Milano
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Acknowledgements:
 * We would like to thank Davide Barbieri, Emanuele Della Valle,
 * Marco Balduini, Soheila Dehghanzadeh, Shen Gao, and
 * Daniele Dell'Aglio for the effort in the development of the software.
 *
 * This work is partially supported by
 * - the European LarKC project (FP7-215535) of DEIB, Politecnico di
 * Milano
 * - the ERC grant “Search Computing” awarded to prof. Stefano Ceri
 * - the European ModaClouds project (FP7-ICT-2011-8-318484) of DEIB,
 * Politecnico di Milano
 * - the IBM Faculty Award 2013 grated to prof. Emanuele Della Valle;
 * - the City Data Fusion for Event Management 2013 project funded
 * by EIT Digital of DEIB, Politecnico di Milano
 * - the Dynamic and Distributed Information Systems Group of the
 * University of Zurich;
 * - INSIGHT NUIG and Science Foundation Ireland (SFI) under grant
 * No. SFI/12/RC/2289
 */
package eu.larkc.csparql.core.engine;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.RDFSRuleReasonerFactory;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;
import com.triple.common.RDFTriple;
import eu.larkc.csparql.cep.api.CepEngine;
import eu.larkc.csparql.cep.api.RdfQuadruple;
import eu.larkc.csparql.cep.api.RdfSnapshot;
import eu.larkc.csparql.cep.api.RdfStream;
import eu.larkc.csparql.cep.esper.EsperEngine;
import eu.larkc.csparql.common.RDFTable;
import eu.larkc.csparql.common.exceptions.ReasonerException;
import eu.larkc.csparql.common.utils.ReasonerChainingType;
import eu.larkc.csparql.core.Configuration;
import eu.larkc.csparql.core.imis.tests.BigLoader;
import eu.larkc.csparql.core.imis.tests.BigQueryTests;
import eu.larkc.csparql.core.new_parser.utility_files.StreamInfo;
import eu.larkc.csparql.core.new_parser.utility_files.Translator;
import eu.larkc.csparql.core.pojo.Rules;
import eu.larkc.csparql.core.reasoner.ForwardReasoner;
import eu.larkc.csparql.core.reasoner.Index;
import eu.larkc.csparql.core.streams.formats.CSparqlQuery;
import eu.larkc.csparql.core.streams.formats.TranslationException;
import eu.larkc.csparql.sparql.api.SparqlEngine;
import eu.larkc.csparql.sparql.jena.JenaEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CsparqlEngineImpl implements Observer, CsparqlEngine {

	private Configuration configuration = null;
	private Collection<CSparqlQuery> queries = null;
	private Map<String, RdfStream> streams = null;
	private Map<CSparqlQuery, RdfSnapshot> snapshots = null;
	private Map<CSparqlQuery, CsparqlQueryResultProxy> results = null;
	private CepEngine cepEngine = null;
	private SparqlEngine sparqlEngine = null;
	private Reasoner reasoner = null;
	private int queryNum = 0;
	private static AtomicInteger index = new AtomicInteger(0);
	private boolean needQuery = false;
	private ArrayList<Index> indexList = null;
	private boolean isIndex = false;
	private boolean isDbIndex = false;
	private int num1 = 0;
	private int num2 = 0;
	private int num3 = 0;
	private int num4 = 0;
	private int count = 0;
	private int countStart = 0;

	static String preIndex = "/Users/yangfanyu/IdeaProjects/csparql_spark/csparql-core/src/main/java/eu/larkc/csparql/core/imis/tests/mapdb";
	static String preIndexnew = "/Users/yangfanyu/IdeaProjects/csparql_spark/csparql-core/src/main/java/eu/larkc/csparql/core/imis/tests/newmapdb";

	// 定义kafka 生产者
//	private KafkaProducer kafkaProducer = null;

	protected final Logger logger = LoggerFactory.getLogger(CsparqlEngineImpl.class);

	public Collection<CSparqlQuery> getAllQueries() {
		return this.queries;
	}

	public static Jedis jedis = new Jedis("localhost");

	public static AtomicInteger gIndex = new AtomicInteger(0);

	public void initialize() {

		this.configuration = Configuration.getCurrentConfiguration();
		this.queries = new ArrayList<CSparqlQuery>();
		this.streams = new HashMap<String, RdfStream>();
		this.snapshots = new HashMap<CSparqlQuery, RdfSnapshot>();
		this.results = new HashMap<CSparqlQuery, CsparqlQueryResultProxy>();
		this.sparqlEngine = this.configuration.createSparqlEngine();
		this.cepEngine = this.configuration.createCepEngine();
		this.reasoner = this.configuration.createReasoner();
		this.cepEngine.initialize();
		this.sparqlEngine.initialize();
		this.setPerformTimestampFunctionVariable(false);
		this.setUpInjecter(0);

	}

	public void initialize(int queueDimension) {
		this.configuration = Configuration.getCurrentConfiguration();
		this.queries = new ArrayList<CSparqlQuery>();
		this.streams = new HashMap<String, RdfStream>();
		this.snapshots = new HashMap<CSparqlQuery, RdfSnapshot>();
		this.results = new HashMap<CSparqlQuery, CsparqlQueryResultProxy>();
		this.sparqlEngine = this.configuration.createSparqlEngine();
		this.cepEngine = this.configuration.createCepEngine();
		this.reasoner = this.configuration.createReasoner();
		this.cepEngine.initialize();
		this.sparqlEngine.initialize();
		this.setPerformTimestampFunctionVariable(false);
		this.setUpInjecter(queueDimension);
	}

	public void initialize(boolean performTimestampFunction) {
		this.configuration = Configuration.getCurrentConfiguration();
		this.queries = new ArrayList<CSparqlQuery>();
		this.streams = new HashMap<String, RdfStream>();
		this.snapshots = new HashMap<CSparqlQuery, RdfSnapshot>();
		this.results = new HashMap<CSparqlQuery, CsparqlQueryResultProxy>();
		this.sparqlEngine = this.configuration.createSparqlEngine();
		this.cepEngine = this.configuration.createCepEngine();
		this.reasoner = this.configuration.createReasoner();
		this.cepEngine.initialize();
		this.sparqlEngine.initialize();
		this.setPerformTimestampFunctionVariable(performTimestampFunction);
		this.setUpInjecter(0);
	}

	public void initialize(int queueDimension, boolean performTimestampFunction) {
		this.configuration = Configuration.getCurrentConfiguration();
		this.queries = new ArrayList<CSparqlQuery>();
		this.streams = new HashMap<String, RdfStream>();
		this.snapshots = new HashMap<CSparqlQuery, RdfSnapshot>();
		this.results = new HashMap<CSparqlQuery, CsparqlQueryResultProxy>();
		this.sparqlEngine = this.configuration.createSparqlEngine();
		this.cepEngine = this.configuration.createCepEngine();
		this.reasoner = this.configuration.createReasoner();
		this.cepEngine.initialize();
		this.sparqlEngine.initialize();
		this.setPerformTimestampFunctionVariable(performTimestampFunction);
		this.setUpInjecter(queueDimension);
	}

	public void setPerformTimestampFunctionVariable(boolean value) {
		if (sparqlEngine.getEngineType().equals("jena")) {
			JenaEngine je = (JenaEngine) sparqlEngine;
			je.setPerformTimestampFunctionVariable(value);
		}
	}

	public void setUpInjecter(int queueDimension) {
		if (cepEngine.getCepEngineType().equals("esper")) {
			EsperEngine ee = (EsperEngine) cepEngine;
			ee.setUpInjecter(queueDimension);
		}
	}

	// @Override
	// public void activateInference() {
	// sparqlEngine.activateInference();
	// }
	//
	// @Override
	// public void activateInference(String rulesFile, String
	// entailmentRegimeType) {
	// sparqlEngine.activateInference(rulesFile, entailmentRegimeType);
	// }
	//
	// @Override
	// public void activateInference(String rulesFile, String
	// entailmentRegimeType, String tBoxFile) {
	// sparqlEngine.activateInference(rulesFile, entailmentRegimeType,
	// tBoxFile);
	// }

	@Override
	public boolean getInferenceStatus() {
		return sparqlEngine.getInferenceStatus();
	}

	@Override
	public void arrestInference(String queryId) {
		try {
			sparqlEngine.arrestInference(queryId);
		} catch (ReasonerException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void restartInference(String queryId) {
		try {
			sparqlEngine.restartInference(queryId);
		} catch (ReasonerException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void updateReasoner(String queryId) {
		sparqlEngine.updateReasoner(queryId);
	}

	@Override
	public void updateReasoner(String queryId, String rulesFile, ReasonerChainingType chainingType) {
		sparqlEngine.updateReasoner(queryId, rulesFile, chainingType);
	}

	@Override
	public void updateReasoner(String queryId, String rulesFile, ReasonerChainingType chainingType, String tBoxFile) {
		sparqlEngine.updateReasoner(queryId, rulesFile, chainingType, tBoxFile);
	}

	@Override
	public void execUpdateQueryOverDatasource(String queryBody) {
		sparqlEngine.execUpdateQueryOverDatasource(queryBody);
	}

	@Override
	public RDFTable evaluateGeneralQueryOverDatasource(String queryBody) {
		return sparqlEngine.evaluateGeneralQueryOverDatasource(queryBody);
	}

	@Override
	public void putStaticNamedModel(String iri, String serialization) {
		sparqlEngine.putStaticNamedModel(iri, serialization);
	}

	@Override
	public void removeStaticNamedModel(String iri) {
		sparqlEngine.removeStaticNamedModel(iri);
	}

	protected CSparqlQuery getQueryByID(final String id) {
		for (final CSparqlQuery q : this.queries) {
			if (q.getId().equalsIgnoreCase(id)) {
				return q;
			}
		}

		return null;
	}

	public RdfStream registerStream(final RdfStream s) {
		this.streams.put(s.getIRI(), s);
		this.cepEngine.registerStream(s);
		return s;
	}

	public void unregisterDataProvider(final RdfStream provider) {
		this.streams.remove(provider);
	}

	private void unregisterAllQueries() {

		for (final CSparqlQuery q : this.queries) {
			this.unregisterQuery(q.getId());
		}
	}

	public void startQuery(final String id) {
		this.cepEngine.startQuery(id);
	}

	public void stopQuery(final String id) {

		this.cepEngine.stopQuery(id);
	}

	private void unregisterQuery(final CSparqlQuery q) {

		this.stopQuery(q.getId());

		if (q != null) {
			this.queries.remove(q);
		}
	}

	public void unregisterQuery(final String id) {
		final CSparqlQuery q = this.getQueryByID(id);
		this.unregisterQuery(q);
	}

	public void unregisterStream(final String iri) {

		final RdfStream r = this.getStreamByIri(iri);

		if (r == null) {
			return;
		}

		this.streams.remove(iri);
	}

	@Override
	public CsparqlQueryResultProxy registerQuery(String command, boolean activateInference) throws ParseException {

		final Translator t = Configuration.getCurrentConfiguration().createTranslator(this);

		CSparqlQuery query = null;

		// Split continuous part from static part
		try {
			query = t.translate(command);
		} catch (final TranslationException e) {
			throw new ParseException(e.getMessage(), 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.debug("CEP query: {}", query.getCepQuery().getQueryCommand());
		logger.debug("SPARQL query: {}", query.getSparqlQuery().getQueryCommand().replace("\n", "").replace("\r", ""));

		// Parse sparql(static) query
		sparqlEngine.parseSparqlQuery(query.getSparqlQuery());

		final RdfSnapshot s = this.cepEngine.registerQuery(query.getCepQuery().getQueryCommand(), query.getId());

		final CsparqlQueryResultProxy result = new CsparqlQueryResultProxy(query.getId());
		result.setSparqlQueryId(query.getSparqlQuery().getId());
		result.setCepQueryId(query.getCepQuery().getId());
		result.setName(query.getName());

		this.queries.add(query);
		this.snapshots.put(query, s);
		this.results.put(query, result);

		s.addObserver(this);

		if (activateInference) {
			logger.debug("RDFS reasoner");
			Resource config = ModelFactory.createDefaultModel().createResource().addProperty(ReasonerVocabulary.PROPsetRDFSLevel, "simple");
			com.hp.hpl.jena.reasoner.Reasoner reasoner = RDFSRuleReasonerFactory.theInstance().create(config);
			sparqlEngine.addReasonerToReasonerMap(query.getSparqlQuery().getId(), reasoner);
		}

		return result;
	}

	public CsparqlQueryResultProxy registerQuery(String command, boolean activateInference, boolean needQuery) throws ParseException {

		final Translator t = Configuration.getCurrentConfiguration().createTranslator(this);

		CSparqlQuery query = null;

		this.needQuery = needQuery;

		// Split continuous part from static part
		try {
			query = t.translate(command);
		} catch (final TranslationException e) {
			throw new ParseException(e.getMessage(), 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.debug("CEP query: {}", query.getCepQuery().getQueryCommand());
		logger.debug("SPARQL query: {}", query.getSparqlQuery().getQueryCommand().replace("\n", "").replace("\r", ""));
//		System.out.println(query.getSparqlQuery().getQueryCommand());
		// Parse sparql(static) query
		sparqlEngine.parseSparqlQuery(query.getSparqlQuery());

		final RdfSnapshot s = this.cepEngine.registerQuery(query.getCepQuery().getQueryCommand(), query.getId());

		final CsparqlQueryResultProxy result = new CsparqlQueryResultProxy(query.getId());
		result.setSparqlQueryId(query.getSparqlQuery().getId());
		result.setCepQueryId(query.getCepQuery().getId());
		result.setName(query.getName());

		this.queries.add(query);
		this.snapshots.put(query, s);
		this.results.put(query, result);

		s.addObserver(this);

		if (activateInference) {
			logger.debug("RDFS reasoner");
			Resource config = ModelFactory.createDefaultModel().createResource().addProperty(ReasonerVocabulary.PROPsetRDFSLevel, "simple");
			com.hp.hpl.jena.reasoner.Reasoner reasoner = RDFSRuleReasonerFactory.theInstance().create(config);
			sparqlEngine.addReasonerToReasonerMap(query.getSparqlQuery().getId(), reasoner);
		}

		return result;
	}

    @Override
    public CsparqlQueryResultProxy registerQuery(String command, boolean activateInference, String tBoxFileSerialization) throws ParseException {

        final Translator t = Configuration.getCurrentConfiguration().createTranslator(this);

        CSparqlQuery query = null;

        // Split continuous part from static part
        try {
            query = t.translate(command);
        } catch (final TranslationException e) {
            throw new ParseException(e.getMessage(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.debug("CEP query: {}", query.getCepQuery().getQueryCommand());
        logger.debug("SPARQL query: {}", query.getSparqlQuery().getQueryCommand().replace("\n", "").replace("\r", ""));

        // Parse sparql(static) query
        sparqlEngine.parseSparqlQuery(query.getSparqlQuery());

        final RdfSnapshot s = this.cepEngine.registerQuery(query.getCepQuery().getQueryCommand(), query.getId());

        final CsparqlQueryResultProxy result = new CsparqlQueryResultProxy(query.getId());
        result.setSparqlQueryId(query.getSparqlQuery().getId());
        result.setCepQueryId(query.getCepQuery().getId());
        result.setName(query.getName());

        this.queries.add(query);
        this.snapshots.put(query, s);
        this.results.put(query, result);

        s.addObserver(this);

        if (activateInference) {
            logger.debug("RDFS reasoner");
            Resource config = ModelFactory.createDefaultModel().createResource().addProperty(ReasonerVocabulary.PROPsetRDFSLevel, "simple");
            com.hp.hpl.jena.reasoner.Reasoner reasoner = RDFSRuleReasonerFactory.theInstance().create(config);
            sparqlEngine.addReasonerToReasonerMap(query.getSparqlQuery().getId(), reasoner);
        }

        return result;
    }

	@Override
	public CsparqlQueryResultProxy registerQuery(String command, boolean activateInference, String rulesFileSerialization, ReasonerChainingType chainingType) throws ParseException {

		final Translator t = Configuration.getCurrentConfiguration().createTranslator(this);

		CSparqlQuery query = null;

		// Split continuous part from static part
		try {
			query = t.translate(command);
		} catch (final TranslationException e) {
			throw new ParseException(e.getMessage(), 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.debug("CEP query: {}", query.getCepQuery().getQueryCommand());
		logger.debug("SPARQL query: {}", query.getSparqlQuery().getQueryCommand().replace("\n", "").replace("\r", ""));

		// Parse sparql(static) query
		sparqlEngine.parseSparqlQuery(query.getSparqlQuery());

		final RdfSnapshot s = this.cepEngine.registerQuery(query.getCepQuery().getQueryCommand(), query.getId());

		final CsparqlQueryResultProxy result = new CsparqlQueryResultProxy(query.getId());
		result.setSparqlQueryId(query.getSparqlQuery().getId());
		result.setCepQueryId(query.getCepQuery().getId());
		result.setName(query.getName());


		this.queries.add(query);
		this.snapshots.put(query, s);
		this.results.put(query, result);

		s.addObserver(this);

		if (activateInference) {
			logger.debug("Generic Rule Engine");
			com.hp.hpl.jena.reasoner.Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(Rule.rulesParserFromReader(new BufferedReader(new StringReader(rulesFileSerialization)))));
			switch (chainingType) {
			case BACKWARD:
				reasoner.setParameter(ReasonerVocabulary.PROPruleMode, "backward");
				break;
			case FORWARD:
				reasoner.setParameter(ReasonerVocabulary.PROPruleMode, "forward");
				break;
			case HYBRID:
				reasoner.setParameter(ReasonerVocabulary.PROPruleMode, "hybrid");
				break;
			default:
				reasoner.setParameter(ReasonerVocabulary.PROPruleMode, "forward");
				break;
			}
			sparqlEngine.addReasonerToReasonerMap(query.getSparqlQuery().getId(), reasoner);
		}

		return result;

	}

	@Override
	public CsparqlQueryResultProxy registerQuery(String command, boolean activateInference, String rulesFileSerialization, ReasonerChainingType chainingType, String tBoxFileSerialization)
			throws ParseException {
		final Translator t = Configuration.getCurrentConfiguration().createTranslator(this);

		CSparqlQuery query = null;

		// Split continuous part from static part
		try {
			query = t.translate(command);
		} catch (final TranslationException e) {
			throw new ParseException(e.getMessage(), 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.debug("CEP query: {}", query.getCepQuery().getQueryCommand());
		logger.debug("SPARQL query: {}", query.getSparqlQuery().getQueryCommand().replace("\n", "").replace("\r", ""));

		// Parse sparql(static) query
		sparqlEngine.parseSparqlQuery(query.getSparqlQuery());

		final RdfSnapshot s = this.cepEngine.registerQuery(query.getCepQuery().getQueryCommand(), query.getId());

		final CsparqlQueryResultProxy result = new CsparqlQueryResultProxy(query.getId());
		result.setSparqlQueryId(query.getSparqlQuery().getId());
		result.setCepQueryId(query.getCepQuery().getId());
		result.setName(query.getName());

		this.queries.add(query);
		this.snapshots.put(query, s);
		this.results.put(query, result);

		s.addObserver(this);

		if (activateInference) {
			logger.debug("Generic Rule Engine");
			com.hp.hpl.jena.reasoner.Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(Rule.rulesParserFromReader(new BufferedReader(new StringReader(rulesFileSerialization)))));
			switch (chainingType) {
			case BACKWARD:
				reasoner.setParameter(ReasonerVocabulary.PROPruleMode, "backward");
				break;
			case FORWARD:
				reasoner.setParameter(ReasonerVocabulary.PROPruleMode, "forward");
				break;
			case HYBRID:
				reasoner.setParameter(ReasonerVocabulary.PROPruleMode, "hybrid");
				break;
			default:
				reasoner.setParameter(ReasonerVocabulary.PROPruleMode, "forward");
				break;
			}
			try {
				reasoner = reasoner.bindSchema(ModelFactory.createDefaultModel().read(new StringReader(tBoxFileSerialization), null, "RDF/XML"));
			} catch (Exception e) {
				try {
					reasoner = reasoner.bindSchema(ModelFactory.createDefaultModel().read(new StringReader(tBoxFileSerialization), null, "N-TRIPLE"));
				} catch (Exception e1) {
					try {
						reasoner = reasoner.bindSchema(ModelFactory.createDefaultModel().read(new StringReader(tBoxFileSerialization), null, "TURTLE"));
					} catch (Exception e2) {
						try {
							reasoner = reasoner.bindSchema(ModelFactory.createDefaultModel().read(new StringReader(tBoxFileSerialization), null, "RDF/JSON"));
						} catch (Exception e3) {
							logger.error(e.getMessage(), e3);
						}
					}
				}
			}
			sparqlEngine.addReasonerToReasonerMap(query.getSparqlQuery().getId(), reasoner);
		}

		return result;
	}

	public void destroy() {

		this.unregisterAllQueries();
		this.cepEngine.destroy();
	}

	// Snapshot received
	// public void update(final GenericObservable<List<RdfQuadruple>> observed,
	// final List<RdfQuadruple> quads) {
	//
	// long starttime = System.nanoTime();
	//
	// final RdfSnapshot r = (RdfSnapshot) observed;
	//
	// final CSparqlQuery csparqlquery = this.getQueryByID(r.getId());
	//
	// final RdfSnapshot augmentedSnapshot = this.reasoner.augment(r);
	//
	// this.snapshots.put(csparqlquery, augmentedSnapshot);
	//
	// this.sparqlEngine.clean();
	//
	// long count = 0;
	//
	// for (final RdfQuadruple q : quads) {
	// if (isStreamUsedInQuery(csparqlquery, q.getStreamName()))
	// {
	// this.sparqlEngine.addStatement(q.getSubject(), q.getPredicate(),
	// q.getObject(), q.getTimestamp());
	// count++;
	// }
	// }
	//
	// if (count == 0)
	// return;
	//
	// final RDFTable result =
	// this.sparqlEngine.evaluateQuery(csparqlquery.getSparqlQuery());
	//
	// timestamp(result, csparqlquery);
	//
	// logger.info("results obtained in "+ (System.nanoTime()-starttime) +
	// " nanoseconds");
	//
	// this.notifySubscribers(csparqlquery, result);
	//
	//
	// }

	// private void timestamp(RDFTable r, CSparqlQuery q) {
	// if (q.getQueryCommand().toLowerCase().contains("register stream"))
	// r.add("timestamp", "0");
	// //TODO: da aggiungere il campo on the fly
	//
	// }

	private boolean isStreamUsedInQuery(CSparqlQuery csparqlquery, String streamName) {
		for (StreamInfo si : csparqlquery.getStreams()) {
			if (si.getIri().equalsIgnoreCase(streamName))
				return true;
		}

		return false;
	}

	private void notifySubscribers(final CSparqlQuery csparqlquery, final RDFTable result) {

		final CsparqlQueryResultProxy res = this.results.get(csparqlquery);

		res.notify(result);
	}

	public RdfStream getStreamByIri(final String iri) {

		if (this.streams.containsKey(iri)) {
			return this.streams.get(iri);
		}

		return null;
	}

	public void registerIndex(ArrayList<Index> indexList) {
		this.indexList = indexList;
		this.isIndex = true;
	}
	public void registerDbIndex(boolean isDbIndex) {
		this.isDbIndex = isDbIndex;
	}

	public void registerCount(int count) {
		this.count = count;
		this.countStart = count;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void update(Observable o, Object arg) {
		System.out.println("执行查询次数: " + (++queryNum));
		final RdfSnapshot r = (RdfSnapshot) o;
		List<RdfQuadruple> quads = (List<RdfQuadruple>) arg;

		logger.debug("current time: {}", this.cepEngine.getCurrentTime());
//		for (final RdfQuadruple q : quads) {
//			//logger.debug(q.getSubject() + "\t" + q.getPredicate() + "\t" + q.getObject() + "\t" + (q.getTimestamp()));
//			logger.debug(q.getTimestamp()+"");
//		}

		final CSparqlQuery csparqlquery = this.getQueryByID(r.getId());

		final RdfSnapshot augmentedSnapshot = this.reasoner.augment(r);

		this.snapshots.put(csparqlquery, augmentedSnapshot);

		this.sparqlEngine.clean();

		long count = 0;
		long end;
		System.out.println("triples --- 输入的 rdf 数量: " + quads.size());
		if (needQuery) {
			long start = System.currentTimeMillis();
			List<RDFTriple> triples = new ArrayList<>();
			for (RdfQuadruple t: quads) {
				triples.add(new RDFTriple(t.getSubject(), t.getPredicate(), t.getObject()));
			}
//			printList(triples, "triples");
			List<RDFTriple> r7_out = ForwardReasoner.r7_out(triples);
//			printList(r7_out, "r7_out");
			r7_out.addAll(triples);
//			printList(r7_out, "r7_out");
			List<RDFTriple> r23_out = ForwardReasoner.r23_out(r7_out);
//			printList(r23_out, "r23_out");
			List<RDFTriple> tp = new ArrayList<>();
			for (RDFTriple t: triples) {
				if (t.predicate.equals(Rules.RDFS_TYPE)) {
					tp.add(t);
				}
			}
			r23_out.addAll(tp);
			List<RDFTriple> r9_out = ForwardReasoner.r9_out(r23_out);

			List<RDFTriple> r123_out = ForwardReasoner.r123_out(r7_out);
			r23_out.addAll(r9_out);
//			r23_out.addAll(r9_out);
			r23_out.addAll(r7_out);
			r23_out.addAll(triples);
			r23_out.addAll(r123_out);
			Set<RDFTriple> tripleSet = new HashSet<>(r23_out);
//			System.out.println("推理Triple: " + tripleSet.size());
			num1 += tripleSet.size();
			end = System.currentTimeMillis();
			System.out.println("推理时间: " + (end - start));
			if (this.isDbIndex) {
				System.out.println("索引 triple 数量: " + r23_out.size());
				// 建立索引
				String path = preIndex + index.get();
				String newPath = preIndexnew + index.get();
				index.incrementAndGet();
//				if (index.get() == 100) index.set(0);
				BigLoader.loadStream(r23_out, path, newPath);
				// 开始查询
				BigQueryTests.query(path, newPath);
				end = System.currentTimeMillis();
				System.out.println("推理+查询: " + (end - start));
			} else {
				if (this.isIndex) {
					long indexStart = System.currentTimeMillis();
					Set<RDFTriple> input = Index.getRelaRDF(tripleSet, this.indexList);
//					System.out.println("常量索引: " + input.size());
					num2 += input.size();
//					end = System.currentTimeMillis();
//					System.out.println("索引时间: " + (end - indexStart));
//					System.out.println("索引 + 推理时间: " + (end - start));
//					for (RDFTriple t: input) {
////						input.add(t);
//						this.sparqlEngine.addStatement(t.subject, t.predicate, t.object);
//						count++;
//					}


					List<RDFTriple> out = Index.getListSet(input, this.indexList);
					end = System.currentTimeMillis();
					System.out.println("索引时间: " + (end - indexStart));
					num3 += out.size();
					this.countStart--;
					if (this.countStart == 0) {
						System.out.println("ave 推理结果: " + (num1 / this.count));
						System.out.println("ave 常量索引: " + (num2 / this.count));
						System.out.println("ave 变量索引: " + (num3 / this.count));
						this.countStart = this.count;
					}
					System.out.println("索引 + 推理时间: " + (end - start));
					for (RDFTriple t: out) {
//						input.add(t);
						this.sparqlEngine.addStatement(t.subject, t.predicate, t.object);
						count++;
					}
				} else {
					end = System.currentTimeMillis();
					System.out.println("无索引 + 推理时间: " + (end - start));
					for (RDFTriple t: tripleSet) {
						this.sparqlEngine.addStatement(t.subject, t.predicate, t.object);
						count++;
					}
				}

			}
//			for (final RdfQuadruple q : quads) {
//				if (isStreamUsedInQuery(csparqlquery, q.getStreamName())) {
//					this.sparqlEngine.addStatement(q.getSubject(), q.getPredicate(), q.getObject(), q.getTimestamp());
////				this.sparqlEngine.addStatement(q.getSubject(), q.getPredicate(), q.getObject());
////					 将消息放入 kafka
////				Producer.sendMessages(Consumer.TO_TOPIC, String.valueOf(1), new com.triple.common.RDFTriple(q.getSubject(), q.getPredicate(), q.getObject()), "127.0.0.1:9092");
////				logger.info(q.toString());
//					count++;
//				}
//			}
//		for (com.triple.common.RDFTriple rdfTriple : SchemeTriple.getSchemeList) {
//			Producer.sendMessages("MsgRDFTriple", String.valueOf("2"), rdfTriple, "127.0.0.1:9092");
//			count++;
//		}
			if (count == 0)
				return;
			System.out.println("index --- 输入的 rdf 数量: " + count);
//			transitiveRedis(quads);
			final RDFTable result = this.sparqlEngine.evaluateQuery(csparqlquery.getSparqlQuery());

//			 timestamp(result, csparqlquery);

//			 logger.info("results obtained in "+ (System.nanoTime()-starttime) +
//			 " nanoseconds");

			this.notifySubscribers(csparqlquery, result);
		} else {
			for (final RdfQuadruple q : quads) {
				if (isStreamUsedInQuery(csparqlquery, q.getStreamName())) {
//					this.sparqlEngine.addStatement(q.getSubject(), q.getPredicate(), q.getObject(), q.getTimestamp());
					this.sparqlEngine.addStatement(q.getSubject(), q.getPredicate(), q.getObject());
					// 将消息放入 kafka
//				Producer.sendMessagesTriple(Consumer.TO_TOPIC, String.valueOf(1), new com.triple.common.RDFTriple(q.getSubject(), q.getPredicate(), q.getObject()), "127.0.0.1:9092");
//				logger.info(q.toString());
					count++;
				}
			}
			if (count == 0)
				return;
			System.out.println("csparql --- 输入的 rdf 数量: " + count);
			final RDFTable result = this.sparqlEngine.evaluateQuery(csparqlquery.getSparqlQuery());

			// timestamp(result, csparqlquery);

			// logger.info("results obtained in "+ (System.nanoTime()-starttime) +
			// " nanoseconds");

			this.notifySubscribers(csparqlquery, result);
		}
	}

	public void transitiveRedis(List<RdfQuadruple> quads) {
		long startTS = System.currentTimeMillis();
		// 定义图的索引
		Set<String> keys = new HashSet<>();
		String graph = "friendOf_";
		for (RdfQuadruple q: quads) {
//			System.out.println("****************");
//			System.out.println("K => " + q.getSubject() + "  V => " + jedis.get(q.getSubject()));
//			System.out.println("K => " + q.getObject() + "  V => " + jedis.get(q.getObject()));
			String sIndex = jedis.get(q.getSubject());
			String oIndex = jedis.get(q.getObject());
			if (sIndex == null && oIndex == null) {
				jedis.set(q.getSubject(), graph + gIndex.get());
				jedis.set(q.getObject(), graph + gIndex.get());
				jedis.sadd(graph + gIndex.get(), q.getSubject());
				jedis.sadd(graph + gIndex.get(), q.getObject());
				keys.add(graph + gIndex.get());
				gIndex.incrementAndGet();
			} else {
				if (sIndex == null) {
					jedis.set(q.getSubject(), oIndex);
					jedis.sadd(oIndex, q.getSubject());
				} else if (oIndex == null) {
					jedis.set(q.getObject(), sIndex);
					jedis.sadd(sIndex, q.getObject());
				} else if (!oIndex.equals(sIndex)) {
					// 获取最小图 index
					// 将图中的所有元素全部设置为 currG
					Set<String> sElements = jedis.smembers(sIndex);
					Set<String> oElements = jedis.smembers(oIndex);
					if (sElements.size() < oElements.size()) {
						for (String element: sElements) {
							jedis.set(element, oIndex);
							jedis.sadd(oIndex, element);
//                            jedis.smove(sIndex, oIndex, element);
						}
						keys.remove(sIndex);
					} else {
						for (String element: oElements) {
							jedis.set(element, sIndex);
							jedis.sadd(sIndex, element);
//                            jedis.smove(oIndex, sIndex, element);
						}
						keys.remove(oIndex);
					}
				}
			}
		}
		int count = 0;
		System.out.println(keys);
		for (String key: keys) {
			Set<String> elements = jedis.smembers(key);
			for (String element: elements) {
				for (String element1: elements) {
					RDFTriple rdfTriple = new RDFTriple(element, Rules.RDFS_FRIENDOF, element1);
//					this.sparqlEngine.addStatement(rdfTriple.getSubject(), rdfTriple.getPredicate(), rdfTriple.getObject());
//					Producer.sendMessagesTriple(Consumer.BACK_TOPIC, String.valueOf(1), rdfTriple, Consumer.BROKER_LIST);
					count++;
				}

			}
		}
		jedis.flushAll();
		long endTS = System.currentTimeMillis();
		System.out.println("Execution Time : " + (endTS - startTS));
		System.out.println("放入 : " + count);
	}

	public static <T> void printList(List<T> triples, String var) {
		System.out.println("*******   打印 " + var + "   *******");
		for (T t: triples) {
			System.out.println(t.toString());
		}
	}
}
