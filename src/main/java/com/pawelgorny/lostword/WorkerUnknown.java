package com.pawelgorny.lostword;

public class WorkerUnknown  extends Worker{

    public WorkerUnknown(Configuration configuration) {
        super(configuration);
    }

    public void run() throws InterruptedException {
        for (int position = 0; position <= configuration.getSIZE() && RESULT == null; position++) {
            processPosition(position);
        }
    }
}
