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

    boolean useLemma=true;
    boolean useStem =true;

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
            }
            else if (s1.indexOf("CATEGORIES:") == 0) {
                categories = s1.substring(12);
            }
            else if (s1.length() > 2 && s1.charAt(0) != '=' && s1.charAt(len - 1) != '=' && s1.charAt(0) != '#') {
                if (!s1.equals("See also") && !s1.equals("References") && !s1.equals("Further reading") && !s1.equals("External links")) {
                    material.append(s1 + " ");
                }
            }
        }
        addDoc(w, title, categories, material.toString());
    }

    public void addDoc(IndexWriter w, String title,String categories, String content) throws IOException{
        Document doc = new Document();
        if(content.equals("")) {
            content = ".";
        }
        if(categories.equals("")) {
            categories = ".";
        }
        if(useLemma) {
            StringBuilder b = new StringBuilder("");
            if (!categories.isEmpty()) {
                for (String lemma : new Sentence(categories.toLowerCase()).lemmas()) {
                    b.append(lemma).append(" ");
                }
                categories = b.toString();
            }
            StringBuilder b1 = new StringBuilder("");
            if (!content.isEmpty()) {
                for (String lemma : new Sentence(content.toLowerCase()).lemmas()) {
                    b1.append(lemma).append(" ");
                }
                content = b1.toString();
            }
        }
        if(useStem){
            PorterStemmer stemmer = new PorterStemmer();
            StringBuilder a2 = new StringBuilder("");
            for(String word: new Sentence(categories.toLowerCase()).words()) {
                stemmer.setCurrent(word);
                stemmer.stem();
                a2.append(stemmer.getCurrent() + " ");
            }
            categories= a2.toString();
            StringBuilder a3 = new StringBuilder("");
            for(String word: new Sentence(categories.toLowerCase()).words()) {
                stemmer.setCurrent(word);
                stemmer.stem();
                a3.append(stemmer.getCurrent() + " ");
            }
            categories= a3.toString();
        }
        doc.add(new StringField("Title", title, Field.Store.YES));
        doc.add(new TextField("categories", categories.trim(), Field.Store.YES));
        doc.add(new TextField("Content", content.trim(), Field.Store.YES));
        w.addDocument(doc);
    }

    public static void main(String[] args) throws IOException {
        IndexCreator indexCreator = new IndexCreator("src/main/resources/stemlemma");
//        indexCreator.parseFiles("src/main/resources/wiki-subset");
        QueryProcess queryProcess = new QueryProcess("src/main/resources/stemlemma");
        System.out.println(queryProcess.evalQuery());
    }
}