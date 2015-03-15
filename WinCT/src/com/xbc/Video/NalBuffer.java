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
	 * ��ȡ��ǰ��NAL��Ԫ��������ʼ�룺00 00 00 01
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
	 * ��ȡ��ǰ��NAL��Ԫ����������ʼ�룺00 00 00 01
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
	 * ��ջ��档
	 *    �ڴ�ʵ�ʲ������¿���һƬ�ѿռ䣬��ջ����NalBufָ���µĻ���ռ䡣
	 * ԭ�����ǲ��ֿռ���û��ָ������򣬳�Ϊ�����������ȴ�ϵͳ���ա�
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
