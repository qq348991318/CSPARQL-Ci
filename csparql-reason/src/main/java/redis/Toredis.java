package redis;

import com.alibaba.fastjson.JSON;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.triple.common.RDFTriple;
import redis.clients.jedis.Jedis;

import java.io.InputStream;

/**
 * @author 杨帆玉
 * @date 2019/11/11 3:15 下午
 */
public class Toredis {
    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost");
        System.out.println("服务正在运行: " + jedis.ping());
        long start  = System.currentTimeMillis();
        String inputFileName = "/Users/yangfanyu/IdeaProjects/uba1.7/University0-49-clean2.nt";

        Model model = ModelFactory.createDefaultModel();

        InputStream in = FileManager.get().open(inputFileName);
        if (in == null) throw new IllegalArgumentException("File: " + inputFileName + " not found");

        model.read(in, "", "N3");
        long end = System.currentTimeMillis();
        System.out.println("开始 : 文件解析时间为: " + (end - start));
        int id = 1;

        StmtIterator iter = model.listStatements();
        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement(); // get next statement
            RDFTriple rdfTriple = new RDFTriple();
            //RDFNode object = stmt.getObject(); // get the object
            String subject = stmt.getSubject().toString(); // get the subject
            rdfTriple.setSubject(subject);
            String predicate = stmt.getPredicate().toString(); // get the predicate
            rdfTriple.setPredicate(predicate);
            String object = stmt.getObject().toString(); // get the object
            rdfTriple.setObject(object);
            jedis.set(String.valueOf(id), JSON.toJSONString(rdfTriple));
            id++;
        }
    }
}
