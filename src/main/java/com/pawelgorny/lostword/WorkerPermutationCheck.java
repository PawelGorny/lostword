package com.pawelgorny.lostword;

import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.MnemonicException;
import org.bouncycastle.crypto.macs.HMac;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        THREADS = Math.min(THREADS, configuration.getSIZE());
        List<List<String>> workPerThread = new ArrayList<>(THREADS);
        int tIx=0;
        for(String w:words){
            if (workPerThread.size()==tIx){
                workPerThread.add(new ArrayList<>(1));
            }
            workPerThread.get(tIx).add(w);
            tIx=(++tIx%THREADS);
        }
        final List<MessageDigest> SHA_256_DIGESTS= new ArrayList<>(THREADS);
        final List<HMac> SHA_512_DIGESTS= new ArrayList<>(THREADS);

        final CountDownLatch latch = new CountDownLatch(THREADS);
        final ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

        for(tIx=0; tIx<THREADS; tIx++){
            try {
                SHA_512_DIGESTS.add(createHmacSha512Digest());
                SHA_256_DIGESTS.add(MessageDigest.getInstance("SHA-256"));
            }catch (Exception e){
            }
            final boolean REPORTER = tIx==0;
            final int T_NUMBER = tIx;
            final List<String> WORDS_TO_WORK = workPerThread.get(tIx);
            executorService.submit(() -> {
                final HMac LOCAL_SHA_512_DIGEST = SHA_512_DIGESTS.get(T_NUMBER);
                final MessageDigest LOCAL_SHA_256_DIGEST = SHA_256_DIGESTS.get(T_NUMBER);
                if (REPORTER) {
                    start = System.currentTimeMillis();
                }
                for (String PREFIX : WORDS_TO_WORK) {
                    if (RESULT != null) {
                        break;
                    }
                    List<String> seedToProcess = new ArrayList<>(configuration.getWORDS());
                    for (int i = 0; i < configuration.getSIZE(); i++) {
                        if (seedToProcess.get(i).equals(PREFIX)) {
                            seedToProcess.remove(i);
                            break;
                        }
                    }
                    String[] target = new String[configuration.getSIZE()];
                    String[] toProcess = new String[configuration.getSIZE() - 1];
                    toProcess = seedToProcess.toArray(toProcess);
                    target[0] = PREFIX;
                    checkAllRecursive(configuration.getSIZE() - 1, toProcess, target, LOCAL_SHA_512_DIGEST, LOCAL_SHA_256_DIGEST, REPORTER);
                }
                latch.countDown();
            });
        }
        latch.await();
        executorService.shutdown();
    }

    private boolean checkAllRecursive(int n, String[] elements, final String[] target, HMac LOCAL_SHA_512_DIGEST, MessageDigest LOCAL_SHA_256_DIGEST, Boolean REPORTER) {
        if(n == 1) {
            return checkElements(target, elements, LOCAL_SHA_512_DIGEST, LOCAL_SHA_256_DIGEST, REPORTER);
        } else {
            for(int i = 0; i < n-1; i++) {
                if (checkAllRecursive(n - 1, elements, target, LOCAL_SHA_512_DIGEST, LOCAL_SHA_256_DIGEST, REPORTER)){
                    return true;
                }
                if(n % 2 == 0) {
                    swap(elements, i, n - 1);
                } else {
                    swap(elements, 0, n - 1);
                }
            }
            if (checkAllRecursive(n - 1, elements, target, LOCAL_SHA_512_DIGEST, LOCAL_SHA_256_DIGEST, REPORTER)){
                return true;
            }
        }
        return RESULT!=null;
    }

    private boolean checkElements(String[] target, String[] input, HMac LOCAL_SHA_512_DIGEST, MessageDigest LOCAL_SHA_256_DIGEST, Boolean REPORTER) {
        System.arraycopy(input,0, target, 1, input.length);
        List<String> mnemonic = Arrays.asList(target);
        try {
            boolean result = check(mnemonic, LOCAL_SHA_512_DIGEST, LOCAL_SHA_256_DIGEST);
            if (result){
                RESULT = new Result(mnemonic);
            }
        }catch (MnemonicException e){
            System.out.println(e.getLocalizedMessage());
        }
        if (REPORTER && (System.currentTimeMillis()-start > STATUS_PERIOD)){
            System.out.println(SDTF.format(new Date())+ " Alive!");
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
