/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.nabiki.ctp4j;

public class CThostFtdcLogoutAllField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcLogoutAllField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcLogoutAllField obj) {
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
        ThostFtdcCtpApiJNI.delete_CThostFtdcLogoutAllField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setFrontID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcLogoutAllField_FrontID_set(swigCPtr, this, value);
  }

  public int getFrontID() {
    return ThostFtdcCtpApiJNI.CThostFtdcLogoutAllField_FrontID_get(swigCPtr, this);
  }

  public void setSessionID(int value) {
    ThostFtdcCtpApiJNI.CThostFtdcLogoutAllField_SessionID_set(swigCPtr, this, value);
  }

  public int getSessionID() {
    return ThostFtdcCtpApiJNI.CThostFtdcLogoutAllField_SessionID_get(swigCPtr, this);
  }

  public void setSystemName(String value) {
    ThostFtdcCtpApiJNI.CThostFtdcLogoutAllField_SystemName_set(swigCPtr, this, value);
  }

  public String getSystemName() {
    return ThostFtdcCtpApiJNI.CThostFtdcLogoutAllField_SystemName_get(swigCPtr, this);
  }

  public CThostFtdcLogoutAllField() {
    this(ThostFtdcCtpApiJNI.new_CThostFtdcLogoutAllField(), true);
  }

}
