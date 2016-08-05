package search.common.word2vec.dl4j;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import search.common.util.JavaLogging;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by soledede.weng on 2016/7/29.
 * 定义一个记忆对象用于保存训练结果从而达到重用的目的
 */
public class Memory {
    private String path;

    private String folderName;

    private String fileName;

    private static Logger logger = LoggerFactory.getLogger(Word2VecUtils.class);

    public Memory(@NonNull String path, Policy policy) throws FileNotFoundException {
        String[] dirs = path.split("[\\\\/]");
        String fileName = dirs[dirs.length - 1];
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < dirs.length - 1; i++) {
            buffer.append(dirs[i]);
            buffer.append(File.separator);
        }
        String folderName = buffer.toString();

        this.fileName = fileName;
        this.folderName = folderName;
        this.path = folderName + fileName;

        if (policy.value == Policy.INIT.value) {
            File file = new File(folderName);
            if (!file.exists())
                file.mkdirs();
            file = new File(this.path);
            if (file.exists()) {
                logger.info(
                        "Memory in path [{}] has already existed,the operation will delete the old file then continue.",
                        this.path);
                file.delete();
            }
        } else if (policy.value == Policy.RESTORE.value) {
            File file = new File(this.path);
            if (!file.exists())
                throw new FileNotFoundException();
        }

        logger.info("Memory certified successfully in path [{}]", this.path);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public enum Policy {
        INIT(1), RESTORE(2);

        public final int value;

        private Policy(int value) {
            this.value = value;
        }
    }
}
