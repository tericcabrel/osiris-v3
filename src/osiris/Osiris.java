/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package osiris;

import javacard.framework.*;

/**
 *
 * @author ZEGEEK
 */
public class Osiris extends Applet {

    /******************** Constants ************************/ 
    public static final byte CLA_OSIRIS = (byte) 0X3A; 
    public static final byte INS_INCREMENTER_COMPTEUR= 0x00; 
    
    private byte[] uid;
    private byte[] name;
    private byte[] birthDate;
    
    /**
     * Installs this applet.
     * 
     * @param bArray
     *            the array containing installation parameters
     * @param bOffset
     *            the starting offset in bArray
     * @param bLength
     *            the length in bytes of the parameter data in bArray
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Osiris();
    }

    /**
     * Only this class's install method should create the applet object.
     */
    protected Osiris() {
        register();
    }

    /**
     * Processes an incoming APDU.
     * 
     * @see APDU
     * @param apdu
     *            the incoming APDU
     */
    public void process(APDU apdu) {
        //Insert your code here
    }
}
