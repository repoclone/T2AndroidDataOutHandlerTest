package com.t2.dataouthandlertest;

import android.os.Looper;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {
    private static ThreadPoolExecutor sExecutor;
    static {
        sExecutor = new ThreadPoolExecutor(0, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10));

        // wrap the thread pool's thread factory in a thing that badges the names of its threads with a
        // prefix so that they can be identified while debugg
        final ThreadFactory originalFactory = sExecutor.getThreadFactory();
        sExecutor.setThreadFactory(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread t = originalFactory.newThread(runnable);
                t.setName("JREngage-ThreadUtils~" + t.getName());
                return t;
            }
        });
    }

    public static void executeInBg(Runnable r) {
        if (Looper.myLooper() == null) {
            r.run();
        } else {
            sExecutor.execute(r);
        }
    }
}
