package com.huaban.analysis.jieba;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;


public class WordDictionary {
    private static volatile WordDictionary singleton;
    private static final String MAIN_DICT = "/data/Jieba/dict.big.txt";
    private static final String USER_DICT_SUFFIX = ".dict";

    public final Map<String, String> parts = new HashMap<>();
    public final Map<String, Double> freqs = new HashMap<>();
    public final Set<String> loadedPath = new HashSet<>();
    private Double minFreq = Double.MAX_VALUE;
    private Double total = 0.0;
    private DictSegment _dict;


    private WordDictionary() {
        this.loadDict();
    }


    public static WordDictionary getInstance() {
        if (singleton == null) {
            synchronized (WordDictionary.class) {
                if (singleton == null) {
                    singleton = new WordDictionary();
                    return singleton;
                }
            }
        }
        return singleton;
    }


    /**
     * for ES to initialize the user dictionary.
     *
     */
    public void init(Path configFile) {
        String abspath = configFile.toAbsolutePath().toString();
        System.out.println("initialize user dictionary:" + abspath);
        synchronized (WordDictionary.class) {
            if (loadedPath.contains(abspath))
                return;
            
            DirectoryStream<Path> stream;
            try {
                stream = Files.newDirectoryStream(configFile, String.format(Locale.getDefault(), "*%s", USER_DICT_SUFFIX));
                for (Path path: stream){
                    System.err.printf(Locale.getDefault(), "loading dict %s%n", path.toString());
                    singleton.loadUserDict(path);
                }
                loadedPath.add(abspath);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
                System.err.printf(Locale.getDefault(), "%s: load user dict failure!%n", configFile);
            }
        }
    }
    
    public void init(String[] paths) {
        synchronized (WordDictionary.class) {
            for (String path: paths){
                if (!loadedPath.contains(path)) {
                    try {
                        System.out.println("initialize user dictionary: " + path);
                        singleton.loadUserDict(path);
                        loadedPath.add(path);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        System.err.printf(Locale.getDefault(), "%s: load user dict failure!%n", path);
                    }
                }
            }
        }
    }
    
    /**
     * let user just use their own dict instead of the default dict
     */
    public void resetDict(){
    	_dict = new DictSegment((char) 0);
    	freqs.clear();
    }


    public void loadDict() {
        _dict = new DictSegment((char) 0);
        InputStream is = this.getClass().getResourceAsStream(MAIN_DICT);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(is), StandardCharsets.UTF_8));

            long s = System.currentTimeMillis();
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("[\t ]+");

                if (tokens.length < 2)
                    continue;

                String word = tokens[0];
                double freq = Double.parseDouble(tokens[1]);
                total += freq;
                word = addWord(word);
                parts.put(word, tokens[2]);
                freqs.put(word, freq);
            }
            // normalize
            for (Entry<String, Double> entry : freqs.entrySet()) {
                entry.setValue((Math.log(entry.getValue() / total)));
                minFreq = Math.min(entry.getValue(), minFreq);
            }
            System.out.printf(Locale.getDefault(), "main dict load finished, time elapsed %d ms%n",
                System.currentTimeMillis() - s);
        }
        catch (IOException e) {
            System.err.printf(Locale.getDefault(), "%s load failure!%n", MAIN_DICT);
        }
        finally {
            try {
                if (null != is)
                    is.close();
            }
            catch (IOException e) {
                System.err.printf(Locale.getDefault(), "%s close failure!%n", MAIN_DICT);
            }
        }
    }


    private String addWord(String word) {
        if (null != word && !"".equals(word.trim())) {
            String key = word.trim().toLowerCase(Locale.getDefault());
            _dict.fillSegment(key.toCharArray());
            return key;
        }
        else
            return null;
    }


    public void loadUserDict(Path userDict) {
        loadUserDict(userDict, StandardCharsets.UTF_8);
    }

    public void loadUserDict(String userDictPath) {
        loadUserDict(userDictPath, StandardCharsets.UTF_8);
    }
    
    public void loadUserDict(Path userDict, Charset charset) {                
        try {
            BufferedReader br = Files.newBufferedReader(userDict, charset);
            long s = System.currentTimeMillis();
            int count = 0;
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("[\t ]+");

                if (tokens.length < 1) {
                    // Ignore empty line
                    continue;
                }

                String word = tokens[0];

                double freq = 3.0d;
                if (tokens.length == 2)
                    freq = Double.parseDouble(tokens[1]);
                word = addWord(word); 
                freqs.put(word, Math.log(freq / total));
                count++;
            }
            System.out.printf(Locale.getDefault(), "user dict %s load finished, tot words:%d, time elapsed:%dms%n", userDict, count, System.currentTimeMillis() - s);
            br.close();
        }
        catch (IOException e) {
            System.err.printf(Locale.getDefault(), "%s: load user dict failure!%n", userDict);
        }
    }

    public void loadUserDict(String userDictPath, Charset charset) {
        InputStream is = this.getClass().getResourceAsStream(userDictPath);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(is), charset));

            long s = System.currentTimeMillis();
            int count = 0;
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("[\t ]+");

                if (tokens.length < 1) {
                    // Ignore empty line
                    continue;
                }

                String word = tokens[0];

                double freq = 3.0d;
                if (tokens.length == 2)
                    freq = Double.parseDouble(tokens[1]);
                word = addWord(word);
                freqs.put(word, Math.log(freq / total));
                count++;
            }
            System.out.printf(Locale.getDefault(), "user dict %s load finished, tot words:%d, time elapsed:%dms%n", userDictPath, count, System.currentTimeMillis() - s);
            br.close();
        }
        catch (IOException e) {
            System.err.printf(Locale.getDefault(), "%s: load user dict failure!%n", userDictPath);
        }
    }
    
    public DictSegment getTrie() {
        return this._dict;
    }


    public boolean containsWord(String word) {
        return freqs.containsKey(word);
    }


    public Double getFreq(String key) {
        if (containsWord(key))
            return freqs.get(key);
        else
            return minFreq;
    }
}
