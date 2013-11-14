import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public final class OriginServer
{
	public static void main(String argv[]) throws Exception
	{
		int port = 2000;
		int sendPort = 800;
		byte[] buffer = new byte[1024];
		byte[] sendBuffer = new byte[1024];
		String message = "HELLO";
		
	    System.out.println("Origin Server is ready");
		
	    @SuppressWarnings("resource")
		DatagramSocket serverSocket = new DatagramSocket(port);
		
		while(true) 
		{
			DatagramPacket recievePacket  = new DatagramPacket(buffer, 1024);
			
			System.out.println("Waiting for packet");
			serverSocket.receive(recievePacket);
			
		    String sentence = new String(recievePacket.getData());
		    System.out.println("RECEIVED: " + sentence);
		    InetAddress IPAddress = recievePacket.getAddress();
		    
		    
		    sendBuffer = message.getBytes();
		    DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, IPAddress, recievePacket.getPort());
		    serverSocket.send(sendPacket);
			System.out.println("Sent a packet back to Proxy");
		}
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
}