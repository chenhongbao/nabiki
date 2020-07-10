package com.nabiki.ctp4j.trader.internal;

import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.ctp4j.trader.CThostFtdcTraderApi;
import com.nabiki.ctp4j.trader.CThostFtdcTraderSpi;

public class CThostFtdcTraderApiImpl extends CThostFtdcTraderApi {
    public CThostFtdcTraderApiImpl(String flowPath) {
        throw new UnsupportedOperationException(
                "CThostFtdcTraderApi not implemented");
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
    public void SubscribePrivateTopic(int type) {

    }

    @Override
    public void SubscribePublicTopic(int type) {

    }

    @Override
    public void RegisterFront(String frontAddress) {

    }

    @Override
    public void RegisterSpi(CThostFtdcTraderSpi spi) {

    }

    @Override
    public void Release() {

    }

    @Override
    public int ReqAuthenticate(CThostFtdcReqAuthenticateField reqAuthenticateField, int requestID) {
        return 0;
    }

    @Override
    public int ReqUserLogin(CThostFtdcReqUserLoginField reqUserLoginField, int requestID) {
        return 0;
    }

    @Override
    public int ReqUserLogout(CThostFtdcUserLogoutField userLogout, int requestID) {
        return 0;
    }

    @Override
    public int ReqSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField settlementInfoConfirm, int requestID) {
        return 0;
    }

    @Override
    public int ReqOrderInsert(CThostFtdcInputOrderField inputOrder, int requestID) {
        return 0;
    }

    @Override
    public int ReqOrderAction(CThostFtdcInputOrderActionField inputOrderAction, int requestID) {
        return 0;
    }

    @Override
    public int ReqQryInstrument(CThostFtdcQryInstrumentField qryInstrument, int requestID) {
        return 0;
    }

    @Override
    public int ReqQryInstrumentCommissionRate(CThostFtdcQryInstrumentCommissionRateField qryInstrumentCommissionRate, int requestID) {
        return 0;
    }

    @Override
    public int ReqQryInstrumentMarginRate(CThostFtdcQryInstrumentMarginRateField qryInstrumentMarginRate, int requestID) {
        return 0;
    }

    @Override
    public int ReqQryTradingAccount(CThostFtdcQryTradingAccountField qryTradingAccount, int requestID) {
        return 0;
    }

    @Override
    public int ReqQryInvestorPositionDetail(CThostFtdcQryInvestorPositionDetailField qryInvestorPositionDetail, int requestID) {
        return 0;
    }
}
