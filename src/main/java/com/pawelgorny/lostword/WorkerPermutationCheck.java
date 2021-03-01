package com.pawelgorny.lostword;

import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.MnemonicException;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class WorkerPermutationCheck extends Worker{

    private MessageDigest SHA_256_DIGEST;
    private long start = 0;

    public WorkerPermutationCheck(Configuration configuration) {
        super(configuration);
        try {
            SHA_256_DIGEST = MessageDigest.getInstance("SHA-256");
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
    }

    @Override
    public void run() throws InterruptedException, MnemonicException {
        System.out.println("Input: " + Utils.SPACE_JOINER.join(configuration.getWORDS()));
        String[] words = new String[0];
        words = configuration.getWORDS().toArray(words);
        start = System.currentTimeMillis();
        printAllRecursive(configuration.getSIZE(), words);
    }

    private boolean printAllRecursive(int n, String[] elements) {
        if(n == 1) {
            return checkElements(elements);
        } else {
            for(int i = 0; i < n-1; i++) {
                if (printAllRecursive(n - 1, elements)){
                    return true;
                }
                if(n % 2 == 0) {
                    swap(elements, i, n - 1);
                } else {
                    swap(elements, 0, n - 1);
                }
            }
            if (printAllRecursive(n - 1, elements)){
                return true;
            }
        }
        return RESULT!=null;
    }

    private boolean checkElements(String[] input) {
        List<String> mnemonic = Arrays.asList(input);
        try {
            boolean result = check(mnemonic, null, SHA_256_DIGEST);
            if (result){
                RESULT = new Result(mnemonic);
            }
        }catch (MnemonicException e){
            System.out.println(e.getLocalizedMessage());
        }
        if (System.currentTimeMillis()-start > STATUS_PERIOD){
            System.out.println(SDF.format(new Date())+ " Alive!");
            start = System.currentTimeMillis();
        }
        return (RESULT!=null);
    }

    protected void swap(String[] input, int a, int b) {
        String tmp = input[a];
        input[a] = input[b];
        input[b] = tmp;
    }
}
