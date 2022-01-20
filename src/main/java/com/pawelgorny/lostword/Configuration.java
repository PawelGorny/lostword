package com.pawelgorny.lostword;

import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.SegwitAddress;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Configuration {
    final static String COMMENT_CHAR = "#";
    public final static String UNKNOWN_CHAR = "?";
    public final static Integer ETHEREUM = 60;
    final static MnemonicCode MNEMONIC_CODE = getMnemonicCode();
    private final NetworkParameters NETWORK_PARAMETERS = MainNetParams.get();
    private final String DEFAULT_PATH = "m/0/0";
    private String targetAddress;
    private int SIZE;
    private List<String> WORDS;
    private List<List<String>> WORDS_POOL;
    private int DPaccount = 0;
    private int DPaddress = -1;
    private int DPaddressMax = -1;
    private boolean DPhard = false;
    private Script.ScriptType DBscriptType = Script.ScriptType.P2PKH;
    private int knownStart = 0;

    private List<ChildNumber> derivationPath;
    private String derivationPathFull;
    private Integer coin;
    private List<ChildNumber> keyPath;

    private LegacyAddress legacyAddress;
    private byte[] legacyAddressHash;
    private SegwitAddress segwitAddress;
    private byte[] segwitAddressHash;
    private String ethereumAddress;

    private final WORK work;

    private boolean missingChecksum = false;
    private int missingChecksumLimit = -1;

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
                }  else if (Script.ScriptType.P2SH.name().equalsIgnoreCase(t[1])) {
                    DBscriptType = Script.ScriptType.P2SH;
                }
            }
            this.targetAddress = t[0].trim();
        } else {
            this.targetAddress = targetAddress.trim();
            DBscriptType = Script.ScriptType.P2PKH;
            if (this.targetAddress.startsWith("bc1")) {
                DBscriptType = Script.ScriptType.P2WPKH;
            }
            if (this.targetAddress.startsWith("3")) {
                DBscriptType = Script.ScriptType.P2SH;
            }
            if (this.targetAddress.startsWith("0x")){
                this.targetAddress = this.targetAddress.substring(2);
                this.ethereumAddress = getTargetAddress().toLowerCase();
                this.coin = Configuration.ETHEREUM;
            }
        }
        if (!WORK.ONE_UNKNOWN_CHECK_ALL.equals(work) && !WORK.PERMUTATION.equals(work) && !WORK.PRINT_SEEDS.equals(work)){
            if (this.ethereumAddress==null){
                switch (getDBscriptType()){
                    case P2PKH:
                        legacyAddress = LegacyAddress.fromBase58(getNETWORK_PARAMETERS(), getTargetAddress());
                        legacyAddressHash = legacyAddress.getHash();
                        break;
                    case P2WPKH:
                        segwitAddress = SegwitAddress.fromBech32(getNETWORK_PARAMETERS(), getTargetAddress());
                        segwitAddressHash = segwitAddress.getHash();
                        break;
                    default:
                        legacyAddress = LegacyAddress.fromBase58(getNETWORK_PARAMETERS(), getTargetAddress());
                        legacyAddressHash = legacyAddress.getHash();
                        break;
                }
            }
        }
        if (Configuration.ETHEREUM.equals(this.coin)){
            System.out.println("Using ETHEREUM");
        }else {
            System.out.println("Using script " + DBscriptType);
        }
    }

    private void parsePath(String path) {
        if (path == null || path.isEmpty()) {
            parsePath(this.DEFAULT_PATH);
            return;
        }
        derivationPathFull = path.replaceAll("'", "H").toUpperCase();
        String[] dpath = path.replaceAll("'", "").split("/");
        try {
            Integer.parseInt(dpath[dpath.length-2]);
            DPaccount = Integer.parseInt(dpath[dpath.length-2]);
            try {
                Integer.parseInt(dpath[dpath.length - 1]);
            }catch (Exception e){
                if (dpath[dpath.length - 1].contains("-")){
                    String[] dpatchAddress = dpath[dpath.length - 1].replaceAll(" ","").split("-");
                    if (dpatchAddress.length!=2){
                        parsePath(this.DEFAULT_PATH);
                    }
                    Integer.parseInt(dpatchAddress[0]);
                    Integer.parseInt(dpatchAddress[1]);
                    DPaddress = Integer.parseInt(dpatchAddress[0]);
                    DPaddressMax =  Integer.parseInt(dpatchAddress[1]);
                }
            }
            if (getDPaddress()==-1) {
                DPaddress = Integer.parseInt(dpath[dpath.length - 1]);
                DPaddressMax = DPaddress;
            }
            DPhard = path.endsWith("'");
            derivationPath = new ArrayList<>(5);
            boolean pathProvided = dpath.length>3;
            if (pathProvided){
                for (int i=1; i<dpath.length-2; i++){
                    String x = dpath[i].replaceAll("'","");
                    derivationPath.add(new ChildNumber(Integer.parseInt(x), true));
                    if (i==2){
                        this.coin = Integer.parseInt(x);
                        this.keyPath = HDUtils.parsePath(this.derivationPathFull.contains("-")?this.derivationPathFull.substring(0, this.derivationPathFull.indexOf("-")):this.derivationPathFull);
                    }
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

    public int getDPaddressMax() {
        return DPaddressMax;
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

    public byte[] getLegacyAddressHash() {
        return legacyAddressHash;
    }

    public SegwitAddress getSegwitAddress() {
        return segwitAddress;
    }

    public byte[] getSegwitAddressHash() {
        return segwitAddressHash;
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

    public Integer getCoin() {
        return coin;
    }

    public List<ChildNumber> getKeyPath() {
        return keyPath;
    }

    public String getEthereumAddress() {
        return ethereumAddress;
    }

    public String getDerivationPathFull() {
        return derivationPathFull;
    }

    public boolean isMissingChecksum() {
        return missingChecksum;
    }

    public void setMissingChecksum(boolean missingChecksum) {
        this.missingChecksum = missingChecksum;
    }

    public int getMissingChecksumLimit() {
        return missingChecksumLimit;
    }

    public void setMissingChecksumLimit(int missingChecksumLimit) {
        this.missingChecksumLimit = missingChecksumLimit;
    }
}
