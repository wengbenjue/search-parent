package search.common.word2vec.dl4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import search.common.util.JavaLogging;
import search.common.util.Util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * Created by soledede.weng on 2016/7/29.
 */
public class TextSentenceFactory implements SentenceFactory {
    private static Logger logger = LoggerFactory.getLogger(Word2VecUtils.class);
    private StringBuffer buffer;
    private String charset;

    private static final String FORMAT = ".txt";

    public TextSentenceFactory(String filePath, String charset) throws IOException {
        if (Charset.isSupported(charset))
            this.charset = charset;
        else
            this.charset = "UTF-8";

        File file = new File(filePath);
        if (!file.exists()) {
            logger.error("Source [" + filePath + "]" + "did not exist!");
            return;
        }
        if (file.isFile() && file.getName().endsWith(FORMAT)) {
            buffer = IOUtils.read(file, this.charset, false);
        } else if (file.isDirectory()) {
            logger.info("Searching files from directory [{}]", file.getName());
            buffer = IOUtils.traverse(file, this.charset, new FormatFileFilter(), false);
        }
    }

    private static class FormatFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return pathname.isFile() ? pathname.getName().endsWith(FORMAT) : false;
        }

    }

    @Override
    public Collection<String> create() {
        // 此正则表达式断句是经过多次优化后得出的，请谨慎修改
        // 使用逗号进行断句既能保证语意完整，同时又不至于过于复杂造成混淆
        // 根据此表达式的测试结果相对来说是最理想的
        // Note:This regular expression tend to be the best practice after
        // several tests,replace it cautiously.

        Collection<String> sentences =  Util.regexSplit(buffer, "[^，,。.？?！!\\\\s]+");
        return sentences;
        //return Util.splitRegex(buffer.toString(), "[，|,|。|.|？|?|！|!|\\s|\\n]+");
        //return Util.splitRegex(buffer.toString(), "[,|\\n|\\s]+");
    }
}
