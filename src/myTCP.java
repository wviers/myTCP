import java.net.* ;

final class myTCP implements Runnable
{
	final static String CRLF = "\r\n";
	Socket socket;
	String fileName;
	int port = 800;
	int sendPort = 2000;

	// Constructor
	public myTCP(String name, Socket socket) throws Exception 
	{
		fileName = name;
		this.socket = socket;
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
	    byte[] receiveData = new byte[1024];
	    byte[] sendData = new byte[1024];
		@SuppressWarnings("resource")
		DatagramSocket serverSocket = new DatagramSocket();
		DatagramPacket receivePacket  = new DatagramPacket(receiveData, 1024);


	    sendData = fileName.getBytes();
	    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, socket.getInetAddress(), sendPort);
	    serverSocket.send(sendPacket);

	    System.out.println("Proxy sent and should be waiting for origin");
		serverSocket.receive(receivePacket);
	    System.out.println("After proxy recieve");
	    String sentence = new String(receivePacket.getData());
	    System.out.println("RECEIVED: " + sentence);
	    InetAddress IPAddress = receivePacket.getAddress();

	    /* Receive
	    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	    serverSocket.receive(receivePacket);
	    String sentence = new String( receivePacket.getData());
	    System.out.println("RECEIVED: " + sentence);
	    InetAddress IPAddress = receivePacket.getAddress();
	    */
	}
}
