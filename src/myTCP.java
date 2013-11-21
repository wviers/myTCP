import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.* ;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Timer;

final class myTCP implements Runnable
{
	final static String CRLF = "\r\n";
	static Socket socket;
	static String fileName;
	static int sendPort = 2000;
	static byte[] recieveData = new byte[1024];
	static byte[] sendData = new byte[1024];
	static byte[] temp = new byte[4];
	static byte[] combineData = null;
	static int port;
	static DatagramSocket serverSocket;
	static boolean fileExists;
	static int TIMEOUT = 500;
	static byte[] convertIntsSeq = new byte[4];
	static byte[] convertIntsAck = new byte[4];

	// Constructor
	public myTCP(String name, Socket theSocket) throws Exception 
	{
		fileName = name;
		socket = theSocket;
		serverSocket = new DatagramSocket();
		serverSocket.setSoTimeout(TIMEOUT);
		SetPort(serverSocket.getPort());
	}

	// Implement the run() method of the Runnable interface.
	public void run()
	{
		try 
		{
			processRequest();
			return;
		} 	
		catch (Exception e) 
		{
			System.out.println(e);
		}
	}

	private void processRequest() throws Exception
	{
		DatagramPacket recievePacket  = new DatagramPacket(recieveData, 1024);

		Handshake();

		DataLoop();
		
		System.out.println("After DataLoop");

		// Send the entity body to browser
		String statusLine = null;
		String contentTypeLine = null;
		String entityBody = null;

		if (fileExists) 
		{
			statusLine = "HTTP/1.1 200 OK: ";
			contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
		} 
		else
		{
			statusLine = "HTTP/1.1 404 Not Found";
			contentTypeLine = "Content-Type: text/html" + CRLF;
			entityBody = "<HTML>" + "<HEAD><TITLE>Not Found</TITLE></HEAD>" + "<BODY>HTTP/1.1 404 Not Found</BODY></HTML>";
		}

		FileInputStream fis = null;
		try 
		{
			fis = new FileInputStream(fileName);
		} 
		catch (FileNotFoundException e) 
		{
			fileExists = false;
		}

		DataOutputStream socketOut = new DataOutputStream(socket.getOutputStream());

		// Send the status line.
		socketOut.writeBytes(statusLine);

		// Send the content type line.
		socketOut.writeBytes(contentTypeLine);

		// Send a blank line to indicate the end of the header lines.
		socketOut.writeBytes(CRLF);

		// Send the entity body.

		if (fileExists)
		{
			sendBytes(fis, socketOut);
			fis.close();
		} 
		else 
		{
			socketOut.writeBytes(entityBody);
		}
        if(socketOut != null) 
		  socketOut.close();
		serverSocket.close();
		recieveData = new byte[1024];
		sendData = new byte[1024];
	}



	private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception
	{
		// Construct a 1K buffer to hold bytes on their way to the socket.
		byte[] buffer = new byte[1024];
		int bytes = 0;

		// Copy requested file into the socket's output stream.
		while((bytes = fis.read(buffer)) != -1 ) 
		{
			os.write(buffer, 0, bytes);
		}
	}


	private static String contentType(String fileName)
	{
		if(fileName.endsWith(".htm") || fileName.endsWith(".html")) 
		{
			return "text/html";
		}

		if(fileName.endsWith(".jpg")) 
		{
			return "image/jpeg";
		}

		if(fileName.endsWith(".pdf")) 
		{
			return "application/pdf";
		}

		return "application/octet-stream";
	}


	private static void Handshake() throws IOException
	{
		boolean checkAck = true;
		DatagramPacket recievePacket  = new DatagramPacket(recieveData, 1024);

		ConstructHeader(1, 0, 0, 0, 0);

		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, socket.getInetAddress(), sendPort);
		serverSocket.send(sendPacket);

		System.out.println("Proxy sent and should be waiting for origin");
		while(checkAck){
			try {
				serverSocket.receive(recievePacket);
				checkAckNumber(sendPacket.getData(), recievePacket.getData(), 1);
				checkAck = false;
				System.out.println("After proxy recieve");
			} catch (InterruptedIOException e) {
				serverSocket.send(sendPacket);
				System.out.println("Packet needs retransmission: SYN packet not acked");
			}

		}

		recieveData = recievePacket.getData();

		if(!(new Byte(recieveData[13]).intValue() == 1 && new Byte(recieveData[14]).intValue() == 1))
			System.out.println("ERROR SYN OR ACK");
		else
			System.out.println("Handshake Successful");
	}


	private static void DataLoop() throws IOException
	{
		int nameLength;
		boolean checkAck = true;

		DatagramPacket recievePacket  = new DatagramPacket(recieveData, 1024);
		//construct filename request(3rd shake)
		convertIntsSeq = addIntsSeq(recieveData,1);
		convertIntsAck = addIntsAck(recieveData,0);
		ConstructHeader(0, 1, 0, returnInt(convertIntsAck[8],convertIntsAck[9],convertIntsAck[10],convertIntsAck[11]), returnInt(convertIntsSeq[4],convertIntsSeq[5],convertIntsSeq[6],convertIntsSeq[7]));

		temp = fileName.getBytes();
		nameLength = temp.length;

		for(int i = 0; i < temp.length; i++)
		{
			sendData[i + 20] = temp[i];
		} 

		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, socket.getInetAddress(), sendPort);
		serverSocket.send(sendPacket);
		for(int i = 0; i < 1024; i++) {
			System.out.print(recieveData[i]);
		}
		System.out.println();
		System.out.println("Filename sent");


		fileExists = false;
		FileOutputStream os = null;
		boolean fileCreated = false;

		//Read all bytes from server till FIN

		while(new Byte(recieveData[15]).intValue() != 1)
		{
			checkAck = true;

			while(checkAck){
				try {
					serverSocket.receive(recievePacket);
					recieveData = recievePacket.getData();
					if(new Byte(recieveData[15]).intValue() != 1 && fileCreated == false) {
						os = new FileOutputStream(fileName.substring(2));
						fileExists = true;
						fileCreated = true;
					}
					if(new Byte(recieveData[15]).intValue() != 1)
					{
						os.write(recieveData, 20, 1004);
					}
					checkAck = false;
				} catch (InterruptedIOException e) {
					serverSocket.send(sendPacket);
					System.out.println("Packet needs retransmission: Data ACK");
				}

			}

			//send Ack for data
			if(new Byte(recieveData[15]).intValue() != 1)
			{
				convertIntsSeq = addIntsSeq(recieveData, recieveData.length-20);
				convertIntsAck = addIntsAck(recieveData,0);
				ConstructHeader(0, 1, 0, returnInt(convertIntsAck[8],convertIntsAck[9],convertIntsAck[10],convertIntsAck[11]), returnInt(convertIntsSeq[4],convertIntsSeq[5],convertIntsSeq[6],convertIntsSeq[7]));
				sendPacket = new DatagramPacket(sendData, sendData.length, recievePacket.getAddress(), recievePacket.getPort());
				serverSocket.send(sendPacket);
			}
		}

		RecieveCloseConn();
		System.out.println("Exit RecieveClose");
		if(os != null)
		  os.close();   
		System.out.println("Closing OS");
	}

	private static void checkAckNumber(byte[] seq, byte[] ack, int off) {
		convertIntsSeq = addIntsSeq(seq,off);
		convertIntsAck = addIntsAck(ack,0);
		if(returnInt(convertIntsSeq[4],convertIntsSeq[5],convertIntsSeq[6],convertIntsSeq[7]) != returnInt(convertIntsAck[8],convertIntsAck[9],convertIntsAck[10],convertIntsAck[11]))
			System.out.println("Ack does not = seq + data");
	}

	private static int returnInt(byte b1, byte b2, byte b3, byte b4) {
		byte[] test = new byte[4];
		test[0] = b1;
		test[1] = b2;
		test[2] = b3;
		test[3] = b4;
		ByteBuffer bb = ByteBuffer.wrap(test);
		return bb.getInt();
	}

	private static byte[] addIntsSeq(byte[] buffer, int off) {
		byte[] test = new byte[4];
		test[0] = buffer[4];
		test[1] = buffer[5];
		test[2] = buffer[6];
		test[3] = buffer[7];
		ByteBuffer bb = ByteBuffer.wrap(test);
		int sum = bb.getInt() + off;

		test = ByteBuffer.allocate(4).putInt(sum).array();
		buffer[4] = test[0];
		buffer[5] = test[1];
		buffer[6] = test[2];
		buffer[7] = test[3];

		return buffer;   
	}

	private static byte[] addIntsAck(byte[] buffer, int off) {
		byte[] test = new byte[4];
		test[0] = buffer[8];
		test[1] = buffer[9];
		test[2] = buffer[10];
		test[3] = buffer[11];
		ByteBuffer bb = ByteBuffer.wrap(test);
		int sum = bb.getInt() + off;

		test = ByteBuffer.allocate(4).putInt(sum).array();
		buffer[8] = test[0];
		buffer[9] = test[1];
		buffer[10] = test[2];
		buffer[11] = test[3];

		return buffer; 
	}

	private static void RecieveCloseConn() throws IOException
	{
		DatagramPacket recievePacket  = new DatagramPacket(recieveData, 1024);

		//ACK the origin's FIN with a FIN-ACK
		convertIntsSeq = addIntsSeq(recieveData, 1);
		convertIntsAck = addIntsAck(recieveData,0);
		ConstructHeader(0, 1, 1, returnInt(convertIntsAck[8],convertIntsAck[9],convertIntsAck[10],convertIntsAck[11]), returnInt(convertIntsSeq[4],convertIntsSeq[5],convertIntsSeq[6],convertIntsSeq[7]));
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, socket.getInetAddress(), sendPort);
		serverSocket.send(sendPacket);
		System.out.println("Proxy sent FIN and should be waiting for origin to ACK");
		int sendCount = 0;
		
		while(sendCount < 10){
			try {
				serverSocket.receive(recievePacket);
				recieveData = recievePacket.getData();
				sendCount = 10;
				System.out.println("After sending FIN-ACK");
			} catch (InterruptedIOException e) {
				sendCount++;
				serverSocket.send(sendPacket);
				System.out.println("Packet needs retransmission: FIN ACK");
			}

		}

		System.out.println("CONN successfully closed");
	}


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
			//int seq = gen.nextInt(127);
			int seq = 0;
			temp = ByteBuffer.allocate(4).putInt(seq).array();
			sendData[4] = temp[0];
			sendData[5] = temp[1];
			sendData[6] = temp[2];
			sendData[7] = temp[3];
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
		else
		{
			temp = ByteBuffer.allocate(4).putInt(0).array();
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


	private static int SetPort(int thePort)
	{
		return port = thePort;
	}
}
