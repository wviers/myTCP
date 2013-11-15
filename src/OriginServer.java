import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Random;

public final class OriginServer
{
    static byte[] copyArray = new byte[4];
	static byte[] sendBuffer = new byte[1024];
	static int port = 2000;
	
	public static void main(String argv[]) throws Exception
	{

		byte[] buffer = new byte[1024];
		byte[] temp = new byte[1024];
		
	    System.out.println("Origin Server is ready");
		
	    @SuppressWarnings("resource")
		DatagramSocket serverSocket = new DatagramSocket(port);
		
		while(true) 
		{
			DatagramPacket recievePacket  = new DatagramPacket(buffer, 1024);
			
			System.out.println("Waiting for packet");
			serverSocket.receive(recievePacket);
		    System.out.println("Recieved a packet");
		    
		    buffer = recievePacket.getData();
		    temp = recievePacket.getData();
		    
	    	//for(int i = 21; i < 1024; i++)
	    	//	temp[i - 21] = buffer[21];
		    
		    //Proxy sent handshake
		    if(new Byte(temp[13]).intValue() == 1)
		    {	
		    	System.out.println(String.format("%8s", Integer.toBinaryString(temp[4] & 0xFF)).replace(' ', '0') + String.format("%8s", Integer.toBinaryString(temp[5] & 0xFF)).replace(' ', '0') + String.format("%8s", Integer.toBinaryString(temp[6] & 0xFF)).replace(' ', '0') + String.format("%8s", Integer.toBinaryString(temp[7] & 0xFF)).replace(' ', '0'));
		    	ConstructHeader(1, 1, 0, 0, ((temp[4] << 24) + (temp[5] << 16) + (temp[6] << 8) + temp[7]) + 1, recievePacket.getPort());

		    	DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
		    	serverSocket.send(sendPacket);
		    	System.out.println("Sent a packet back to Proxy");
		    }
		}
	}
	
	
	private static void ConstructHeader(int SYN, int ACK, int FIN, int seqNumber, int ackNumber, int sendPort)
	{
		//byte 0-1: source port #
		copyArray = ByteBuffer.allocate(4).putInt(port).array();
		sendBuffer[0] = copyArray[2];
		sendBuffer[1] = copyArray[3];
		
		//byte 2-3: Dest port #
		copyArray = ByteBuffer.allocate(4).putInt(sendPort).array();
		sendBuffer[2] = copyArray[2];
		sendBuffer[3] = copyArray[3];
		
		//byte 4-7: seq #
		if(SYN == 1 && ACK == 1)
		{
			Random gen = new Random();
			int seq = gen.nextInt(127);
			copyArray = ByteBuffer.allocate(4).putInt(seq).array();
			sendBuffer[4] = copyArray[0];
			sendBuffer[5] = copyArray[1];
			sendBuffer[6] = copyArray[2];
			sendBuffer[7] = copyArray[3];
		}
		else
		{
			copyArray = ByteBuffer.allocate(4).putInt(seqNumber).array();
			sendBuffer[4] = copyArray[0];
			sendBuffer[5] = copyArray[1];
			sendBuffer[6] = copyArray[2];
			sendBuffer[7] = copyArray[3];
		}
		
		//byte 8-11: ack #
		if(ACK == 1)
		{
			copyArray = ByteBuffer.allocate(4).putInt(ackNumber).array();
			sendBuffer[8] = copyArray[0];
			sendBuffer[9] = copyArray[1];
			sendBuffer[10] = copyArray[2];
			sendBuffer[11] = copyArray[3];
		}
		
		//byte 12: data offset
		copyArray = ByteBuffer.allocate(4).putInt(20).array();
		sendBuffer[12] = copyArray[3];
		
		//byte 13: SYN flag
		copyArray = ByteBuffer.allocate(4).putInt(SYN).array();
		sendBuffer[13] = copyArray[3];
		
		//byte 14: ACK flag
		copyArray = ByteBuffer.allocate(4).putInt(ACK).array();
		sendBuffer[14] = copyArray[3];
		
		//byte 15: FIN flag
		copyArray = ByteBuffer.allocate(4).putInt(FIN).array();
		sendBuffer[15] = copyArray[3];
		
		//byte 16-17: Window Size
		copyArray = ByteBuffer.allocate(4).putInt(1).array();
		sendBuffer[16] = copyArray[2];
		sendBuffer[17] = copyArray[3];		
	}
}