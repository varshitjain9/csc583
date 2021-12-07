import edu.stanford.nlp.simple.Sentence;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;

public class IndexCreator {
    private String indexPath;
    boolean useLemma=false;
    boolean useStem =false;
    public IndexCreator(String indexPath) {
        this.indexPath= indexPath;
    }

    public void parseFiles(String directory) throws java.io.FileNotFoundException,java.io.IOException {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = FSDirectory.open(new File(indexPath).toPath());
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);
        File dir = new File(directory);
        for(String file : dir.list()) {
            openFile(directory+"/"+file, w);
        }
        w.close();
        index.close();
    }

    public void openFile(String file, IndexWriter w)throws IOException {

        String categories="";
        StringBuilder material = new StringBuilder();
        String titles="";
        File f = new File(file);
        Scanner sc = new Scanner(f);
        String title="";
        StringBuilder ans = new StringBuilder();
        while(sc.hasNextLine()) {
            String s1 = sc.nextLine();
            int len = s1.length();
            if (s1.length() > 4 && s1.substring(0, 2).equals("[[") && s1.substring(len - 2, len).equals("]]")) {
                title = title.toLowerCase(Locale.ROOT);
                if (!title.contains("file:") && !title.contains("image:")) {
                    addDoc(w, title, categories, material.toString());
                }
                title = (s1.substring(s1.indexOf("[[") + 2, s1.indexOf("]]")));
                categories = "";
                material = new StringBuilder();
            } else if (s1.indexOf("CATEGORIES:") == 0) {
                categories = s1.substring(12);
            } else if (s1.length() > 2 && s1.charAt(0) != '=' && s1.charAt(len - 1) != '=' && s1.charAt(0) != '#') {
                if (!s1.equals("See also") && !s1.equals("References") && !s1.equals("Further reading") && !s1.equals("External links")) {
                    material.append(s1 + " ");
                }
            }
        }
        addDoc(w, title, categories, material.toString());
    }

    public void addDoc(IndexWriter w, String title,String categories, String content) throws IOException{
        Document doc = new Document();

        StringBuilder cat = new StringBuilder("");
        StringBuilder txt = new StringBuilder("");
        if(content.equals("")) {
            content = ".";
        }
        if(categories.equals("")) {
            categories = ".";
        }
        if(useLemma) {
            categories = convertLemma(cat, categories);
            content = convertLemma(txt, content);
        }
        if(useStem){
            categories = convertStem(cat, categories);
            content = convertStem(txt, content);
        }
        doc.add(new StringField("Title", title, Field.Store.YES));
        doc.add(new TextField("categories", categories.trim(), Field.Store.YES));
        doc.add(new TextField("Content", content.trim(), Field.Store.YES));
        w.addDocument(doc);
    }
    private String convertLemma(StringBuilder b, String s) {
        if (s.isEmpty()) {
            return s;
        }
        for (String lemma : new Sentence(s.toLowerCase()).lemmas()) {
            b.append(lemma).append(" ");
        }
        return b.toString();
    }

    private String convertStem(StringBuilder b, String s) {
        for(String word: new Sentence(s.toLowerCase()).words()) {
            b.append(getStem(word) + " ");
        }
        return b.toString();
    }

    private String getStem(String term) {
        PorterStemmer stemmer = new PorterStemmer();
        stemmer.setCurrent(term);
        stemmer.stem();
        return stemmer.getCurrent();
    }

    public static void main(String[] args) throws IOException {
        IndexCreator indexCreator = new IndexCreator("src/main/resources/index");
        //indexCreator.parseFiles("src/main/resources/wiki-subset");
        QueryProcess queryProcess = new QueryProcess("src/main/resources/stemlemma");
        System.out.println(queryProcess.evalQuery());
    }
}