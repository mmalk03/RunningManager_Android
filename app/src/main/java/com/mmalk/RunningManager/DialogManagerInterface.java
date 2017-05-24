package com.mmalk.RunningManager;

/**
 * interface used to send data from threads communicating with server
 * back to the thread handling user interface
 */

public interface DialogManagerInterface {

    /**
     * method overridden in Result class, to update dialog box
     * "str" is a parameter specified in threads communicating with the external server
     * it says what message should appear on the screen
     */
    void updateDialog(String str);
}
