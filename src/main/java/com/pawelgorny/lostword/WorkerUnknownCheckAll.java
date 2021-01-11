package com.pawelgorny.lostword;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.pawelgorny.lostword.util.PBKDF2SHA512;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicException;
import org.bouncycastle.crypto.macs.HMac;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WorkerUnknownCheckAll extends Worker{

    private MessageDigest LOCAL_SHA_256_DIGEST = null;
    private final HMac LOCAL_SHA_512_DIGEST = createHmacSha512Digest();
    private static final Gson GSON = new Gson();

    public WorkerUnknownCheckAll(Configuration configuration) {
        super(configuration);
        try {
            LOCAL_SHA_256_DIGEST = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getLocalizedMessage());
            System.exit(-1);
        }
    }

    public void run() throws InterruptedException {
        for (int position = 0; position < configuration.getSIZE() && RESULT == null; position++) {
            this.processPosition(position);
        }
    }

    @Override
    protected void processPosition(int position) throws InterruptedException {
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
        final List<String> SEED = new ArrayList<>(mnemonic);
        for (int bipPosition = 0; RESULT == null && bipPosition < Configuration.MNEMONIC_CODE.getWordList().size(); bipPosition++) {
            SEED.set(position, Configuration.MNEMONIC_CODE.getWordList().get(bipPosition));
            try {
                if (checkOnline(SEED, LOCAL_SHA_512_DIGEST, LOCAL_SHA_256_DIGEST)){
                    System.out.print("OK! "+Utils.SPACE_JOINER.join(SEED));
                    RESULT = new Result(SEED);
                }
            } catch (MnemonicException | IOException e) {
                System.out.println(e.getLocalizedMessage());
            }
        }
    }

    private boolean checkOnline(final List<String> mnemonic, HMac SHA512DIGEST, MessageDigest sha256) throws MnemonicException, IOException, InterruptedException {
        if (!checksumCheck(mnemonic, sha256)){
            return false;
        }
        byte[] seed = PBKDF2SHA512.derive(Utils.SPACE_JOINER.join(mnemonic).getBytes(StandardCharsets.UTF_8), SALT, 2048, 64);
        DeterministicKey deterministicKey = createMasterPrivateKey(seed, SHA512DIGEST == null ? this.SHA_512_DIGEST : SHA512DIGEST);
        DeterministicKey receiving = HDKeyDerivation.deriveChildKey(deterministicKey, configuration.getDPchild0());
        int ADDRESSES_TO_CHECK = 10;
        List<String> addresses = new ArrayList<>(ADDRESSES_TO_CHECK);
        for (int a=0; a<ADDRESSES_TO_CHECK; a++){
            DeterministicKey newAddress = HDKeyDerivation.deriveChildKey(receiving, new ChildNumber(a, configuration.isDPhard()));
            addresses.add(Address.fromKey(configuration.getNETWORK_PARAMETERS(), newAddress, configuration.getDBscriptType()).toString());
        }
        System.out.print("Checking balance of [" + Utils.SPACE_JOINER.join(mnemonic) + "] ");
        long balance = checkBalance(addresses);
        if (balance == 0){
            System.out.println(balance);
        }
        return balance>0;
    }

    private static long checkBalance(List<String> addresses) throws IOException, InterruptedException {
        String urlbalance = "https://blockchain.info/balance?active=";
        BufferedReader in;
        URL url = new URL(urlbalance + Joiner.on(",").join(addresses));
        in = new BufferedReader(new InputStreamReader(url.openStream()));
        String json="";
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            json += inputLine;
        }
        boolean x = false;
        Map<String, LinkedTreeMap> addressBalance = GSON.fromJson(json, Map.class);
        long final_balance = 0L;
        for (Map.Entry<String, LinkedTreeMap> entry:addressBalance.entrySet()){
            final_balance = ((Double)entry.getValue().get("final_balance")).longValue();
            if (final_balance>0){
                if (!x){
                    System.out.println("");
                }
                System.out.println(Utils.SPACE_JOINER.join(addresses)+" "+entry.getKey()+" "+final_balance);
                x = true;
            }
        }
        Thread.sleep(2000);
        return final_balance;
    }
}
