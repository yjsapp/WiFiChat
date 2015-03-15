package com.xbc.Video;

class NalBuffer{
	private byte[] NalBuf;
	private boolean isReadable;
	private boolean isWriteable;
	private int NalLen;
	NalBuffer(){
		NalBuf=new byte[50000];
		isReadable=false;
		isWriteable=true;
		NalLen=0;
	}
	
	public synchronized void IsReadAble()
	{	
		if(!isReadable)
		{
			try
			{
				this.wait();
			}catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	/**
	 * 获取当前的NAL单元，包括开始码：00 00 00 01
	 * */
	public synchronized byte[] getReadable_NalBuf()
	{
		if(!isReadable){
			try{
				this.wait();
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
//		byte[] tmp_NalBuf =new byte[this.NalLen];
		byte[] tmp_NalBuf =new byte[50000];
		System.arraycopy(this.NalBuf, 0, tmp_NalBuf, 0, this.NalLen); //to make sure useable info copyed
    	return tmp_NalBuf;	
	}
	
	/**
	 * 获取当前的NAL单元，不包括开始码：00 00 00 01
	 * */
	public synchronized byte[] getReadable_NalBuf2()
	{
		if(!isReadable){
			try{
				this.wait();
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
		byte[] tmp_NalBuf =new byte[this.NalLen];
		System.arraycopy(this.NalBuf, 4, tmp_NalBuf, 0, this.NalLen); //to make sure useable info copyed
    	return tmp_NalBuf;	
	}
	
	public synchronized void ReadLock(){
		isReadable=false;
		isWriteable=true;
		notify();
	}
	public synchronized byte[] getWriteable_Nalbuf()
	{
	/*	if(!isWriteable){
			try{
				this.wait();
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}*/
		return this.NalBuf;		
	}
	public synchronized void IsWriteAble()
	{	
		if(!isWriteable)
		{
			try
			{
				this.wait();
			}catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	public synchronized void WriteLock(){
		isReadable=true;
		isWriteable=false;
		notify();
	}
	/**
	 * 清空缓存。
	 *    内存实际操作是新开辟一片堆空间，让栈区的NalBuf指向新的缓存空间。
	 * 原来的那部分空间变成没有指向的区域，成为“垃圾”，等待系统回收。
	 * */
	public synchronized void cleanNalBuf(){
		NalBuf=new byte[50000];
	}
	public  synchronized void setNalLen(int NalLen){
		this.NalLen=NalLen;
	}
	public int getNalLen(){
		
		return this.NalLen;
	}
	public synchronized void addNalLen(int Len){
		this.NalLen+=Len;
	}	
	
	
	
	

}
