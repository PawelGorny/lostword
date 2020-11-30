package com.pawelgorny.lostword;

import org.bitcoinj.crypto.MnemonicException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, MnemonicException, InterruptedException {
        if (args.length < 1 || "--help".equals(args[0])) {
            showFile("help.txt");
            showFile("footer.txt");
            System.exit(0);
        }
        Configuration configuration = readConfiguration(args[0]);
        Worker worker = new Worker(configuration);
        worker.run();
        showFile("footer.txt");
    }

    private static void showFile(String fileName) {
        String line;
        try {
            BufferedReader bufferReader = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/" + fileName)));
            while ((line = bufferReader.readLine()) != null) {
                line = line.trim();
                System.out.println(line);
            }
            bufferReader.close();
        } catch (IOException e) {
            System.err.println("error: " + e.getLocalizedMessage());
            System.exit(-1);
        }
    }

    private static Configuration readConfiguration(String file) {
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);
        } catch (FileNotFoundException e) {
            System.err.println("not found: " + file);
            System.exit(-1);
        }
        BufferedReader bufferReader = new BufferedReader(fileReader);
        String line;
        int lineNumber = 0;
        int size = 0;
        String targetAddress = null;
        List<String> words = new ArrayList<>(0);
        String path = null;
        try {
            while ((line = bufferReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(Configuration.COMMENT_CHAR)) {
                    continue;
                }
                switch (lineNumber) {
                    case 0:
                        targetAddress = line;
                        break;
                    case 1:
                        try {
                            Integer.parseInt(line);
                        } catch (NumberFormatException e) {
                            System.out.println("Cannot parse number of words '" + line + "'. Expected: 12, 16, 24...");
                            System.exit(1);
                        }
                        size = Integer.valueOf(line);
                        words = new ArrayList<>(size - 1);
                        break;
                    default:
                        if (words.size() == size - 1) {
                            path = line;
                        } else if (words.size() < size - 1) {
                            if (!Configuration.MNEMONIC_CODE.getWordList().contains(line)) {
                                System.out.println("WORD not in BIP39: " + line);
                                System.exit(1);
                            }
                            words.add(line);
                        }
                        break;
                }
                lineNumber++;
            }
            if (words.size() + 1 < size) {
                System.out.println(words.size() + " words found, expected " + (size - 1));
                System.exit(2);
            }
            if (targetAddress != null) {
                System.out.println("Expected address: '" + targetAddress + "'");
            }
        } catch (IOException e) {
            System.err.println("error: " + e.getLocalizedMessage());
            System.exit(-1);
        } finally {
            try {
                bufferReader.close();
            } catch (IOException ioe) {
                //file problem?
            }
        }
        return new Configuration(targetAddress, path, words);
    }
}
