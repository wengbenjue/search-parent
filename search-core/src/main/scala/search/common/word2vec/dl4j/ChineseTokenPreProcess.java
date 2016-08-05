package search.common.word2vec.dl4j;

import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;

/**
 * Created by soledede.weng on 2016/7/29.
 * 分词前预处理
 */
public class ChineseTokenPreProcess implements TokenPreProcess {
    @Override
    public String preProcess(String token) {
        if (token == null)
            return null;
        return token.replaceAll("[^\u4e00-\u9fa5\\w]+", " ");
    }
}
