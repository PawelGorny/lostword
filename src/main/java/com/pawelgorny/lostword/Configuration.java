package com.pawelgorny.lostword;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;

import java.util.List;

public final class Configuration {
    final static String COMMENT_CHAR = "#";
    final static MnemonicCode MNEMONIC_CODE = getMnemonicCode();
    private final NetworkParameters NETWORK_PARAMETERS = MainNetParams.get();
    private final String DEFAULT_PATH = "m/0/0";
    private String targetAddress;
    private int SIZE;
    private List<String> WORDS;
    private int DPaccount = 0;
    private int DPaddress = 0;
    private boolean DPhard = false;
    private Script.ScriptType DBscriptType = Script.ScriptType.P2PKH;

    public Configuration(String targetAddress, String path, List<String> words) {
        this.WORDS = words;
        this.SIZE = words.size() + 1;
        parsePath(path);
        parseScript(targetAddress);
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
        System.out.println("Using script " + DBscriptType);
    }

    private void parsePath(String path) {
        if (path == null || path.isEmpty()) {
            parsePath(this.DEFAULT_PATH);
            return;
        }
        String[] dpath = path.replaceAll("'", "").split("/");
        try {
            Integer.parseInt(dpath[1]);
            Integer.parseInt(dpath[2]);
            DPaccount = Integer.parseInt(dpath[1]);
            DPaddress = Integer.parseInt(dpath[2]);
            DPhard = path.endsWith("'");
            System.out.println("Using derivation path " + path);
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
}
