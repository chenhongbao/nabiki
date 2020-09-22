package com.nabiki.ctp4j;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class CThostFtdcMdApiTest {
    private static final Logger logger = Logger.getLogger(CThostFtdcTraderApiTest.class.getName());

    static {
        System.loadLibrary("thostmduserapi_se");
        System.loadLibrary("thosttraderapi_se");
        System.loadLibrary("thostctpapi_se-6.3.19-P1");
    }

    class TestMdSpi extends CThostFtdcMdSpi {
        private final CThostFtdcMdApi api;

        private final String brokerID = "6020";
        private final String userID = "100912699";
        private final String password = "chb_1987_1013";
        private final String appID = "client_UTP_3.0.0";
        private final String authCode = "5DE2NSQZ8122UXW6";
        private final String userProductInfo = "_md_";

        private final Lock lock = new ReentrantLock();
        private final Condition cond = lock.newCondition();

        private int requestID = 0;

        TestMdSpi(CThostFtdcMdApi api) {
            this.api = api;
        }

        @Override
        public void OnFrontConnected() {
            logger.info("connected");
            signal();
        }

        @Override
        public void OnFrontDisconnected(int nReason) {
            logger.info("disconnected(" + nReason + ")");
        }

        @Override
        public void OnHeartBeatWarning(int nTimeLapse) {
            logger.info("heartbeat warning(" + nTimeLapse + ")");
        }

        @Override
        public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
            if (pRspInfo != null && pRspInfo.getErrorID() != 0)
                logger.severe("login failed[" + pRspInfo.getErrorID() + "]: " + pRspInfo.getErrorMsg());
            logger.info("login");
            signal();
        }

        @Override
        public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
            if (pRspInfo != null && pRspInfo.getErrorID() != 0)
                logger.severe("logout failed[" + pRspInfo.getErrorID() + "]: " + pRspInfo.getErrorMsg());
            logger.info("logout");
            signal();
        }

        @Override
        public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
            if (pRspInfo == null)
                logger.severe("rsp info null");
            logger.severe("failed[" + pRspInfo.getErrorID() + "]: " + pRspInfo.getErrorMsg());
            signal();
        }

        @Override
        public void OnRspSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
            if (pRspInfo.getErrorID() != 0)
                logger.severe("subscription failed");
            logger.info("sub rsp " + pSpecificInstrument.getInstrumentID());
            signal();
        }

        @Override
        public void OnRspUnSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
            if (pRspInfo.getErrorID() != 0)
                logger.severe("subscription failed");
            logger.info("un-sub rsp " + pSpecificInstrument.getInstrumentID());
            signal();
        }

        @Override
        public void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField pDepthMarketData) {
            logger.info(pDepthMarketData.getInstrumentID() + ", " + pDepthMarketData.getUpdateTime());
        }

        int login() {
            var field = new CThostFtdcReqUserLoginField();
            field.setBrokerID(this.brokerID);
            field.setUserID(this.userID);
            field.setPassword(this.password);
            field.setUserProductInfo(this.userProductInfo);
            return this.api.ReqUserLogin(field, ++this.requestID);
        }

        int logout() {
            var field = new CThostFtdcUserLogoutField();
            field.setBrokerID(this.brokerID);
            field.setUserID(this.userID);
            return this.api.ReqUserLogout(field, ++this.requestID);
        }

        int subscribe(String instrumentID) {
            String[] req = new String[1];
            req[0] = instrumentID;
            return this.api.SubscribeMarketData(req, 1);
        }

        int unSubscribe(String instrumentID) {
            String[] req = new String[1];
            req[0] = instrumentID;
            return this.api.UnSubscribeMarketData(req, 1);
        }

        boolean waitRsp(long millis) {
            this.lock.lock();
            try {
                return this.cond.await(millis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            } finally {
                this.lock.unlock();
            }
        }

        void signal() {
            this.lock.lock();
            try {
                this.cond.signal();
            } finally {
                this.lock.unlock();
            }
        }
    }

    private boolean waitMillis(int millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.severe("sleep interrupted");
            return false;
        }
    }


    @Test
    public void basic() {
        var api = CThostFtdcMdApi.CreateFtdcMdApi("md/");
        var spi = new TestMdSpi(api);

        boolean[] released = new boolean[1];
        released[0] = false;

        api.RegisterSpi(spi);
        api.RegisterFront("tcp://180.166.37.178:41215");
        api.Init();

        int r = 0;

        if (!spi.waitRsp(2000))
            logger.severe("connect timeout");

        waitMillis(2000);

        spi.login();
        if (!spi.waitRsp(2000))
            logger.severe("login timeout");

        spi.subscribe("c2101");
        if (!spi.waitRsp(2000))
            logger.severe("sub timeout");

        waitMillis(5 * 2000);

        spi.unSubscribe("c2101");
        if (!spi.waitRsp(2000))
            logger.severe("un-sub timeout");

        waitMillis(2000);

        spi.logout();
        if (!spi.waitRsp(2000))
            logger.severe("logout timeout");

        // Wait release.
        waitMillis(100);
    }
}