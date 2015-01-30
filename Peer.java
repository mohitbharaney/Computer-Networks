
import java.util.ArrayList;


public class Peer {

	private int peerID;
	private String host;
	private int port;
	private int hasFile;
	//The file to be distributed among peers
	private String fileName;
	private int fileSize;
	private int noPreferredNeighbours;
	private int unchokingInterval;
	private int optimUnchokingInterval;
	private int pieceSize;
	public int originalHasFile;
	
	private boolean connArray[];
	//This contains bit fields of all the peers
	private byte[] bitfieldArray;
	//This maintains the choked neighbours.
	private ArrayList<Integer> chokedNeighbours = new ArrayList<Integer>();
	//This contains the peers from which data is being downloaded
	private int downloadNeighbours;
	
	public Peer(int id,String host,int port, int hasFile){
		this.peerID=id;
		this.host=host;
		this.port=port;
		this.hasFile=hasFile;
		originalHasFile=hasFile;
	}	
	
	public int getPeerID() {
		return peerID;
	}
	public void setPeerID(int peerID) {
		this.peerID = peerID;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int isHasFile() {
		return hasFile;
	}
	public void setHasFile(int hasFile) {
		this.hasFile = hasFile;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public int getFileSize() {
		return fileSize;
	}
	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}
	public int getNoPreferredNeighbours() {
		return noPreferredNeighbours;
	}
	public void setNoPreferredNeighbours(int noPreferredNeighbours) {
		this.noPreferredNeighbours = noPreferredNeighbours;
	}
	public int getUnchokingInterval() {
		return unchokingInterval;
	}
	public void setUnchokingInterval(int unchokingInterval) {
		this.unchokingInterval = unchokingInterval;
	}
	public int getOptimUnchokingInterval() {
		return optimUnchokingInterval;
	}
	public void setOptimUnchokingInterval(int optimUnchokingInterval) {
		this.optimUnchokingInterval = optimUnchokingInterval;
	}
	public int getPieceSize() {
		return pieceSize;
	}
	public void setPieceSize(int pieceSize) {
		this.pieceSize = pieceSize;
	}
	public boolean[] getConnArray() {
		return connArray;
	}
	public void setConnArray(boolean[] connArray) {
		this.connArray = connArray;
	}
	public byte[] getBitfieldArray() {
		return bitfieldArray;
	}
	public void setBitfieldArray(byte[] bitfieldArray) {
		this.bitfieldArray = bitfieldArray;
	}
	public ArrayList<Integer> getChokedNeighbours() {
		return chokedNeighbours;
	}
	public void setChokedNeighbour(Integer neigh) {
		this.chokedNeighbours.add(neigh);
	}
	public int getDownloadNeighbours() {
		return downloadNeighbours;
	}
	public void setDownloadNeighbours(int downloadNeighbours) {
		this.downloadNeighbours = downloadNeighbours;
	}
}


