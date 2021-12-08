import edu.stanford.nlp.simple.Sentence;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.PorterStemmer;
import org.apache.lucene.search.QueryRescorer;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.*;


public class QueryProcess {

    String queryPath;
    String indexPath;
    private Optional<Similarity> s;

    QueryProcess(String indexPath){
        this.queryPath="src/main/resources/query";
        this.indexPath=indexPath;
        this.s=Optional.empty();
    }

    QueryProcess(Similarity similarity, String indexPath){
        this.s = Optional.of(similarity);
        this.queryPath="src/main/resources/query";
        this.indexPath=indexPath;
    }

    String evalQuery() throws IOException {
        File file = new File(queryPath+"/"+"questions.txt");
        Scanner scanner = new Scanner(file);
        int ttl =0, lineNo = 0, correct = 0, incorrecct = 0, lineNoCase = 0, reCorrect =0, reIncorrect = 0;
        String cat = "", query= "", ans="";
        while(scanner.hasNextLine()){
            String line = scanner.nextLine();
            lineNoCase = lineNo%4;
            switch (lineNoCase){
                case 0:
                    if(line.contains("(Alex:")) {
                        cat = line.substring(0, line.indexOf("(A")).trim();
                    }
                    else {
                        cat = line.trim();
                    }
                    break;
                case 1:
                    query = line.trim();
                    break;
                case 2:
                    ans = line.trim();
                    break;
                default:
                    List<ResultClass> res = runQuery(query+" "+cat, false);
                    if(ans.toLowerCase(Locale.ROOT).contains(res.get(0).docName.get("Title"))){
                        correct++;
                    }
                    else{
                        incorrecct++;
                    }
                    List<ResultClass> resRank = runQuery(query+" "+cat, true);
                    if(ans.toLowerCase(Locale.ROOT).contains(res.get(0).docName.get("Title"))){
                        reCorrect++;
                    }
                    else{
                        reIncorrect++;
                    }
                    ttl++;
            }
            lineNo++;
        }
        String res = "Correct: "+correct+" Incorrect: "+incorrecct;
        double div = (double)correct/ttl;
        String reRank = "Correct: "+reCorrect+"Incorrect: "+reIncorrect;
        double reDiv = (double)reCorrect/ttl;
        return "P@1: "+ correct+"/"+ttl+"="+div+"\nReranking Result\n"+"P@1: "+reCorrect+"/"+ttl+"="+reDiv;
    }

    public List<ResultClass> runQuery(String query, boolean rerank) throws IOException{
        List<ResultClass> ans = new ArrayList<ResultClass>();
        StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
        StringBuilder stem = new StringBuilder();
        StringBuilder b = new StringBuilder();
        PorterStemmer stemmer = new PorterStemmer();
        try {
            query= query.replaceAll("[^ a-zA-Z\\d]", " ").toLowerCase().trim();
            for (String lemma : new Sentence(query.toLowerCase()).lemmas()) {
                b.append(lemma).append(" ");
            }
            query = b.toString();
            for(String word: new Sentence(query.toLowerCase()).words()) {
                stemmer.setCurrent(word);
                stemmer.stem();
                stem.append(stemmer.getCurrent()+ " ");
            }
            query = stem.toString();
            Directory index = FSDirectory.open(new File(indexPath).toPath());
            Query query1 = new QueryParser("Content", standardAnalyzer).parse(QueryParser.escape(query));
            IndexReader indexReader = DirectoryReader.open(index);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            if (s.isPresent()){
                indexSearcher.setSimilarity(s.get());
            }
            int hitsperpage = 10;
            TopDocs docs = indexSearcher.search(query1, hitsperpage);
            ScoreDoc[] hits = docs.scoreDocs;
            if (rerank){
                ans = reRank(query, docs, indexSearcher);
            }
            else {
                for (int j = 0; j < hits.length; j++) {
                    ResultClass r = new ResultClass();
                    int docId = hits[j].doc;
                    Document d = indexSearcher.doc(docId);
                    r.docName = d;
                    r.docScore = hits[j].score;
                    ans.add(r);
                }
            }
            indexReader.close();
        }catch (Exception e){
            e.printStackTrace();
        };
        return ans;
    }

    public List<ResultClass> reRank(String queryExpr, TopDocs topDocs, IndexSearcher searcher) throws IOException {

        List<ResultClass> res = new ArrayList<>();
        String[] queryArr = queryExpr.split(" ");
        Map<Integer, List<String>> docMap = new HashMap<>();
        for(ScoreDoc scoreDoc:topDocs.scoreDocs){
            List<String> content = Arrays.asList(searcher.doc(scoreDoc.doc).get("Content").split(" "));
            docMap.put(scoreDoc.doc, content);
        }
        for(ScoreDoc scoreDoc : topDocs.scoreDocs){
            int docID = scoreDoc.doc;
            Document doc = searcher.doc(docID);
            List<String> tokens = docMap.get(scoreDoc.doc);
            double score = 1;
            for(String c : queryArr){
                int num = tokens.size();
                int tf = Collections.frequency(tokens, c);
                double total = 0, occurances = 0;
                for(int docid : docMap.keySet()){
                    occurances = findoccurence(docid,c,docMap);
                    total += docMap.get(docid).size();
                }
                double param = calculate(occurances, total, num, tf);
                score *= param;
            }
            res.add(new ResultClass(doc, score));
        }
        return res;
    }

    public int findoccurence(int docid , String c, Map<Integer, List<String>> docMap){
        int o=0;
        o += Collections.frequency(docMap.get(docid), c);
        return o;
    }

    public double calculate(double o, double t, int num, int tf){
        double PtMc = o / t;
        double param = (tf + 0.5 * PtMc) / (num + 0.5);
        return param;
    }
}