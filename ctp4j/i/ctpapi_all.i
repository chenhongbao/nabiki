%module(directors="1") ThostFtdcCtpApi

/*
 * Work with char**.
 * Pointer to pointer to char, used to subscribe and un-subscribe depth market data.
 */
%include "various.i"
%apply char **STRING_ARRAY { char *ppInstrumentID[] }

%{
/*
 * Disable size_t convertion warning.
 */
#pragma warning(disable:4267)

/*
 * Include headers so CTP types are defined.
 */
#include "ThostFtdcTraderApi.h"
#include "ThostFtdcMdApi.h"

/*
 * Link to specific libs so we don't need to edit the build configiuration or source file.
 */
#pragma comment(lib, "thostmduserapi_se.lib")
#pragma comment(lib, "thosttraderapi_se.lib")
%}
 
/*
 * Handle Chinese characters in GB2312.
 */
%typemap(out) char[ANY], char[] {
	jclass str_cls = jenv->FindClass("java/lang/String");
	jmethodID constructor_mid = jenv-> GetMethodID(str_cls,"<init>","([BLjava/lang/String;)V");
	jbyteArray bytes = jenv->NewByteArray(strlen(result));
	jenv->SetByteArrayRegion(bytes, 0, strlen(result),(const jbyte*) result);
	jstring charset = jenv->NewStringUTF("gb2312");
	jresult = (jstring)jenv->NewObject(str_cls, constructor_mid, bytes, charset);
	jenv->DeleteLocalRef(charset);
	jenv->DeleteLocalRef(bytes);
	jenv->DeleteLocalRef(str_cls);
}

/*
 * The call back from C++ to target language.
 */
%feature("director") CThostFtdcTraderSpi;
%feature("director") CThostFtdcMdSpi;

/*
 * Those marcos are ill-defined.
 */
%ignore THOST_FTDC_VTC_BankBankToFuture;
%ignore THOST_FTDC_VTC_BankFutureToBank;
%ignore THOST_FTDC_VTC_FutureBankToFuture;
%ignore THOST_FTDC_VTC_FutureFutureToBank;
%ignore THOST_FTDC_FTC_BankLaunchBankToBroker;
%ignore THOST_FTDC_FTC_BrokerLaunchBankToBroker;
%ignore THOST_FTDC_FTC_BankLaunchBrokerToBank;
%ignore THOST_FTDC_FTC_BrokerLaunchBrokerToBank;  

/*
 * Parse objects in the following headers.
 */
%include "ThostFtdcUserApiDataType.h"
%include "ThostFtdcUserApiStruct.h" 
%include "ThostFtdcTraderApi.h"   
%include "ThostFtdcMdApi.h"