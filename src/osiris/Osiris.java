package osiris;

import javacard.framework.*;

/**
 *
 * @author ZEGEEK
 */
public class Osiris extends Applet {

    /*********************** Constants ***************************/ 
    public static final byte CLA_OSIRIS = (byte) 0x3A; 
    public static final byte INS_GET_DATA = 0x00;
    public static final byte INS_SET_DATA = 0x01;
    public static final byte INS_SET_NAME = 0x02;
    public static final byte INS_SET_BIRTHDATE = 0x03;
    public static final byte INS_RESET_DATA = 0x04;
    
    /*********************** Variables ***************************/
    // Unique identifier that represent the user
    private byte[] uid;
    
    // The name of the user
    private byte[] name;
    
    // The date of birth of the user
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
        uid = new byte[] { 0x65, 0x66, 0x67 };
        name = new byte[] { 0x68, 0x69, 0x70 };
        birthDate = new byte[] { 0x71, 0x72, 0x73 };
    }

    /**
     * Processes an incoming APDU.
     * 
     * @see APDU
     * @param apdu
     *            the incoming APDU
     */
    public void process(APDU apdu) {
        // Extract the value of the five first fields of the APDU sent: CLA, INS, P1, P2 and Lc.
        byte[] buffer = apdu.getBuffer();
        
        // Return and error if the applet is not selected
        if (selectingApplet()) return;
        
        if(buffer[ISO7816.OFFSET_CLA] != CLA_OSIRIS){
            // SW_CLA_NOT_SUPPORTED => SW1 = 0x6E et SW2=0x00
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
        
        switch(buffer[ISO7816.OFFSET_INS]) {
            case INS_GET_DATA:
                short i = 0;
                for(i = 0; i < uid.length; i++) {
                    buffer[i] = uid[i];
                }
                
                // Add a separator between uid and name
                buffer[i] = 0x7c;
                
                for(i = 0; i < name.length; i++) {
                    buffer[(short) (uid.length + 1 + i)] = name[i];
                }
                
                // Add a separator between name and birth date
                buffer[ (short) (uid.length + name.length + 1) ] = 0x7c;
                
                for(i = 0; i < birthDate.length; i++) {
                    buffer[ (short) (uid.length + name.length + 2 + i) ] = birthDate[i];
                }
                
                apdu.setOutgoingAndSend((short)0, (short) (uid.length + name.length + birthDate.length + 2));
                break;
            case INS_SET_DATA:
                
                break;
            case INS_SET_NAME:
                
                break;
            case INS_SET_BIRTHDATE:
                
                break;
            case INS_RESET_DATA:
                
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
