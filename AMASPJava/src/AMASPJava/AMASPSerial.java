/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AMASPJava;

import com.fazecast.jSerialComm.SerialPort;
import java.util.Arrays;

/**
 * AMASP Abstract class
 * @author Andre L. Delai
 * 
 */

public abstract class AMASPSerial
{
    /**
     * Enumeration to the packet types available in AMASP and the Timeout condition.
     */
    public enum PacketType
    {
        MRP(0), SRP(1), SIP(2), CEP(3), Timeout(4);
        final private int typeValue;

        PacketType(int typeValue)
        {
            this.typeValue = typeValue;
        }

        public int getValor()
        {
            return typeValue;
        }
    }

    /**
     * Store the information and the data from a packet.
     */
    public class PacketData
    {

        /**
         * @return the type
         */
        public PacketType getType()
        {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(PacketType type)
        {
            this.type = type;
        }

        /**
         * @return the deviceId
         */
        public int getDeviceId()
        {
            return deviceId;
        }

        /**
         * @param deviceId the deviceId to set
         */
        public void setDeviceId(int deviceId)
        {
            this.deviceId = deviceId;
        }

        /**
         * @return the message
         */
        public byte[] getMessage()
        {
            return message;
        }

        /**
         * @param message the message to set
         */
        public void setMessage(byte[] message)
        {
            this.message = message;
        }

        /**
         * @return the codeLength
         */
        public int getCodeLength()
        {
            return codeLength;
        }

        /**
         * @param codeLength the codeLength to set
         */
        public void setCodeLength(int codeLength)
        {
            this.codeLength = codeLength;
        }
        private PacketType type;
        private int deviceId;
        private byte[] message;
        private int codeLength;
    }

    public final int MSGMAXSIZE = 4096;
    private final int PKTMAXSIZE = MSGMAXSIZE + 14;

    SerialPort serialCom;

    /**
     * Stablish a serial connection.
     * @param serialCom A JSerialComm object to serial communication.
     * @return True if the serial connection was stablished or false if not.
     */
    public boolean begin(SerialPort serialCom)
    {
        this.serialCom = serialCom;
        if (serialCom != null)
        {
            if (!serialCom.openPort())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Close the serial connection.
     */
    public void end()
    {
        if (serialCom != null)
        {
            serialCom.closePort();
        }
    }

    /**
     * Send a CEP packet (Communication Error Packet).
     * @param deviceID Id of the target device in communication.
     * @param errorCode The communication error code.
     */
    public void sendError(int deviceID, int errorCode)
    {
        byte[] hex;
        byte[] pkt = new byte[13];

        //Packet Type
        pkt[0] = (byte) '!';
        pkt[1] = (byte) '~';
        //Device ID
        hex = String.format("%1$03X", deviceID).getBytes();
        pkt[2] = (byte) hex[0];
        pkt[3] = (byte) hex[1];
        pkt[4] = (byte) hex[2];
        //Error Code       
        hex = String.format("%1$02X", errorCode).getBytes();
        pkt[5] = (byte) hex[0];
        pkt[6] = (byte) hex[1];
        //LRC
        hex = String.format("%1$04X", LRC(pkt, 7)).getBytes();
        pkt[7] = (byte) hex[0];
        pkt[8] = (byte) hex[1];
        pkt[9] = (byte) hex[2];
        pkt[10] = (byte) hex[3];
        //Packet End
        pkt[11] = (byte) '\r';
        pkt[12] = (byte) '\n';

        serialCom.writeBytes(pkt, 13);
    }

    
    /**
     * Check if a valid packet is available and read it.
     * @return A PacketData Object which contains the information and data from a packet.
     */
    public PacketData readPacket()
    {
        PacketData pktData = new PacketData();

        byte[] buffer = new byte[PKTMAXSIZE];
        byte[] auxBuf = new byte[PKTMAXSIZE - 8];
        int aux;
        double milisecPerByte = 1 / ((double) serialCom.getBaudRate() / 8000);

        pktData.setType(PacketType.Timeout);
        pktData.setDeviceId(0);
        pktData.setCodeLength(0);
        pktData.setMessage(null);

        try
        {
            while (serialCom.readBytes(buffer, 1) > 0)
            {
                if (buffer[0] == '!')
                {
                    //Reading packet type
                    if (serialCom.readBytes(auxBuf, 1) == 1)
                    {
                        buffer[1] = auxBuf[0];
                        switch (buffer[1])
                        {

                            //MRP packet
                            case (byte) '?':
                                //Reading device ID and msg length
                                aux = 0;
                                while (serialCom.bytesAvailable() < 6 && aux <= serialCom.getReadTimeout())
                                {
                                    aux++;
                                    Thread.currentThread().sleep(1);
                                }
                                if (serialCom.readBytes(auxBuf, 6) == 6)
                                {
                                    System.arraycopy(auxBuf, 0, buffer, 2, 6);
                                    //Extracting device ID

                                    //String straux = new String(Arrays.copyOfRange(buffer, 2, 5));
                                    pktData.setDeviceId(Integer.parseInt(new String(Arrays.copyOfRange(buffer, 2, 5)), 16));

                                    //Extracting message length
                                    pktData.setCodeLength(Integer.parseInt(new String(Arrays.copyOfRange(buffer, 5, 8)), 16));

                                    //Reading message, LRC and end packet chars
                                    aux = 0;
                                    while (serialCom.bytesAvailable() < (pktData.getCodeLength()) + 6 && aux <= serialCom.getReadTimeout())
                                    {
                                        aux++;
                                        Thread.currentThread().sleep(1);
                                    }
                                    if (serialCom.readBytes(auxBuf, pktData.getCodeLength() + 6) == pktData.getCodeLength() + 6)
                                    {
                                        System.arraycopy(auxBuf, 0, buffer, 8, pktData.getCodeLength() + 6);
                                        aux = Integer.parseInt(new String(Arrays.copyOfRange(buffer, pktData.getCodeLength() + 8, pktData.getCodeLength() + 12)), 16);
                                        //LRC checking
                                        if (aux == LRC(buffer, pktData.getCodeLength() + 8))
                                        {
                                            //End chars checking
                                            if (buffer[pktData.getCodeLength() + 12] == '\r' || buffer[pktData.getCodeLength() + 13] == '\n')
                                            {
                                                //Extracting message
                                                pktData.setMessage(new byte[pktData.getCodeLength()]);
                                                System.arraycopy(buffer, 8, pktData.getMessage(), 0, pktData.getCodeLength());
                                                pktData.setType(PacketType.MRP); //MRP recognized
                                            }
                                        }
                                    }
                                }

                            //SRP packet
                            case (byte) '#':
                                //Reading device ID and msg length
                                aux = 0;
                                while (serialCom.bytesAvailable() < 6 && aux <= serialCom.getReadTimeout())
                                {
                                    aux++;
                                    Thread.currentThread().sleep(1);
                                }
                                if (serialCom.readBytes(auxBuf, 6) == 6)
                                {
                                    System.arraycopy(auxBuf, 0, buffer, 2, 6);
                                    //Extracting device ID

                                    //String straux = new String(Arrays.copyOfRange(buffer, 2, 5));
                                    pktData.setDeviceId(Integer.parseInt(new String(Arrays.copyOfRange(buffer, 2, 5)), 16));

                                    //Extracting message length
                                    pktData.setCodeLength(Integer.parseInt(new String(Arrays.copyOfRange(buffer, 5, 8)), 16));

                                    //Reading message, LRC and end packet chars
                                    aux = 0;
                                    while (serialCom.bytesAvailable() < (pktData.getCodeLength()) + 6 && aux <= serialCom.getReadTimeout())
                                    {
                                        aux++;
                                        Thread.currentThread().sleep(1);
                                    }
                                    if (serialCom.readBytes(auxBuf, pktData.getCodeLength() + 6) == pktData.getCodeLength() + 6)
                                    {
                                        System.arraycopy(auxBuf, 0, buffer, 8, pktData.getCodeLength() + 6);
                                        aux = Integer.parseInt(new String(Arrays.copyOfRange(buffer, pktData.getCodeLength() + 8, pktData.getCodeLength() + 12)), 16);
                                        //LRC checking
                                        if (aux == LRC(buffer, pktData.getCodeLength() + 8))
                                        {
                                            //End chars checking
                                            if (buffer[pktData.getCodeLength() + 12] == '\r' || buffer[pktData.getCodeLength() + 13] == '\n')
                                            {
                                                //Extracting message
                                                pktData.setMessage(new byte[pktData.getCodeLength()]);
                                                System.arraycopy(buffer, 8, pktData.getMessage(), 0, pktData.getCodeLength());
                                                pktData.setType(PacketType.SRP); //SRP recognized
                                                return pktData;
                                            }
                                        }
                                    }
                                }

                            //CEP packet
                            case (byte) '~':
                                //Reading device ID and msg length
                                aux = 0;
                                while (serialCom.bytesAvailable() < 11 && aux <= serialCom.getReadTimeout())
                                {
                                    aux++;
                                    Thread.currentThread().sleep(1);
                                }
                                if (serialCom.readBytes(auxBuf, 11) == 11)
                                {
                                    System.arraycopy(auxBuf, 0, buffer, 2, 13);
                                    aux = Integer.parseInt(new String(Arrays.copyOfRange(buffer, 7, pktData.getCodeLength() + 11)), 16);
                                    if (aux == LRC(buffer, 7))
                                    {
                                        //Extracting device ID
                                        //String straux = new String(Arrays.copyOfRange(buffer, 2, 5));
                                        pktData.setDeviceId(Integer.parseInt(new String(Arrays.copyOfRange(buffer, 2, 5)), 16));

                                        //Extraction errorCode
                                        pktData.setCodeLength(Integer.parseInt(new String(Arrays.copyOfRange(buffer, 5, 7)), 16));

                                        pktData.setType(PacketType.CEP); //CEP recognized
                                        return pktData;
                                    }
                                }
                                break;

                            //SIP packet
                            case (byte) '!':
                                //Reading device ID and msg length
                                aux = 0;
                                while (serialCom.bytesAvailable() < 11 && aux <= serialCom.getReadTimeout())
                                {
                                    aux++;
                                    Thread.currentThread().sleep(1);
                                }
                                if (serialCom.readBytes(auxBuf, 11) == 11)
                                {
                                    System.arraycopy(auxBuf, 0, buffer, 2, 13);
                                    aux = Integer.parseInt(new String(Arrays.copyOfRange(buffer, 7, pktData.getCodeLength() + 11)), 16);
                                    if (aux == LRC(buffer, 7))
                                    {
                                        //Extracting device ID
                                        //String straux = new String(Arrays.copyOfRange(buffer, 2, 5));
                                        pktData.setDeviceId(Integer.parseInt(new String(Arrays.copyOfRange(buffer, 2, 5)), 16));

                                        //Extraction interruptCode
                                        pktData.setCodeLength(Integer.parseInt(new String(Arrays.copyOfRange(buffer, 5, 7)), 16));

                                        pktData.setType(PacketType.SIP); //SIP recognized
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
        } catch (Exception e)
        {

        }
        return pktData;
    }

    
    /**
     * Calculates the LRC (Longitudinal Reduncancy Check).
     * @param data Bytes to calculate the LRC.
     * @param dataLength Data length.
     * @return the calculated LRC.
     */
    protected int LRC(byte[] data, int dataLength)
    {
        int lrc = 0;
        for (int i = 0; i < dataLength; i++)
        {
            lrc = (lrc + data[i]) & 0xFFFF;
        }
        lrc = (((lrc ^ 0xFFFF) + 1) & 0xFFFF);
        return lrc;
    }
    
    protected int CRC16(byte[] data, int dataLength)
    {
        int crc = 0x0000;

            for (int i = 0; i < dataLength; i++)
            {
                crc = crc16_DNP(crc, data[i]);
            }
            
            return (~crc);
    }
    
    private int crc16_DNP(int crcValue,  byte newByte) // DNP, IEC 870, M-BUS, wM-BUS, ...
        {
            int i;

            final int POLY = 0x3D65;

            for (i = 0; i < 8; i++)
            {

                if ((((crcValue & 0x8000) >> 8) ^ (newByte & 0x80)) != 0)
                {
                    crcValue = (crcValue << 1) ^ POLY;
                }
                else
                {
                    crcValue = (crcValue << 1);
                }

                newByte <<= 1;
            }

            return crcValue;
        }

}
