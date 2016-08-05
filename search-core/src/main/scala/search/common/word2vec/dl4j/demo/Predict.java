package search.common.word2vec.dl4j.demo;

import org.deeplearning4j.models.word2vec.Word2Vec;
import search.common.word2vec.dl4j.Memory;
import search.common.word2vec.dl4j.Word2VecUtils;

import java.io.FileNotFoundException;

/**
 * Created by soledede.weng on 2016/7/29.
 */
public class Predict {
    public static void main(String[] args) throws FileNotFoundException {
        Word2Vec word2Vec = Word2VecUtils.load(new Memory("D:\\test_memory.txt", Memory.Policy.RESTORE));
        System.out.println(word2Vec.wordsNearestSum("工场", 10));
    }
}
