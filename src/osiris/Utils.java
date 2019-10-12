package osiris;

/**
 *
 * @author ZEGEEK
 */
public class Utils {
    public static short getDataLength(byte[] buffer, short startIndex, byte delimiter) {
        short length = 0;
       
        for(short i = startIndex; i < buffer.length; i++) {
            if(buffer[i] == delimiter) {
                break;
            }
            length++;
        }
        
        return length;
    }
    
    public static byte[] getDataFromBuffer(byte[] buffer, short startIndex, short length) {
        byte[] bytes = new byte[length];
        
        for(short i = 0; i < length; i++) {
            bytes[i] = buffer[(short)(startIndex + i)];
        }
        return bytes;
    }
}
