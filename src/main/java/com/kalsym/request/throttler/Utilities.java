package com.kalsym.request.throttler;

import com.kalsym.utility.ConfigReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ali Khan
 */
public class Utilities {

    private static Logger logger = LoggerFactory.getLogger("application");

    private static String url = "http://127.0.0.1:7070";

    public static void setURL(String forwardURL) {
        url = forwardURL;
        logger.info("[" + Main.VERSION + "] Froward URL set to: [" + url + "]");
    }

    public static String normalizeNumberWithCountryCode(String msisdn, String countryCode) {
        msisdn = msisdn.trim();
        if (msisdn.startsWith("0")) {
            msisdn = msisdn.substring(1);
        }
        if (!msisdn.startsWith(countryCode)) {
            msisdn = countryCode + msisdn;
        }
        return msisdn;
    }

    public static void processTruncatedSMSRequest(String aParty, String bParty, String refId, String serverId, String msg) throws IOException {
        try {
            StringBuilder restReqURL = new StringBuilder(url).append("/moRequest?aParty=").append(aParty).append("&bParty=").append(bParty).append("&refId=").append(refId).append("&serverId=").append(serverId).append("&message=").append(msg);
            logger.debug("Sending HTTP GET REST request: " + restReqURL.toString());
            sendGETReq(restReqURL.toString());
        } catch (Exception exp) {
            logger.error("Error in processing TruncatedSMSRequest ", exp);
        }
    }

    public static void sendGETReq(String url) throws IOException {
        URL obj = new URL(url);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");

        con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        con.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        con.setRequestProperty("Cache-Control", "max-age=0");
        con.setRequestProperty("Connection", "keep-alive");
//        con.setRequestProperty("Host", "e5c906ac.ngrok.io");
        con.setRequestProperty("If-None-Match", "W/\"2-nOO9QiTIwXgNtWtBJezz8kv3SLc");
        con.setRequestProperty("Upgrade-Insecure-Requests", "1");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
        con.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        con.setRequestProperty("Keep-Alive", "115");

        logger.debug("Sending HTTP_GET request to: " + url);
        int responseCode = con.getResponseCode();
        logger.debug("GET Response Code :: " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result
            logger.info(response.toString());
        } else {
            logger.error("GET request did not work");
        }
    }
}
