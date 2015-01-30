import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class PeerProcess {

	static Logger log = Logger.getLogger(PeerProcess.class);

	public static void main(String[] args) throws InterruptedException{


		final Comparator<Integer[]> arrayComparator = new Comparator<Integer[]>() {
			@Override
			public int compare(Integer[] o1, Integer[] o2) {
				return o1[1].compareTo(o2[1]);
			}
		};

		try{
			ConcurrentHashMap<Integer,Peer> peerMap=Util.readPeerConfig();
			Util.readCommonConfig();
			int peerId=Integer.parseInt(args[0]);
			Util.setPeerId(peerId);
			log.info("Peer process "+peerId);
			int portNo=peerMap.get(peerId).getPort();
			log.info("Port number "+ portNo);

			boolean hasFileFlag=false;
			/*
			 * create the bit field of the peer running on this machine.
			 */
			if(peerMap.get(peerId).isHasFile()==1)
			{
				BitField bf=new BitField(Util.getBitFieldSize());
				//log.info(Util.getBitFieldSize());
				byte[] data=bf.get();
				for(int i=0;i<data.length;i++)
					data[i]=-1;
				synchronized(Util.bitfieldMap)
				{
					Util.bitfieldMap.put(peerId, bf);
				}
				hasFileFlag=true;
			}
			else
			{
				BitField bf=new BitField(Util.getBitFieldSize());
				synchronized(Util.bitfieldMap)
				{
					Util.bitfieldMap.put(peerId, bf);
				}
			}
			ServerSocket mySocket = new ServerSocket(portNo);
			Thread thread=new Thread(new ServerThread(portNo, peerId, mySocket));
			thread.start();

			/*
			 * search for all the peer ids lesser than current peerid, and establish connection with them
			 * 
			 */

			for(Entry<Integer,Peer> entry:peerMap.entrySet())
			{
				int pid=entry.getKey();
				if(pid < Integer.parseInt(args[0]))
				{
					Socket socket=new Socket(entry.getValue().getHost(),entry.getValue().getPort());
					log.info("Peer [" +peerId +"] makes a connection to Peer ["+ entry.getKey()+"].");

					InputStream in=socket.getInputStream();
					OutputStream out=socket.getOutputStream();
					/*
					 * once connection is established, being the person who established the connection
					 * send the hello msg, and then receive the hello msg
					 * not the peer id to which u are connected.
					 * 
					 *  send your bitField and receive their bitField.term
					 */
					Message helloSend=new HandshakeMessage(peerId);
					out.write(helloSend.getMessageBytes());

					byte[] helloRcv=new byte[32];
					in.read(helloRcv);
					ByteBuffer data=ByteBuffer.wrap(helloRcv);
					int connectedTo=data.getInt(28);

					/*
					 * send the bit field message
					 */

					Message bitFieldMessage=new NormalMessage((byte) 5);

					out.write(bitFieldMessage.getMessageBytes());

					byte[] bitFieldRecv=new byte[(int)(Math.ceil(Util.getBitFieldSize().doubleValue()/8))+5];
					in.read(bitFieldRecv);

					BitField bitfield=new BitField(Util.getBitFieldSize());
					byte[] bits=bitfield.get();


					for(int i=0;i<bits.length;i++)
					{
						bits[i]=bitFieldRecv[i+5];
					}


					synchronized(Util.class)
					{
						Util.bitfieldMap.put(connectedTo, bitfield);
					}

					/*
					 * spawn a new thread which handles the connection and put the reference in a hashMap
					 * with the peerId to which its connected as the key
					 */
					ConnectionHandler ch=new ConnectionHandler(socket,connectedTo);
					Thread connectionThread=new Thread(ch);    		
					synchronized (Util.connectionHandlerMap) {
						Util.connectionMap.put(connectedTo,connectionThread);
						Util.connectionHandlerMap.put(connectedTo, ch);
					}
					connectionThread.start();

				}

			}

			long startTime=System.currentTimeMillis();
			long optimisticStartTime=System.currentTimeMillis();
			long stopTime=0;
			while(Util.terminationFlag)
			{		

				stopTime=System.currentTimeMillis();
				if(stopTime-optimisticStartTime>Util.optimUnchokingIntegererval*1000)
				{
					ArrayList<Integer> chokedNeighbours=Util.getPeermap().get(Util.myPeerId).getChokedNeighbours();
					Random index=new Random();
					if(!chokedNeighbours.isEmpty()){
						int optimisticUnchokedPeer=chokedNeighbours.remove(index.nextInt(chokedNeighbours.size()));
						Message unchokeMsg=new NormalMessage((byte)1);
						Util.connectionHandlerMap.get(optimisticUnchokedPeer).sendQueue.add(unchokeMsg);
						log.info("Peer ["+Util.myPeerId +"] is sending 'optimistic unchoke' to  Peer ["+ optimisticUnchokedPeer+ "]");
						
					}
					optimisticStartTime = System.currentTimeMillis();
				}
				if(stopTime-startTime>Util.unchokingIntegererval*1000)
				{
					synchronized(Util.lock)
					{
						log.debug("Trying to choose preferred neighbours");	
						Util.interruptFlag=true;
						int cnt=0;

						synchronized (Util.connectionHandlerMap) {

							int arraySize=Util.connectionHandlerMap.size();
							Integer[][] downloadRate=new Integer[arraySize][2];
							int i=0;
							for(Entry<Integer,ConnectionHandler> entry:Util.connectionHandlerMap.entrySet())
							{
								int pid=entry.getKey();
								/**
								 * To flush existing messages, clear the send queue*/
								
								if(pid !=Util.myPeerId)
								{
									downloadRate[i][0]=pid;
									downloadRate[i][1]=entry.getValue().getBytesRead();
									log.debug("Peer ["+ downloadRate[i][0]+"]\t"+ " has " +
											"download rate " +downloadRate[i][1]);
									i++;
								}
							}

							Arrays.sort(downloadRate,arrayComparator);

							/*
							 * un choke messages
							 */
							if(downloadRate.length>0)
							{
								if(hasFileFlag)
								{
									log.info("Peer "+peerId +" is choosing random neighbours since it has entire file");
									StringBuffer sbf = new StringBuffer("Peer [" + peerId +"] has the preferred neighbors [");

									ArrayList<ConnectionHandler> list=new ArrayList<ConnectionHandler>();
									for(Entry<Integer,ConnectionHandler> entry:Util.connectionHandlerMap.entrySet())
									{
										if(entry.getKey()!=Util.myPeerId)
											list.add(entry.getValue());
									}
									int cnt1=0;
									Random rno=new Random();
									while(!list.isEmpty())
									{
										int index=rno.nextInt(list.size());
										ConnectionHandler temp=list.remove(index);
										if(cnt1<Util.noPreferredNeighbours)
										{
											Message e=new NormalMessage((byte)1);
											temp.sendQueue.add(e);
											if(cnt1 != 0){
												sbf.append(",");
											}
											sbf.append(temp.connectedTo);
											cnt1++;
											log.debug("sending unchoke to "+ temp.connectedTo);
										}
										else
										{
											Message e=new NormalMessage((byte)0);
											temp.sendQueue.add(e);
											Util.getPeermap().get(Util.myPeerId).setChokedNeighbour(temp.connectedTo);
											log.debug("sending choke to "+ temp.connectedTo);
										}

									}
									sbf.append("]");
									log.info(sbf);
								}
								else{
									log.debug("Peer "+peerId +" is choosing preferred neighbours");
									StringBuffer sbf = new StringBuffer("Peer [" + peerId +"] has the preferred neighbors [");

									int index=downloadRate.length-1;
									cnt=0;
									int loopCount=0;

									while(cnt<Util.noPreferredNeighbours&&loopCount<downloadRate.length)
									{
										if(cnt != 0){
											sbf.append(",");
										}
										int connectedTo=downloadRate[index][0];
										if(Util.getPeermap().get(connectedTo).isHasFile()!=1)
										{
											Message m=new NormalMessage((byte)1);
											log.debug("sending unchoke to "+connectedTo);
											sbf.append(connectedTo);
											Util.connectionHandlerMap.get(connectedTo).sendQueue.add(m);
											cnt++;
										}
										index--;
										loopCount++;
									}
									sbf.append("]");
									log.info(sbf);
									/*
									 * choke messages
									 */
									for(int k=index;k>=0;k--)
									{
										int connectedTo=downloadRate[k][0];
										if(Util.getPeermap().get(connectedTo).isHasFile()!=1)
										{
											Message m=new NormalMessage((byte)0);
											Util.connectionHandlerMap.get(connectedTo).sendQueue.add(m);
											Util.getPeermap().get(Util.myPeerId).setChokedNeighbour(connectedTo);
											log.debug("sending choke to "+connectedTo);
										}
									}//End of for loop for choked neighbours

								}// End of else
							}


						}
						/**
						 * Send the final bit fields to all the peers**/
						
						 
					//	if(!hasFileFlag)
						//{	
							synchronized(Util.bitfieldMap){
							BitField toCheckWith=Util.bitfieldMap.get(Util.peerWithFile);
							
							boolean flag=true;
							for(Entry<Integer,BitField> entry:Util.bitfieldMap.entrySet())
							{
								int pid=entry.getKey();
								log.info("Checking for Peer "+pid);
								BitField mine=Util.bitfieldMap.get(pid);
								ArrayList<Integer> itemsRemainig=mine.getList(toCheckWith.get());
								if(!itemsRemainig.isEmpty())
								{
									log.info("Size of the list is "+ itemsRemainig.size());
									flag=false;
								//	break;
								}
								else
								{
									Util.getPeermap().get(pid).setHasFile(1);
								}
							}
							if(flag && Util.bitfieldMap.size()==Util.getPeermap().size()){
								log.info("termination set");
								Util.terminationFlag =false;
							}
						}

						Util.interruptFlag=false;	
						Util.lock.notifyAll();
						startTime=System.currentTimeMillis();

					}
				}

			}

			for(Entry<Integer,Thread> entry:Util.connectionMap.entrySet())
			{
				entry.getValue().join();
			}
			mySocket.close();
		//	thread.stop();
			log.debug("Waiting for other threads to finish.");

		} catch (FileNotFoundException e) {
			log.error(e);
		}catch (UnknownHostException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
		finally{

		}
		
		if(Util.getPeermap().get(Util.myPeerId).originalHasFile!=1)
		{
		String directoryPath=Util.myPeerId.toString();
		int fileCount= new File(directoryPath).list().length;
		//need to remove this hardcoding
		File mainFile = new File(directoryPath+"/"+Util.fileName);
	    try {
			mainFile.createNewFile();
		
			log.info(fileCount);
			for(int i=1;i<=fileCount;i++){
				File file =new File(directoryPath+"/"+i);
				FileInputStream fin = null;
				
					fin = new FileInputStream(file);
				    byte fileContent[] = new byte[(int)file.length()];
				    fin.read(fileContent);
				    String s = new String(fileContent);
				    fin.close();
				    
				   FileOutputStream fos=new FileOutputStream(mainFile,true);
			    	fos.write(fileContent);
			    	fos.close();
			    	file.delete();
			    	//Closing BufferedWriter Stream
		    	
			}
			
			 } catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
	    
		}

	}

}
