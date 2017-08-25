package search.common.word2vec.dl4j;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.InMemoryLookupCache;
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import search.common.util.JavaLogging;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by soledede.weng on 2016/7/29.
 */
public class Word2VecUtils {
    private static Logger logger = LoggerFactory.getLogger(Word2VecUtils.class);

    private static String CHARSET = "UTF-8";
    private static int MIN_WORD_FREQUENCY = 5;
    private static float LEARNING_RATE = 0.025f;
    private static int LAYER_SIZE = 100;
    private static int SEED = 42;
    private static int WINDOW_SIZE = 2;
    //private static Dictionary DICTIONARY = null;

    public static Word2Vec fit(String filePath, Memory memory) throws IOException {
        SentenceFactory spliter = new TextSentenceFactory(filePath, CHARSET);
        return fit(spliter.create(), memory);
    }

    public static Word2Vec fit(Collection<String> sentences, Memory memory) {

        if (CollectionUtils.isEmpty(sentences))
            return null;
        SentenceIterator iterator = new CollectionSentenceIterator(sentences);
        // TokenizerFactory tokenizerFactory = new ANSJTokenizerFactory(DICTIONARY);
        TokenizerFactory tokenizerFactory = new ANSJTokenizerFactory();
        tokenizerFactory.setTokenPreProcessor(new ChineseTokenPreProcess());

        return fit(iterator, tokenizerFactory, memory);
    }

    private static Word2Vec fit(SentenceIterator iterator, TokenizerFactory tokenizerFactory, Memory memory) {

        logger.info("Building model....");
        InMemoryLookupCache cache = new InMemoryLookupCache();
        WeightLookupTable<VocabWord> table = new InMemoryLookupTable.Builder<VocabWord>()
                .vectorLength(100)
                .useAdaGrad(false)
                .cache(cache)
                .lr(LEARNING_RATE)
                .build();

        Word2Vec vec = new Word2Vec.Builder().minWordFrequency(MIN_WORD_FREQUENCY)
                .iterations(1)
                .epochs(1)
                .layerSize(LAYER_SIZE)
                .seed(SEED)
                .windowSize(WINDOW_SIZE)
                .iterate(iterator)
                .tokenizerFactory(tokenizerFactory)
                .lookupTable(table)
                .vocabCache(cache)
                .build();

        logger.info("Fitting Word2Vec model....");
        vec.fit();

        if (memory != null) {
            WordVectorSerializer.writeFullModel(vec, memory.getPath());
            logger.info("The training has completed successfully and the result has been saved to Path[{}]",
                    memory.getPath());
        }

        return vec;
    }

    public static Word2Vec load(@NonNull Memory memory) throws FileNotFoundException {

        Word2Vec vec = WordVectorSerializer.loadFullModel(memory.getPath());
        return vec;
    }

    public static class Config {

        private int minWordFrequency = 0;
        private String charset = null;
        private float learningRate = 0;
        private int layerSize = 0;
        private int seed = 0;
        private int windowSize = 0;
       // private Dictionary dictionary = null;

        public Config minWordFrequency(int minWordFrequency) {
            this.minWordFrequency = minWordFrequency;
            return this;
        }

        public Config charset(String charset) {
            this.charset = charset;
            return this;
        }

        public Config learningRate(float learningRate) {
            this.learningRate = learningRate;
            return this;
        }

        public Config layerSize(int layerSize) {
            this.layerSize = layerSize;
            return this;
        }

        public Config seed(int seed) {
            this.seed = seed;
            return this;
        }

        public Config windowSize(int windowSize) {
            this.windowSize = windowSize;
            return this;
        }

      /*  public Config dictionary(Dictionary dictionary) {
            this.dictionary = dictionary;
            return this;
        }*/

        public void apply() {
            if (minWordFrequency > 0)
                MIN_WORD_FREQUENCY = minWordFrequency;
            if (charset != null)
                CHARSET = charset;
            if (learningRate > 0)
                LEARNING_RATE = learningRate;
            if (layerSize > 0)
                LAYER_SIZE = layerSize;
            if (seed > 0)
                SEED = seed;
            if (windowSize > 0)
                WINDOW_SIZE = windowSize;
           /* if (dictionary != null)
                DICTIONARY = dictionary;*/
        }
    }
}
