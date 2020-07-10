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

package com.nabiki.ctp4j.trader;

import com.nabiki.ctp4j.jni.flag.ThostTeResumeType;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.ctp4j.trader.internal.CThostFtdcTraderApiImpl;

/**
 * {@code CThostFtdcTraderApi} manages native resources for a trading session between
 * client and counter server.
 *
 * <p>There are two objects in the native codes serving a Java instance. And they are
 * session between client and server where trades happen, and channel between the
 * above session and JNI interface where data are moved from the native codes to JVM.
 * </p>
 *
 * <p>Request from Java to remote counter is moved from JVM to the native via JNI
 * interface, and then directly calls the native API to send request. It is no need
 * to route via the channel.
 * </p>
 *
 * <p>Data from remote counter are read via JNI by a Java thread and processed via
 * {@link CThostFtdcTraderSpi} callback methods.
 * </p>
 *
 * <p>To stop a trader api instance in Java, the class first destroys the native
 * session between client and remote counter, then the channel reading thread, and
 * then the channel. All resources must be released when a trader api instance is
 * no longer in use because the information of the native resources could be lost
 * after JVM garbage collection, thus those native resources are leaked.
 * </p>
 */
public abstract class CThostFtdcTraderApi {

	protected CThostFtdcTraderApi() {
	}

	/**
	 * Create new trader api instance. The flow path {@link String} must points to
	 * a valid directory path, or the native code would fail.
	 *
	 * @param flowPath path to the directory where native code keeps flow data
	 * @return new trader api instance
	 */
	public static CThostFtdcTraderApi CreateFtdcTraderApi(String flowPath) {
		return new CThostFtdcTraderApiImpl(flowPath);
	}

	/**
	 * Get API version.
	 *
	 * @return API version string
	 */
	public abstract String GetApiVersion();

	/**
	 * Get trading day in string. If the session is not login, the method returns
	 * null.
	 *
	 * @return trading day string
	 */
	public abstract String GetTradingDay();

	/**
	 * Initialize session to remote counter. It is not login yet after the method
	 * returns.
	 */
	public abstract void Init();

	/**
	 * Wait until the session is released. The method returns after
	 * {@link CThostFtdcTraderApi#Release()} is called.
	 */
	public abstract void Join();

	/**
	 * Set subscription type for private topic. If the specified type is invalid,
	 * nothing happens.
	 *
	 * @param type constants defined in {@link ThostTeResumeType}
	 */
	public abstract void SubscribePrivateTopic(int type);

	/**
	 * Set subscription type for public topic. If the specified type is invalid,
	 * nothing happens.
	 *
	 * @param type constants defined in {@link ThostTeResumeType}
	 */
	public abstract void SubscribePublicTopic(int type);

	/**
	 * Register front address for the session. Client can have more than one front
	 * addresses and the native client chooses randomly to connect the remote counter.
	 *
	 * @param frontAddress front address in the format {@code tcp://127.0.0.1:40010}
	 */
	public abstract void RegisterFront(String frontAddress);

	/**
	 * Register SPI for the trader session.
	 *
	 * @param spi callback SPI for responses
	 */
	public abstract void RegisterSpi(CThostFtdcTraderSpi spi);

	/**
	 * Release the native resources for the trader session.
	 */
	public abstract void Release();

	/**
	 * Request client authentication for the specified session.
	 * {@code OnRspAuthenticate} is called on the authentication response.
	 *
	 * <p>The method doesn't throw exception.
	 *
	 * @param reqAuthenticateField authentication request
	 * @param requestID            identifier for this request
	 * @return returned value from native function
	 * <ul>
	 * <li>0, successful sending
	 * <li>-1, network failure
	 * <li>-2, too many requests that are not processed
	 * <li>-3, too many requests in last second
	 * </ul>
	 */
	public abstract int ReqAuthenticate(
			CThostFtdcReqAuthenticateField reqAuthenticateField, int requestID);

	/**
	 * Request client login for the specified session. {@code OnRspUserLogin} is
	 * called on login response.
	 *
	 * <p>The method doesn't throw exception.
	 *
	 * @param reqUserLoginField login request
	 * @param requestID         identifier for this request
	 * @return returned value from native function
	 */
	public abstract int ReqUserLogin(CThostFtdcReqUserLoginField reqUserLoginField,
									 int requestID);

	/**
	 * Request client logout for the specified session. {@code OnRspUserLogout} is
	 * called on logout response.
	 *
	 * <p>The method doesn't throw exception.
	 *
	 * @param userLogout logout request
	 * @param requestID  identifier for this request
	 * @return returned value from native function
	 */
	public abstract int ReqUserLogout(CThostFtdcUserLogoutField userLogout,
									  int requestID);

	/**
	 * Request settlement confirm. {@code OnRspSettlementInfoConfirm} is called on
	 * response.
	 *
	 * @param settlementInfoConfirm confirm request
	 * @param requestID             identifier for this request
	 * @return returned value from native function
	 */
	public abstract int ReqSettlementInfoConfirm(
			CThostFtdcSettlementInfoConfirmField settlementInfoConfirm,
			int requestID);

	/**
	 * Request inserting order for the specified session. Different methods are
	 * called on different errors or response.
	 * <ul>
	 * <li>{@code OnErrRtnOrderInsert} or {@code OnRspOrderInsert} is called on
	 * incorrect fields in request.
	 * <li>{@code OnRtnOrder} is called on order status update.
	 * <li>{@code OnRtnTrade} is called on trade update.
	 * </ul>
	 *
	 * @param inputOrder order request
	 * @param requestID  identifier for this request
	 * @return returned value from native function
	 */
	public abstract int ReqOrderInsert(CThostFtdcInputOrderField inputOrder,
									   int requestID);

	/**
	 * Request cancelling an existing order from the specified session. There are
	 * two ways to cancel an order:
	 * <ul>
	 * <li>{@code OrderSysID}, the field is in order status update after execution
	 * of an order
	 * <li>{@code FrontID + SessionID + OrderRef}, order reference is maintained by
	 * client and the previous two fields are in successful login response, or in
	 * order status update.
	 * </ul>
	 * <p>Different methods are called on different errors or response:
	 * <ul>
	 * <li>{@code OnErrRtnOrderAction} or {@code OnRspOrderAction} is called on
	 * incorrect fields in action request.
	 * <li>{@code OnRtnOrder} is called on order status update.
	 * </ul>
	 *
	 * @param inputOrderAction action request
	 * @param requestID        identifier for this request
	 * @return returned value from native function
	 */
	public abstract int ReqOrderAction(
			CThostFtdcInputOrderActionField inputOrderAction, int requestID);

	/**
	 * Request query instrument information of the specified session.
	 * {@code OnRspQryInstrument} is called on response.
	 *
	 * @param qryInstrument query request
	 * @param requestID     identifier for this request
	 * @return returned value from native function
	 */
	public abstract int ReqQryInstrument(CThostFtdcQryInstrumentField qryInstrument,
										 int requestID);

	/**
	 * Request query commission rate from the specified session.
	 * {@code OnRspQryInstrumentCommissionRate} is called on response.
	 *
	 * @param qryInstrumentCommissionRate query request
	 * @param requestID                   identifier for this request
	 * @return returned value from native function
	 */
	public abstract int ReqQryInstrumentCommissionRate(
			CThostFtdcQryInstrumentCommissionRateField qryInstrumentCommissionRate,
			int requestID);

	/**
	 * Request query margin rate from the specified session.
	 * {@code OnRspQryInstrumentMarginRate} is called on response.
	 *
	 * @param qryInstrumentMarginRate query request
	 * @param requestID               identifier for this request
	 * @return returned value from native function
	 */
	public abstract int ReqQryInstrumentMarginRate(
			CThostFtdcQryInstrumentMarginRateField qryInstrumentMarginRate,
			int requestID);

	/**
	 * Request query trading account for the login user.
	 * {@code OnRspQryTradingAccount} is called on response.
	 *
	 * @param qryTradingAccount query request
	 * @param requestID         identifier for this request
	 * @return returned value from native function
	 */
	public abstract int ReqQryTradingAccount(
			CThostFtdcQryTradingAccountField qryTradingAccount, int requestID);

	/**
	 * Request query position detail for the login user.
	 * {@code OnRspQryInvestorPositionDetail} is called on response.
	 *
	 * @param qryInvestorPositionDetail query request
	 * @param requestID                 identifier for this request
	 * @return returned value from native function
	 */
	public abstract int ReqQryInvestorPositionDetail(
			CThostFtdcQryInvestorPositionDetailField qryInvestorPositionDetail,
			int requestID);
}
