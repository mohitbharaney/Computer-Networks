import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;


public class ServerThread implements Runnable{

	int peerId;
	int portNo;
	ServerSocket serverSocket;

	static Logger log = Logger.getLogger(ServerThread.class);

	public ServerThread(int portNo,int peerId, ServerSocket serverSocket)
	{
		this.portNo=portNo;
		this.peerId=peerId;
		this.serverSocket = serverSocket;
	}
	@Override
	public void run() {
		try {
			//serverSocket = new ServerSocket(portNo);

			log.info("ServerSocket is created by process "+ this.peerId + " at port "+ this.portNo);

			while(true){
				Socket socket = null;
				socket=serverSocket.accept();
				
				InputStream in=socket.getInputStream();
				OutputStream out=socket.getOutputStream();

				byte[] helloRcv=new byte[32];
				in.read(helloRcv);
				ByteBuffer data=ByteBuffer.wrap(helloRcv);
				int connectedTo=data.getInt(28);

				Message helloSend=new HandshakeMessage(peerId);
				out.write(helloSend.getMessageBytes());

				log.info("Peer ["+ this.peerId +"] is connected from Peer ["+connectedTo+"]");

				byte[] bitFieldRecv=new byte[(int)(Math.ceil(Util.getBitFieldSize().doubleValue()/8))+5];
				in.read(bitFieldRecv);
				BitField bitfield=new BitField(Util.getBitFieldSize());
				byte[] bits=bitfield.get();			
				for(int i=0;i<bits.length;i++)
				{
					bits[i]=bitFieldRecv[i+5];
				}


				Message bitFieldMessage=new NormalMessage((byte) 5);
				out.write(bitFieldMessage.getMessageBytes());

				synchronized(Util.bitfieldMap)
				{
					Util.bitfieldMap.put(connectedTo, bitfield);
				}

				log.info("Checking the bitfield of peer "+connectedTo+"->"+bitfield.get(1));
				ConnectionHandler ch=new ConnectionHandler(socket,connectedTo);
				Thread connectionThread=new Thread(ch);

				synchronized (Util.connectionHandlerMap) {
					Util.connectionMap.put(connectedTo,connectionThread);
					Util.connectionHandlerMap.put(connectedTo, ch);
				}
				connectionThread.start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			log.info("Returning from Server Thread");
			return;
		}

	}

}
