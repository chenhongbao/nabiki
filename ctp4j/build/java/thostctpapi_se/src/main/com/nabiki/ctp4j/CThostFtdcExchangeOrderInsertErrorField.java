/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.nabiki.ctp4j;

public class CThostFtdcExchangeOrderInsertErrorField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcExchangeOrderInsertErrorField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcExchangeOrderInsertErrorField obj) {
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
        ThostFtdcCtpApiJNI.delete_CThostFtdcExchangeOrderInsertErrorField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setExchangeID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_ExchangeID_set(swigCPtr, this, value);
  }

  public String getExchangeID() {
    return ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_ExchangeID_get(swigCPtr, this);
  }

  public void setParticipantID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_ParticipantID_set(swigCPtr, this, value);
  }

  public String getParticipantID() {
    return ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_ParticipantID_get(swigCPtr, this);
  }

  public void setTraderID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_TraderID_set(swigCPtr, this, value);
  }

  public String getTraderID() {
    return ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_TraderID_get(swigCPtr, this);
  }

  public void setInstallID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_InstallID_set(swigCPtr, this, value);
  }

  public int getInstallID() {
    return ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_InstallID_get(swigCPtr, this);
  }

  public void setOrderLocalID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_OrderLocalID_set(swigCPtr, this, value);
  }

  public String getOrderLocalID() {
    return ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_OrderLocalID_get(swigCPtr, this);
  }

  public void setErrorID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_ErrorID_set(swigCPtr, this, value);
  }

  public int getErrorID() {
    return ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_ErrorID_get(swigCPtr, this);
  }

  public void setErrorMsg(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_ErrorMsg_set(swigCPtr, this, value);
  }

  public String getErrorMsg() {
    return ThostFtdcCtpApiJNI.CThostFtdcExchangeOrderInsertErrorField_ErrorMsg_get(swigCPtr, this);
  }

  public CThostFtdcExchangeOrderInsertErrorField() {
    this(ThostFtdcCtpApiJNI.new_CThostFtdcExchangeOrderInsertErrorField(), true);
  }

}
