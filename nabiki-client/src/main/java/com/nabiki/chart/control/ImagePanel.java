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

package com.nabiki.chart.control;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public abstract class ImagePanel extends JPanel {
  private final Object sync = new Object();
  private Image image;

  public ImagePanel() {
    prepare();
  }

  public ImagePanel(Image img) {
    this.image = img;
    prepare();
  }

  protected void setImage(Image img) {
    synchronized (this.sync) {
      this.image = img;
    }
  }

  protected Image getImage() {
    synchronized (this.sync) {
      return this.image;
    }
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    synchronized (this.sync) {
      if (this.image != null)
        g.drawImage(this.image, 0, 0, null);
    }
  }

  protected abstract void onResize(Dimension newSize);

  protected abstract void onShown(Dimension newSize);

  protected abstract void onHidden(Dimension newSize);

  private void prepare() {
    super.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        try {
          onResize(e.getComponent().getSize());
        } catch (Throwable th) {
          th.printStackTrace();
        }
      }

      @Override
      public void componentShown(ComponentEvent e) {
        try {
          onShown(e.getComponent().getSize());
        } catch (Throwable th) {
          th.printStackTrace();
        }
      }

      @Override
      public void componentHidden(ComponentEvent e) {
        try {
          onHidden(e.getComponent().getSize());
        } catch (Throwable th) {
          th.printStackTrace();
        }
      }
    });
  }
}
