package com.xbc.Video;

import java.net.DatagramSocket;

import com.xbc.Audio.AudioInfo;

import jlibrtp.Participant;
import jlibrtp.RTPSession;

public class RtpThread extends Thread {
	
    public  RTP rtpRecv ;
	public	DatagramSocket rtpSocket = null;
	Participant p = null;
	
   
	
	
	public void run() {
		
		
	
					try {
						rtpRecv = new RTP();
			
						rtpSocket = new DatagramSocket();										
					} catch (Exception e) {
						e.printStackTrace();
					}										
					rtpRecv.rtpSession = new RTPSession(rtpSocket, null);
					rtpRecv.rtpSession.naivePktReception(false);
					rtpRecv.rtpSession.RTPSessionRegister(rtpRecv, null, null);		
					p = new Participant(AudioInfo.ChatIp,10086,10087);										
					rtpRecv.rtpSession.addParticipant(p);
				 
					  
	
	}
				
				
	 
}


