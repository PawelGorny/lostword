// baeldung : https://www.baeldung.com/java-array-permutations
package com.pawelgorny.lostword;

import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.MnemonicException;

import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

public class WorkerPermutation extends Worker{

    private MessageDigest SHA_256_DIGEST;
    private FileWriter fileWriter = null;
    private final String fileName;
    private int counter = 0;

    public WorkerPermutation(Configuration configuration)  {
        super(configuration);
        try {
            SHA_256_DIGEST = MessageDigest.getInstance("SHA-256");
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        fileName = "PERMUTATATIONS_"+(System.currentTimeMillis())+".txt";
        System.out.println("Saving to file: "+fileName);
    }

    @Override
    public void run() throws InterruptedException, MnemonicException {
        System.out.println("Input: " + Utils.SPACE_JOINER.join(configuration.getWORDS()));
        String[] words = new String[0];
        words = configuration.getWORDS().toArray(words);
        printAllRecursive(configuration.getSIZE(), words);
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

    private void printAllRecursive(int n, java.lang.String[] elements) {
        if(n == 1) {
            printArray(elements);
        } else {
            for(int i = 0; i < n-1; i++) {
                printAllRecursive(n - 1, elements);
                if(n % 2 == 0) {
                    swap(elements, i, n-1);
                } else {
                    swap(elements, 0, n-1);
                }
            }
            printAllRecursive(n - 1, elements);
        }
    }
    private void printArray(String[] input) {
        List<String> mnemonic = Arrays.asList(input);
        if (checksumCheck(mnemonic, SHA_256_DIGEST)) {
            String data = Utils.SPACE_JOINER.join(mnemonic);
            System.out.println(data);
            if (fileWriter == null){
                try {
                    fileWriter = new FileWriter(fileName);
                } catch (IOException e) {
                    System.out.println(e.getLocalizedMessage());
                }
            }
            try {
                fileWriter.write(data);
                fileWriter.write("\n");
                fileWriter.flush();
                counter ++;
            }catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
            }
        }
    }
    private void swap(String[] input, int a, int b) {
        String tmp = input[a];
        input[a] = input[b];
        input[b] = tmp;
    }
}
