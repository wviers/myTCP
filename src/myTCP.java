import java.io.IOException;
import java.net.* ;
import java.nio.ByteBuffer;
import java.util.Random;

final class myTCP implements Runnable
{
	final static String CRLF = "\r\n";
	static Socket socket;
	static String fileName;
	static int port = 800;
	static int sendPort = 2000;
    static byte[] recieveData = new byte[1024];
    static byte[] sendData = new byte[1024];
    static byte[] temp = new byte[4];

	// Constructor
	public myTCP(String name, Socket theSocket) throws Exception 
	{
		fileName = name;
		socket = theSocket;
	}

	// Implement the run() method of the Runnable interface.
	public void run()
	{
		try 
		{
			processRequest();
		} 	
		catch (Exception e) 
		{
			System.out.println(e);
		}
	}

	private void processRequest() throws Exception
	{
		Handshake();
		//temp = fileName.getBytes();
		
		//for(int i = 0; i < temp.length && i < 1024; i++)
		//{
		//    sendData[i + 21] = temp[i];
		//} 
	}
	
	private static void Handshake() throws IOException
	{
		@SuppressWarnings("resource")
		DatagramSocket serverSocket = new DatagramSocket();
		DatagramPacket recievePacket  = new DatagramPacket(recieveData, 1024);
		
		ConstructHeader(1, 0, 0, 0, 0);

	    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, socket.getInetAddress(), sendPort);
	    serverSocket.send(sendPacket);
	    
	    System.out.println("Proxy sent and should be waiting for origin");
		serverSocket.receive(recievePacket);
	    System.out.println("After proxy recieve");

	    recieveData = recievePacket.getData();
	    
		System.out.println("THE + 1 VAL");
		System.out.println((recieveData[8] << 24) + (recieveData[9] << 16) + (recieveData[10] << 8) + recieveData[11]);
	    
	    if(!(new Byte(recieveData[13]).intValue() == 1 && new Byte(recieveData[14]).intValue() == 1))
	    	System.out.println("ERROR SYN OR ACK");
	    else
	    	System.out.println("Handshake Successful");
	}
	
	//	in case I need this    String sentence = new String(receivePacket.getData());
	
	private static void ConstructHeader(int SYN, int ACK, int FIN, int seqNumber, int ackNumber)
	{
		//byte 0-1: source port #
		temp = ByteBuffer.allocate(4).putInt(port).array();
		sendData[0] = temp[2];
		sendData[1] = temp[3];
		
		//byte 2-3: destination port #
		temp = ByteBuffer.allocate(4).putInt(sendPort).array();
		sendData[2] = temp[2];
		sendData[3] = temp[3];
		
		//byte 4-7: seq #
		if(SYN == 1 && ACK == 0)
		{
			Random gen = new Random();
			int seq = gen.nextInt(127);
			System.out.println("THE PROXY SEQ");
			System.out.println(seq);
			temp = ByteBuffer.allocate(4).putInt(seq).array();
			sendData[4] = temp[0];
			sendData[5] = temp[1];
			sendData[6] = temp[2];
			sendData[7] = temp[3];
			System.out.println((sendData[4] << 24) + (sendData[5] << 16) + (sendData[6] << 8) + sendData[7]);
			System.out.println(String.format("%8s", Integer.toBinaryString(sendData[4] & 0xFF)).replace(' ', '0') + String.format("%8s", Integer.toBinaryString(sendData[5] & 0xFF)).replace(' ', '0') + String.format("%8s", Integer.toBinaryString(sendData[6] & 0xFF)).replace(' ', '0') + String.format("%8s", Integer.toBinaryString(sendData[7] & 0xFF)).replace(' ', '0'));
		}
		else
		{
			temp = ByteBuffer.allocate(4).putInt(seqNumber).array();
			sendData[4] = temp[0];
			sendData[5] = temp[1];
			sendData[6] = temp[2];
			sendData[7] = temp[3];
		}
		
		//byte 8-11: ack #
		if(ACK == 1)
		{
			temp = ByteBuffer.allocate(4).putInt(ackNumber).array();
			sendData[8] = temp[0];
			sendData[9] = temp[1];
			sendData[10] = temp[2];
			sendData[11] = temp[3];
		}
		
		//byte 12: data offset
		temp = ByteBuffer.allocate(4).putInt(20).array();
		sendData[12] = temp[3];
		
		//byte 13: SYN flag
		temp = ByteBuffer.allocate(4).putInt(SYN).array();
		sendData[13] = temp[3];
		
		//byte 14: ACK flag
		temp = ByteBuffer.allocate(4).putInt(ACK).array();
		sendData[14] = temp[3];
		
		//byte 15: FIN flag
		temp = ByteBuffer.allocate(4).putInt(FIN).array();
		sendData[15] = temp[3];
		
		//byte 16-17: Window Size
		temp = ByteBuffer.allocate(4).putInt(1).array();
		sendData[16] = temp[2];
		sendData[17] = temp[3];		
	}
}
