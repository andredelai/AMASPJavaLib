/*    
  Created by Andre L. Delai.

  This is a free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package AMASPJava;

import com.fazecast.jSerialComm.SerialPort;
import java.util.Arrays;

/**
 * AMASP Abstract class
 *
 * @author Andre L. Delai
 *
 */
public abstract class AMASPSerial {

    private ErrorCheckType errorCheckType = ErrorCheckType.None;
    
    
    /**
     * Enumeration to the packet types available in AMASP and the Timeout
     * condition.
     * MRP(0), SRP(1), SIP(2), CEP(3), Timeout(4).
     */
    public enum PacketType {
        MRP(0), SRP(1), SIP(2), CEP(3), Timeout(4);
        final private int typeValue;

        PacketType(int typeValue) {
            this.typeValue = typeValue;
        }

        public int getValue() {
            return typeValue;
        }
    }

    
    /**
     * Enumeration to the error checking algorithms available in AMASP.
     * None(0), XOR8(1), checksum16(2), LRC16(3), fletcher16(4), CRC16(5).
     */
    public enum ErrorCheckType {
        None(0), XOR8(1), checksum16(2), LRC16(3), fletcher16(4), CRC16(5);
        final private int echeckValue;

        ErrorCheckType(int echeckValue) {
            this.echeckValue = echeckValue;
        }
        
        static ErrorCheckType fromValue(int value) {
            for (ErrorCheckType my : ErrorCheckType.values()) {
                if (my.echeckValue == value) {
                    return my;
                }
            }
            return null;
        }
        
        public int getValue() {
            return echeckValue;
        }
    }
    
    /**
     * Sets the error checking algorithm to be used in the sent packets.
     *
     * @param errorCheckType The error checking algorithm.
     */
    public void setErrorCheckType(ErrorCheckType errorCheckType)
    {
        this.errorCheckType = errorCheckType;
    }
    
    /**
     * Gets the current error checking algorithm to be used in the sent packets.
     *
     * @return The current error checking algorithm.
     */
    public ErrorCheckType getErrorCheckType()
    {
        return errorCheckType;
    }
        
    /**
     * Store the data and metadata of the packet.
     */
    public class PacketData {

        /**
         * @return the type
         */
        public PacketType getType() {
            return type;
        }

        /**
         * @return the deviceId
         */
        public int getDeviceId() {
            return deviceId;
        }

        /**
         * @return the message
         */
        public byte[] getMessage() {
            return message;
        }
        
        /**
         * @return the errorCheckType
         */
        public ErrorCheckType getErrorCheckType()
        {
            return errorCheckType;
        }
        
        /**
         * @return the codeLength
         */
        public int getCodeLength() {
            return codeLength;
        }

        private PacketType type;
        private int deviceId;
        private byte[] message;
        private int codeLength;
        private ErrorCheckType errorCheckType;
    }

    public final int MSGMAXSIZE = 4096;
    private final int PKTMAXSIZE = MSGMAXSIZE + 15;

    SerialPort serialCom;

    /**
     * Establishes a serial connection.
     *
     * @param serialCom A JSerialComm object to serial communication.
     * @return True if the serial connection was stablished or false if not.
     */
    public boolean begin(SerialPort serialCom) {
        this.serialCom = serialCom;
        if (serialCom != null) {
            if (!serialCom.openPort()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Close the serial connection.
     */
    public void end() {
        if (serialCom != null) {
            serialCom.closePort();
        }
    }

    /**
     * Send a CEP packet (Communication Error Packet).
     *
     * @param deviceID Id of the target device in communication.
     * @param errorCode The communication error code (0 to 255).
     */
    public void sendError(int deviceID, int errorCode) {
        byte[] hex;
        byte[] pkt = new byte[14];

        //Packet Type
        pkt[0] = (byte) '!';
        pkt[1] = (byte) '~';
        //ECA
        hex = String.format("%1$01X", errorCheckType.ordinal()).getBytes();
        pkt[2] = (byte)hex[0];
        //Device ID
        hex = String.format("%1$03X", deviceID).getBytes();
        pkt[3] = (byte) hex[0];
        pkt[4] = (byte) hex[1];
        pkt[5] = (byte) hex[2];
        //Error Code       
        hex = String.format("%1$02X", errorCode).getBytes();
        pkt[6] = (byte) hex[0];
        pkt[7] = (byte) hex[1];
        //LRC
        hex = String.format("%1$04X", errorCheck(pkt, 8, getErrorCheckType())).getBytes();
        pkt[8] = (byte) hex[0];
        pkt[9] = (byte) hex[1];
        pkt[10] = (byte) hex[2];
        pkt[11] = (byte) hex[3];
        //Packet End
        pkt[12] = (byte) '\r';
        pkt[13] = (byte) '\n';

        serialCom.writeBytes(pkt, 14);
    }

    /**
     * Check if a valid packet is available and read it.
     *
     * @return A PacketData Object which contains the information and data from
     * a packet.
     */
    public PacketData readPacket() {
        PacketData pktData = new PacketData();
        byte[] buffer = new byte[PKTMAXSIZE];
        byte[] auxBuf = new byte[PKTMAXSIZE - 9];
        int aux;
        ErrorCheckType eCheck;
        double milisecPerByte = 1 / ((double) serialCom.getBaudRate() / 8000);

        pktData.type= PacketType.Timeout;
        pktData.deviceId = 0x000;
        pktData.codeLength = 0x000;
        pktData.message = null;

        try {
            while (serialCom.readBytes(buffer, 1) > 0) {
                if (buffer[0] == '!') {
                    //Reading packet type, crc check type and device ID bytes
                    aux = 0;
                    while (serialCom.bytesAvailable() < 5 && aux <= serialCom.getReadTimeout()) {
                        aux++;
                        Thread.currentThread().sleep(1);
                    }
                    if (serialCom.readBytes(auxBuf, 5) == 5) {
                        buffer[1] = auxBuf[0];//pkt type byte
                        buffer[2] = auxBuf[1];//error check type byte
                        buffer[3] = auxBuf[2];//device ID byte2
                        buffer[4] = auxBuf[3];//device ID byte1
                        buffer[5] = auxBuf[4];//devide ID byte0
                        //Pre-check of ECA value
                        if (buffer[2] < '0' || buffer[2] > '5')
                        {
                            //ECA no identified (ignore the packet) 
                            pktData.type = PacketType.Timeout;
                             return pktData;
                        }
                        //Extracting error check type                                   
                        eCheck = errorCheckType.fromValue(buffer[2] - '0');
                        
                        //Extracting device ID                      
                        try
                        {
                            pktData.deviceId = (Integer.parseInt(new String(Arrays.copyOfRange(buffer, 3, 6)), 16));
                        }
                        catch (Exception e)
                        {
                            //devide ID extracting error
                            return pktData;
                        }
                        switch (buffer[1]) {

                            //MRP packet
                            case (byte) '?':
                                //Reading error check type, device ID and msg length
                                aux = 0;
                                while (serialCom.bytesAvailable() < 3 && aux <= serialCom.getReadTimeout()) {
                                    aux++;
                                    Thread.currentThread().sleep(1);
                                }
                                if (serialCom.readBytes(auxBuf, 3) == 3) {
                                    System.arraycopy(auxBuf, 0, buffer, 6, 3);
                                                                        
                                    //Extracting message length
                                    pktData.codeLength = (Integer.parseInt(new String(Arrays.copyOfRange(buffer, 6, 9)), 16));

                                    //Reading message, error check data and end packet chars
                                    aux = 0;
                                    while (serialCom.bytesAvailable() <= (pktData.getCodeLength() + 6) && aux <= serialCom.getReadTimeout()) {
                                        aux++;
                                        Thread.currentThread().sleep(1);
                                    }
                                    if (serialCom.readBytes(auxBuf, pktData.getCodeLength() + 6) == pktData.getCodeLength() + 6) {
                                        System.arraycopy(auxBuf, 0, buffer, 9, pktData.getCodeLength() + 6);
                                        aux = Integer.parseInt(new String(Arrays.copyOfRange(buffer, pktData.getCodeLength() + 9, pktData.getCodeLength() + 13)), 16);
                                        //checking for errors
                                        if (aux == errorCheck(buffer, pktData.getCodeLength() + 9, eCheck)) {
                                            //End chars checking
                                            if (buffer[pktData.getCodeLength() + 13] == '\r' || buffer[pktData.getCodeLength() + 14] == '\n') {
                                                //Extracting message
                                                pktData.message = (new byte[pktData.getCodeLength()]);
                                                System.arraycopy(buffer, 9, pktData.getMessage(), 0, pktData.getCodeLength());
                                                pktData.type = (PacketType.MRP); //MRP recognized
                                                pktData.errorCheckType = eCheck;
                                                return pktData;
                                            }
                                        }
                                    }
                                }
                            break;
                            //SRP packet
                            case (byte) '#':
                                //Reading error check type, device ID and msg length
                                aux = 0;
                                while (serialCom.bytesAvailable() < 3 && aux <= serialCom.getReadTimeout()) {
                                    aux++;
                                    Thread.currentThread().sleep(1);
                                }
                                if (serialCom.readBytes(auxBuf, 3) == 3) {
                                    System.arraycopy(auxBuf, 0, buffer, 6, 3);
                                                                        
                                    //Extracting message length
                                    pktData.codeLength = (Integer.parseInt(new String(Arrays.copyOfRange(buffer, 6, 9)), 16));

                                    //Reading message, error check data and end packet chars
                                    aux = 0;
                                    while (serialCom.bytesAvailable() <= (pktData.getCodeLength() + 6) && aux <= serialCom.getReadTimeout()) {
                                        aux++;
                                        Thread.currentThread().sleep(1);
                                    }
                                    if (serialCom.readBytes(auxBuf, pktData.getCodeLength() + 6) == pktData.getCodeLength() + 6) {
                                        System.arraycopy(auxBuf, 0, buffer, 9, pktData.getCodeLength() + 6);
                                        aux = Integer.parseInt(new String(Arrays.copyOfRange(buffer, pktData.getCodeLength() + 9, pktData.getCodeLength() + 13)), 16);
                                        //checking for errors
                                        if (aux == errorCheck(buffer, pktData.getCodeLength() + 9, eCheck)) {
                                            //End chars checking
                                            if (buffer[pktData.getCodeLength() + 13] == '\r' || buffer[pktData.getCodeLength() + 14] == '\n') {
                                                //Extracting message
                                                pktData.message = (new byte[pktData.getCodeLength()]);
                                                System.arraycopy(buffer, 9, pktData.getMessage(), 0, pktData.getCodeLength());
                                                pktData.type = (PacketType.SRP); //MRP recognized
                                                pktData.errorCheckType = eCheck;
                                                return pktData;
                                            }
                                        }
                                    }
                                }
                            break;
                            //CEP packet
                            case (byte) '~':
                                //Reading error code
                                aux = 0;
                                while (serialCom.bytesAvailable() < 8 && aux <= serialCom.getReadTimeout()) {
                                    aux++;
                                    Thread.currentThread().sleep(1);
                                }
                                if (serialCom.readBytes(auxBuf, 8) == 8) {
                                    System.arraycopy(auxBuf, 0, buffer, 6, 8);
                                    aux = Integer.parseInt(new String(Arrays.copyOfRange(buffer, 8, 12)), 16);
                                    if (aux == errorCheck(buffer, 8, eCheck)) {
                                        //Extraction errorCode
                                        pktData.codeLength = (Integer.parseInt(new String(Arrays.copyOfRange(buffer, 6, 8)), 16));
                                        pktData.errorCheckType = eCheck;
                                        pktData.type = PacketType.CEP; //CEP recognized
                                        return pktData;
                                    }
                                }
                                break;

                            //SIP packet
                            case (byte) '!':
                                //Reading error code
                                aux = 0;
                                while (serialCom.bytesAvailable() < 8 && aux <= serialCom.getReadTimeout()) {
                                    aux++;
                                    Thread.currentThread().sleep(1);
                                }
                                if (serialCom.readBytes(auxBuf, 8) == 8) {
                                    System.arraycopy(auxBuf, 0, buffer, 6, 8);
                                    aux = Integer.parseInt(new String(Arrays.copyOfRange(buffer, 8, 12)), 16);
                                    if (aux == errorCheck(buffer, 8, eCheck)) {
                                        //Extraction errorCode
                                        pktData.codeLength = (Integer.parseInt(new String(Arrays.copyOfRange(buffer, 6, 8)), 16));
                                        pktData.errorCheckType = eCheck;
                                        pktData.type = PacketType.SIP; //CEP recognized
                                        return pktData;
                                    }
                                }
                                break;

                            default:
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
        return pktData;
    }

    
    protected short CRC16Check(byte[] data, int dataLength) {
        short crc = (short) 0xFFFF;
        for (int pos = 0; pos < dataLength; pos++) {
            crc ^=  (short)data[pos];          // XOR byte into least sig. byte of crc

            for (int i = 8; i != 0; i--) // Loop over each bit
            {                                 
                if ((crc & 0x0001) != 0) // If the LSB is set
                {
                    crc >>>= 1;
                    crc&= 0x7FFF;
                    crc ^= 0xA001; // Polynomial
                }
                else
                {
                    crc >>>= 1;
                    crc&= 0x7FFF;
                }
            }
        }
        // Note, this number has low and high bytes swapped, so use it accordingly (or swap bytes)
        return crc;
    }

    protected int LRC16Check(byte[] data, int dataLength) {
        int lrc = 0;
        for (int i = 0; i < dataLength; i++) {
            lrc = (lrc + data[i]) & 0xFFFF;
        }
        lrc = (((lrc ^ 0xFFFF) + 1) & 0xFFFF);
        return lrc;
    }

    protected int XORCheck(byte[] data, int dataLength) {
        byte xorCheck = 0;
        for (int i = 0; i < dataLength; i++) {
            xorCheck ^= data[i];
        }
        return (int)xorCheck;
    }

    //Classical checksum
    protected int checksum16Check(byte[] message, int dataLength) {
        int sum = 0;
        for (int i = 0; i < dataLength; i++) {
            sum += message[i];
        }
        return sum;
    }
    
    
    protected int fletcher16Checksum(byte[] data, int dataLength) {
        
        int sum1 = 0, sum2 = 0, index;

        for (index = 0; index < dataLength; ++index) {
            sum1 = (sum1 + data[index]) % 255;
            sum2 = (sum2 + sum1) % 255;
        }

        return (sum2 << 8) | sum1;
        
    }

    protected int errorCheck(byte[] data, int dataLength, ErrorCheckType eCheckType) {
        int ret;
        switch (eCheckType) {
            case XOR8:
                ret = XORCheck(data, dataLength);
                break;

            case checksum16:
                ret = checksum16Check(data, dataLength);
                break;

            case LRC16:
                ret = LRC16Check(data, dataLength);
                break;

            case fletcher16:
                ret = fletcher16Checksum(data, dataLength);
                break;

            case CRC16:
                ret = CRC16Check(data, dataLength);
                break;

            default:
                ret = 0x00;
                break;
        }
        return ret;
    }

}
