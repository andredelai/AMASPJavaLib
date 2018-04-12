/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AMASPJava;

/**
 * AMASP Master class
 * @author Andre L. Delai
 * 
 */
public class AMASPSerialMaster extends AMASPSerial
{

    /**
     * Send a MRP packet to a slave computer.
     * @param deviceID Id of the requested device in slave. 
     * @param message The message to be send.
     * @param msgLength The message length.
     */
    public void sendRequest(int deviceID, byte message[], int msgLength)
    {
        byte[] hex;
        
        if (message.length < msgLength)
        {
            msgLength = message.length; //saturating
        }

        //mounting the packet
        byte[] pkt = new byte[msgLength + 14];

        //Packet Type
        pkt[0] = (byte) '!';
        pkt[1] = (byte) '?';
        //Device ID
        hex = String.format("%1$03X", deviceID).getBytes();
        pkt[2] = (byte) hex[0];
        pkt[3] = (byte) hex[1];
        pkt[4] = (byte) hex[2];
        //Message Length
        hex = String.format("%1$03X", msgLength).getBytes();
        pkt[5] = (byte) hex[0];
        pkt[6] = (byte) hex[1];
        pkt[7] = (byte) hex[2];
        //Message (payload)
        for (int i = 0; i < msgLength; i++)
        {
            pkt[8 + i] = (byte) message[i];
        }
        //CRC       
        hex = String.format("%1$03X", CRC16SerialModbus(pkt, msgLength + 8)).getBytes();
        pkt[8 + msgLength] = (byte) hex[0];
        pkt[8 + msgLength + 1] = (byte) hex[1];
        pkt[8 + msgLength + 2] = (byte) hex[2];
        pkt[8 + msgLength + 3] = (byte) hex[3];
        //Packet End
        pkt[8 + msgLength + 4] = (byte) '\r';
        pkt[8 + msgLength + 5] = (byte) '\n';

        //Sending request
        serialCom.writeBytes(pkt, 14 + msgLength);
    }

    public void sendRequest(int deviceID, String message, int msgLength)
    {       
        sendRequest(deviceID, message.getBytes(), msgLength);
    }
}
