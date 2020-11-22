package com.pawelgorny.lostword;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Main {

    private static MnemonicCode MNEMONIC_CODE;
    private static String TARGET;
    private static String RESULT = null;

    private static final NetworkParameters PARAMETERS = MainNetParams.get();
    private static final int SIZE = 24;

    private static List<String> WORDS = new ArrayList<>(SIZE-1);

    public static void main(String[] args) throws IOException, MnemonicException {
        if (args==null || args.length!=SIZE){
            System.out.println("usage: java -jar lostWord.jar ADDRESS word1 word2 ... word23");
            System.exit(1);
        }
        MNEMONIC_CODE = new MnemonicCode();
        TARGET = args[0];
        for (int w=1; w<args.length; w++){
            if (!MNEMONIC_CODE.getWordList().contains(args[w])){
                System.out.println("WORD not in BIP39: "+args[w]);
                System.exit(2);
            }
            WORDS.add(args[w]);
        }
        int position = check();
        if (RESULT==null){
            System.out.println("not found");
            System.exit(0);
        }
        System.out.println("MISSING: "+RESULT+" at position: "+position);
        System.exit(0);
    }

    private static int check() throws MnemonicException {
        List<String> mnemonic = new ArrayList<>(SIZE);
        for (int i=0;i<SIZE;i++){
            mnemonic.add("");
        }
        for (int position=0; position<=SIZE; position++){
            System.out.println("Checking missing words at position "+(position+1));
            Iterator<String> iterator = WORDS.iterator();
            int p = 0;
            while (iterator.hasNext()){
                if (p==position){
                    p++;
                }
                mnemonic.set(p++, iterator.next());
            }
            for (int bipPosition=0; bipPosition<MNEMONIC_CODE.getWordList().size(); bipPosition++){
                if (bipPosition%100==0){
                    System.out.println("Checking possible word nr "+(bipPosition+1));
                }
                mnemonic.set(position, MNEMONIC_CODE.getWordList().get(bipPosition));
                if (check(mnemonic)){
                    RESULT = MNEMONIC_CODE.getWordList().get(bipPosition);
                    return 1+position;
                }
            }
        }
        return -1;
    }

    private static boolean check(List<String> mnemonic) throws MnemonicException {
        byte[] seed = MnemonicCode.toSeed(mnemonic, "");
        DeterministicKey deterministicKey = HDKeyDerivation.createMasterPrivateKey(seed);
        DeterministicKey receiving = HDKeyDerivation.deriveChildKey(deterministicKey, new ChildNumber(0, false));
        DeterministicKey new_address_key = HDKeyDerivation.deriveChildKey(receiving, new ChildNumber(0, false));
        if (TARGET.equalsIgnoreCase(Address.fromKey(PARAMETERS, new_address_key, Script.ScriptType.P2PKH).toString())){
            System.out.println(mnemonic);
            return true;
        }
        return false;
    }
}
