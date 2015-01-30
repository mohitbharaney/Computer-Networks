import java.nio.ByteBuffer;



public class HandshakeMessage extends Message{

	private byte[] header="HELLO".getBytes();
	private byte[] zeroBitArray=new byte[23];
	private int peerID;

	public HandshakeMessage(int peerId){
		this.peerID=peerId;
	}

	@Override
	protected byte[] getMessageBytes() {
		ByteBuffer res=ByteBuffer.allocate(32);
		res.put(header).put(zeroBitArray).putInt(peerID);
		return res.array();

	}

}
