package com.kalsym.request.throttler;

import com.kalsym.utility.XMLReader;
import java.util.List;
import com.kalsym.utility.StringUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ali Khan
 */
public class ProcessRequest implements com.kalsym.persistentserver.ProcessRequest {

    private static Logger logger = LoggerFactory.getLogger("application");

    // schedule rate limiter ticks every 100 milliseconds
    // ensures single threaded execution, if the existing executor takes longer to execute than the scheduled time then the next executor is not run untill the previous is finished. In this case the next schedule is executed immediatly
    final RequestThrottler rt = new RequestThrottler((Main.tps * 10));

    public String testMethod() {
        return "success processRequest";
    }

    @Override
    public String doProcess(final String xmlRequest) {
        try {
            rt.addRequest(xmlRequest);
            logger.debug("Received request: " + xmlRequest);
            return "Processing request started";
        } catch (Exception exp) {
            logger.error("doProcess error", exp);
            return "error";
        }
    }

    public void extractRequests(int requestCount) {
        List requests = rt.extractRequests(requestCount);
        int extractedCount = requests.size();
        for (int i = 0; i < requests.size(); i++) {
            final String xmlRequest = (String) requests.get(i);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    doProcessThreaded(xmlRequest);
                }
            }).start();
        }
        logger.debug(extractedCount + " requests ready to be sent");
    }

    public void doProcessThreaded(String xmlRequest) {
        String opcode = "";
        String refId = "";
        String aMsisdn = "";
        String bMsisdn = "";
        String aOrBMsisdn = "";
        String serverId = "";
        String isSameTelco = "";
        String msg = "";
        long aParty = -1;
        long bParty = -1;
        long msisdn = -1;
        try {
            //<xmlRequest><function>processExpiredSms</function><aMsisdn>923455210120</aMsisdn><bMsisdn>923338017544</bMsisdn><serverId>1</serverId><msg>Hello are you there</msg></xmlRequest>
            logger.debug("Sending request: " + xmlRequest);
            xmlRequest = xmlRequest.replaceAll("\\p{Cntrl}", "");
            XMLReader xmlProcess = new XMLReader(xmlRequest);
            xmlProcess.load();
            opcode = xmlProcess.readOneElement("function");
            refId = xmlProcess.readOneElement("refId");
            aMsisdn = xmlProcess.readOneElement("aMsisdn");
            bMsisdn = xmlProcess.readOneElement("bMsisdn");
            serverId = xmlProcess.readOneElement("serverId");
            isSameTelco = xmlProcess.readOneElement("isOnnet");
            msg = xmlProcess.readOneElement("msg");
            aOrBMsisdn = xmlProcess.readOneElement("msisdn");

            if ("".equalsIgnoreCase(refId)) {
                refId = xmlProcess.readOneElement("RefID");
            }

            if (!"".equalsIgnoreCase(aMsisdn)) {
                aParty = Long.parseLong(aMsisdn);
            }
            if (!"".equalsIgnoreCase(bMsisdn)) {
                bParty = Long.parseLong(bMsisdn);
            }
            if (opcode.equalsIgnoreCase("moRequest")) {
                logger.debug("[" + refId + "] Processing MO Request...");
                aMsisdn = xmlProcess.readOneElement("sender");
                bMsisdn = xmlProcess.readOneElement("dest");
                msg = xmlProcess.readOneElement("message");
                if (!"".equalsIgnoreCase(aMsisdn)) {
                    aParty = Long.parseLong(aMsisdn);
                }
                //utilities.writeCDR(refId, opcode, "IN", "" + aParty, bMsisdn, msg, "", "", "");
                if (!"".equalsIgnoreCase(bMsisdn)) {
//                    if (bMsisdn.startsWith("66")) {
//                        bMsisdn = Utilities.normalizeNumberWithCountryCode(bMsisdn.substring(2), "92");
//                    }
                    bParty = Long.parseLong(bMsisdn);
                }
                if ("".equalsIgnoreCase(refId)) {
                    refId = StringUtility.createRefId("SSMS");
                }

//                Utilities.processTruncatedSMSRequest(Long.toString(aParty), Long.toString(bParty), refId, serverId, msg);
                Utilities.processTruncatedSMSRequest(""+aParty, ""+bParty, refId, serverId, msg);
            }
        } catch (Exception ex) {
            logger.error("[" + refId + "] Error in ProcessRequest funtion", ex);
//            logger.log(Level.SEVERE, "[" + refId + "] Error in ProcessRequest funtion", ex);
//            return "Error in parsing xml";
        }
    }
}
