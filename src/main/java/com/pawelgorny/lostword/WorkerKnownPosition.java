package com.pawelgorny.lostword;

import org.bitcoinj.crypto.MnemonicException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerKnownPosition extends Worker{

    private static int NUMBER_UNKNOWN;
    private long start = 0;
    private static final int STATUS_PERIOD = 1000 * 60;

    public WorkerKnownPosition(Configuration configuration) {
        super(configuration);
    }

    void run() throws InterruptedException, MnemonicException {
        check();
    }

    private void check() throws MnemonicException, InterruptedException {
        NUMBER_UNKNOWN = 0;
        int position = -1;
        int c=0;
        for (String word : configuration.getWORDS()){
            if (Configuration.UNKNOWN_CHAR.equalsIgnoreCase(word)){
                NUMBER_UNKNOWN++;
                if (position==-1) {
                    position = c;
                }
            }
            c++;
        }
        if (NUMBER_UNKNOWN == 1){
            checkOne(position);
        }
        else{
            checkUnknown(position);
        }
    }

    private int getNextUnknown(int last, List<String> list){
        for (int p0=(1+last); p0<list.size(); p0++){
            if (Configuration.UNKNOWN_CHAR.equalsIgnoreCase(list.get(p0))){
                return p0;
            }
        }
        return -1;
    }

    private void checkUnknown(int position) throws InterruptedException {
        System.out.println("Warning: "+((Double)Math.pow(Configuration.MNEMONIC_CODE.getWordList().size(), NUMBER_UNKNOWN)).longValue()+" possibilities!");
        List<String> mnemonic = new ArrayList<>(configuration.getWORDS());
        int dicsize = Configuration.MNEMONIC_CODE.getWordList().size();
        List<List<String>> DICTIONARY = split();
        int nextPosition = getNextUnknown(position, configuration.getWORDS());
        for (int w0=0; RESULT==null && w0<dicsize; w0++){
            System.out.println("Processing word "+(w0+1)+"/"+dicsize+" on position "+(position+1)+"! "+SDF.format(new Date()));
            mnemonic.set(position, Configuration.MNEMONIC_CODE.getWordList().get(w0));
            final CountDownLatch latch = new CountDownLatch(THREADS);
            final ExecutorService executorService = Executors.newFixedThreadPool(THREADS);
            for (int t = 0; t < THREADS; t++) {
                final int WORKING_POSITION = nextPosition;
                final List<String> SEED = new ArrayList<>(mnemonic);
                final List<String> WORDS_TO_WORK = DICTIONARY.get(t);
                final boolean REPORTER = t==0;
                executorService.submit(() -> {
                    if (REPORTER) {
                        start = System.currentTimeMillis();
                    }
                    try {
                        for (int bipPosition = 0; RESULT == null && bipPosition < WORDS_TO_WORK.size(); bipPosition++) {
                            SEED.set(WORKING_POSITION, WORDS_TO_WORK.get(bipPosition));
                            processSeed(SEED, 2, WORKING_POSITION, REPORTER);
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

    private void processSeed(final List<String> seed, int depth, int positionSet, boolean reporter) throws MnemonicException {
        if (NUMBER_UNKNOWN==depth){
            if (check(seed)){
                RESULT = new Result(seed);
                return;
            }
            if (reporter && (System.currentTimeMillis()-start > STATUS_PERIOD)){
                System.out.println(SDF.format(new Date())+ " Alive!");
                start = System.currentTimeMillis();
            }
        }else{
            int nextPosition = getNextUnknown(positionSet, seed);
            for (int w = 0; RESULT==null && w<Configuration.MNEMONIC_CODE.getWordList().size(); w++){
                seed.set(nextPosition, Configuration.MNEMONIC_CODE.getWordList().get(w));
                processSeed(seed, (depth+1), nextPosition, reporter);
            }
        }
    }

    private void checkOne(int position) throws InterruptedException {
        processPosition(position);
    }
}
