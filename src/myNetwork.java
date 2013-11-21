import java.net.Socket;
import java.util.Random;


final class myNetwork implements Runnable
{
	int dropRate = 0;
	static Socket socket;
	static String fileName;
	
	public myNetwork(String name, Socket theSocket) {
		fileName = name;
		socket = theSocket;
	}
	
	private int dropPacket() {
		int packetLossStat = 0;
		Random rad = new Random();
		dropRate = rad.nextInt(5);
		
		if(packetLossStat == dropRate)
			return 1;
		else
			return 0;
	}

	@Override
	public void run() {
		try {
			myTCP request = new myTCP(fileName,socket);
			Thread thread = new Thread(request);  
			thread.start();
			thread.join(); 
		} catch(Exception E) {
			E.printStackTrace();
		}
	}
	
	

}