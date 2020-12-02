package com.pawelgorny.lostword;

import org.bitcoinj.core.Address;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Worker {

    protected static Result RESULT = null;
    protected final Configuration configuration;
    protected int THREADS = 2;
    protected final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Worker(Configuration configuration) {
        this.configuration = configuration;
        int procs = Runtime.getRuntime().availableProcessors();
        if (procs > 1) {
            if (procs % 2 == 1) {
                procs--;
            }
            THREADS = procs;
        }
    }

    void run() throws InterruptedException, MnemonicException {
        Worker worker = null;
        switch (configuration.getWork()){
            case ONE_UNKNOWN:
                worker = new WorkerUnknown(configuration);
                break;
            case KNOWN_POSITION:
                worker = new WorkerKnownPosition(configuration);
                break;
        }
        System.out.println("--- Starting worker --- "+SDF.format(new Date())+" ---");
        System.out.println("Expected address: '" + configuration.getTargetAddress() + "'");
        System.out.println("Using " + THREADS + " threads");
        worker.run();
        System.out.println("");
        System.out.println("--- Work finished ---");
        if (RESULT == null) {
            System.out.println("Missing word not found!");
        } else {
            System.out.println("Found missing word!");
            System.out.println(RESULT.toString());
        }
    }

    protected boolean check(final List<String> mnemonic) throws MnemonicException {
        try {
            Configuration.MNEMONIC_CODE.check(mnemonic);
        } catch (MnemonicException.MnemonicChecksumException checksumException) {
            return false;
        }
        DeterministicKey deterministicKey = HDKeyDerivation.createMasterPrivateKey(MnemonicCode.toSeed(mnemonic, ""));
        DeterministicKey receiving = HDKeyDerivation.deriveChildKey(deterministicKey, configuration.getDPchild0());
        DeterministicKey new_address_key = HDKeyDerivation.deriveChildKey(receiving, configuration.getDPchild1());
        if (configuration.getTargetAddress().equalsIgnoreCase(Address.fromKey(configuration.getNETWORK_PARAMETERS(), new_address_key, configuration.getDBscriptType()).toString())) {
            System.out.println(mnemonic);
            return true;
        }
        return false;
    }

    protected List<List<String>> split() {
        List<List<String>> result = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            result.add(new ArrayList<>(Configuration.MNEMONIC_CODE.getWordList().size() / THREADS));
        }
        for (int w = 0; w < Configuration.MNEMONIC_CODE.getWordList().size(); w++) {
            int n = w % THREADS;
            result.get(n).add(Configuration.MNEMONIC_CODE.getWordList().get(w));
        }
        return result;
    }

    protected void processPosition(int position) throws InterruptedException {
        List<List<String>> DICTIONARY = split();
        System.out.println("Checking missing word at position " + (position + 1));
        Iterator<String> iterator = configuration.getWORDS().iterator();
        int p = 0;
        List<String> mnemonic = new ArrayList<>(configuration.getSIZE());
        for (int i = 0; i < configuration.getSIZE(); i++) {
            mnemonic.add("");
        }
        while (iterator.hasNext()) {
            String word = iterator.next();
            if (Configuration.UNKNOWN_CHAR.equalsIgnoreCase(word)){
                continue;
            }
            if (p == position) {
                p++;
            }
            mnemonic.set(p++, word);
        }
        final CountDownLatch latch = new CountDownLatch(THREADS);
        final ExecutorService executorService = Executors.newFixedThreadPool(THREADS);
        for (int t = 0; t < THREADS; t++) {
            final int WORKING_POSITION = position;
            final List<String> WORDS_TO_WORK = DICTIONARY.get(t);
            final List<String> SEED = new ArrayList<>(mnemonic);
            executorService.submit(() -> {
                try {
                    for (int bipPosition = 0; RESULT == null && bipPosition < WORDS_TO_WORK.size(); bipPosition++) {
                        SEED.set(WORKING_POSITION, WORDS_TO_WORK.get(bipPosition));
                        if (check(SEED)) {
                            RESULT = new Result(1 + WORKING_POSITION, WORDS_TO_WORK.get(bipPosition));
                        }
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

    static class Result {
        private int position;
        private String word;

        private List<String> seed;

        public Result(List<String> seed){
            this.seed = seed;
        }

        public Result(int position, String word) {
            this.position = position;
            this.word = word;
        }

        @Override
        public String toString() {
            if (seed==null){
                return "position=" + position + ", word='" + word + '\'';
            }else {
                return seed.toString();
            }
        }
    }
}
