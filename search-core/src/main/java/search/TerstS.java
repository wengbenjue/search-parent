package search;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

/**
 * Created by soledede.weng on 2017-06-15.
 */
public class TerstS {
    protected static final StanfordCoreNLP annotatorPipeline = new StanfordCoreNLP("CoreNLP-chinese.properties");
    public static void main(String[] args) {

        Tree tree = parseAsTree("江泽明");
        System.out.println(tree);
    }




    public static Tree parseAsTree(String query) {
        Annotation annotation = annotatorPipeline.process(query);
        CoreMap sentence = annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0);
        return sentence.get(TreeCoreAnnotations.TreeAnnotation.class).firstChild();
    }

}
