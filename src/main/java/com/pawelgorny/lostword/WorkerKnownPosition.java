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

    private int getNextUnknown(int startSearch, List<String> list){
        for (int p0=startSearch; p0<list.size(); p0++){
            if (Configuration.UNKNOWN_CHAR.equals(list.get(p0))){
                return p0;
            }
        }
        return -1;
    }

    private void checkUnknown(int position) throws InterruptedException {
        System.out.println("Warning: "+((Double)Math.pow(DICTIONARY_SIZE, NUMBER_UNKNOWN)).longValue()+" possibilities!");
        List<String> mnemonic = new ArrayList<>(configuration.getWORDS());
        List<List<String>> DICTIONARY = split();
        int nextPosition = getNextUnknown(1+position, configuration.getWORDS());
        for (int w0=configuration.getKnownStart(); RESULT==null && w0<DICTIONARY_SIZE; w0++){
            String processedWord = Configuration.MNEMONIC_CODE.getWordList().get(w0);
            System.out.println("Processing word "+(w0+1)+"/"+DICTIONARY_SIZE+" on position "+(position+1)+"! '"+processedWord+"' "+SDF.format(new Date()));
            mnemonic.set(position, processedWord);
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
                        int WORKING_POSITION_PLUS = WORKING_POSITION+1;
                        for (int bipPosition = 0; RESULT == null && bipPosition < WORDS_TO_WORK.size(); bipPosition++) {
                            SEED.set(WORKING_POSITION, WORDS_TO_WORK.get(bipPosition));
                            processSeed(SEED, 2, WORKING_POSITION_PLUS, REPORTER);
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

    private void processSeed(final List<String> seed, int depth, int positionStartSearch, boolean reporter) throws MnemonicException {
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
            int position = getNextUnknown(positionStartSearch, seed);
            int positionStartNextSearch = 0;
            int nextDepth = depth + 1;
            if (nextDepth <NUMBER_UNKNOWN ){
                positionStartNextSearch = position+1;
            }
            for (int w = 0; RESULT==null && w<DICTIONARY_SIZE; w++){
                seed.set(position, Configuration.MNEMONIC_CODE.getWordList().get(w));
                processSeed(seed, nextDepth, positionStartNextSearch, reporter);
            }
        }
    }

    private void checkOne(int position) throws InterruptedException {
        processPosition(position);
    }
}
