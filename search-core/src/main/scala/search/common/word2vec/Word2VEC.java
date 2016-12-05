package search.common.word2vec;

import lombok.val;
import search.common.util.Util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;


public class Word2VEC {
    private HashMap<String, float[]> wordMap = new HashMap<String, float[]>();
    private int words;
    private int size;
    private int topNSize = 40;

    public int getTopNSize() {
        return topNSize;
    }

    public void setTopNSize(int topNSize) {
        this.topNSize = topNSize;
    }

    public HashMap<String, float[]> getWordMap() {
        return wordMap;
    }

    public int getWords() {
        return words;
    }

    public int getSize() {
        return size;
    }


    public void loadModel(String path) throws IOException {
        loadModel(new FileInputStream(path));
    }

    /**
     * @return void
     * @Description:load model
     */
    public void loadModel(InputStream in) throws IOException {
        DataInputStream dis = null;
        BufferedInputStream bis = null;
        double len = 0;
        float vector = 0;
        try {
            bis = new BufferedInputStream(in);
            dis = new DataInputStream(bis);
            // //读取词数
            words = Integer.parseInt(readString(dis));
            // //大小
            size = Integer.parseInt(readString(dis));

            String word;
            float[] vectors = null;
            for (int i = 0; i < words; i++) {
                word = readString(dis);
                vectors = new float[size];
                len = 0;
                for (int j = 0; j < size; j++) {
                    vector = readFloat(dis);
                    len += vector * vector;
                    vectors[j] = (float) vector;
                }
                len = Math.sqrt(len);

                for (int j = 0; j < vectors.length; j++) {
                    vectors[j] = (float) (vectors[j] / len);
                }
                wordMap.put(word, vectors);
                dis.read();
            }

        } finally {
            bis.close();
            dis.close();
        }
    }

    private static final int MAX_SIZE = 50;

    /**
     * @return Set<WordEntry>
     * @Description:得到近义词
     */
    public Set<WordEntry> distance(String word) {
        float[] wordVector = getWordVector(word);
        if (wordVector == null) {
            return null;
        }
        Set<Entry<String, float[]>> entrySet = wordMap.entrySet();
        float[] tempVector = null;
        List<WordEntry> wordEntrys = new ArrayList<WordEntry>(topNSize);
        String name = null;
        for (Entry<String, float[]> entry : entrySet) {
            name = entry.getKey();
            if (name.equals(word)) {
                continue;
            }
            float dist = 0;
            tempVector = entry.getValue();
            for (int i = 0; i < wordVector.length; i++) {
                dist += wordVector[i] * tempVector[i];
            }
            insertTopN(name, dist, wordEntrys);
        }
        return new TreeSet<WordEntry>(wordEntrys);
    }

    /**
     * @return TreeSet<WordEntry>
     * @Description:近义词
     */
    public TreeSet<WordEntry> analogy(String word0, String word1, String word2) {
        float[] wv0 = getWordVector(word0);
        float[] wv1 = getWordVector(word1);
        float[] wv2 = getWordVector(word2);

        if (wv1 == null || wv2 == null || wv0 == null) {
            return null;
        }
        float[] wordVector = new float[size];
        for (int i = 0; i < size; i++) {
            wordVector[i] = wv1[i] - wv0[i] + wv2[i];
        }
        float[] tempVector;
        String name;
        List<WordEntry> wordEntrys = new ArrayList<WordEntry>(topNSize);
        for (Entry<String, float[]> entry : wordMap.entrySet()) {
            name = entry.getKey();
            if (name.equals(word0) || name.equals(word1) || name.equals(word2)) {
                continue;
            }
            float dist = 0;
            tempVector = entry.getValue();
            for (int i = 0; i < wordVector.length; i++) {
                dist += wordVector[i] * tempVector[i];
            }
            insertTopN(name, dist, wordEntrys);
        }
        return new TreeSet<WordEntry>(wordEntrys);
    }

    /**
     * @return void
     * @Description:插入权重最高的相关词
     */
    private void insertTopN(String name, float score, List<WordEntry> wordsEntrys) {
        if (wordsEntrys.size() < topNSize) {
            wordsEntrys.add(new WordEntry(name, score));
            return;
        }
        float min = Float.MAX_VALUE;
        int minOffe = 0;
        for (int i = 0; i < topNSize; i++) {
            WordEntry wordEntry = wordsEntrys.get(i);
            if (min > wordEntry.score) {
                min = wordEntry.score;
                minOffe = i;
            }
        }
        if (score > min) {
            wordsEntrys.set(minOffe, new WordEntry(name, score));
        }
    }

    public class WordEntry implements Comparable<WordEntry> {
        public String name;
        public float score;

        public WordEntry(String name, float score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public String toString() {
            return this.name + "\t" + score;
        }

        @Override
        public int compareTo(WordEntry o) {
            if (this.score > o.score) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    /**
     * @return float[]
     * @Description:得到词向量
     */
    public float[] getWordVector(String word) {
        return wordMap.get(word);
    }

    public static float readFloat(InputStream is) throws IOException {
        byte[] bytes = new byte[4];
        is.read(bytes);
        return getFloat(bytes);
    }

    /**
     * 读取一个float
     *
     * @param b
     * @return
     */
    public static float getFloat(byte[] b) {
        int accum = 0;
        accum = accum | (b[0] & 0xff) << 0;
        accum = accum | (b[1] & 0xff) << 8;
        accum = accum | (b[2] & 0xff) << 16;
        accum = accum | (b[3] & 0xff) << 24;
        return Float.intBitsToFloat(accum);
    }

    /**
     * @return String
     * @Description:读取一个字符串
     */
    private static String readString(DataInputStream dis) throws IOException {
        byte[] bytes = new byte[MAX_SIZE];
        byte b = dis.readByte();
        int i = -1;
        StringBuilder sb = new StringBuilder();
        while (b != 32 && b != 10) {
            i++;
            bytes[i] = b;
            b = dis.readByte();
            if (i == 49) {
                sb.append(new String(bytes));
                i = -1;
                bytes = new byte[MAX_SIZE];
            }
        }
        sb.append(new String(bytes, 0, i + 1));
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        Word2VEC word2VEC = new Word2VEC();
        int REL_NEWS_SIZE = 20;
        word2VEC.setTopNSize(REL_NEWS_SIZE);
        // InputStream in = Util.getContextClassLoader().getResourceAsStream("model/newsfinal11.bin");
        //InputStream in = Util.getContextClassLoader().getResourceAsStream("model/hotnews_2016.bin");
        //word2VEC.loadModel("D:/workspace/search/word2vec/newsfinal11.bin");
       // word2VEC.loadModel(in);
        word2VEC.loadModel("D:/workspace/search/word2vec/hotnews_2016.bin");
        Set<WordEntry> persononVectory = word2VEC.distance("工行");
        System.out.println(persononVectory);
       long startTime = System.currentTimeMillis();
        Set<WordEntry> persononVectory1 = word2VEC.distance("黄山旅游");
        long endTime = System.currentTimeMillis();
        System.out.println("cost:"+(endTime-startTime));
        System.out.println(persononVectory1);

    }
}