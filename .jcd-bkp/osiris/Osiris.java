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
    private final static byte INS_SET_FINGERPRINT = (byte) 0x07;
    private final static byte INS_GET_FINGERPRINT = (byte) 0x08;
    
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
    
    // The fingerprint's template of the user
    private byte[] fingerPrint;
    
    byte[] fingerInfo;
    byte[] dataCount;
    
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
        //--JCD-INSTALL
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
        fingerPrint = new byte[] { };
    }

    public boolean select() {
        // The applet declines to be selected if the pin is blocked.
        // if ( pin.getTriesRemaining() == 0 ) return false;
        
        fingerInfo = JCSystem.makeTransientByteArray((short)3, JCSystem.CLEAR_ON_DESELECT);
        dataCount = JCSystem.makeTransientByteArray((short)1, JCSystem.CLEAR_ON_DESELECT);
    
        fingerInfo[0] = 0x00;
        fingerInfo[1] = 0x00;
        fingerInfo[2] = 0x00;
        dataCount[0] = 0x00;
        
        return true;
    }
    
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
        //--JCD-PROCESS{apdu}
        
        // Extract the value of the five first fields of the APDU sent: CLA, INS, P1, P2 and Lc.
        byte[] buffer = apdu.getBuffer();
        
        // Return and error if the applet is not selected
        if (selectingApplet()) return;
        
        if(buffer[ISO7816.OFFSET_CLA] != CLA_OSIRIS){
            // SW_CLA_NOT_SUPPORTED => SW1 = 0x6E and SW2=0x00
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
                byte fpLength = (byte) fingerPrint.length;
                //! calling doFoo with buffer {fpLength}
                short finalLength = (short) (uid.length + name.length + birthDate.length + 2 + 2);
                
                buffer[(short)(finalLength - 2)] = DATA_DELIMITER;
                buffer[(short)(finalLength - 1)] = fpLength;
                
                apdu.setOutgoingAndSend((short)0, finalLength);
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
                fingerPrint = new byte[] { };
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
            case INS_SET_FINGERPRINT:
                byte bufferDataLength = (byte)(apdu.setIncomingAndReceive());
                
                //We want to start data copy
                if (fingerInfo[0] == 0x00) {
                    // If the length equal to 0, it means we want to clear data
                    if(bufferDataLength == 3 && buffer[ISO7816.OFFSET_CDATA] == 0x00) {
                        fingerPrint = new byte[] { };
                    } else {
                        fingerInfo[0] = (byte) 638;
                        fingerInfo[1] = buffer[ISO7816.OFFSET_CDATA + 1];
                        fingerInfo[2] = (byte) 7;
                        fingerPrint = new byte[(short) 638];
                    }
                } else {
                    // copying the apdu data into byte array Data
                    // array copy: (src, offset, target, offset,copy size)
                    Util.arrayCopy(
                            buffer,
                            (short) ISO7816.OFFSET_CDATA,
                            fingerPrint,
                            (short) ((short) dataCount[0] * 100),
                            (short) bufferDataLength
                    );
                    dataCount[0] = (byte) ((short) dataCount[0] + 1);
                    
                    // If all data copied, reset the info
                    if ((short)dataCount[0] > (short) fingerInfo[2]) {
                        fingerInfo = new byte[]{ 0x00, 0x00, 0x00 };
                        dataCount[0] = (byte) 0;
                    }
                }
                    
                // TODO Make sure it's not empty
                // fingerPrint = Utils.getDataFromBuffer(buffer, ISO7816.OFFSET_CDATA, apdu.getIncomingLength());
                break;
            case INS_GET_FINGERPRINT:
                // apdu.setOutgoing();
                // apdu.setOutgoingLength((short)fingerPrint.length);
                // apdu.sendBytesLong(fingerPrint, (short)0, (short)fingerPrint.length);
                    short toSend = (short) fingerPrint.length;
                    short counter = 0;
                    
                    try {
                        apdu.setOutgoing();
                        apdu.setOutgoingLength(toSend);
                        
                        while (toSend > 0) {
                            Util.arrayCopyNonAtomic(fingerPrint, (short) (counter * 32), buffer, (short) 0, (short) 32);
                            apdu.sendBytes((short) 0, (short) 32);
                            toSend = (short) (toSend - 32);
                            counter = (byte) (counter + 1);
                        }
                    } catch (Exception e) {
                        if (e instanceof APDUException) {
                            APDUException ae = (APDUException) e;
                            short reason = ae.getReason();
                            if (reason == APDUException.BAD_LENGTH)
                                ISOException.throwIt((short) 0x9990);
                            else
                                ISOException.throwIt((short) 0x8887);
                        } else {
                            ISOException.throwIt((short) 0x8888);
                        }
                    }
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
