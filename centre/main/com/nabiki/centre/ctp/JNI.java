/*
 * Copyright (c) 2020 Hongbao Chen <chenhongbao@outlook.com>
 *
 * Licensed under the  GNU Affero General Public License v3.0 and you may not use
 * this file except in compliance with the  License. You may obtain a copy of the
 * License at
 *
 *                    https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Permission is hereby  granted, free of charge, to any  person obtaining a copy
 * of this software and associated  documentation files (the "Software"), to deal
 * in the Software  without restriction, including without  limitation the rights
 * to  use, copy,  modify, merge,  publish, distribute,  sublicense, and/or  sell
 * copies  of  the Software,  and  to  permit persons  to  whom  the Software  is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE  IS PROVIDED "AS  IS", WITHOUT WARRANTY  OF ANY KIND,  EXPRESS OR
 * IMPLIED,  INCLUDING BUT  NOT  LIMITED TO  THE  WARRANTIES OF  MERCHANTABILITY,
 * FITNESS FOR  A PARTICULAR PURPOSE AND  NONINFRINGEMENT. IN NO EVENT  SHALL THE
 * AUTHORS  OR COPYRIGHT  HOLDERS  BE  LIABLE FOR  ANY  CLAIM,  DAMAGES OR  OTHER
 * LIABILITY, WHETHER IN AN ACTION OF  CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE  OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.nabiki.centre.ctp;

import com.nabiki.ctp4j.*;
import com.nabiki.objects.*;

public class JNI {
    public static CRspInfo toLocal(CThostFtdcRspInfoField jni) {
        var local = new CRspInfo();
        local.ErrorID = jni.getErrorID();
        local.ErrorMsg = jni.getErrorMsg();
        return local;
    }

    public static CRspAuthenticate toLocal(CThostFtdcRspAuthenticateField jni) {
        var local = new CRspAuthenticate();
        local.BrokerID = jni.getBrokerID();
        local.UserID = jni.getUserID();
        local.UserProductInfo = jni.getUserProductInfo();
        local.AppID = jni.getAppID();
        local.AppType = (byte)jni.getAppType();
        return local;
    }

    public static CRspUserLogin toLocal(CThostFtdcRspUserLoginField jni) {
        var local = new CRspUserLogin();
        local.TradingDay = jni.getTradingDay();
        local.LoginTime = jni.getLoginTime();
        local.BrokerID = jni.getBrokerID();
        local.UserID = jni.getUserID();
        local.SystemName = jni.getSystemName();
        local.FrontID = jni.getFrontID();
        local.SessionID = jni.getSessionID();
        local.MaxOrderRef = jni.getMaxOrderRef();
        local.SHFETime = jni.getSHFETime();
        local.DCETime = jni.getDCETime();
        local.CZCETime = jni.getCZCETime();
        local.FFEXTime = jni.getFFEXTime();
        local.INETime = jni.getINETime();
        return local;
    }

    public static CUserLogout toLocal(CThostFtdcUserLogoutField jni) {
        var local = new CUserLogout();
        local.BrokerID = jni.getBrokerID();
        local.UserID = jni.getUserID();
        return local;
    }

    public static CInputOrder toLocal(CThostFtdcInputOrderField jni) {
        var local = new CInputOrder();
        local.BrokerID = jni.getBrokerID();
        local.InvestorID = jni.getInvestorID();
        local.InstrumentID = jni.getInstrumentID();
        local.OrderRef = jni.getOrderRef();
        local.UserID = jni.getUserID();
        local.OrderPriceType = (byte)jni.getOrderPriceType();
        local.Direction = (byte)jni.getDirection();
        local.CombOffsetFlag = (byte)jni.getCombOffsetFlag().charAt(0);
        local.CombHedgeFlag = (byte)jni.getCombHedgeFlag().charAt(0);
        local.LimitPrice = jni.getLimitPrice();
        local.VolumeTotalOriginal = jni.getVolumeTotalOriginal();
        local.TimeCondition = (byte)jni.getTimeCondition();
        local.GTDDate = jni.getGTDDate();
        local.VolumeCondition = (byte)jni.getVolumeCondition();
        local.MinVolume = jni.getMinVolume();
        local.ContingentCondition = (byte)jni.getContingentCondition();
        local.StopPrice = jni.getStopPrice();
        local.ForceCloseReason = (byte)jni.getForceCloseReason();
        local.IsAutoSuspend = jni.getIsAutoSuspend();
        local.BusinessUnit = jni.getBusinessUnit();
        local.RequestID = jni.getRequestID();
        local.UserForceClose = jni.getUserForceClose();
        local.IsSwapOrder = jni.getIsSwapOrder();
        local.ExchangeID = jni.getExchangeID();
        local.InvestUnitID = jni.getInvestUnitID();
        local.AccountID = jni.getAccountID();
        local.CurrencyID = jni.getCurrencyID();
        local.ClientID = jni.getClientID();
        local.IPAddress = jni.getIPAddress();
        local.MacAddress = jni.getMacAddress();
        return local;
    }

    public static CInputOrderAction toLocal(CThostFtdcInputOrderActionField jni) {
        var local = new CInputOrderAction();
        local.BrokerID = jni.getBrokerID();
        local.InvestorID = jni.getInvestorID();
        local.OrderActionRef = jni.getOrderActionRef();
        local.OrderRef = jni.getOrderRef();
        local.RequestID = jni.getRequestID();
        local.FrontID = jni.getFrontID();
        local.SessionID = jni.getSessionID();
        local.ExchangeID = jni.getExchangeID();
        local.OrderSysID = jni.getOrderSysID();
        local.ActionFlag = (byte)jni.getActionFlag();
        local.LimitPrice = jni.getLimitPrice();
        local.VolumeChange = jni.getVolumeChange();
        local.UserID = jni.getUserID();
        local.InstrumentID = jni.getInstrumentID();
        local.InvestUnitID = jni.getInvestUnitID();
        local.IPAddress = jni.getIPAddress();
        local.MacAddress = jni.getMacAddress();
        return local;
    }

    public static CSettlementInfoConfirm toLocal(CThostFtdcSettlementInfoConfirmField jni) {
        var local = new CSettlementInfoConfirm();
        local.BrokerID = jni.getBrokerID();
        local.InvestorID = jni.getInvestorID();
        local.ConfirmDate = jni.getConfirmDate();
        local.ConfirmTime = jni.getConfirmTime();
        local.SettlementID = jni.getSettlementID();
        local.AccountID = jni.getAccountID();
        local.CurrencyID = jni.getCurrencyID();
        return local;
    }

    public static CInstrumentMarginRate toLocal(CThostFtdcInstrumentMarginRateField jni) {
        var local = new CInstrumentMarginRate();
        local.InstrumentID = jni.getInstrumentID();
        local.InvestorRange = (byte)jni.getInvestorRange();
        local.BrokerID = jni.getBrokerID();
        local.InvestorID = jni.getInvestorID();
        local.HedgeFlag = (byte)jni.getHedgeFlag();
        local.LongMarginRatioByMoney = jni.getLongMarginRatioByMoney();
        local.LongMarginRatioByVolume = jni.getLongMarginRatioByVolume();
        local.ShortMarginRatioByMoney = jni.getShortMarginRatioByMoney();
        local.ShortMarginRatioByVolume = jni.getShortMarginRatioByVolume();
        local.IsRelative = jni.getIsRelative();
        local.ExchangeID = jni.getExchangeID();
        local.InvestUnitID = jni.getInvestUnitID();
        return local;
    }

    public static CInstrumentCommissionRate toLocal(CThostFtdcInstrumentCommissionRateField jni) {
        var local = new CInstrumentCommissionRate();
        local.InstrumentID = jni.getInstrumentID();
        local.InvestorRange = (byte)jni.getInvestorRange();
        local.BrokerID = jni.getBrokerID();
        local.InvestorID = jni.getInvestorID();
        local.OpenRatioByMoney = jni.getOpenRatioByMoney();
        local.OpenRatioByVolume = jni.getOpenRatioByVolume();
        local.CloseRatioByMoney = jni.getCloseRatioByMoney();
        local.CloseRatioByVolume = jni.getCloseRatioByVolume();
        local.CloseTodayRatioByMoney = jni.getCloseTodayRatioByMoney();
        local.CloseTodayRatioByVolume = jni.getCloseTodayRatioByVolume();
        local.ExchangeID = jni.getExchangeID();
        local.BizType = (byte)jni.getBizType();
        local.InvestUnitID = jni.getInvestUnitID();
        return local;
    }

    public static CInstrument toLocal(CThostFtdcInstrumentField jni) {
        var local = new CInstrument();
        local.InstrumentID = jni.getInstrumentID();
        local.ExchangeID = jni.getExchangeID();
        local.InstrumentName = jni.getInstrumentName();
        local.ExchangeInstID = jni.getExchangeInstID();
        local.ProductID = jni.getProductID();
        local.ProductClass = (byte)jni.getProductClass();
        local.DeliveryYear = jni.getDeliveryYear();
        local.DeliveryMonth = jni.getDeliveryMonth();
        local.MaxMarketOrderVolume = jni.getMaxMarketOrderVolume();
        local.MinMarketOrderVolume = jni.getMinMarketOrderVolume();
        local.MaxLimitOrderVolume = jni.getMaxLimitOrderVolume();
        local.MinLimitOrderVolume = jni.getMinLimitOrderVolume();
        local.VolumeMultiple = jni.getVolumeMultiple();
        local.PriceTick = jni.getPriceTick();
        local.CreateDate = jni.getCreateDate();
        local.OpenDate = jni.getOpenDate();
        local.ExpireDate = jni.getExpireDate();
        local.StartDelivDate = jni.getStartDelivDate();
        local.EndDelivDate = jni.getEndDelivDate();
        local.InstLifePhase = (byte)jni.getInstLifePhase();
        local.IsTrading = jni.getIsTrading();
        local.PositionType = (byte)jni.getPositionType();
        local.PositionDateType = (byte)jni.getPositionDateType();
        local.LongMarginRatio = jni.getLongMarginRatio();
        local.ShortMarginRatio = jni.getShortMarginRatio();
        local.MaxMarginSideAlgorithm = (byte)jni.getMaxMarginSideAlgorithm();
        local.UnderlyingInstrID = jni.getUnderlyingInstrID();
        local.StrikePrice = jni.getStrikePrice();
        local.OptionsType = (byte)jni.getOptionsType();
        local.UnderlyingMultiple = jni.getUnderlyingMultiple();
        local.CombinationType = (byte)jni.getCombinationType();
        return local;
    }

    public static COrder toLocal(CThostFtdcOrderField jni) {
        var local = new COrder();
        local.BrokerID = jni.getBrokerID();
        local.InvestorID = jni.getInvestorID();
        local.InstrumentID = jni.getInstrumentID();
        local.OrderRef = jni.getOrderRef();
        local.UserID = jni.getUserID();
        local.OrderPriceType = (byte)jni.getOrderPriceType();
        local.Direction = (byte)jni.getDirection();
        local.CombOffsetFlag = (byte)jni.getCombOffsetFlag().charAt(0);
        local.CombHedgeFlag = (byte)jni.getCombHedgeFlag().charAt(0);
        local.LimitPrice = jni.getLimitPrice();
        local.VolumeTotalOriginal = jni.getVolumeTotalOriginal();
        local.TimeCondition = (byte)jni.getTimeCondition();
        local.GTDDate = jni.getGTDDate();
        local.VolumeCondition = (byte)jni.getVolumeCondition();
        local.MinVolume = jni.getMinVolume();
        local.ContingentCondition = (byte)jni.getContingentCondition();
        local.StopPrice = jni.getStopPrice();
        local.ForceCloseReason = (byte)jni.getForceCloseReason();
        local.IsAutoSuspend = jni.getIsAutoSuspend();
        local.BusinessUnit = jni.getBusinessUnit();
        local.RequestID = jni.getRequestID();
        local.OrderLocalID = jni.getOrderLocalID();
        local.ExchangeID = jni.getExchangeID();
        local.ParticipantID = jni.getParticipantID();
        local.ClientID = jni.getClientID();
        local.ExchangeInstID = jni.getExchangeInstID();
        local.TraderID = jni.getTraderID();
        local.InstallID = jni.getInstallID();
        local.OrderSubmitStatus = (byte)jni.getOrderSubmitStatus();
        local.NotifySequence = jni.getNotifySequence();
        local.TradingDay = jni.getTradingDay();
        local.SettlementID = jni.getSettlementID();
        local.OrderSysID = jni.getOrderSysID();
        local.OrderSource = (byte)jni.getOrderSource();
        local.OrderStatus = (byte)jni.getOrderStatus();
        local.OrderType = (byte)jni.getOrderType();
        local.VolumeTraded = jni.getVolumeTraded();
        local.VolumeTotal = jni.getVolumeTotal();
        local.InsertDate = jni.getInsertDate();
        local.InsertTime = jni.getInsertTime();
        local.ActiveTime = jni.getActiveTime();
        local.SuspendTime = jni.getSuspendTime();
        local.UpdateTime = jni.getUpdateTime();
        local.CancelTime = jni.getCancelTime();
        local.ActiveTraderID = jni.getActiveTraderID();
        local.ClearingPartID = jni.getClearingPartID();
        local.SequenceNo = jni.getSequenceNo();
        local.FrontID = jni.getFrontID();
        local.SessionID = jni.getSessionID();
        local.UserProductInfo = jni.getUserProductInfo();
        local.StatusMsg = jni.getStatusMsg();
        local.UserForceClose = jni.getUserForceClose();
        local.ActiveUserID = jni.getActiveUserID();
        local.BrokerOrderSeq = jni.getBrokerOrderSeq();
        local.RelativeOrderSysID = jni.getRelativeOrderSysID();
        local.ZCETotalTradedVolume = jni.getZCETotalTradedVolume();
        local.IsSwapOrder = jni.getIsSwapOrder();
        local.BranchID = jni.getBranchID();
        local.InvestUnitID = jni.getInvestUnitID();
        local.AccountID = jni.getAccountID();
        local.CurrencyID = jni.getCurrencyID();
        local.IPAddress = jni.getIPAddress();
        local.MacAddress = jni.getMacAddress();
        return local;
    }

    public static CTrade toLocal(CThostFtdcTradeField jni) {
        var local = new CTrade();
        local.BrokerID = jni.getBrokerID();
        local.InvestorID = jni.getInvestorID();
        local.InstrumentID = jni.getInstrumentID();
        local.OrderRef = jni.getOrderRef();
        local.UserID = jni.getUserID();
        local.ExchangeID = jni.getExchangeID();
        local.TradeID = jni.getTradeID();
        local.Direction = (byte)jni.getDirection();
        local.OrderSysID = jni.getOrderSysID();
        local.ParticipantID = jni.getParticipantID();
        local.ClientID = jni.getClientID();
        local.TradingRole = (byte)jni.getTradingRole();
        local.ExchangeInstID = jni.getExchangeInstID();
        local.OffsetFlag = (byte)jni.getOffsetFlag();
        local.HedgeFlag = (byte)jni.getHedgeFlag();
        local.Price = jni.getPrice();
        local.Volume = jni.getVolume();
        local.TradeDate = jni.getTradeDate();
        local.TradeTime = jni.getTradeTime();
        local.TradeType = (byte)jni.getTradeType();
        local.PriceSource = (byte)jni.getPriceSource();
        local.TraderID = jni.getTraderID();
        local.OrderLocalID = jni.getOrderLocalID();
        local.ClearingPartID = jni.getClearingPartID();
        local.BusinessUnit = jni.getBusinessUnit();
        local.SequenceNo = jni.getSequenceNo();
        local.TradingDay = jni.getTradingDay();
        local.SettlementID = jni.getSettlementID();
        local.BrokerOrderSeq = jni.getBrokerOrderSeq();
        local.TradeSource = (byte)jni.getTradeSource();
        local.InvestUnitID = jni.getInvestUnitID();
        return local;
    }

    public static COrderAction toLocal(CThostFtdcOrderActionField jni) {
        var local = new COrderAction();
        local.BrokerID = jni.getBrokerID();
        local.InvestorID = jni.getInvestorID();
        local.OrderActionRef = jni.getOrderActionRef();
        local.OrderRef = jni.getOrderRef();
        local.RequestID = jni.getRequestID();
        local.FrontID = jni.getFrontID();
        local.SessionID = jni.getSessionID();
        local.ExchangeID = jni.getExchangeID();
        local.OrderSysID = jni.getOrderSysID();
        local.ActionFlag = (byte)jni.getActionFlag();
        local.LimitPrice = jni.getLimitPrice();
        local.VolumeChange = jni.getVolumeChange();
        local.ActionDate = jni.getActionDate();
        local.ActionTime = jni.getActionTime();
        local.TraderID = jni.getTraderID();
        local.InstallID = jni.getInstallID();
        local.OrderLocalID = jni.getOrderLocalID();
        local.ActionLocalID = jni.getActionLocalID();
        local.ParticipantID = jni.getParticipantID();
        local.ClientID = jni.getClientID();
        local.BusinessUnit = jni.getBusinessUnit();
        local.OrderActionStatus = (byte)jni.getOrderActionStatus();
        local.UserID = jni.getUserID();
        local.StatusMsg = jni.getStatusMsg();
        local.InstrumentID = jni.getInstrumentID();
        local.BranchID = jni.getBranchID();
        local.InvestUnitID = jni.getInvestUnitID();
        local.IPAddress = jni.getIPAddress();
        local.MacAddress = jni.getMacAddress();
        return local;
    }

    public static CSpecificInstrument toLocal(CThostFtdcSpecificInstrumentField jni) {
        var local = new CSpecificInstrument();
        local.InstrumentID = jni.getInstrumentID();
        return local;
    }

    public static CDepthMarketData toLocal(CThostFtdcDepthMarketDataField jni) {
        var local = new CDepthMarketData();
        local.TradingDay = jni.getTradingDay();
        local.InstrumentID = jni.getInstrumentID();
        local.ExchangeID = jni.getExchangeID();
        local.ExchangeInstID = jni.getExchangeInstID();
        local.LastPrice = jni.getLastPrice();
        local.PreSettlementPrice = jni.getPreSettlementPrice();
        local.PreClosePrice = jni.getPreClosePrice();
        local.PreOpenInterest = jni.getPreOpenInterest();
        local.OpenPrice = jni.getOpenPrice();
        local.HighestPrice = jni.getHighestPrice();
        local.LowestPrice = jni.getLowestPrice();
        local.Volume = jni.getVolume();
        local.Turnover = jni.getTurnover();
        local.OpenInterest = jni.getOpenInterest();
        local.ClosePrice = jni.getClosePrice();
        local.SettlementPrice = jni.getSettlementPrice();
        local.UpperLimitPrice = jni.getUpperLimitPrice();
        local.LowerLimitPrice = jni.getLowerLimitPrice();
        local.PreDelta = jni.getPreDelta();
        local.CurrDelta = jni.getCurrDelta();
        local.UpdateTime = jni.getUpdateTime();
        local.UpdateMillisec = jni.getUpdateMillisec();
        local.BidPrice1 = jni.getBidPrice1();
        local.BidVolume1 = jni.getBidVolume1();
        local.AskPrice1 = jni.getAskPrice1();
        local.AskVolume1 = jni.getAskVolume1();
        local.BidPrice2 = jni.getBidPrice2();
        local.BidVolume2 = jni.getBidVolume2();
        local.AskPrice2 = jni.getAskPrice2();
        local.AskVolume2 = jni.getAskVolume2();
        local.BidPrice3 = jni.getBidPrice3();
        local.BidVolume3 = jni.getBidVolume3();
        local.AskPrice3 = jni.getAskPrice3();
        local.AskVolume3 = jni.getAskVolume3();
        local.BidPrice4 = jni.getBidPrice4();
        local.BidVolume4 = jni.getBidVolume4();
        local.AskPrice4 = jni.getAskPrice4();
        local.AskVolume4 = jni.getAskVolume4();
        local.BidPrice5 = jni.getBidPrice5();
        local.BidVolume5 = jni.getBidVolume5();
        local.AskPrice5 = jni.getAskPrice5();
        local.AskVolume5 = jni.getAskVolume5();
        local.AveragePrice = jni.getAveragePrice();
        local.ActionDay = jni.getActionDay();
        return local;
    }

    public static CThostFtdcReqUserLoginField toJni(CReqUserLogin local) {
        var jni = new CThostFtdcReqUserLoginField();
        jni.setTradingDay(local.TradingDay);
        jni.setBrokerID(local.BrokerID);
        jni.setUserID(local.UserID);
        jni.setPassword(local.Password);
        jni.setUserProductInfo(local.UserProductInfo);
        jni.setInterfaceProductInfo(local.InterfaceProductInfo);
        jni.setProtocolInfo(local.ProtocolInfo);
        jni.setMacAddress(local.MacAddress);
        jni.setOneTimePassword(local.OneTimePassword);
        jni.setClientIPAddress(local.ClientIPAddress);
        jni.setLoginRemark(local.LoginRemark);
        jni.setClientIPPort(local.ClientIPPort);
        return jni;
    }

    public static CThostFtdcUserLogoutField toJni(CUserLogout local) {
        var jni = new CThostFtdcUserLogoutField();
        jni.setBrokerID(local.BrokerID);
        jni.setUserID(local.UserID);
        return jni;
    }

    public static CThostFtdcSettlementInfoConfirmField toJni(CSettlementInfoConfirm local) {
        var jni = new CThostFtdcSettlementInfoConfirmField();
        jni.setBrokerID(local.BrokerID);
        jni.setInvestorID(local.InvestorID);
        jni.setConfirmDate(local.ConfirmDate);
        jni.setConfirmTime(local.ConfirmTime);
        jni.setSettlementID(local.SettlementID);
        jni.setAccountID(local.AccountID);
        jni.setCurrencyID(local.CurrencyID);
        return jni;
    }

    public static CThostFtdcReqAuthenticateField toJni(CReqAuthenticate local) {
        var jni = new CThostFtdcReqAuthenticateField();
        jni.setBrokerID(local.BrokerID);
        jni.setUserID(local.UserID);
        jni.setUserProductInfo(local.UserProductInfo);
        jni.setAuthCode(local.AuthCode);
        jni.setAppID(local.AppID);
        return jni;
    }

    public static CThostFtdcQryInstrumentField toJni(CQryInstrument local) {
        var jni = new CThostFtdcQryInstrumentField();
        jni.setInstrumentID(local.InstrumentID);
        jni.setExchangeID(local.ExchangeID);
        jni.setExchangeInstID(local.ExchangeInstID);
        jni.setProductID(local.ProductID);
        return jni;
    }

    public static CThostFtdcInputOrderField toJni(CInputOrder local) {
        var jni = new CThostFtdcInputOrderField();
        jni.setBrokerID(local.BrokerID);
        jni.setInvestorID(local.InvestorID);
        jni.setInstrumentID(local.InstrumentID);
        jni.setOrderRef(local.OrderRef);
        jni.setUserID(local.UserID);
        jni.setOrderPriceType((char)local.OrderPriceType);
        jni.setDirection((char)local.Direction);
        jni.setCombOffsetFlag(String.valueOf(local.CombOffsetFlag));
        jni.setCombHedgeFlag(String.valueOf(local.CombHedgeFlag));
        jni.setLimitPrice(local.LimitPrice);
        jni.setVolumeTotalOriginal(local.VolumeTotalOriginal);
        jni.setTimeCondition((char)local.TimeCondition);
        jni.setGTDDate(local.GTDDate);
        jni.setVolumeCondition((char)local.VolumeCondition);
        jni.setMinVolume(local.MinVolume);
        jni.setContingentCondition((char)local.ContingentCondition);
        jni.setStopPrice(local.StopPrice);
        jni.setForceCloseReason((char)local.ForceCloseReason);
        jni.setIsAutoSuspend(local.IsAutoSuspend);
        jni.setBusinessUnit(local.BusinessUnit);
        jni.setRequestID(local.RequestID);
        jni.setUserForceClose(local.UserForceClose);
        jni.setIsSwapOrder(local.IsSwapOrder);
        jni.setExchangeID(local.ExchangeID);
        jni.setInvestUnitID(local.InvestUnitID);
        jni.setAccountID(local.AccountID);
        jni.setCurrencyID(local.CurrencyID);
        jni.setClientID(local.ClientID);
        jni.setIPAddress(local.IPAddress);
        jni.setMacAddress(local.MacAddress);
        return jni;
    }

    public static CThostFtdcInputOrderActionField toJni(CInputOrderAction local) {
        var jni = new CThostFtdcInputOrderActionField();
        jni.setBrokerID(local.BrokerID);
        jni.setInvestorID(local.InvestorID);
        jni.setOrderActionRef(local.OrderActionRef);
        jni.setOrderRef(local.OrderRef);
        jni.setRequestID(local.RequestID);
        jni.setFrontID(local.FrontID);
        jni.setSessionID(local.SessionID);
        jni.setExchangeID(local.ExchangeID);
        jni.setOrderSysID(local.OrderSysID);
        jni.setActionFlag((char)local.ActionFlag);
        jni.setLimitPrice(local.LimitPrice);
        jni.setVolumeChange(local.VolumeChange);
        jni.setUserID(local.UserID);
        jni.setInstrumentID(local.InstrumentID);
        jni.setInvestUnitID(local.InvestUnitID);
        jni.setIPAddress(local.IPAddress);
        jni.setMacAddress(local.MacAddress);
        return jni;
    }

    public static CThostFtdcQryInstrumentMarginRateField toJni(CQryInstrumentMarginRate local) {
        var jni = new CThostFtdcQryInstrumentMarginRateField();
        jni.setBrokerID(local.BrokerID);
        jni.setInvestorID(local.InvestorID);
        jni.setInstrumentID(local.InstrumentID);
        jni.setHedgeFlag((char)local.HedgeFlag);
        jni.setExchangeID(local.ExchangeID);
        jni.setInvestUnitID(local.InvestUnitID);
        return jni;
    }

    public static CThostFtdcQryInstrumentCommissionRateField toJni(CQryInstrumentCommissionRate local) {
        var jni = new CThostFtdcQryInstrumentCommissionRateField();
        jni.setBrokerID(local.BrokerID);
        jni.setInvestorID(local.InvestorID);
        jni.setInstrumentID(local.InstrumentID);
        jni.setExchangeID(local.ExchangeID);
        jni.setInvestUnitID(local.InvestUnitID);
        return jni;
    }
}
