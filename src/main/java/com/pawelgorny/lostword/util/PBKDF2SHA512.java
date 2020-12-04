package com.pawelgorny.lostword.util;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;

public class PBKDF2SHA512 {
    public PBKDF2SHA512() {
    }

    public static byte[] derive(byte[] mnemonicBytes, byte[] salt, int c, int dkLen) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec key = new SecretKeySpec(mnemonicBytes, mac.getAlgorithm());
            mac.init(key);

            for(int i = 1; i <= 4; ++i) {
                byte[] T = F(salt, c, i, mac);
                baos.write(T);
            }
        } catch (Exception var9) {
            throw new RuntimeException(var9);
        }

        byte[] var10 = new byte[dkLen];
        System.arraycopy(baos.toByteArray(), 0, var10, 0, var10.length);

        return var10;
    }

    private static byte[] F(byte[] salt, int c, int i, Mac mac) throws Exception {
        byte[] U_LAST = null;
        byte[] U_XOR = null;
        for(int j = 0; j < c; ++j) {
            byte[] baU;
            if(j == 0) {
                baU = salt;
                byte[] var12 = INT(i);
                byte[] baU1 = new byte[12];
                System.arraycopy(baU, 0, baU1, 0, 8);
                System.arraycopy(var12, 0, baU1, 8, 4);
                U_XOR = mac.doFinal(baU1);
                U_LAST = U_XOR;
            } else {
                baU = mac.doFinal(U_LAST);
                for(int k = 0; k < U_XOR.length; ++k) {
                    U_XOR[k] ^= baU[k];
                }
                U_LAST = baU;
            }
            mac.reset();
        }

        return U_XOR;
    }

    private static byte[] INT(int i) {
        byte[] bytes = new byte[4];
        bytes[3] = (byte)i;
        return bytes;
//        ByteBuffer bb = ByteBuffer.allocate(4);
//        bb.order(ByteOrder.BIG_ENDIAN);
//        bb.putInt(i);
//        return bb.array();
    }
}