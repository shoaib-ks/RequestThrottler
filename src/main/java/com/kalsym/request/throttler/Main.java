package com.kalsym.request.throttler;

import com.kalsym.persistentserver.PersistentServer;
import com.kalsym.utility.ConfigReader;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ali Khan
 */
public class Main {

    private static Logger logger = LoggerFactory.getLogger("application");

    ProcessRequest processRequest;

    static int tps = 2;

    public static final String VERSION = "1.1";

    public static void mainOLD(String[] args) throws InterruptedException {
        // capacity of 10 and a rate of 1/second
//        final RequestThrottler limiter = new RequestThrottler(10, 1);
        logger.debug("starting ...");
        try {
            BlockingQueue bq = new ArrayBlockingQueue(1);
            bq.add(bq);
            bq.add(bq);
        } catch (Exception exp) {
            logger.error("Exception", exp);
        }
        // schedule rate limiter ticks every 100 milliseconds
        // ensures single threaded execution, if the existing executor takes longer to execute than the scheduled time then the next executor is not run untill the previous is finished. In this case the next schedule is executed immediatly
        final RequestThrottler rt = new RequestThrottler(100);

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 1; i <= Integer.MAX_VALUE; i++) {
//                        Thread.sleep(10);
                        rt.addRequest(i);
                    }
                    logger.debug("All Reqeusts finished");
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        t1.start();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
//				limiter.tick();
                logger.debug("doing some blah blah...");
                try {
                    List reqs = rt.extractRequests(10);
                    logger.debug("extracted requests size: " + reqs.size());
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
                logger.debug("done doing some blah blah...");
            }
         ;}, 0, 1000, TimeUnit.MILLISECONDS);

        // bark 100 times, but limit using the rate limiter
        for (int n = 0; n < 100; n += 1) {
//            limiter.limit();
            //System.out.println("bark #" + n);
        }

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(1000);
                        logger.debug("Quering List Count: " + rt.checkListCount());
                    }
                } catch (InterruptedException ex) {
                    logger.error("Exception in getting list count");
                }
            }
        });

        t2.start();
    }

//    Main m;
    public static void main(String[] args) {
        String configFile = System.getProperty("request-throttler.configuration", "request-throttler.properties");
        ConfigReader.init(configFile);

        tps = ConfigReader.getPropertyAsInt("forward.request.tps", 2);
        
        String forwardURL = ConfigReader.getProperty("forward.request.http.url", "http://127.0.0.1:7070");
        if(forwardURL.trim().endsWith("/")){
            forwardURL = forwardURL.substring(0, forwardURL.lastIndexOf("/"));
        }
        Utilities.setURL(forwardURL);
        Main m = new Main();
        m.processRequest = new ProcessRequest();
        m.requestThrottler(tps);
        m.startPersistentServer();
    }

    public void requestThrottler(final int tps) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    processRequest.extractRequests(tps);
                } catch (Exception ex) {
                    logger.error("Exception exp ", ex);
                }
            }
         ;}, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public static PersistentServer persistentServer;

    private void startPersistentServer() {
        int persistentPort = Integer.parseInt(ConfigReader.getProperty("persistent_port", "5533"));
        int persistentPoolSize = Integer.parseInt(ConfigReader.getProperty("persistent_pool_size"));
        int persistentMaxPoolSize = Integer.parseInt(ConfigReader.getProperty("persistent_max_pool_size"));
        long persistentKeepAliveTime = Long.parseLong(ConfigReader.getProperty("persistent_keep_alive_time", "1000000"));
        persistentServer = new PersistentServer(persistentPort, persistentPoolSize, persistentMaxPoolSize, persistentKeepAliveTime, processRequest);
        persistentServer.start();
        logger.info("[" + VERSION + "] Request Throttler Persistent Server started, listerning on Port: " + persistentPort);
    }
}
