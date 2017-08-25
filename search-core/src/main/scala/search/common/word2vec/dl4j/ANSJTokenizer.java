package search.common.word2vec.dl4j;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.apache.commons.lang.StringUtils;
import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by soledede.weng on 2016/7/29.
 * 中文分词器组件
 */
public class ANSJTokenizer implements Tokenizer {
    private List<String> tokenizer;
    private TokenPreProcess tokenPreProcess;
    private int index = 0;

    public ANSJTokenizer(String toTokenize) {
        List<Term> terms = ToAnalysis.parse(toTokenize);
        tokenizer = new ArrayList<String>();
        String word;
        for (Term term : terms) {
            word = term.getName();
            if (StringUtils.isNotBlank(word)) {
                tokenizer.add(word);
            }
        }
    }

    @Override
    public boolean hasMoreTokens() {
        return index < tokenizer.size();
    }

    @Override
    public int countTokens() {
        return tokenizer.size();
    }

    @Override
    public String nextToken() {
        String base = tokenizer.get(index++);
        if (tokenPreProcess != null)
            base = tokenPreProcess.preProcess(base);
        return base;
    }

    @Override
    public List<String> getTokens() {
        return tokenizer;
    }

    @Override
    public void setTokenPreProcessor(TokenPreProcess tokenPreProcessor) {
        this.tokenPreProcess = tokenPreProcessor;
    }

}
