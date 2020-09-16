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

package com.nabiki.client.portal;

import com.nabiki.client.sdk.TradeClient;
import com.nabiki.objects.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PositionUpdater extends Updater implements Runnable {
    private final TradeClient client;
    private final JTable table;

    private Thread daemon;
    private String instrument;
    private PositionType type;
    private JButton src;

    enum PositionType {
        SUMMARY, DETAIL
    }

    private final String[] headers = new String[]{
            "\u5408\u7EA6",
            "\u65B9\u5411",
            "\u5728\u624B",
            "\u5df2\u5e73",
            "\u4ECA\u6628",
            "\u5F00\u4ED3\u4EF7",
            "\u6628\u7ED3",
            "\u7ED3\u7B97\u4EF7",
            "\u6301\u4ED3\u76C8\u4E8F",
            "\u5E73\u4ED3\u76C8\u4E8F"
    };

    PositionUpdater(JTable table, TradeClient client) {
        this.client = client;
        this.table = table;
        setupTable();
    }

    public void query(String instrument, PositionType type, Object src) {
        this.src = (JButton)src;
        this.instrument = instrument;
        this.type = type;
        super.fire();
    }

    public void start() {
        daemon = new Thread(this);
        daemon.setDaemon(true);
        daemon.start();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                super.waitFire();
                queryPosition();
            } catch (Throwable th) {
                showMsg(th.getMessage());
            }
        }
    }

    private void queryPosition() throws Exception {
        if (type == PositionType.SUMMARY)
            queryInvestorPosition();
        else
            queryDetail();
    }

    private void queryInvestorPosition() throws Exception {
        var req = new CQryInvestorPosition();
        req.InstrumentID = instrument;
        var rsp = client.queryPosition(
                req, UUID.randomUUID().toString());
        src.setEnabled(false);
        sleep(Constants.GLOBAL_WAIT_SECONDS, TimeUnit.SECONDS);
        src.setEnabled(true);
        if (!rsp.hasResponse())
            showMsg("\u65E0\u6301\u4ED3");
        else {
            CRspInfo error = null;
            CInvestorPosition p = null;
            var positions = new HashSet<CInvestorPosition>();
            while ((p = rsp.poll()) != null){
                positions.add(p);
                var info = rsp.getRspInfo(p);
                if (info.ErrorID != ErrorCodes.NONE) {
                    error = info;
                    break;
                }
            }
            if (error != null)
                showMsg(String.format("[%d]%s", error.ErrorID, error.ErrorMsg));
            else
                updatePositionTable(positions);
        }
    }

    private void queryDetail() throws Exception {
        var req = new CQryInvestorPositionDetail();
        req.InstrumentID = instrument;
        var rsp = client.queryPositionDetail(
                req, UUID.randomUUID().toString());
        src.setEnabled(false);
        sleep(Constants.GLOBAL_WAIT_SECONDS, TimeUnit.SECONDS);
        src.setEnabled(true);
        if (!rsp.hasResponse())
            showMsg("\u65E0\u6301\u4ED3");
        else {
            CRspInfo error = null;
            CInvestorPositionDetail p = null;
            var positions = new HashSet<CInvestorPositionDetail>();
            while ((p = rsp.poll()) != null){
                positions.add(p);
                var info = rsp.getRspInfo(p);
                if (info.ErrorID != ErrorCodes.NONE) {
                    error = info;
                    break;
                }
            }
            if (error != null)
                showMsg(String.format("[%d]%s", error.ErrorID, error.ErrorMsg));
            else
                updateDetailTable(positions);
        }
    }

    private String getPosiDirection(byte posi) {
        switch ((char)posi) {
            case PosiDirectionType.LONG:
                return "\u591A";
            case PosiDirectionType.SHORT:
                return "\u7A7A";
            case PosiDirectionType.NET:
                return "\u51c0";
        }
        return "\u672A\u77E5";
    }

    private String getDirection(byte direction) {
        switch (direction) {
            case DirectionType.DIRECTION_BUY:
                return "\u4E70";
            case DirectionType.DIRECTION_SELL:
                return "\u5356";

        }
        return "\u672A\u77E5";
    }

    private void updateDetailTable(Collection<CInvestorPositionDetail> details) {
        var objects = new Object[details.size()][];
        int idx = 0;
        for (var d : details) {
            int todayVol = 0, ydVol = 0;
            String tok;
            if (client.getTradingDay().compareTo(d.TradingDay) == 0) {
                tok = "\u4ECA";
            } else {
                tok = "\u6628";
            }
            var row = new Object[]{
                    d.InstrumentID,
                    getDirection(d.Direction),
                    d.Volume,
                    d.CloseVolume,
                    tok,
                    d.OpenPrice,
                    d.LastSettlementPrice,
                    d.SettlementPrice,
                    d.PositionProfitByDate,
                    d.CloseProfitByDate
            };
            objects[idx++] = row;
        }
        var model = (DefaultTableModel)table.getModel();
        model.setDataVector(objects, headers);
        model.fireTableDataChanged();
    }

    private void updatePositionTable(Collection<CInvestorPosition> positions) {
        var objects = new Object[positions.size()][];
        int idx = 0;
        for (var p : positions) {
            var openPrice = p.Position == 0 ? 0.0D : p.PositionCost / p.Position;
            var row = new Object[]{
                    p.InstrumentID,
                    getPosiDirection(p.PosiDirection),
                    p.Position,
                    p.CloseVolume,
                    "/",
                    openPrice,
                    p.PreSettlementPrice,
                    p.SettlementPrice,
                    p.PositionProfit,
                    p.CloseProfit
            };
            objects[idx++] = row;
        }
        var model = (DefaultTableModel)table.getModel();
        model.setDataVector(objects, headers);
        model.fireTableDataChanged();
    }

    private void showMsg(String msg) {
        MessageDialog.showDefault(msg);
    }

    private void setupTable() {
        table.setModel(new DefaultTableModel(headers, 0) {
            final Class[] columnTypes = new Class[]{
                    String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class
            };

            public Class getColumnClass(int columnIndex) {
                return columnTypes[columnIndex];
            }

            final boolean[] columnEditables = new boolean[]{
                    false, false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int row, int column) {
                return columnEditables[column];
            }
        });
    }
}
