import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public final class OriginServer
{
    static byte[] copyArray = new byte[4];
	static byte[] sendBuffer = new byte[1024];
	static final int port = 2000;
	static boolean currentlyShaking = false;
	static int TIMEOUT = 500;
	
	public static void main(String argv[]) throws Exception
	{
		byte[] recieveBuffer = new byte[1024];
		byte[] temp = new byte[1024];
		byte[] convertIntsSeq = new byte[4];
		byte[] convertIntsAck = new byte[4];
		
	    System.out.println("Origin Server is ready");
		@SuppressWarnings("resource")
		DatagramSocket serverSocket = new DatagramSocket(port);
		DatagramPacket sendPacket;
		
		while(true) 
		{
			boolean checkAck = true;
			DatagramPacket recievePacket  = new DatagramPacket(recieveBuffer, 1024);
			
			System.out.println();
			System.out.println();
			System.out.println();
			System.out.println("Waiting for packet");
			
			//loop for resending handshake
			if(currentlyShaking==true) {
				while(checkAck){
		        	try {
		        		serverSocket.receive(recievePacket);
		        		recieveBuffer = recievePacket.getData();
		        		if(returnInt(recieveBuffer[8],recieveBuffer[9],recieveBuffer[10],recieveBuffer[11]) == (returnInt(sendBuffer[4], sendBuffer[5], sendBuffer[6], sendBuffer[7]) + 1)) {
		        			serverSocket.setSoTimeout(0);
		        			checkAck = false;
		        			currentlyShaking = false;
		        			System.out.println("1st Handshake recieved");
		        		}
		        	} catch (InterruptedIOException e) {
		        		sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
		        		serverSocket.send(sendPacket);
		        		System.out.println("Packet needs retransmission: Need SYN, ACK transmission");
		        	}

		        }
			}
			else {
				serverSocket.receive(recievePacket);
			    System.out.println("Recieved a packet");
			}
		    
		    recieveBuffer = recievePacket.getData();
		    temp = recievePacket.getData();
		    
		    
		    //Proxy sent handshake
		    if(new Byte(temp[13]).intValue() == 1) //SYN check
		    {    
		    	convertIntsSeq = (addIntsSeq(recieveBuffer,1));
		    	ConstructHeader(1, 1, 0, 0, returnInt(convertIntsSeq[4], convertIntsSeq[5], convertIntsSeq[6], convertIntsSeq[7]), recievePacket.getPort());
		        
		    	sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
		    	serverSocket.send(sendPacket);
		    	currentlyShaking = true;
		    	serverSocket.setSoTimeout(TIMEOUT);
		    	System.out.println("Sent SYN back to Proxy");
		    }
		    //Proxy sent data request
		    else if(new Byte(temp[14]).intValue() == 1)
		    {	
		    	int count = 0;
		    	for(int i = 20; i < 1024; i++)
		    	{
		    		if(recieveBuffer[i] != 0)
		    			count++;
		    		temp[i - 20] = recieveBuffer[i];
		    	}
		    	String fileName = new String(temp);
		    	
				FileInputStream fis = null;
				boolean fileExists = true;
				
				File file = new File("./origin" + fileName.substring(1).trim());
				try 
				{
					fis = new FileInputStream(file);
				} 
				catch (FileNotFoundException e) 
				{
					fileExists = false;
				}
				
				boolean waitForACK = false;
				if(fileExists) //Send all of the bytes
				{
					convertIntsAck = (addIntsAck(recieveBuffer,0));
					convertIntsSeq = addIntsSeq(recieveBuffer,count);
			       	ConstructHeader(0, 1, 0, returnInt(convertIntsAck[0], convertIntsAck[1], convertIntsAck[2], convertIntsAck[3]), returnInt(convertIntsSeq[0], convertIntsSeq[1],convertIntsSeq[2],convertIntsSeq[3]), recievePacket.getPort());

			       	int bytes = 0;
			       	while((bytes = fis.read(sendBuffer, 20, 1004)) != -1)
			       	{
			       		if(bytes < 1004) {
			       			byte[] lastPack = new byte[bytes+20];
			       			for(int i = 0; i < bytes+20; i++)
			       				lastPack[i] = sendBuffer[i];
			       			sendPacket = new DatagramPacket(lastPack, lastPack.length, recievePacket.getAddress(), recievePacket.getPort());
			       			serverSocket.send(sendPacket);	
			       			System.out.println("if");
			       		}
			       		else {	
			       			sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
			       			serverSocket.send(sendPacket);	
			       		}
				    	
				    	//recieve ACK for data
				    	recievePacket = new DatagramPacket(recieveBuffer, recieveBuffer.length, sendPacket.getAddress(), sendPacket.getPort());
				    	serverSocket.receive(recievePacket);
				    	recieveBuffer = recievePacket.getData();
				    	
				    	checkAckNumber(recieveBuffer, bytes);
				    	
				    	ConstructHeader(0, 1, 0, ((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11]), ((recieveBuffer[4] << 24) + (recieveBuffer[5] << 16) + (recieveBuffer[6] << 8) + recieveBuffer[7]), recievePacket.getPort());
				    	
				    	//Possible cum ack implementation
				    	/*serverSocket.receive(recievePacket);
					    recieveBuffer = recievePacket.getData();
					    
				    	if(((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11]) == ((sendBuffer[8] << 24) + (sendBuffer[9] << 16) + (sendBuffer[10] << 8) + sendBuffer[11]) + 1004)
				    	{
				    		ConstructHeader(0, 1, 0, ((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11]), ((recieveBuffer[4] << 24) + (recieveBuffer[5] << 16) + (recieveBuffer[6] << 8) + recieveBuffer[7]), recievePacket.getPort());
				    	}
				    	else
				    	{
				    		
				    	}
				    	*/
			       	}
			       	
			       	ConstructHeader(0, 1, 1, ((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11]), ((recieveBuffer[4] << 24) + (recieveBuffer[5] << 16) + (recieveBuffer[6] << 8) + recieveBuffer[7]), recievePacket.getPort());
		       		sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
			    	serverSocket.send(sendPacket);
			       	
			    	System.out.println("Sent all of the packets for the file.");
			       	System.out.println();
			       	System.out.println("Sent FIN");
			       	
			        //recieve ACK
			    	serverSocket.receive(recievePacket);		    
				    recieveBuffer = recievePacket.getData();
				    
				    if((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11] == (sendBuffer[4] << 24) + (sendBuffer[5] << 16) + (sendBuffer[6] << 8) + sendBuffer[7] + 1)
				    {	
				    	System.out.println("FIN CORRECTLY ACKED");
					    
				    	serverSocket.receive(recievePacket);		    
					    recieveBuffer = recievePacket.getData();
					    
					    //ACK the proxie's FIN
						ConstructHeader(0, 1, 0, ((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11]), ((recieveBuffer[4] << 24) + (recieveBuffer[5] << 16) + (recieveBuffer[6] << 8) + recieveBuffer[7]) + 1, recievePacket.getPort());
					    sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
					    serverSocket.send(sendPacket);
					    
					    currentlyShaking = false;
				    }
				    else
				    {
				    	System.out.println("TEARDOWN FAILED");
				    }
				}
				else //Start teardown				
				{
					//Send FIN
			       	ConstructHeader(0, 1, 1, ((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11]), ((recieveBuffer[4] << 24) + (recieveBuffer[5] << 16) + (recieveBuffer[6] << 8) + recieveBuffer[7]) + count, recievePacket.getPort());

			    	sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
			    	serverSocket.send(sendPacket);
			    	System.out.println("Sent FIN");

			    	//recieve ACK
			    	serverSocket.receive(recievePacket);		    
				    recieveBuffer = recievePacket.getData();
				    
				    if((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11] == (sendBuffer[4] << 24) + (sendBuffer[5] << 16) + (sendBuffer[6] << 8) + sendBuffer[7] + 1)
				    {	
				    	System.out.println("FIN CORRECTLY ACKED");
					    
				    	serverSocket.receive(recievePacket);		    
					    recieveBuffer = recievePacket.getData();
					    
					    //ACK the proxie's FIN
						ConstructHeader(0, 1, 0, ((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11]), ((recieveBuffer[4] << 24) + (recieveBuffer[5] << 16) + (recieveBuffer[6] << 8) + recieveBuffer[7]) + 1, recievePacket.getPort());
					    sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
					    serverSocket.send(sendPacket);
					    
					    currentlyShaking = false;
				    }
				    else
				    {
				    	System.out.println("TEARDOWN FAILED");
				    }
				}
		    }	    
		}
	}
	
	private static void checkAckNumber(byte[] recieveBuffer, int bytes) {
		if(((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11]) != ((sendBuffer[4] << 24) + (sendBuffer[5] << 16) + (sendBuffer[6] << 8) + sendBuffer[7]) + bytes)
		  System.out.println("Seq + data.length != ack Number");
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