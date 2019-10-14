package osiris;

import javacard.framework.*;

/**
 *
 * @author ZEGEEK
 */
public class Osiris extends Applet {

    /*********************** Constants ***************************/ 
    private static final byte CLA_OSIRIS = (byte) 0x3A; 
    private static final byte INS_GET_DATA = 0x00;
    private static final byte INS_SET_DATA = 0x01;
    private static final byte INS_SET_NAME = 0x02;
    private static final byte INS_SET_BIRTHDATE = 0x03;
    private static final byte INS_RESET_DATA = 0x04;
    private final static byte INS_PIN_AUTH = (byte) 0x05;
    private final static byte INS_PIN_UNBLOCK = (byte) 0x06;
    
    private static final byte DATA_DELIMITER = 0x7c;
    
    // Maximum number of incorrect tries before the PIN is blocked
    private final static byte PIN_TRY_LIMIT =(byte)0x03;
  
    // Maximum size PIN
    private final static byte MAX_PIN_SIZE =(byte)0x06;
    
    // Signal that the PIN verification failed
    // Decimal value: 25344
    private final static short SW_VERIFICATION_FAILED = 0x6300;
   
    // Signal the PIN validation is required for an action
    // Decimal value: 25345
    private final static short SW_PIN_VERIFICATION_REQUIRED = 0x6301;
    
    
    /*********************** Variables ***************************/
    // Object responsible for PIN Management
    OwnerPIN pin;
    
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
        (new Osiris()).register();
    }

    /**
     * Only this class's install method should create the applet object.
     */
    protected Osiris() {
        // Initialize PIN configuration
        pin = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);
   
        // Set the default PIN Code to 123456
        pin.update(new byte[]{ 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 }, (short) 0, (byte) 6);

        uid = new byte[] { };
        name = new byte[] { };
        birthDate = new byte[] { };
    }

    /*public boolean select() {
        // The applet declines to be selected if the pin is blocked.
        if ( pin.getTriesRemaining() == 0 ) return false;
        return true;
    }*/
    
    public void deselect() {
        // reset the pin value
        pin.reset();
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
        
        if(pin.getTriesRemaining() == 0 && buffer[ISO7816.OFFSET_INS] != INS_PIN_UNBLOCK) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        switch(buffer[ISO7816.OFFSET_INS]) {
            case INS_GET_DATA:
                short i = 0;
                for(i = 0; i < uid.length; i++) {
                    buffer[i] = uid[i];
                }
                
                // Add a separator between uid and name
                buffer[i] = DATA_DELIMITER;
                
                for(i = 0; i < name.length; i++) {
                    buffer[(short) (uid.length + 1 + i)] = name[i];
                }
                
                // Add a separator between name and birth date
                buffer[ (short) (uid.length + name.length + 1) ] = DATA_DELIMITER;
                
                for(i = 0; i < birthDate.length; i++) {
                    buffer[ (short) (uid.length + name.length + 2 + i) ] = birthDate[i];
                }
                
                apdu.setOutgoingAndSend((short)0, (short) (uid.length + name.length + birthDate.length + 2));
                break;
            case INS_SET_DATA:
                apdu.setIncomingAndReceive();
                
                JCSystem.beginTransaction();
                    short uidLength = Utils.getDataLength(buffer, ISO7816.OFFSET_CDATA, DATA_DELIMITER);
                    uid = Utils.getDataFromBuffer(buffer, ISO7816.OFFSET_CDATA, uidLength);

                    short nameStartIndex = (short) (ISO7816.OFFSET_CDATA + uidLength + 1);
                    short nameLength = Utils.getDataLength(buffer, nameStartIndex, DATA_DELIMITER);
                    name = Utils.getDataFromBuffer(buffer, nameStartIndex, nameLength);

                    short birthDateLength = (short) (apdu.getIncomingLength() - (uidLength + nameLength + 2));
                    birthDate = Utils.getDataFromBuffer(buffer, (short) (nameStartIndex + nameLength + 1), birthDateLength);
                JCSystem.commitTransaction();
                break;
            case INS_SET_NAME:
                apdu.setIncomingAndReceive();
                // TODO Make sure it's not empty
                name = Utils.getDataFromBuffer(buffer, ISO7816.OFFSET_CDATA, apdu.getIncomingLength());
                break;
            case INS_SET_BIRTHDATE:
                apdu.setIncomingAndReceive();
               
                // TODO validate date format with regex
                birthDate = Utils.getDataFromBuffer(buffer, ISO7816.OFFSET_CDATA, apdu.getIncomingLength());
                break;
            case INS_RESET_DATA:
                if (!pin.isValidated()) {
                    ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
                }
    
                uid = new byte[] { };
                name = new byte[] { };
                birthDate = new byte[] { };
                break;
            case INS_PIN_AUTH:
                byte byteRead = (byte)(apdu.setIncomingAndReceive());
   
                // Check pin the PIN data is read into the APDU buffer
                // at the offset ISO7816.OFFSET_CDATA the PIN data length = byteRead
                if (pin.check(buffer, ISO7816.OFFSET_CDATA, byteRead) == false) {
                    ISOException.throwIt(SW_VERIFICATION_FAILED);
                }
                break;
            case INS_PIN_UNBLOCK:
                pin.resetAndUnblock();
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
