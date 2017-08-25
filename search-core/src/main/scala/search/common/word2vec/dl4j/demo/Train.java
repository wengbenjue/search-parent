package search.common.word2vec.dl4j.demo;

import org.deeplearning4j.models.word2vec.Word2Vec;
import search.common.word2vec.dl4j.Memory;
import search.common.word2vec.dl4j.Word2VecUtils;

import java.io.IOException;

/**
 * Created by soledede.weng on 2016/7/29.
 */
public class Train {
    public static void main(String[] args) throws IOException {

        new Word2VecUtils.Config().charset("GB2312").apply();
        Memory memory = new Memory("D:\\test_memory.txt", Memory.Policy.INIT);
        Word2Vec word2Vec = Word2VecUtils.fit("D:\\word2venTest.txt", memory);
        System.out.println(word2Vec.wordsNearestSum("宇宙", 10));
    }
}
