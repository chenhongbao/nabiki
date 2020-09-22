/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.nabiki.ctp4j;

public class CThostFtdcBrokerUserOTPParamField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcBrokerUserOTPParamField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcBrokerUserOTPParamField obj) {
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
        ThostFtdcCtpApiJNI.delete_CThostFtdcBrokerUserOTPParamField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setBrokerID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_BrokerID_set(swigCPtr, this, value);
  }

  public String getBrokerID() {
    return ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_BrokerID_get(swigCPtr, this);
  }

  public void setUserID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_UserID_set(swigCPtr, this, value);
  }

  public String getUserID() {
    return ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_UserID_get(swigCPtr, this);
  }

  public void setOTPVendorsID(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_OTPVendorsID_set(swigCPtr, this, value);
  }

  public String getOTPVendorsID() {
    return ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_OTPVendorsID_get(swigCPtr, this);
  }

  public void setSerialNumber(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_SerialNumber_set(swigCPtr, this, value);
  }

  public String getSerialNumber() {
    return ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_SerialNumber_get(swigCPtr, this);
  }

  public void setAuthKey(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_AuthKey_set(swigCPtr, this, value);
  }

  public String getAuthKey() {
    return ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_AuthKey_get(swigCPtr, this);
  }

  public void setLastDrift(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_LastDrift_set(swigCPtr, this, value);
  }

  public int getLastDrift() {
    return ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_LastDrift_get(swigCPtr, this);
  }

  public void setLastSuccess(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_LastSuccess_set(swigCPtr, this, value);
  }

  public int getLastSuccess() {
    return ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_LastSuccess_get(swigCPtr, this);
  }

  public void setOTPType(char value) {
    ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_OTPType_set(swigCPtr, this, value);
  }

  public char getOTPType() {
    return ThostFtdcCtpApiJNI.CThostFtdcBrokerUserOTPParamField_OTPType_get(swigCPtr, this);
  }

  public CThostFtdcBrokerUserOTPParamField() {
    this(ThostFtdcCtpApiJNI.new_CThostFtdcBrokerUserOTPParamField(), true);
  }

}
