/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.nabiki.ctp4j;

public class CThostFtdcErrExecOrderActionField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcErrExecOrderActionField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcErrExecOrderActionField obj) {
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
        ThostFtdcCtpApiJNI.delete_CThostFtdcErrExecOrderActionField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setBrokerID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_BrokerID_set(swigCPtr, this, value);
  }

  public String getBrokerID() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_BrokerID_get(swigCPtr, this);
  }

  public void setInvestorID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_InvestorID_set(swigCPtr, this, value);
  }

  public String getInvestorID() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_InvestorID_get(swigCPtr, this);
  }

  public void setExecOrderActionRef(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ExecOrderActionRef_set(swigCPtr, this, value);
  }

  public int getExecOrderActionRef() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ExecOrderActionRef_get(swigCPtr, this);
  }

  public void setExecOrderRef(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ExecOrderRef_set(swigCPtr, this, value);
  }

  public String getExecOrderRef() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ExecOrderRef_get(swigCPtr, this);
  }

  public void setRequestID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_RequestID_set(swigCPtr, this, value);
  }

  public int getRequestID() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_RequestID_get(swigCPtr, this);
  }

  public void setFrontID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_FrontID_set(swigCPtr, this, value);
  }

  public int getFrontID() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_FrontID_get(swigCPtr, this);
  }

  public void setSessionID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_SessionID_set(swigCPtr, this, value);
  }

  public int getSessionID() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_SessionID_get(swigCPtr, this);
  }

  public void setExchangeID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ExchangeID_set(swigCPtr, this, value);
  }

  public String getExchangeID() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ExchangeID_get(swigCPtr, this);
  }

  public void setExecOrderSysID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ExecOrderSysID_set(swigCPtr, this, value);
  }

  public String getExecOrderSysID() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ExecOrderSysID_get(swigCPtr, this);
  }

  public void setActionFlag(char value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ActionFlag_set(swigCPtr, this, value);
  }

  public char getActionFlag() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ActionFlag_get(swigCPtr, this);
  }

  public void setUserID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_UserID_set(swigCPtr, this, value);
  }

  public String getUserID() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_UserID_get(swigCPtr, this);
  }

  public void setInstrumentID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_InstrumentID_set(swigCPtr, this, value);
  }

  public String getInstrumentID() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_InstrumentID_get(swigCPtr, this);
  }

  public void setInvestUnitID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_InvestUnitID_set(swigCPtr, this, value);
  }

  public String getInvestUnitID() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_InvestUnitID_get(swigCPtr, this);
  }

  public void setIPAddress(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_IPAddress_set(swigCPtr, this, value);
  }

  public String getIPAddress() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_IPAddress_get(swigCPtr, this);
  }

  public void setMacAddress(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_MacAddress_set(swigCPtr, this, value);
  }

  public String getMacAddress() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_MacAddress_get(swigCPtr, this);
  }

  public void setErrorID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ErrorID_set(swigCPtr, this, value);
  }

  public int getErrorID() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ErrorID_get(swigCPtr, this);
  }

  public void setErrorMsg(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ErrorMsg_set(swigCPtr, this, value);
  }

  public String getErrorMsg() {
    return ThostFtdcCtpApiJNI.CThostFtdcErrExecOrderActionField_ErrorMsg_get(swigCPtr, this);
  }

  public CThostFtdcErrExecOrderActionField() {
    this(ThostFtdcCtpApiJNI.new_CThostFtdcErrExecOrderActionField(), true);
  }

}
