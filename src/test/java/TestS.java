import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.junit.jupiter.api.Test;

public class TestS {

    @Test
    public void test1(){
        String index = "src/main/resources/stemlemma";
        String stemm = "src/main/resources/stem";
        String lemm = "src/main/resources/lemma";
        String neither = "src/main/resources/index";

        try{
            String queryProcess = new QueryProcess(index).evalQuery();
            String queryProcess1 = new QueryProcess(new BooleanSimilarity(),index).evalQuery();
            String queryProcess3 = new QueryProcess(new LMJelinekMercerSimilarity((float)0.5),index).evalQuery();
            System.out.println("--------With Stemming and Lemmatization----------");
            System.out.println("BM25: "+queryProcess);
            System.out.println("Boolean: "+queryProcess1);
            System.out.println("Jelinick: "+queryProcess3);

//            String queryProcess4 = new QueryProcess(stemm).evalQuery();
//            String queryProcess5 = new QueryProcess(new BooleanSimilarity(),stemm).evalQuery();
//            String queryProcess6 = new QueryProcess(new LMJelinekMercerSimilarity((float)0.5),stemm).evalQuery();
//            System.out.println("--------With Stemming ----------");
//            System.out.println("BM25: "+queryProcess4);
//            System.out.println("Boolean: "+queryProcess5);
//            System.out.println("Jelinick: "+queryProcess6);
//
//            String queryProcess7 = new QueryProcess(lemm).evalQuery();
//            String queryProcess8 = new QueryProcess(new BooleanSimilarity(),lemm).evalQuery();
//            String queryProcess9 = new QueryProcess(new LMJelinekMercerSimilarity((float)0.5),lemm).evalQuery();
//            System.out.println("--------With Lemmatization----------");
//            System.out.println("BM25: "+queryProcess7);
//            System.out.println("Boolean: "+queryProcess8);
//            System.out.println("Jelinick: "+queryProcess9);
//
//            String queryProcess10 = new QueryProcess(neither).evalQuery();
//            String queryProcess11 = new QueryProcess(new BooleanSimilarity(),neither).evalQuery();
//            String queryProcess12 = new QueryProcess(new LMJelinekMercerSimilarity((float)0.5),neither).evalQuery();
//            System.out.println("--------Without Stemming and Lemmatization----------");
//            System.out.println("BM25: "+queryProcess10);
//            System.out.println("Boolean: "+queryProcess11);
//            System.out.println("Jelinick: "+queryProcess12);
        }
        catch (Exception e){
            e.printStackTrace();
        };
    }
}
