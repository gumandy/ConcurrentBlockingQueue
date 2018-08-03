package cn.gg.fastqueue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 *
 * @author gumandy
 * @date 2018年4月21日 下午3:16:14
 * @version 1.0
 *
 */
public class Test {
    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<String> q1 = new ConcurrentBlockingQueue<>(100);
        BlockingQueue<String> q2 = new LinkedBlockingQueue<>(100);
        long loop = 10000_000;
        long s1 = tps(q1, loop);
        System.out.println(s1);
        long s2 = tps(q2, loop);
        System.out.println(s2);
    }

    static long tps(final BlockingQueue<String> queue, final long loop)
            throws InterruptedException {
        Thread t1 = new Thread("t1") {
            @Override
            public void run() {
                for (int i = 0; i < loop; i++) {
                    try {
                        queue.put("t1_" + i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread t2 = new Thread("t2") {
            @Override
            public void run() {
                for (int i = 0; i < loop; i++) {
                    try {
                        queue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        long s = System.nanoTime();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        long e = System.nanoTime();

        return loop * 1000_000_000 / (e - s);
    }
}
