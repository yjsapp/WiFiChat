package com.xbc.Video;


import android.os.Handler;

public class VideoInfo {
	
	
	public static int width=176;
	public static int height=144;
	
	public static boolean firstPktReceived = false;  
	/**��¼�����Ƭ������*/
	public static int pktflag = 0; 
	/**��δ�����ĩ�������״̬һֱΪtrue*/
	public static boolean status = true;   
	/**�����Ƭ����*/
	public static int divide_length = 800;
	/**��Ƭ��־λ*/
	public static boolean endView=false;
	public static boolean dividingFrame = false;
	public static Handler Called;
	public static Handler Video_Play;
	public static Handler Video_Off_1;
    public static Handler Video_Off_2;
    public static Handler Video_Listen;
    public static  RTP rtpRecv = new RTP();
	
	

}
