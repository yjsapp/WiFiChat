package com.xbc.Video;



import java.net.DatagramSocket;

import com.xbc.Audio.AudioInfo;

import jlibrtp.Participant;
import jlibrtp.RTPSession;
import android.util.Log;


public class RTPSending {

	
	
	DatagramSocket rtpSocket1 = null;
	DatagramSocket rtcpSocket1 = null;
	
	public RTPSending() {
		
		try {
			//设置RTP会话的两个接口
			rtpSocket1 = new DatagramSocket(10086);
			rtcpSocket1 = new DatagramSocket(10087);
		} catch (Exception e) {
			System.out.println("RTPSession failed to obtain port");
		}
	
		
		
		VideoInfo.rtpRecv.rtpSession = new RTPSession(rtpSocket1, rtcpSocket1);
 		VideoInfo.rtpRecv.rtpSession.naivePktReception(false);
 		VideoInfo.rtpRecv.rtpSession.RTPSessionRegister(VideoInfo.rtpRecv, null, null);	
 		Participant p1 = new Participant(AudioInfo.ChatIp,10086,10087);
 		VideoInfo.rtpRecv.rtpSession.addParticipant(p1);
		
 		
	}	
	


}
