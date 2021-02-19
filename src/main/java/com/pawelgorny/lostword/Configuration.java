package com.pawelgorny.lostword;

import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.SegwitAddress;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Configuration {
    final static String COMMENT_CHAR = "#";
    public final static String UNKNOWN_CHAR = "?";
    final static MnemonicCode MNEMONIC_CODE = getMnemonicCode();
    private final NetworkParameters NETWORK_PARAMETERS = MainNetParams.get();
    private final String DEFAULT_PATH = "m/0/0";
    private String targetAddress;
    private int SIZE;
    private List<String> WORDS;
    private List<List<String>> WORDS_POOL;
    private int DPaccount = 0;
    private int DPaddress = 0;
    private boolean DPhard = false;
    private Script.ScriptType DBscriptType = Script.ScriptType.P2PKH;
    private int knownStart = 0;

    private List<ChildNumber> derivationPath;

    private LegacyAddress legacyAddress;
    private SegwitAddress segwitAddress;

    private final WORK work;

    public Configuration(WORK work, String targetAddress, String path, List<String> words, int knownStarter) {
        this.work = work;
        this.WORDS = words;
        this.SIZE = words.size();
        if (WORK.ONE_UNKNOWN.equals(work) || WORK.ONE_UNKNOWN_CHECK_ALL.equals(work)){
            this.SIZE++;
        }
        parseScript(targetAddress);
        parsePath(path);
        this.knownStart = knownStarter;
    }

    private static MnemonicCode getMnemonicCode() {
        try {
            return new MnemonicCode();
        } catch (Exception e) {
            return null;
        }
    }

    private void parseScript(String targetAddress) {
        if (targetAddress.contains(",")) {
            String[] t = targetAddress.split(",");
            if (t[1] != null) {
                t[1] = t[1].trim();
                if (Script.ScriptType.P2PKH.name().equalsIgnoreCase(t[1].trim())) {
                    DBscriptType = Script.ScriptType.P2PKH;
                } else if (Script.ScriptType.P2WPKH.name().equalsIgnoreCase(t[1])) {
                    DBscriptType = Script.ScriptType.P2WPKH;
                }
            }
            this.targetAddress = t[0].trim();
        } else {
            this.targetAddress = targetAddress.trim();
            DBscriptType = Script.ScriptType.P2PKH;
            if (this.targetAddress.startsWith("bc1")) {
                DBscriptType = Script.ScriptType.P2WPKH;
            }
        }
        if (!WORK.ONE_UNKNOWN_CHECK_ALL.equals(work)){
            switch (getDBscriptType()){
                case P2PKH:
                    legacyAddress = LegacyAddress.fromBase58(getNETWORK_PARAMETERS(), getTargetAddress());
                    break;
                case P2WPKH:
                    segwitAddress = SegwitAddress.fromBech32(getNETWORK_PARAMETERS(), getTargetAddress());
                    break;
            }
        }
        System.out.println("Using script " + DBscriptType);
    }

    private void parsePath(String path) {
        if (path == null || path.isEmpty()) {
            parsePath(this.DEFAULT_PATH);
            return;
        }
        String[] dpath = path.replaceAll("'", "").split("/");
        try {
            Integer.parseInt(dpath[dpath.length-2]);
            Integer.parseInt(dpath[dpath.length-1]);
            DPaccount = Integer.parseInt(dpath[dpath.length-2]);
            DPaddress = Integer.parseInt(dpath[dpath.length-1]);
            DPhard = path.endsWith("'");
            derivationPath = new ArrayList<>(5);
            boolean pathProvided = dpath.length>3;
            if (pathProvided){
                for (int i=1; i<dpath.length-2; i++){
                    String x = dpath[i].replaceAll("'","");
                    derivationPath.add(new ChildNumber(Integer.parseInt(x), true));
                }
            }
            derivationPath.add(new ChildNumber(getDPaccount(), false));
            derivationPath.add(new ChildNumber(getDPaddress(), isDPhard()));
            String derivationPathString = "m/"+derivationPath.stream().map(Object::toString)
                    .collect(Collectors.joining("/")).replaceAll("H", "'");
            System.out.println("Using derivation path " + derivationPathString);
        } catch (Exception e) {
            parsePath(this.DEFAULT_PATH);
        }
    }

    public int getSIZE() {
        return SIZE;
    }

    public List<String> getWORDS() {
        return WORDS;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public NetworkParameters getNETWORK_PARAMETERS() {
        return NETWORK_PARAMETERS;
    }

    public int getDPaccount() {
        return DPaccount;
    }

    public int getDPaddress() {
        return DPaddress;
    }

    public boolean isDPhard() {
        return DPhard;
    }

    public Script.ScriptType getDBscriptType() {
        return DBscriptType;
    }

    public WORK getWork() {
        return work;
    }

    public int getKnownStart() {
        return knownStart;
    }

    public LegacyAddress getLegacyAddress() {
        return legacyAddress;
    }

    public void setLegacyAddress(LegacyAddress legacyAddress) {
        this.legacyAddress = legacyAddress;
    }

    public SegwitAddress getSegwitAddress() {
        return segwitAddress;
    }

    public void setSegwitAddress(SegwitAddress segwitAddress) {
        this.segwitAddress = segwitAddress;
    }

    public List<List<String>> getWORDS_POOL() {
        return WORDS_POOL;
    }

    public List<ChildNumber> getDerivationPath() {
        return derivationPath;
    }

    public void setWORDS_POOL(List<List<String>> WORDS_POOL) {
        this.WORDS_POOL = WORDS_POOL;
        this.SIZE = WORDS_POOL.size();
    }
}
