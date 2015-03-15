package com.xbc.Video;


import android.os.Handler;

public class VideoInfo {
	
	
	public static int width=176;
	public static int height=144;
	
	public static boolean firstPktReceived = false;  
	/**记录打包分片的索引*/
	public static int pktflag = 0; 
	/**若未打包到末包，则此状态一直为true*/
	public static boolean status = true;   
	/**打包分片长度*/
	public static int divide_length = 800;
	/**分片标志位*/
	public static boolean endView=false;
	public static boolean dividingFrame = false;
	public static Handler Called;
	public static Handler Video_Play;
	public static Handler Video_Off_1;
    public static Handler Video_Off_2;
    public static Handler Video_Listen;
    public static  RTP rtpRecv = new RTP();
	
	

}
