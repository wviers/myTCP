import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Random;

public final class OriginServer
{
    static byte[] copyArray = new byte[4];
	static byte[] sendBuffer = new byte[1024];
	static final int port = 2000;
	
	
	public static void main(String argv[]) throws Exception
	{
		byte[] recieveBuffer = new byte[1024];
		byte[] temp = new byte[1024];
		
	    System.out.println("Origin Server is ready");
		@SuppressWarnings("resource")
		DatagramSocket serverSocket = new DatagramSocket(port);
		
		while(true) 
		{
			DatagramPacket recievePacket  = new DatagramPacket(recieveBuffer, 1024);
			
			System.out.println();
			System.out.println();
			System.out.println();
			System.out.println("Waiting for packet");
			serverSocket.receive(recievePacket);
		    System.out.println("Recieved a packet");
		    
		    recieveBuffer = recievePacket.getData();
		    temp = recievePacket.getData();
		    
		    
		    //Proxy sent handshake
		    if(new Byte(temp[13]).intValue() == 1)
		    {	
		    	ConstructHeader(1, 1, 0, 0, ((recieveBuffer[4] << 24) + (recieveBuffer[5] << 16) + (recieveBuffer[6] << 8) + recieveBuffer[7]) + 1, recievePacket.getPort());

		    	DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
		    	serverSocket.send(sendPacket);
		    	System.out.println("Sent SYN back to Proxy");
		    }
		    //Proxy sent data request
		    else if(new Byte(temp[14]).intValue() == 1)
		    {	
		    	int count = 0;
		    	for(int i = 21; i < 1024; i++)
		    	{
		    		if(recieveBuffer[i] != 0)
		    			count++;
		    		temp[i - 21] = recieveBuffer[i];
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
				
				if(fileExists) //Send all of the bytes
				{
			       	ConstructHeader(0, 1, 0, ((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11]), ((recieveBuffer[4] << 24) + (recieveBuffer[5] << 16) + (recieveBuffer[6] << 8) + recieveBuffer[7]) + count, recievePacket.getPort());
			       	
			       	while(fis.read(sendBuffer, 20, 1004) != -1)
			       	{
			       		DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
				    	serverSocket.send(sendPacket);
				    	ConstructHeader(0, 1, 0, ((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11] + 1004), ((recieveBuffer[4] << 24) + (recieveBuffer[5] << 16) + (recieveBuffer[6] << 8) + recieveBuffer[7]), recievePacket.getPort());
			       	}
			       	
			       	ConstructHeader(0, 1, 1, ((recieveBuffer[8] << 24) + (recieveBuffer[9] << 16) + (recieveBuffer[10] << 8) + recieveBuffer[11]), ((recieveBuffer[4] << 24) + (recieveBuffer[5] << 16) + (recieveBuffer[6] << 8) + recieveBuffer[7]), recievePacket.getPort());
		       		DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
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

			    	DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, recievePacket.getAddress(), recievePacket.getPort());
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
				    }
				    else
				    {
				    	System.out.println("TEARDOWN FAILED");
				    }
				}
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