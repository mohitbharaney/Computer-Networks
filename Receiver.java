import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.log4j.Logger;

public class Receiver implements Runnable {

	Socket socket;
	Integer connectedTo;
	ArrayList<Message> sendQueue;
	int pieceRequested=-1;
	int pieceReceived;
	Integer totalBytesRead=0;
	
	static Logger log = Logger.getLogger(Receiver.class);
	
	public Receiver(Socket socket, Integer connectedTo, ArrayList<Message> sendQueue)
	{
		this.socket=socket;
		this.connectedTo=connectedTo;
		this.sendQueue=sendQueue;
	}

	public Integer getBytesRead()
	{
		int temp=totalBytesRead;
		totalBytesRead=0;
		return temp;
	}

	public void run()
	{
		BufferedInputStream in = null;
		boolean isChoked=true;
		byte[] payload= null;
		log.info("Reciever thread started for connection between Peer [" + Util.myPeerId + "] " +
				"and peer [+"+connectedTo + "]");

		try {
			in= new BufferedInputStream(socket.getInputStream());
			int msgLength=0;

			socket.setSoTimeout(200);
			while(Util.terminationFlag){

				byte[] msgLengthBytes=new byte[4];
				byte type=-1;
				try{
					if(Util.interruptFlag)
					{
						synchronized(Util.lock)
						{
							while(Util.interruptFlag)
							{
								Util.lock.wait();
							}

							log.debug("Resuming receiver thread for connection with "+connectedTo);
							pieceRequested=-1;
							Util.requested=new boolean[Util.getBitFieldSize()+2];
							isChoked=true;
						}
					}

					in.read(msgLengthBytes,0,4);
					msgLength=ByteBuffer.wrap(msgLengthBytes).getInt();

					if(msgLength<=0)
						continue;

					payload=new byte[msgLength-1];
					type=(byte) in.read();
					int bytesRead = 0;
					
					while(true){
						
						if(bytesRead < msgLength-1){
							bytesRead += in.read(payload,bytesRead,msgLength-1-bytesRead);
						}else{
							break;
						}
						
					}
					if(type==(byte)7){
						totalBytesRead+=msgLength;
					}

				}catch(SocketTimeoutException se){
					if(pieceRequested != -1){
						Util.requested[pieceRequested] = false;
					}
					pieceRequested = -1;
				}
				/*
				 * wrap it in a byte buffer to extract data easily
				 */
				switch(type){
				
				case 0:
					//choke
					log.info("Peer ["+ Util.myPeerId + "] is choked by " +
							"["+ connectedTo + "]");
					isChoked=true;
					break;
					
				case 1:
					//unchoke
					log.info("Peer ["+ Util.myPeerId + "] is unchoked by " +
							"["+ connectedTo + "]");
					isChoked=false;
					break;
					
				case 2:
					//interested
					log.info("Peer [" + Util.myPeerId +"] received an 'interested' " +
							"message from [" + connectedTo + "]");
					break;
					
				case 3:
					//not interested
					log.info("Peer [" + Util.myPeerId +"] received an 'not interested' " +
							"message from [" + connectedTo + "]");
					break;
						
				case 4:
					//have
					synchronized (Util.bitfieldMap) {
						int pieceNumber=ByteBuffer.wrap(payload).getInt();
						Util.bitfieldMap.get(connectedTo).set(pieceNumber);
						log.info("Peer [" + Util.myPeerId +"] received a 'have' message from [" + 
								connectedTo + "] for the piece [" + pieceNumber +"]." );
						
						/*This means that peer has the piece*/
						if(Util.bitfieldMap.get(Util.myPeerId).get(pieceNumber) != 0){
							
							Message m = new NormalMessage((byte)3);
							sendQueue.add(m);
							log.info("Peer [" + Util.myPeerId +"] sent a 'Not Interested' message to [" + 
									connectedTo + "] for the piece [" + pieceNumber +"]." );
							
						}else{
							
							Message m = new NormalMessage((byte)2);
							sendQueue.add(m);
							log.info("Peer [" + Util.myPeerId +"] sent an 'Interested' message to [" + 
									connectedTo + "] for the piece [" + pieceNumber +"]." );
						}
						
					}
					break;
					
				case 5:
					//bit field
					//log.info("Peer [" + Util.myPeerId +"] received BITFIELD from "+connectedTo.toString());
					
					synchronized (Util.bitfieldMap) {
						Util.bitfieldMap.get(connectedTo).set(payload);
					}
					break;
					
				case 6:
					//request
					int pieceNumber=ByteBuffer.wrap(payload).getInt();
					
					if(pieceNumber>Util.getBitFieldSize()|| 
							Util.bitfieldMap.get(Util.myPeerId).get(pieceNumber)==0){
						break;
					}
					
					Message toSend=new NormalMessage((byte)7,pieceNumber);
					synchronized (sendQueue) {
						log.info("Peer [" + Util.myPeerId +"] received a 'request' message from [" + 
								connectedTo + "] for the piece [" + pieceNumber +"]." );
						sendQueue.add(toSend);
					}
					break;
					
				case 7:
					//piece	
					ByteBuffer data=ByteBuffer.wrap(payload, 0, 4);
					pieceReceived=data.getInt();

					if(pieceReceived==pieceRequested){				
						byte[] piece=new byte[msgLength-5];
						System.arraycopy(payload, 4, piece,0,piece.length);

						FileOutputStream fos=new FileOutputStream(new File(Util.myPeerId.toString()+"/"+pieceReceived));
						fos.write(piece);
						fos.close();
						/*Reset the piece requested*/
						pieceRequested=-1;
						/*Update the bit field*/
						synchronized (Util.bitfieldMap) {
							Util.bitfieldMap.get(Util.myPeerId).set(pieceReceived);
							log.info("Peer [" + Util.myPeerId +"] received a 'piece' message from [" + 
									connectedTo + "] for the piece [" + pieceReceived +"]." );
						}

						/**Construct a have message and send it to all peers. Put that message
						 * in front of the send queue so that other peers can request that 
						 * message from you asap.  
						 * */
						Message haveMsg=new NormalMessage((byte)4,pieceReceived);
						for(Entry<Integer,ConnectionHandler> handler:Util.connectionHandlerMap.entrySet()){
							if(handler.getKey()!=Util.myPeerId){
								ConnectionHandler connHandler=handler.getValue();
								synchronized (connHandler.sendQueue) {
									connHandler.sendQueue.add(0,haveMsg);
								}
							}
						}
					}
					break;

				default: break;
				}

				if(!isChoked && pieceRequested==-1)
				{
					sendNewRequest();
				}

			}

			//	in.close();
			//socket.close();
		}

		catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		
	}

	public  void sendNewRequest()
	{
		Random no=new Random();
		ArrayList<Integer> toSelectFrom;

		synchronized(this.getClass())
		{
			BitField bits=Util.bitfieldMap.get(Util.myPeerId);
			byte[] otherBitfield=Util.bitfieldMap.get(connectedTo).get();
			toSelectFrom =bits.getList(otherBitfield);
		}

		if(toSelectFrom.isEmpty())
			return;

		while(!toSelectFrom.isEmpty())
		{
			int index=no.nextInt(toSelectFrom.size());
			int pno=toSelectFrom.get(index);

			synchronized(Util.requested)
			{
				if(!Util.requested[pno])
				{

					synchronized(sendQueue)
					{
						Message requestMsg=new NormalMessage((byte)6,pno);
						sendQueue.add(requestMsg);
						log.info("Peer [" + Util.myPeerId +"] is sending a piece " + pno + " " +
								"request to Peer ["+connectedTo+ "]");
						pieceRequested=pno;
						return;
					}
				}
				else
				{
					toSelectFrom.remove(index);
				}
			}

		}

	}
}
