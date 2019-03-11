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
     * @param message The message in bytes to be send.
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
        byte[] pkt = new byte[msgLength + 15];
        //Packet Type
        pkt[0] = (byte) '!';
        pkt[1] = (byte) '?';
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
        //Error checking      
        hex = String.format("%1$04X", (short)errorCheck(pkt, msgLength + 9, getErrorCheckType())).getBytes();
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
     * Send a MRP packet to a slave computer.
     * @param deviceID Id of the requested device in slave. 
     * @param message The string message to be send.
     * @param msgLength The message length.
     */
    public void sendRequest(int deviceID, String message, int msgLength)
    {       
        sendRequest(deviceID, message.getBytes(), msgLength);
    }
}
