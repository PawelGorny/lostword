package com.pawelgorny.lostword;

import com.pawelgorny.lostword.util.PBKDF2SHA512;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDDerivationException;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.script.Script;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Worker {

    protected final int DICTIONARY_SIZE = Configuration.MNEMONIC_CODE.getWordList().size();
    protected static Result RESULT = null;
    protected final Configuration configuration;
    protected int THREADS = 2;
    protected final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    protected final HMac SHA_512_DIGEST;
    protected static final byte[] SALT = "mnemonic".getBytes(StandardCharsets.UTF_8);

    private final byte[] BITCOIN_SEED_BYTES = "Bitcoin seed".getBytes();
    private final long CREATION_SECONDS = Utils.currentTimeSeconds();

    public Worker(Configuration configuration)  {
        this.configuration = configuration;
        int procs = Runtime.getRuntime().availableProcessors();
        if (procs > 1) {
            if (procs % 2 == 1) {
                procs--;
            }
            THREADS = procs;
        }
        SHA_512_DIGEST = createHmacSha512Digest();
    }

    public void run() throws InterruptedException, MnemonicException {
        Worker worker = null;
        switch (configuration.getWork()){
            case ONE_UNKNOWN:
                worker = new WorkerUnknown(configuration);
                break;
            case KNOWN_POSITION:
                worker = new WorkerKnownPosition(configuration);
                break;
            case ONE_UNKNOWN_CHECK_ALL:
                worker = new WorkerUnknownCheckAll(configuration);
                break;
            case POOL:
                worker = new WorkerPool(configuration);
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
        return check(mnemonic, null, null);
    }

    protected boolean check(final List<String> mnemonic, HMac SHA512DIGEST, MessageDigest sha256) throws MnemonicException {
        if (!checksumCheck(mnemonic, sha256)){
            return false;
        }
        byte[] seed = PBKDF2SHA512.derive(Utils.SPACE_JOINER.join(mnemonic).getBytes(StandardCharsets.UTF_8), SALT, 2048, 64);
        DeterministicKey deterministicKey = createMasterPrivateKey(seed, SHA512DIGEST==null?this.SHA_512_DIGEST:SHA512DIGEST);
        for (int i=0; i<configuration.getDerivationPath().size(); i++){
            deterministicKey = HDKeyDerivation.deriveChildKey(deterministicKey, configuration.getDerivationPath().get(i));
        }
        if (configuration.getDBscriptType().equals(Script.ScriptType.P2WPKH)){
            return Address.fromKey(configuration.getNETWORK_PARAMETERS(), deterministicKey, Script.ScriptType.P2WPKH).equals(configuration.getSegwitAddress());
        }else {
            return Address.fromKey(configuration.getNETWORK_PARAMETERS(), deterministicKey, configuration.getDBscriptType()).equals(configuration.getLegacyAddress());
        }
    }

    protected boolean checksumCheck(final List<String> mnemonic, MessageDigest sha256){
        if (sha256 == null){
            try {
                sha256 = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException var1) {
                throw new RuntimeException(var1);
            }
        }
        return checksumCheckBcJ(mnemonic, sha256);
    }

    public boolean checksumCheckBcJ(List<String> words, MessageDigest sha256){
        int concatLenBits = words.size() * 11;
        boolean[] concatBits = new boolean[concatLenBits];
        int wordindex = 0;

        int hash;
        for(Iterator checksumLengthBits = words.iterator(); checksumLengthBits.hasNext(); ++wordindex) {
            String entropyLengthBits = (String)checksumLengthBits.next();
            int entropy = Collections.binarySearch(Configuration.MNEMONIC_CODE.getWordList(), entropyLengthBits);
            if(entropy < 0) {
                return false;
            }
            for(hash = 0; hash < 11; ++hash) {
                concatBits[wordindex * 11 + hash] = (entropy & 1 << 10 - hash) != 0;
            }
        }

        int var11 = concatLenBits / 33;
        int var12 = concatLenBits - var11;
        byte[] var13 = new byte[var12 / 8];

        for(hash = 0; hash < var13.length; ++hash) {
            for(int hashBits = 0; hashBits < 8; ++hashBits) {
                if(concatBits[hash * 8 + hashBits]) {
                    var13[hash] = (byte)(var13[hash] | 1 << 7 - hashBits);
                }
            }
        }

        byte[] var14 = hash(var13, 0, var13.length, sha256);
        boolean[] var15 = bytesToBits(var14);

        for(int i = 0; i < var11; ++i) {
            if(concatBits[var12 + i] != var15[i]) {
                return false;
            }
        }
        return true;
    }
    private static boolean[] bytesToBits(byte[] data) {
        boolean[] bits = new boolean[data.length * 8];

        for(int i = 0; i < data.length; ++i) {
            for(int j = 0; j < 8; ++j) {
                bits[i * 8 + j] = (data[i] & 1 << 7 - j) != 0;
            }
        }
        return bits;
    }
    public static byte[] hash(byte[] input, int offset, int length, MessageDigest sha256) {
        sha256.reset();
        sha256.update(input, offset, length);
        return sha256.digest();
    }

    public HMac createHmacSha512Digest() {
        SHA512Digest digest = new SHA512Digest();
        HMac hMac = new HMac(digest);
        hMac.init(new KeyParameter(BITCOIN_SEED_BYTES));
        return hMac;
    }
    private byte[] hmacSha512(HMac hmacSha512, byte[] input) {
        hmacSha512.reset();
        hmacSha512.update(input, 0, input.length);
        byte[] out = new byte[64];
        hmacSha512.doFinal(out, 0);
        return out;
    }

    protected DeterministicKey createMasterPrivateKey(byte[] seed, HMac SHA512DIGEST) throws HDDerivationException {
        byte[] i = hmacSha512(SHA512DIGEST, seed);
        byte[] il = Arrays.copyOfRange(i, 0, 32);
        byte[] ir = Arrays.copyOfRange(i, 32, 64);
        Arrays.fill(i, (byte)0);
        DeterministicKey masterPrivKey = HDKeyDerivation.createMasterPrivKeyFromBytes(il, ir);
        Arrays.fill(il, (byte)0);
        Arrays.fill(ir, (byte)0);
        masterPrivKey.setCreationTimeSeconds(CREATION_SECONDS);
        return masterPrivKey;
    }

    protected List<List<String>> split() {
        List<List<String>> result = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            result.add(new ArrayList<>(DICTIONARY_SIZE / THREADS));
        }
        for (int w = 0; w < DICTIONARY_SIZE; w++) {
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
                    final MessageDigest LOCAL_SHA_256_DIGEST = MessageDigest.getInstance("SHA-256");
                    final HMac LOCAL_SHA_512_DIGEST = createHmacSha512Digest();
                    for (int bipPosition = 0; RESULT == null && bipPosition < WORDS_TO_WORK.size(); bipPosition++) {
                        SEED.set(WORKING_POSITION, WORDS_TO_WORK.get(bipPosition));
                        if (check(SEED, LOCAL_SHA_512_DIGEST, LOCAL_SHA_256_DIGEST)) {
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
                return Utils.SPACE_JOINER.join(seed);
            }
        }
    }
}
