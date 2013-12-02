import java.net.Socket;
import java.util.Random;


final class myNetwork 
{
	int dropRate = 0;
	static Socket socket;
	static String fileName;
	
	@SuppressWarnings("unused") 
	int dropPacket() {
		int packetLossStat = 0;
		Random rad = new Random();
		dropRate = rad.nextInt(10);
		
		if(packetLossStat == dropRate)
			return 1;
		else
			return 0;
	}
}