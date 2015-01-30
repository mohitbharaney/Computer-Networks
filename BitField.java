import java.util.ArrayList;


public class BitField {
	
	Integer size;
	byte[] bitfield;
	public BitField(Integer size)
	{
		this.size=size;
		bitfield=new byte[(int) Math.ceil(size.doubleValue()/8)];
		if(size%8!=0)
		{
			for(int i=size%8;i<8;i++)
				bitfield[size/8]=(byte) (bitfield[size/8]+(1<<i));
		}
	}
	
	public byte get(int pieceNo)
	{
		pieceNo--;
		int index=pieceNo/8;
		int position = pieceNo%8;
		return (byte) (bitfield[index]&(1<<position));
	}
	
	public byte[] get()
	{
		return bitfield;
	}
	
	public void set(int pieceNo)
	{
		pieceNo--;
		int index=pieceNo/8;
		int position = pieceNo%8;
		bitfield[index]=(byte) (bitfield[index]|(1<<position));
	}
	
	public void set(byte[] bitfield)
	{
		this.bitfield=bitfield;
	}
	
	public  ArrayList<Integer> getList(byte[] other)
	{
		byte[] temp=bitfield.clone();
		
		for(int i=0;i<bitfield.length;i++)
			bitfield[i]=(byte) ((~bitfield[i])&other[i]);
		
		ArrayList<Integer> selectFrom=new ArrayList<Integer>();
		for(int i=1;i<=size;i++)
		{
			if(get(i)!=0)
				selectFrom.add(i);
		}
		bitfield=temp;
		return selectFrom;
	}

}
