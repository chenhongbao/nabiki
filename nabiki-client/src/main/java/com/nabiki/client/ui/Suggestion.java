package com.nabiki.client.ui;

import com.nabiki.commons.ctpobj.CInvestorPosition;
import com.nabiki.commons.ctpobj.CTradingAccount;

import java.util.Collection;

public class Suggestion {
  private String instrumentID;
  private String exchangeID;
  private Character direction;
  private Integer position, posDiff;
  private Double priceHigh, priceLow;
  private SuggestionState state;
  private CTradingAccount account = null; /* init as nil so it knows qry isn't sent yet */
  private Collection<CInvestorPosition> investorPos = null;

  private final SuggestionListener listener;

  public Suggestion(SuggestionListener listener) {
    this.listener = listener;
  }

  public String getInstrumentID() {
    return instrumentID;
  }

  public void setInstrumentID(String instrumentID) {
    this.instrumentID = instrumentID;
  }

  public String getExchangeID() {
    return exchangeID;
  }

  public void setExchangeID(String exchangeID) {
    this.exchangeID = exchangeID;
  }

  public Character getDirection() {
    return direction;
  }

  public void setDirection(Character direction) {
    this.direction = direction;
  }

  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer position) {
    this.position = position;
  }

  public Integer getPosDiff() {
    return posDiff;
  }

  public void setPosDiff(Integer posDiff) {
    this.posDiff = posDiff;
  }

  public Double getPriceHigh() {
    return priceHigh;
  }

  public void setPriceHigh(Double priceHigh) {
    this.priceHigh = priceHigh;
  }

  public Double getPriceLow() {
    return priceLow;
  }

  public void setPriceLow(Double priceLow) {
    this.priceLow = priceLow;
  }

  public SuggestionState getState() {
    return state;
  }

  public void setState(SuggestionState state) {
    if (state != this.state) {
      try {
        listener.onStateChange(this);
      } catch (Throwable th) {
        th.printStackTrace();
      }
    }
    this.state = state;
  }

  public CTradingAccount getAccount() {
    return account;
  }

  public void setAccount(CTradingAccount account) {
    this.account = account;
  }

  public Collection<CInvestorPosition> getInvestorPos() {
    return investorPos;
  }

  public void setInvestorPos(Collection<CInvestorPosition> investorPos) {
    this.investorPos = investorPos;
  }
}
