import java.net.* ;


//The ProxyServer class waits for a request from the browser.
//When a request is received, it creates an HttpRequest thread to handle it and goes back to waiting for the browser. 
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
			System.out.println();
			System.out.println();
		    System.out.println("Server is ready");
			Socket clientSock = serverSocket.accept();
			
			// Construct an object to process the HTTP request message.
			HttpRequest request = new HttpRequest(clientSock);

			// Create a new thread to process the request.
			Thread thread = new Thread(request);  

			// Start the thread.
			thread.start();
			
			thread.join();
		}
	}
}


