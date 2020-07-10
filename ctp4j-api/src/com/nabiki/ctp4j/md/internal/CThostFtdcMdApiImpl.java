package com.nabiki.ctp4j.md.internal;

import com.nabiki.ctp4j.jni.struct.CThostFtdcReqUserLoginField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcUserLogoutField;
import com.nabiki.ctp4j.md.CThostFtdcMdApi;
import com.nabiki.ctp4j.md.CThostFtdcMdSpi;

public class CThostFtdcMdApiImpl extends CThostFtdcMdApi {
    public CThostFtdcMdApiImpl(String flowPath, boolean isUsingUdp,
                               boolean isMulticast) {
        throw new UnsupportedOperationException("CThostFtdcMdApi not implemented");
    }

    @Override
    public String GetApiVersion() {
        return null;
    }

    @Override
    public String GetTradingDay() {
        return null;
    }

    @Override
    public void Init() {

    }

    @Override
    public void Join() {

    }

    @Override
    public void RegisterFront(String frontAddress) {

    }

    @Override
    public void RegisterSpi(CThostFtdcMdSpi spi) {

    }

    @Override
    public void Release() {

    }

    @Override
    public int ReqUserLogin(CThostFtdcReqUserLoginField reqUserLoginField,
                            int requestID) {
        return 0;
    }

    @Override
    public int ReqUserLogout(CThostFtdcUserLogoutField userLogout, int requestID) {
        return 0;
    }

    @Override
    public int SubscribeMarketData(String[] instrumentID, int count) {
        return 0;
    }

    @Override
    public int UnSubscribeMarketData(String[] instrumentID, int count) {
        return 0;
    }
}
