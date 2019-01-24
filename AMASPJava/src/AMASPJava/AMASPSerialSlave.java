/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AMASPJava;

/**
 * AMASP Slave class
 * @author delai
 * 
 */
public class AMASPSerialSlave extends AMASPSerial
{

    /**
     * Send a SRP (Slave Response Packet) to a master computer.
     * @param deviceID Id of the slave device who answered. 
     * @param message The response message to be send.
     * @param msgLength The message length.
     */
    public void sendResponse(int deviceID, byte[] message, int msgLength)
    {
        byte[] hex;
        
        if (message.length < msgLength)
        {
            msgLength = message.length; //saturating
        }

        //mounting the packet
        byte[] pkt = new byte[msgLength + 15];

        //Packet Type
        pkt[0] = (byte) '!';
        pkt[1] = (byte) '#';
        //ECA
        hex = String.format("%1$01X", getErrorCheckType().ordinal()).getBytes();
        pkt[2] = (byte)hex[0];
        //Device ID
        hex = String.format("%1$03X", deviceID).getBytes();
        pkt[3] = (byte) hex[0];
        pkt[4] = (byte) hex[1];
        pkt[5] = (byte) hex[2];
        //Message Length
        hex = String.format("%1$03X", msgLength).getBytes();
        pkt[6] = (byte) hex[0];
        pkt[7] = (byte) hex[1];
        pkt[8] = (byte) hex[2];
        //Message (payload)
        for (int i = 0; i < msgLength; i++)
        {
            pkt[9 + i] = (byte) message[i];
        }
        //CRC       
        hex = String.format("%1$04X", errorCheck(pkt, msgLength + 9)).getBytes();
        pkt[9 + msgLength] = (byte) hex[0];
        pkt[9 + msgLength + 1] = (byte) hex[1];
        pkt[9 + msgLength + 2] = (byte) hex[2];
        pkt[9 + msgLength + 3] = (byte) hex[3];
        //Packet End
        pkt[9 + msgLength + 4] = (byte) '\r';
        pkt[9 + msgLength + 5] = (byte) '\n';

        //Sending request
        serialCom.writeBytes(pkt, 15 + msgLength);
    }
    
    /**
     * Send a SRP (Slave Response Packet) to a master computer.
     * @param deviceID Id of the slave device who answered. 
     * @param message The response message to be send.
     * @param msgLength The message length.
     */
    public void sendResponse(int deviceID, String message, int msgLength)
    {
        sendResponse(deviceID, message.getBytes(), msgLength);
    }
    
    /**
     * Send a SIP (Slave Interrupt Packet). 
     * @param deviceID Id of the slave device who generated the interruption.
     * @param InterrupCode The code of the interruption.
     */
    public void sendInterruption(int deviceID, int InterrupCode)
    {
        byte[] hex;
        byte[] pkt = new byte[14];

        //Packet Type
        pkt[0] = (byte) '!';
        pkt[1] = (byte) '!';
        //ECA
        hex = String.format("%1$01X", getErrorCheckType().ordinal()).getBytes();
        pkt[2] = (byte)hex[0];
        //Device ID
        hex = String.format("%1$03X", deviceID).getBytes();
        pkt[3] = (byte) hex[0];
        pkt[4] = (byte) hex[1];
        pkt[5] = (byte) hex[2];
        //Error Code       
        hex = String.format("%1$02X", InterrupCode).getBytes();
        pkt[6] = (byte) hex[0];
        pkt[7] = (byte) hex[1];
        //CRC
        hex = String.format("%1$04X", errorCheck(pkt, 7)).getBytes();
        pkt[8] = (byte) hex[0];
        pkt[9] = (byte) hex[1];
        pkt[10] = (byte) hex[2];
        pkt[11] = (byte) hex[3];
        //Packet End
        pkt[12] = (byte) '\r';
        pkt[13] = (byte) '\n';
    }

}
