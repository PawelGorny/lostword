package com.pawelgorny.lostword;

import org.bitcoinj.crypto.MnemonicException;
import org.bouncycastle.crypto.macs.HMac;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerPool extends Worker {

    private static int NUMBER_UNKNOWN;
    private long start = 0;

    public WorkerPool(Configuration configuration) {
        super(configuration);
    }

    public void run() throws InterruptedException, MnemonicException {
        check();
    }

    private void check() throws MnemonicException, InterruptedException {
        NUMBER_UNKNOWN = 0;
        Double possibilities = 1D;
        int position = -1;
        int c=0;
        for (List<String> words : configuration.getWORDS_POOL()){
            if (words.size()>1){
                NUMBER_UNKNOWN++;
                if (position==-1) {
                    position = c;
                }
            }
            possibilities = possibilities*words.size();
            c++;
        }
        System.out.println("Warning: "+possibilities.longValue()+" possibilities!");
        checkUnknown(position);
    }

    private void checkUnknown(int position) throws InterruptedException {
        List<String> mnemonic = new ArrayList<>(configuration.getWORDS());
        int nextPosition = getNextUnknown(1+position, configuration.getWORDS());
        List<List<String>> DICTIONARY = split(configuration.getWORDS_POOL().get(nextPosition));

        final List<MessageDigest> SHA_256_DIGESTS= new ArrayList<>(THREADS);
        final List<HMac> SHA_512_DIGESTS= new ArrayList<>(THREADS);
        for (int t=0; t<THREADS; t++){
            try {
                SHA_512_DIGESTS.add(createHmacSha512Digest());
                SHA_256_DIGESTS.add(MessageDigest.getInstance("SHA-256"));
            }catch (Exception e){
            }
        }
        int DIC_SIZE = configuration.getWORDS_POOL().get(position).size();
        for (int w0=0; RESULT==null && w0<DIC_SIZE; w0++){
            String processedWord = configuration.getWORDS_POOL().get(position).get(w0);
            System.out.println("Processing word "+(w0+1)+"/"+DIC_SIZE+" on position "+(position+1)+"! '"+processedWord+"' "+SDF.format(new Date()));
            mnemonic.set(position, processedWord);
            final CountDownLatch latch = new CountDownLatch(THREADS);
            final ExecutorService executorService = Executors.newFixedThreadPool(THREADS);
            for (int t = 0; t < THREADS; t++) {
                final int WORKING_POSITION = nextPosition;
                final List<String> SEED = new ArrayList<>(mnemonic);
                final List<String> WORDS_TO_WORK = DICTIONARY.get(t);
                final boolean REPORTER = t==0;
                final int T_NUMBER=t;
                executorService.submit(() -> {
                    if (REPORTER) {
                        start = System.currentTimeMillis();
                    }
                    final HMac LOCAL_SHA_512_DIGEST = SHA_512_DIGESTS.get(T_NUMBER);
                    final MessageDigest LOCAL_SHA_256_DIGEST = SHA_256_DIGESTS.get(T_NUMBER);
                    try {
                        int WORKING_POSITION_PLUS = WORKING_POSITION + 1;
                        for (int bipPosition = 0; RESULT == null && bipPosition < WORDS_TO_WORK.size(); bipPosition++) {
                            SEED.set(WORKING_POSITION, WORDS_TO_WORK.get(bipPosition));
                            processSeed(SEED, 2, WORKING_POSITION_PLUS, REPORTER, LOCAL_SHA_512_DIGEST, LOCAL_SHA_256_DIGEST);
                        }
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                    latch.countDown();
                });
            }
            latch.await();
            executorService.shutdown();
        }
    }

    private void processSeed(final List<String> seed, int depth, int positionStartSearch, boolean reporter, HMac SHA_512_DIGEST, MessageDigest SHA_256_DIGEST) throws MnemonicException {
        if (NUMBER_UNKNOWN==depth){
            if (check(seed, SHA_512_DIGEST, SHA_256_DIGEST)){
                System.out.println(seed);
                RESULT = new Result(new ArrayList<>(seed));
                return;
            }
            if (reporter && (System.currentTimeMillis()-start > STATUS_PERIOD)){
                System.out.println(SDF.format(new Date())+ " Alive!");
                start = System.currentTimeMillis();
            }
        }else{
            int nextDepth = depth + 1;
            int position = getNextUnknown(positionStartSearch, seed);
            if(position == -1){
                if (check(seed, SHA_512_DIGEST, SHA_256_DIGEST)){
                    System.out.println(seed);
                    RESULT = new Result(new ArrayList<>(seed));
                }
                return;
            }
            int positionStartNextSearch = 0;
            if (nextDepth <NUMBER_UNKNOWN ){
                positionStartNextSearch = position+1;
            }
            for (int w = 0; RESULT==null && w<configuration.getWORDS_POOL().get(position).size(); w++){
                seed.set(position, configuration.getWORDS_POOL().get(position).get(w));
                processSeed(seed, nextDepth, positionStartNextSearch, reporter, SHA_512_DIGEST, SHA_256_DIGEST);
            }
            seed.set(position, Configuration.UNKNOWN_CHAR);
        }
    }

    private int getNextUnknown(int startSearch, List<String> list){
        for (int p0=startSearch; p0<list.size(); p0++){
            if (Configuration.UNKNOWN_CHAR.equals(list.get(p0))){
                return p0;
            }
        }
        return -1;
    }

    protected List<List<String>> split(List<String> dictionary) {
        int t = dictionary.size()>THREADS?THREADS:dictionary.size();
        THREADS = t;
        System.out.println("Using " + THREADS + " threads");
        List<List<String>> result = new ArrayList<>(t);
        for (int i = 0; i < t; i++) {
            result.add(new ArrayList<>(dictionary.size() / t));
        }
        for (int w = 0; w < dictionary.size(); w++) {
            int n = w % t;
            result.get(n).add(dictionary.get(w));
        }
        return result;
    }
}
