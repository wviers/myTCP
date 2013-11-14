import java.net.* ;

public final class ProxyServer
{
	public static void main(String argv[]) throws Exception
	{
		int port = 800;
	    System.out.println("Server is ready");
		
	    @SuppressWarnings("resource")
		ServerSocket serverSocket = new ServerSocket(port);
		
		while (true) 
		{
			Socket clientSock = serverSocket.accept();
			
			// Construct an object to process the HTTP request message.
			HttpRequest request = new HttpRequest(clientSock);

			// Create a new thread to process the request.
			Thread thread = new Thread(request);  

			// Start the thread.
			thread.start();
		}
	}
}


