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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyEventDemo extends JFrame
    implements KeyListener,
    ActionListener {
  JTextArea displayArea;
  JTextField typingArea;
  static final String newline = System.getProperty("line.separator");

  public static void main(String[] args) {
    /* Use an appropriate Look and Feel */
    try {
      //UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
      //UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
      UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
    } catch (UnsupportedLookAndFeelException ex) {
      ex.printStackTrace();
    } catch (IllegalAccessException ex) {
      ex.printStackTrace();
    } catch (InstantiationException ex) {
      ex.printStackTrace();
    } catch (ClassNotFoundException ex) {
      ex.printStackTrace();
    }
    /* Turn off metal's use of bold fonts */
    UIManager.put("swing.boldMetal", Boolean.FALSE);

    //Schedule a job for event dispatch thread:
    //creating and showing this application's GUI.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
  }

  /**
   * Create the GUI and show it.  For thread safety,
   * this method should be invoked from the
   * event-dispatching thread.
   */
  private static void createAndShowGUI() {
    //Create and set up the window.
    KeyEventDemo frame = new KeyEventDemo("KeyEventDemo");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    //Set up the content pane.
    frame.addComponentsToPane();


    //Display the window.
    frame.pack();
    frame.setVisible(true);
  }

  private void addComponentsToPane() {

    JButton button = new JButton("Clear");
    button.addActionListener(this);

    typingArea = new JTextField(20);
    typingArea.addKeyListener(this);

    //Uncomment this if you wish to turn off focus
    //traversal.  The focus subsystem consumes
    //focus traversal keys, such as Tab and Shift Tab.
    //If you uncomment the following line of code, this
    //disables focus traversal and the Tab events will
    //become available to the key event listener.
    //typingArea.setFocusTraversalKeysEnabled(false);

    displayArea = new JTextArea();
    displayArea.setEditable(false);
    JScrollPane scrollPane = new JScrollPane(displayArea);
    scrollPane.setPreferredSize(new Dimension(375, 125));

    getContentPane().add(typingArea, BorderLayout.PAGE_START);
    getContentPane().add(scrollPane, BorderLayout.CENTER);
    getContentPane().add(button, BorderLayout.PAGE_END);
  }

  public KeyEventDemo(String name) {
    super(name);
  }


  /**
   * Handle the key typed event from the text field.
   */
  public void keyTyped(KeyEvent e) {
    displayInfo(e, "KEY TYPED: ");
  }

  /**
   * Handle the key pressed event from the text field.
   */
  public void keyPressed(KeyEvent e) {
    displayInfo(e, "KEY PRESSED: ");
  }

  /**
   * Handle the key released event from the text field.
   */
  public void keyReleased(KeyEvent e) {
    displayInfo(e, "KEY RELEASED: ");
  }

  /**
   * Handle the button click.
   */
  public void actionPerformed(ActionEvent e) {
    //Clear the text components.
    displayArea.setText("");
    typingArea.setText("");

    //Return the focus to the typing area.
    typingArea.requestFocusInWindow();
  }

  /*
   * We have to jump through some hoops to avoid
   * trying to print non-printing characters
   * such as Shift.  (Not only do they not print,
   * but if you put them in a String, the characters
   * afterward won't show up in the text area.)
   */
  private void displayInfo(KeyEvent e, String keyStatus) {

    //You should only rely on the key char if the event
    //is a key typed event.
    int id = e.getID();
    String keyString;
    if (id == KeyEvent.KEY_TYPED) {
      char c = e.getKeyChar();
      keyString = "key character = '" + c + "'";
    } else {
      int keyCode = e.getKeyCode();
      keyString = "key code = " + keyCode
          + " ("
          + KeyEvent.getKeyText(keyCode)
          + ")";
    }

    int modifiersEx = e.getModifiersEx();
    String modString = "extended modifiers = " + modifiersEx;
    String tmpString = KeyEvent.getModifiersExText(modifiersEx);
    if (tmpString.length() > 0) {
      modString += " (" + tmpString + ")";
    } else {
      modString += " (no extended modifiers)";
    }

    String actionString = "action key? ";
    if (e.isActionKey()) {
      actionString += "YES";
    } else {
      actionString += "NO";
    }

    String locationString = "key location: ";
    int location = e.getKeyLocation();
    if (location == KeyEvent.KEY_LOCATION_STANDARD) {
      locationString += "standard";
    } else if (location == KeyEvent.KEY_LOCATION_LEFT) {
      locationString += "left";
    } else if (location == KeyEvent.KEY_LOCATION_RIGHT) {
      locationString += "right";
    } else if (location == KeyEvent.KEY_LOCATION_NUMPAD) {
      locationString += "numpad";
    } else { // (location == KeyEvent.KEY_LOCATION_UNKNOWN)
      locationString += "unknown";
    }

    displayArea.append(keyStatus + newline
        + "    " + keyString + newline
        + "    " + modString + newline
        + "    " + actionString + newline
        + "    " + locationString + newline);
    displayArea.setCaretPosition(displayArea.getDocument().getLength());
  }
}
