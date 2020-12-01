package com.pawelgorny.lostword;

import org.bitcoinj.core.Address;
import org.bitcoinj.crypto.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Worker {

    private static Result RESULT = null;
    private final Configuration configuration;
    private final int THREADS_MIN = 2;
    private int THREADS = THREADS_MIN;

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
        System.out.println("Expected address: '" + configuration.getTargetAddress() + "'");
        System.out.println("Using " + THREADS + " threads");
        check();
        System.out.println("");
        System.out.println("--- Work finished ---");
        if (RESULT == null) {
            System.out.println("Missing word not found!");
        } else {
            System.out.println("Found missing word!");
            System.out.println(RESULT.toString());
        }
    }

    private void check() throws MnemonicException, InterruptedException {
        List<String> mnemonic = new ArrayList<>(configuration.getSIZE());
        for (int i = 0; i < configuration.getSIZE(); i++) {
            mnemonic.add("");
        }

        final List<List<String>> DICTIONARY = split();

        for (int position = 0; position <= configuration.getSIZE() && RESULT == null; position++) {
            System.out.println("Checking missing word at position " + (position + 1));
            Iterator<String> iterator = configuration.getWORDS().iterator();
            int p = 0;
            while (iterator.hasNext()) {
                if (p == position) {
                    p++;
                }
                mnemonic.set(p++, iterator.next());
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
    }

    private List<List<String>> split() {
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

    private boolean check(List<String> mnemonic) throws MnemonicException {
        DeterministicKey deterministicKey = HDKeyDerivation.createMasterPrivateKey(MnemonicCode.toSeed(mnemonic, ""));
        DeterministicKey receiving = HDKeyDerivation.deriveChildKey(deterministicKey, new ChildNumber(configuration.getDPaccount(), false));
        DeterministicKey new_address_key = HDKeyDerivation.deriveChildKey(receiving, new ChildNumber(configuration.getDPaddress(), configuration.isDPhard()));
        if (configuration.getTargetAddress().equalsIgnoreCase(Address.fromKey(configuration.getNETWORK_PARAMETERS(), new_address_key, configuration.getDBscriptType()).toString())) {
            System.out.println(mnemonic);
            return true;
        }
        return false;
    }

    static class Result {
        private int position;
        private String word;

        public Result(int position, String word) {
            this.position = position;
            this.word = word;
        }

        @Override
        public String toString() {
            return "position=" + position + ", word='" + word + '\'';
        }
    }
}
