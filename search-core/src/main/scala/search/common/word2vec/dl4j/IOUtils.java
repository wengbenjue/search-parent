package search.common.word2vec.dl4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import search.common.util.JavaLogging;

import java.io.*;

/**
 * Created by soledede.weng on 2016/7/29.
 */
public class IOUtils {
   // private static final Logger logger = JavaLogging.log();
   private static Logger logger = LoggerFactory.getLogger(Word2VecUtils.class);

    private static final String LINE_BREAKER = "\n";

    public static StringBuffer read(File file, String charset, boolean lineBreak) throws IOException {
        StringBuffer sb = new StringBuffer();
        readToBuffer(file, sb, charset, lineBreak);
        return sb;
    }

    public static StringBuffer traverse(File file, String charset, FileFilter filter, boolean breakLine)
            throws IOException {
        StringBuffer buffer = new StringBuffer();
        traverseFolder(buffer, file, charset, filter, breakLine);
        return buffer;
    }

    private static void traverseFolder(StringBuffer buffer, File folder, String charset, FileFilter filter,
                                       boolean breakLine) throws IOException {
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isFile() && filter.accept(file)) {
                readToBuffer(file, buffer, charset, breakLine);
            } else if (file.isDirectory()) {
                traverseFolder(buffer, file, charset, filter, breakLine);
            }
        }
    }

    private static void readToBuffer(File file, StringBuffer buffer, String charset, boolean lineBreak)
            throws IOException {
        InputStream is = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(new BufferedInputStream(is, 10 * 1024), charset);
        BufferedReader br = new BufferedReader(isr);
        String line = br.readLine();
        while (line != null) {
            buffer.append(line);
            if (lineBreak)
                buffer.append(LINE_BREAKER);
            line = br.readLine();
        }
        br.close();
        isr.close();
        is.close();
        logger.info("Read CharSequence successfully from path [{}]", file.getAbsolutePath());
    }

}
