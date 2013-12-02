import java.net.Socket;
import java.util.Random;

//The myNetwork class determines if a packet will be dropped.
//Every myTCP packet send in the myTCP.java and OriginServer.java files have an 
//if statement around them calling the dropPacket function.
//Packets have a 1/DROP_RATE chance of being dropped
final class myNetwork 
{
	final int DROP_RATE = 25;  
	int drop = 0;
	static Socket socket;
	static String fileName;
	
	int dropPacket() 
	{
		int packetLossTrue = 0;
		Random rad = new Random();
		drop = rad.nextInt(DROP_RATE);
		
		if(packetLossTrue == drop)
			return 1;
		else
			return 0;
	}
}