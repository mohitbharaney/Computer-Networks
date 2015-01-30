import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import org.apache.log4j.Logger;


public class ConnectionHandler implements Runnable{
	private Socket socket;
	public Integer connectedTo;
	public ArrayList<Message> sendQueue=new ArrayList<Message>();
	public Receiver recieve;
	static Logger log = Logger.getLogger(ConnectionHandler.class);

	public ConnectionHandler(Socket socket,Integer connectedTo){
		this.socket=socket;
		this.connectedTo=connectedTo;
	}

	public Integer getBytesRead()
	{
		return recieve.getBytesRead();
	}

	@Override
	public void run() {

		recieve=new Receiver(socket,connectedTo,sendQueue);

		Thread receiverThread=new Thread(recieve);
		receiverThread.start();
		OutputStream out=null;
		try {
			out=socket.getOutputStream();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		while(Util.terminationFlag)
		{
			if(Util.interruptFlag)
			{
				synchronized(Util.lock)
				{	
					while(Util.interruptFlag)
					{
						try {
							Util.lock.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			}
			synchronized(sendQueue)
			{
				while(!sendQueue.isEmpty())
				{	
					try {
						Message m=sendQueue.remove(0);
						byte[] data=m.getMessageBytes();

						NormalMessage x=(NormalMessage)m;

						out.write(data);
						
						out.flush();

					}catch (SocketException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					} 
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
		}
		try {
			receiverThread.join();
			out.close();
			socket.close();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//log.error(e);
		}

	}

}
