import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;


//The OriginServer class runs constantly waiting for a myTCP request for a handshake.
//When a packet with the FIN bit set high is received, a connection is established.
//The server then waits for the packet containing the name of the file to be transmitted.
//The server returns a 404 if the file is not found or sends the file requested.
//After all of the bytes of the file is sent the OriginServer class initiates the connection tear-down. 
public final class OriginServer
{
    static byte[] copyArray = new byte[4];
	static byte[] sendBuffer = new byte[1024];
	static final int port = 2000;
	static boolean currentlyShaking = false;
	static boolean currentlyConnected = false;
	static int TIMEOUT = 50;  						//Number of milliseconds the socket waits before recieve ends.
	static byte[] convertIntsSeq = new byte[4];
	static byte[] convertIntsAck = new byte[4];
	static myNetwork net = new myNetwork();
	static ArrayList<byte[]> senderBuffer = new ArrayList<byte[]>();
	
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
		DatagramPacket recievePacket  = new DatagramPacket(recieveBuffer, 1024);
		
		while(true) 
		{
			boolean checkAck = true;
			
			System.out.println("Waiting for packet");
			
			//Receive when a connection is in progress
			if(currentlyShaking == true) 
			{
				while(checkAck)
				{
		        	try 
		        	{
		        		serverSocket.receive(recievePacket);
		        		recieveBuffer = recievePacket.getData();
		        		if(returnInt(recieveBuffer[8],recieveBuffer[9],recieveBuffer[10],recieveBuffer[11]) == (returnInt(sendBuffer[4], sendBuffer[5], sendBuffer[6], sendBuffer[7]) + 1)) 
		        		{
		        			checkAck = false;
		        			currentlyShaking = false;
		        			currentlyConnected = true;
		        			System.out.println("1st Handshake recieved");
		        		}
		        	} 
		        	catch (InterruptedIOException e)   //Resends the SYN ACK if timeout occurs
		        	{
		        		sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
		        		serverSocket.send(sendPacket);
		        		System.out.println("Packet needs retransmission: Need SYN, ACK transmission");
		        	}
		        }
			}
			else  //Receive when a connection is not in progress
			{
				serverSocket.receive(recievePacket);
			    System.out.println("Recieved a packet");
			}
		    
		    recieveBuffer = recievePacket.getData();
		    
		    
		    //If received packet is a SYN
		    if(new Byte(recieveBuffer[13]).intValue() == 1) //SYN check
		    {    
		    	convertIntsSeq = (addIntsSeq(recieveBuffer,1));
		    	ConstructHeader(1, 1, 0, 0, returnInt(convertIntsSeq[4], convertIntsSeq[5], convertIntsSeq[6], convertIntsSeq[7]), recievePacket.getPort());
		        sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
		        
		        //Random chance to drop a packet
		        if(net.dropPacket() == 0)			   
		        	serverSocket.send(sendPacket);
		        
		    	currentlyShaking = true;
		    	
		    	serverSocket.setSoTimeout(TIMEOUT);    //Socket timeout activated now that server has made a connection
		    	System.out.println("Sent SYN back to Proxy");
		    }
		    //If received packet is a data request
		    else if(new Byte(recieveBuffer[14]).intValue() == 1 && currentlyConnected == true)
		    {	
		    	int count = 0;
		    	
		    	//parse filename from packet
		    	for(int i = 20; i < 1024; i++)
		    	{
		    		if(recieveBuffer[i] != 0)
		    			count++;
		    		temp[i - 20] = recieveBuffer[i];
		    	}
		    	String fileName = new String(temp);
		    	
				FileInputStream fis = null;
				boolean fileExists = true;
				
				try 
				{
					fis = new FileInputStream("./origin" + fileName.substring(1).trim());
				} 
				catch (FileNotFoundException e) 
				{
					fileExists = false;
				}
				

				if(fileExists) 
				{
					//Header for first data packet
					convertIntsAck = (addIntsAck(recieveBuffer,0));
					convertIntsSeq = addIntsSeq(recieveBuffer,count);
			       	ConstructHeader(0, 1, 0, returnInt(convertIntsAck[8], convertIntsAck[9],convertIntsAck[10],convertIntsAck[11]), returnInt(convertIntsSeq[4], convertIntsSeq[5], convertIntsSeq[6], convertIntsSeq[7]), recievePacket.getPort());

			       	int bytes = 0;
			        //Send all of the bytes
			       	while((bytes = fis.read(sendBuffer, 20, 1004)) != -1)
			       	{
			       		checkAck = true;
			       		
			       		//Condition to ensure no extra bytes are sent in the last packet
			       		if(bytes < 1004) 
			       		{
			       			byte[] lastPack = new byte[bytes + 20];
			       			
			       			for(int i = 0; i < bytes+20; i++)
			       				lastPack[i] = sendBuffer[i];
			       			
			       			sendPacket = new DatagramPacket(lastPack, lastPack.length, recievePacket.getAddress(), recievePacket.getPort());
			       			senderBuffer.add(lastPack);
			       			
					        //Random chance to drop a packet
					        if(net.dropPacket() == 0) 
					        	serverSocket.send(sendPacket);     
			       		}
			       		else 
			       		{	
			       			sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
			       			senderBuffer.add(sendBuffer);
			       			
			       			//Random chance to drop a packet
					        if(net.dropPacket() == 0) 
			       			    serverSocket.send(sendPacket);   
			       		}
				    	
				    	//recieve ACK for data
			    	    while(checkAck)
			    	    {
			            	try 
			            	{
			            		recievePacket = new DatagramPacket(recieveBuffer, recieveBuffer.length, sendPacket.getAddress(), sendPacket.getPort());
			            		serverSocket.receive(recievePacket);
			            		checkAckNumber(sendPacket.getData(), recievePacket.getData(), sendBuffer.length - 20);
			            		checkAck = false;
			            	} 
			            	catch (InterruptedIOException e) //Resend data ACK on timeout
			            	{
			            		serverSocket.send(sendPacket);
			            		System.out.println("Packet needs retransmission: data packet");
			            	}
			    	    }
	            		recieveBuffer = recievePacket.getData();
			    	    
	            		//Header for next data packet
			    	    convertIntsAck = addIntsAck(recieveBuffer,0);
						convertIntsSeq = addIntsSeq(recieveBuffer,0);
				       	ConstructHeader(0, 1, 0, returnInt(convertIntsAck[8], convertIntsAck[9],convertIntsAck[10],convertIntsAck[11]), returnInt(convertIntsSeq[4], convertIntsSeq[5], convertIntsSeq[6], convertIntsSeq[7]), recievePacket.getPort());
			       	}
			       	
			       	
			       	//Construct header for FIN packet since all data has been sent
			       	convertIntsAck = addIntsAck(recieveBuffer,0);
					convertIntsSeq = addIntsSeq(recieveBuffer,0);
			       	ConstructHeader(0, 1, 1, returnInt(convertIntsAck[8], convertIntsAck[9],convertIntsAck[10],convertIntsAck[11]), returnInt(convertIntsSeq[4], convertIntsSeq[5], convertIntsSeq[6], convertIntsSeq[7]), recievePacket.getPort());
			       	sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
			        
			      //Random chance to drop a packet
			       	if(net.dropPacket() == 0)
			        	serverSocket.send(sendPacket);
			       	System.out.println("Sent FIN");
			       	checkAck = true;
			       	
			        //Receive FIN-ACK
		    	    while(checkAck)
		    	    {
		            	try 
		            	{
		            		recievePacket = new DatagramPacket(recieveBuffer, recieveBuffer.length, sendPacket.getAddress(), sendPacket.getPort());
		            		serverSocket.receive(recievePacket);
		            		checkAckNumber(sendPacket.getData(), recievePacket.getData(), 1);
		            		checkAck = false;
		            	} 
		            	catch (InterruptedIOException e) 
		            	{
		            		serverSocket.send(sendPacket);
		            		System.out.println("Packet needs retransmission: FIN");
		            	}
		    	    }


	      			serverSocket.setSoTimeout(0);  //Server will go back to waiting state, so disable timeout
	      			

				    //ACK the proxie's FIN
				    ConstructHeader(0, 1, 0, ((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11]), ((recieveBuffer[4] << 24) + (recieveBuffer[5] << 16) + (recieveBuffer[6] << 8) + recieveBuffer[7]) + 1, recievePacket.getPort());
				    sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
			        
				    //Random chance to drop a packet
				    if(net.dropPacket() == 0)
			        	serverSocket.send(sendPacket);


				    System.out.println("CONNECTION CLOSED");
					System.out.println();
					System.out.println();
				    currentlyConnected = false;
				    
				  //Reset buffers
				    for(int i = 0; i < 1024; i++) 
				    {
				    	recieveBuffer[i] = 0;
				    	sendBuffer[i] = 0;
				    }
				    senderBuffer = new ArrayList<byte[]>(); 
				}
				else //Start tear down if file doesn't exist			
				{					
					//Send FIN
			       	convertIntsAck = addIntsAck(recieveBuffer,0);
					convertIntsSeq = addIntsSeq(recieveBuffer,0);
				   	ConstructHeader(0, 1, 1, returnInt(convertIntsAck[8], convertIntsAck[9],convertIntsAck[10],convertIntsAck[11]), returnInt(convertIntsSeq[4], convertIntsSeq[5], convertIntsSeq[6], convertIntsSeq[7]), recievePacket.getPort());

			    	sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
			    	
			    	//Random chance to drop a packet
			        if(net.dropPacket() == 0)
			        	serverSocket.send(sendPacket);
			    	System.out.println("Sent FIN");

			       	checkAck = true;
			       	
			        //receive FIN-ACK
		    	    while(checkAck)
		    	    {
		            	try
		            	{
		            		recievePacket = new DatagramPacket(recieveBuffer, recieveBuffer.length, sendPacket.getAddress(), sendPacket.getPort());
		            		serverSocket.receive(recievePacket);
		            		checkAckNumber(sendPacket.getData(), recievePacket.getData(), 1);
		            		checkAck = false;
		            	} 
		            	catch (InterruptedIOException e) 
		            	{
		            		serverSocket.send(sendPacket);
		            		System.out.println("Packet needs retransmission: FIN");
		            	}
		    	    }
	      			

				    //ACK the proxie's FIN-ACK
			       	convertIntsAck = addIntsAck(recieveBuffer,1);
					convertIntsSeq = addIntsSeq(recieveBuffer,0);
				    ConstructHeader(0, 1, 1, returnInt(convertIntsAck[8], convertIntsAck[9],convertIntsAck[10],convertIntsAck[11]), returnInt(convertIntsSeq[4], convertIntsSeq[5], convertIntsSeq[6], convertIntsSeq[7]), recievePacket.getPort());

				    sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
				    
				    //Random chance to drop a packet
			        if(net.dropPacket() == 0)
			        	serverSocket.send(sendPacket);

				    serverSocket.setSoTimeout(0);  //Server will go back to waiting state, so disable timeout

				    System.out.println("CONNECTION CLOSED");
					System.out.println();
					System.out.println();
				    currentlyConnected = false;
				    
				    //Reset buffers
				    for(int i = 0; i < 1024; i++) 
				    {
				      recieveBuffer[i] = 0;
				      sendBuffer[i] = 0;
				    }
				    senderBuffer = new ArrayList< byte[]>();
				}
		    }	    
		}
	}
	
	
	//This function Checks if the seq converted into an integer + off = the ack
	private static void checkAckNumber(byte[] seq, byte[] ack, int off) 
	{
		convertIntsSeq = addIntsSeq(seq,off);
		convertIntsAck = addIntsAck(ack,0);
		if(returnInt(convertIntsSeq[4],convertIntsSeq[5],convertIntsSeq[6],convertIntsSeq[7]) != returnInt(convertIntsAck[8],convertIntsAck[9],convertIntsAck[10],convertIntsAck[11]))
		  System.out.println("Ack does not = seq + data");
	}
	
	
	//This function converts the four bytes to an integer
	private static int returnInt(byte b1, byte b2, byte b3, byte b4) 
	{
		byte[] test = new byte[4];
	    test[0] = b1;
	    test[1] = b2;
	    test[2] = b3;
	    test[3] = b4;
	    ByteBuffer bb = ByteBuffer.wrap(test);
	    return bb.getInt();
	}
	
	
	//This function converts the seq number into an integer and adds the offset
	private static byte[] addIntsSeq(byte[] buffer, int off) 
	{
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
	
	
	//This function converts the ack number into an integer and adds the offset
	private static byte[] addIntsAck(byte[] buffer, int off) 
	{
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

	
	//This function fills the send buffer to set up the next send's myTCP header
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
		if(SYN == 1 && ACK == 1) //If first send, initialize a random seq #
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