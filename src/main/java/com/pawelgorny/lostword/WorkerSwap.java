package com.pawelgorny.lostword;

import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.MnemonicException;

import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WorkerSwap extends Worker {

    private MessageDigest SHA_256_DIGEST;
    private FileWriter fileWriter = null;
    private final String fileName;
    private int counter = 0;
    private final Set<String> result = new HashSet<>();

    public WorkerSwap(Configuration configuration){
        super(configuration);
        try {
            SHA_256_DIGEST = MessageDigest.getInstance("SHA-256");
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        fileName = "SWAPS_"+(System.currentTimeMillis())+".txt";
        System.out.println("Saving to file: "+fileName);
    }

    @Override
    public void run() throws InterruptedException, MnemonicException {
        System.out.println("Input: " + Utils.SPACE_JOINER.join(configuration.getWORDS()));
        String[] words = new String[0];
        words = configuration.getWORDS().toArray(words);
        printAllSwaps(configuration.getSIZE(), words);
        if (!result.isEmpty()){
            for (String r : result){
                System.out.println(r);
            }
        }
        if (fileName!=null){
            if (fileWriter!=null) {
                try {
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    System.out.println(e.getLocalizedMessage());
                }
                System.out.println("File " + fileName + " created, "+counter+" results");
            }
        }
    }

    private void printAllSwaps(int size, String[] words) {

        List<String> mnemonic = Arrays.asList(words);
        if (checksumCheck(mnemonic, SHA_256_DIGEST)) {
            result.add(Utils.SPACE_JOINER.join(mnemonic));
        }
        String[] WORDS = new String[size];
        for (int i=0; i<size; i++){
            for (int w=0; w<size; w++){
                WORDS[w] = words[w];
            }
            for (int j=0; j<size; j++){
                swap(WORDS, i, j);
                mnemonic = Arrays.asList(WORDS);
                if (checksumCheck(mnemonic, SHA_256_DIGEST)) {
                    result.add(Utils.SPACE_JOINER.join(mnemonic));
                }
                swap(WORDS, j, i);
            }
        }

    }

    private void swap(String[] words, int i, int j) {
        String t = words[i];
        words[i] = words[j];
        words[j] = t;
    }


}
