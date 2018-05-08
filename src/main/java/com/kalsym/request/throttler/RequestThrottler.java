package com.kalsym.request.throttler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple BlockingQueue based rate limiter. Usage: call limit() to throttle
 * the current thread (blocks) and call tick() at regular intervals from a
 * separate thread.
 */
public class RequestThrottler {

    private static Logger logger = LoggerFactory.getLogger("application");

//    private final long fillPeriod;
    private final BlockingQueue<Object> queue;
    private long timer;

//    public RequestThrottler(int capacity){
//         this.queue = new ArrayBlockingQueue<Object>(capacity);
//    }
    /**
     * Create a simple blocking queue based rate limiter with a certain capacity
     * and fill rate. Be careful when handling lots of requests with a high
     * capacity as memory usage scales with capacity.
     *
     * @param capacity capacity before rate limiting kicks in
     * @param rate rate limit in allowed calls per second
     */
    public RequestThrottler(int capacity) {
//        if (rate <= 0) {
//            this.fillPeriod = Long.MAX_VALUE;
//        } else {
//            this.fillPeriod = (long) (1000000000L / rate);
//        }
        this.queue = new ArrayBlockingQueue<Object>(capacity);
        logger.info("Created queue with capacity: " + capacity);
        this.timer = System.nanoTime();
    }

    public int checkListCount() {
        return queue.size();
    }

    /**
     * Tick the rate limiter, advancing the timer and possibly unblocking calls
     * to limit()
     */
    public synchronized List extractRequests(int numberOfRequests) {
        long elapsedTime = System.nanoTime() - timer;
//        int numToRemove = (int) (elapsedTime / fillPeriod);
//
//        // advance timer
//        timer += fillPeriod * numToRemove;

        List<Object> bulkRequests = new ArrayList<Object>(numberOfRequests);
        queue.drainTo(bulkRequests, numberOfRequests);
        return bulkRequests;
    }

    /**
     * A call to this method blocks when it is called too often (depleted
     * capacity).
     *
     * @param requestToAddInQueue
     * @return false when interrupted, otherwise true
     */
    public boolean addRequest(Object requestToAddInQueue) {
        try {
            queue.put(requestToAddInQueue);
            logger.debug("Request added in queue, new size: " + queue.size());
        } catch (InterruptedException e) {
            logger.error("Could not add request", e);
            return false;
        }
        return true;
    }
}
