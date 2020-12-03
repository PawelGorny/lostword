import com.google.common.base.Stopwatch;
import com.pawelgorny.lostword.Configuration;
import com.pawelgorny.lostword.WORK;
import com.pawelgorny.lostword.Worker;
import org.bitcoinj.crypto.MnemonicException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class Test {

    private static final int LOOP_SIZE = 500;
    private static final String TARGET = "1AcuLxsQSMTi6fLEbJ6F6sNsZ4NyqnUNSo";
    private static final List<String> RESULT = new ArrayList(){{add("brother");add("canal");add("medal");
        add("remove");add("pitch");add("hill");}};
    private static final String PATH = "m/0/2'";

    private static final String TARGET2 = "bc1qfnmxy77huxfyashpjquxhgv9urw506s2nmzs5t";
    private static final List<String> RESULT2 = new ArrayList(){{add("abandon");add("ability");add("ability");
        add("home");add("car");add("test");}};
    private static final String PATH2 = "m/0/1";

    @org.junit.Test
    public void testCheck() throws MnemonicException {
        Configuration configuration = new Configuration(null, TARGET, PATH, RESULT, 0);
        WorkerTester workerTester = new WorkerTester(configuration);
        assertTrue(workerTester.check(RESULT));
        System.out.println("worker.check OK");
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (int i=0; i<LOOP_SIZE; i++){
            workerTester.check(RESULT);
        }
        stopwatch.stop();
        System.out.println(stopwatch.elapsed());
    }

    @org.junit.Test
    public void testONE_UNKNOWN() throws MnemonicException, InterruptedException {
        List<String> words = RESULT.subList(1,RESULT.size());
        Configuration configuration = new Configuration(WORK.ONE_UNKNOWN, TARGET, "m/0/2'", words, 0);
        WorkerTester workerTester = new WorkerTester(configuration);
        ((Worker)workerTester).run();
        assertTrue(workerTester.isResult());
    }

    @org.junit.Test
    public void testKNOWN_POSITION1() throws MnemonicException, InterruptedException {
        List<String> words = new ArrayList<>(RESULT);
        words.set(0,Configuration.UNKNOWN_CHAR);
        Configuration configuration = new Configuration(WORK.KNOWN_POSITION, TARGET, PATH, words, 0);
        WorkerTester workerTester = new WorkerTester(configuration);
        ((Worker)workerTester).run();
        assertTrue(workerTester.isResult());
    }

    @org.junit.Test
    public void testKNOWN_POSITION2() throws MnemonicException, InterruptedException {
        List<String> words = new ArrayList<>(RESULT2);
        words.set(2,Configuration.UNKNOWN_CHAR);
        words.set(1,Configuration.UNKNOWN_CHAR);
        Configuration configuration = new Configuration(WORK.KNOWN_POSITION, TARGET2, PATH2, words, 0);
        WorkerTester workerTester = new WorkerTester(configuration);
        ((Worker)workerTester).run();
        assertTrue(workerTester.isResult());
    }

    private class WorkerTester extends Worker{
        public WorkerTester(Configuration configuration) {
            super(configuration);
        }

        @Override
        protected void processPosition(int position) throws InterruptedException {
            super.processPosition(position);
        }

        @Override
        protected List<List<String>> split() {
            return super.split();
        }

        @Override
        protected boolean check(List<String> mnemonic) throws MnemonicException {
            return super.check(mnemonic);
        }

        public boolean isResult(){
            return RESULT!=null;
        }


    }
}
