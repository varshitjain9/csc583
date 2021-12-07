import org.apache.lucene.document.Document;

public class ResultClass {
    Document docName;
    double docScore;

    ResultClass(Document doc, double score){
        this.docName = doc;
        this.docScore = score;
    }
    ResultClass(){}
}
