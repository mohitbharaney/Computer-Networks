
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Util {

	private static final String commonConfig = "Common.cfg";
	private static final String peerConfig = "PeerInfo.cfg";
	private static final String filePath = "/";
	public static Integer myPeerId;
	private static final String payLoadPath = "/";
	private static final ConcurrentHashMap<Integer, Peer> peerMap= new ConcurrentHashMap<Integer, Peer>();
	public static Integer noPreferredNeighbours=0;
	public static Integer unchokingIntegererval=0;
	public static Integer optimUnchokingIntegererval=0;
	public static Integer pieceSize=0;
	public static Integer fileSize=0;
	public static  String fileName = "";
	public static ConcurrentHashMap<Integer,Thread> connectionMap =new ConcurrentHashMap<Integer,Thread>();
	public static ConcurrentHashMap<Integer,BitField> bitfieldMap=new ConcurrentHashMap<Integer,BitField>();
	public static ConcurrentHashMap<Integer,ConnectionHandler> connectionHandlerMap=new ConcurrentHashMap<Integer, ConnectionHandler>();
	public static Lock lock=new ReentrantLock();
	public static volatile boolean interruptFlag=false;
	public static volatile boolean terminationFlag=true;
	public static Integer peerWithFile=0;
	public static boolean[] requested;
	public static volatile boolean stopFalg=true;
	
	public static void setPeerId(Integer getPeerId)
	{
		myPeerId=getPeerId;
	}
	
	public static Integer getPeerId()
	{
		return myPeerId;
	}
	
	public static byte[] getBitField(Integer getPeerId)
	{
		return bitfieldMap.get(getPeerId).get();
	}
	
	public static byte[] getBitField()
	{
		return bitfieldMap.get(myPeerId).get();
	}
	
	public static String getCommonConfig() {
		return commonConfig;
	}
	
	public static String getPeerConfig() {
		return peerConfig;
	}
	
	public static String getFilePath() {
		return filePath;
	}
	public static String getPayLoadPath() {
		return payLoadPath;
	}
	
	public static ConcurrentHashMap<Integer, Peer> getPeermap() {
		return peerMap;
	}
	
	/*
	 * getBitFieldSizze returns the no of pieces, not the no of bytes in bitfield
	 */
	public static Integer getBitFieldSize()
	{
		Integer size=(int) Math.ceil(fileSize.doubleValue()/(pieceSize.doubleValue()));
		return size;
	}
	
	public static ConcurrentHashMap<Integer,Peer> readPeerConfig() throws FileNotFoundException{
		Integer peerID=0;
		String host="";
		Integer port=0;
		Integer hasFile=0;
	
		
		String fName = Util.getPeerConfig();
		Scanner sc = new Scanner(new File(fName));
		
		while(sc.hasNext()){
		
			peerID = sc.nextInt();
			host = sc.next();
			port = sc.nextInt();
			hasFile = sc.nextInt();
			if(hasFile==1)
				peerWithFile=peerID;
			
			peerMap.put(peerID, new Peer(new Integer(peerID),host,port,hasFile));
		}
		
		return peerMap;
	}
	 
	public static void readCommonConfig() throws FileNotFoundException
	 {
		String fName = Util.getCommonConfig();
		Scanner sc = new Scanner(new File(fName));
		sc = new Scanner(new File(fName));
		sc.next();
		noPreferredNeighbours = sc.nextInt();
		sc.next();
		unchokingIntegererval = sc.nextInt();
		sc.next();
		optimUnchokingIntegererval = sc.nextInt();
		sc.next();
		fileName = sc.next();
		sc.next();
		fileSize=sc.nextInt();
		sc.next();
		pieceSize=sc.nextInt();
		sc.close();
	
		requested=new boolean[(int)(fileSize.doubleValue()/pieceSize.doubleValue())+2];
		
	}
}
