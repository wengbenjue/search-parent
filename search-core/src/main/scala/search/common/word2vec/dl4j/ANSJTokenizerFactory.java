package search.common.word2vec.dl4j;

import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

import java.io.InputStream;

/**
 * Created by soledede.weng on 2016/7/29.
 */
public class ANSJTokenizerFactory implements TokenizerFactory {
    private TokenPreProcess tokenPreProcess;

    /* public ANSJTokenizerFactory(Dictionary dic) {
         if (dic != null)
             dic.expand();
     }*/
    public ANSJTokenizerFactory() {

    }



    @Override
    public Tokenizer create(String toTokenize) {
        Tokenizer t = new ANSJTokenizer(toTokenize);
        t.setTokenPreProcessor(tokenPreProcess);
        return t;
    }

    @Override
    public Tokenizer create(InputStream toTokenize) {
        throw new UnsupportedOperationException("Could not create Tokenizer with InputStream,Try with String");
    }

    @Override
    public void setTokenPreProcessor(TokenPreProcess preProcessor) {
        this.tokenPreProcess = preProcessor;
    }
}
