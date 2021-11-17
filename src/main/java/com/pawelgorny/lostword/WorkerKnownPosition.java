package com.pawelgorny.lostword;

import org.bitcoinj.crypto.MnemonicException;
import org.bouncycastle.crypto.macs.HMac;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerKnownPosition extends Worker{

    private static int NUMBER_UNKNOWN;
    private long start = 0;

    public WorkerKnownPosition(Configuration configuration) {
        super(configuration);
    }

    public void run() throws InterruptedException, MnemonicException {
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

        final List<MessageDigest> SHA_256_DIGESTS= new ArrayList<>(THREADS);
        final List<HMac> SHA_512_DIGESTS= new ArrayList<>(THREADS);
        for (int t=0; t<THREADS; t++){
            try {
                SHA_512_DIGESTS.add(createHmacSha512Digest());
                SHA_256_DIGESTS.add(MessageDigest.getInstance("SHA-256"));
            }catch (Exception e){
            }
        }

        for (int w0=configuration.getKnownStart(); RESULT==null && w0<DICTIONARY_SIZE; w0++){
            String processedWord = Configuration.MNEMONIC_CODE.getWordList().get(w0);
            System.out.println("Processing word "+(w0+1)+"/"+DICTIONARY_SIZE+" on position "+(position+1)+"! '"+processedWord+"' "+ SDTF.format(new Date()));
            mnemonic.set(position, processedWord);
            final CountDownLatch latch = new CountDownLatch(THREADS);
            final ExecutorService executorService = Executors.newFixedThreadPool(THREADS);
            for (int t = 0; t < THREADS; t++) {
                final int WORKING_POSITION = nextPosition;
                final List<String> SEED = new LinkedList<>(mnemonic);
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
                        int WORKING_POSITION_PLUS = WORKING_POSITION+1;
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

    private Boolean processSeed(final List<String> seed, int depth, int positionStartSearch, boolean reporter, HMac SHA_512_DIGEST, MessageDigest SHA_256_DIGEST) throws MnemonicException {
        if (NUMBER_UNKNOWN==depth){
            Boolean checkResult = check(seed, SHA_512_DIGEST, SHA_256_DIGEST);
            if (checkResult!=null&&checkResult){
                System.out.println(seed);
                RESULT = new Result(new ArrayList<>(seed));
                return true;
            }
            if (reporter && (System.currentTimeMillis()-start > STATUS_PERIOD)){
                System.out.println(SDTF.format(new Date())+ " Alive!");
                start = System.currentTimeMillis();
            }
            return configuration.isMissingChecksum()?(checkResult==null?false:null):checkResult;
        }else{
            int nextDepth = depth + 1;
            int position = getNextUnknown(positionStartSearch, seed);
            if(position == -1){
                Boolean checkResult = check(seed, SHA_512_DIGEST, SHA_256_DIGEST);
                if (checkResult!=null&&checkResult){
                    System.out.println(seed);
                    RESULT = new Result(new ArrayList<>(seed));
                }
                return false;
            }
            int positionStartNextSearch = 0;
            if (nextDepth <NUMBER_UNKNOWN ){
                positionStartNextSearch = position+1;
            }
            int checksumCheckLimit = configuration.getMissingChecksumLimit();
            for (int w = 0; RESULT==null && w<DICTIONARY_SIZE; w++){
                seed.set(position, Configuration.MNEMONIC_CODE.getWordList().get(w));
                Boolean result = processSeed(seed, nextDepth, positionStartNextSearch, reporter, SHA_512_DIGEST, SHA_256_DIGEST);
                if(result == null && --checksumCheckLimit==0){
                    break;
                }
            }
            seed.set(position, Configuration.UNKNOWN_CHAR);
        }
        return false;
    }

    private void checkOne(int position) throws InterruptedException {
        processPosition(position);
    }
}
