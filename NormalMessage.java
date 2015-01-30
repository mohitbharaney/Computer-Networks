import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;	


public class NormalMessage extends Message{
	public int msgLength;
	public byte type;
	private byte[] payLoad;
	private Integer pieceNumber;
	static Logger log = Logger.getLogger(NormalMessage.class);
	public NormalMessage(byte type)
	{
		this.type=type;
		msgLength=getLength(type);
	}
	public NormalMessage(byte type, int pieceNum){
		this.type=type;
		this.pieceNumber=pieceNum;
		msgLength=getLength(type);
	}

	@Override
	protected byte[] getMessageBytes() {
		ByteBuffer res=ByteBuffer.allocate(msgLength+4);
		if(type<4)
		{
			res.putInt(msgLength).put(type);
			return res.array();
		}
		else if(type==4||type==6)
		{
			res.putInt(msgLength).put(type).putInt(pieceNumber);
			return res.array();
		}
		else if(type==5)
		{
			res.putInt(msgLength).put(type).put(Util.getBitField());
			return res.array();
		}
		/*else if(type==6)
		{
			res.putInt(msgLength).put(type).putInt(pieceNumber);
			return res.array();
		}*/
		else if(type==7)
		{
//			log.info("making msg for request "+pieceNumber);
			//byte[] payLoad=new byte[msgLength-5];
			byte[] payLoad=new byte[msgLength-5];
			if(Util.getPeermap().get(Util.myPeerId).originalHasFile==1)
			{
		
				String fName=Util.myPeerId.toString()+"/"+Util.fileName;
				FileInputStream in;
				
				try {
					in=new FileInputStream(new File(fName));
					in.skip((pieceNumber-1)*Util.pieceSize);
					if(pieceNumber==Util.getBitFieldSize())
					{
						in.read(payLoad,0 , Util.fileSize-((pieceNumber-1)*Util.pieceSize));
						log.info("last piece bytes are "+(Util.fileSize-((pieceNumber-1)*Util.pieceSize)));
					}
					else
					in.read(payLoad,0 , Util.pieceSize);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
			{
				
			String fName=Util.myPeerId.toString()+"/"+pieceNumber.toString();
			FileInputStream in;
			//byte[] payLoad=new byte[Util.pieceSize];
			try {
				in = new FileInputStream(new File(fName));
				in.read(payLoad);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			}
			res.putInt(msgLength).put(type).putInt(pieceNumber).put(payLoad);
			return res.array();
		}
		return res.array();
	}

	
	private int getLength(byte type)
	{
		if(type<4)
		{
			return 1;
		}
		else if(type==4||type==6)
		{
			return 5;
		}
		else if(type==5)
		{
			return 1+(int)Math.ceil(Util.getBitFieldSize().doubleValue()/8);
		}
		else if(type==7)
		{
			if(pieceNumber==Util.getBitFieldSize())
				return 1+(Util.fileSize-(pieceNumber-1)*Util.pieceSize)+4;
			return 1+Util.pieceSize+4;
		}
		else return 0;
	}
	
	

}
