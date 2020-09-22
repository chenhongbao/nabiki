/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.nabiki.ctp4j;

public class CThostFtdcRspFutureSignInField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcRspFutureSignInField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcRspFutureSignInField obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        ThostFtdcCtpApiJNI.delete_CThostFtdcRspFutureSignInField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setTradeCode(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_TradeCode_set(swigCPtr, this, value);
  }

  public String getTradeCode() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_TradeCode_get(swigCPtr, this);
  }

  public void setBankID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_BankID_set(swigCPtr, this, value);
  }

  public String getBankID() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_BankID_get(swigCPtr, this);
  }

  public void setBankBranchID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_BankBranchID_set(swigCPtr, this, value);
  }

  public String getBankBranchID() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_BankBranchID_get(swigCPtr, this);
  }

  public void setBrokerID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_BrokerID_set(swigCPtr, this, value);
  }

  public String getBrokerID() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_BrokerID_get(swigCPtr, this);
  }

  public void setBrokerBranchID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_BrokerBranchID_set(swigCPtr, this, value);
  }

  public String getBrokerBranchID() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_BrokerBranchID_get(swigCPtr, this);
  }

  public void setTradeDate(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_TradeDate_set(swigCPtr, this, value);
  }

  public String getTradeDate() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_TradeDate_get(swigCPtr, this);
  }

  public void setTradeTime(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_TradeTime_set(swigCPtr, this, value);
  }

  public String getTradeTime() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_TradeTime_get(swigCPtr, this);
  }

  public void setBankSerial(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_BankSerial_set(swigCPtr, this, value);
  }

  public String getBankSerial() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_BankSerial_get(swigCPtr, this);
  }

  public void setTradingDay(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_TradingDay_set(swigCPtr, this, value);
  }

  public String getTradingDay() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_TradingDay_get(swigCPtr, this);
  }

  public void setPlateSerial(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_PlateSerial_set(swigCPtr, this, value);
  }

  public int getPlateSerial() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_PlateSerial_get(swigCPtr, this);
  }

  public void setLastFragment(char value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_LastFragment_set(swigCPtr, this, value);
  }

  public char getLastFragment() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_LastFragment_get(swigCPtr, this);
  }

  public void setSessionID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_SessionID_set(swigCPtr, this, value);
  }

  public int getSessionID() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_SessionID_get(swigCPtr, this);
  }

  public void setInstallID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_InstallID_set(swigCPtr, this, value);
  }

  public int getInstallID() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_InstallID_get(swigCPtr, this);
  }

  public void setUserID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_UserID_set(swigCPtr, this, value);
  }

  public String getUserID() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_UserID_get(swigCPtr, this);
  }

  public void setDigest(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_Digest_set(swigCPtr, this, value);
  }

  public String getDigest() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_Digest_get(swigCPtr, this);
  }

  public void setCurrencyID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_CurrencyID_set(swigCPtr, this, value);
  }

  public String getCurrencyID() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_CurrencyID_get(swigCPtr, this);
  }

  public void setDeviceID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_DeviceID_set(swigCPtr, this, value);
  }

  public String getDeviceID() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_DeviceID_get(swigCPtr, this);
  }

  public void setBrokerIDByBank(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_BrokerIDByBank_set(swigCPtr, this, value);
  }

  public String getBrokerIDByBank() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_BrokerIDByBank_get(swigCPtr, this);
  }

  public void setOperNo(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_OperNo_set(swigCPtr, this, value);
  }

  public String getOperNo() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_OperNo_get(swigCPtr, this);
  }

  public void setRequestID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_RequestID_set(swigCPtr, this, value);
  }

  public int getRequestID() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_RequestID_get(swigCPtr, this);
  }

  public void setTID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_TID_set(swigCPtr, this, value);
  }

  public int getTID() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_TID_get(swigCPtr, this);
  }

  public void setErrorID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_ErrorID_set(swigCPtr, this, value);
  }

  public int getErrorID() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_ErrorID_get(swigCPtr, this);
  }

  public void setErrorMsg(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_ErrorMsg_set(swigCPtr, this, value);
  }

  public String getErrorMsg() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_ErrorMsg_get(swigCPtr, this);
  }

  public void setPinKey(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_PinKey_set(swigCPtr, this, value);
  }

  public String getPinKey() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_PinKey_get(swigCPtr, this);
  }

  public void setMacKey(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_MacKey_set(swigCPtr, this, value);
  }

  public String getMacKey() {
    return ThostFtdcCtpApiJNI.CThostFtdcRspFutureSignInField_MacKey_get(swigCPtr, this);
  }

  public CThostFtdcRspFutureSignInField() {
    this(ThostFtdcCtpApiJNI.new_CThostFtdcRspFutureSignInField(), true);
  }

}
