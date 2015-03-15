package com.xbc.Audio;



import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.ty.winchat.R;
import com.ty.winchat.WinChatApplication;
import com.ty.winchat.listener.Listener;
import com.ty.winchat.model.UDPMessage;
import com.ty.winchat.service.ChatService;
import com.ty.winchat.service.ChatService.MyBinder;
import com.xbc.Audio.AudioReceiver.MyServiceConnection;
import com.xbc.control_message.MessageInfo;

import jlibrtp.DataFrame;
import jlibrtp.Participant;
import jlibrtp.RTPAppIntf;
import jlibrtp.RTPSession;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AudioCallActivity extends Activity{
	
	RTPSession rtpSession = null;
    DatagramSocket rtpSocket = null;
	DatagramSocket rtcpSocket = null;
	ReceiveRTPData recRTPData = new ReceiveRTPData();
	int frameSizeG711 = 160;
	boolean G711Running = false;
	
	
	public static MyBinder binder;
	MyServiceConnection connection;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
    setContentView(R.layout.audio_call);
    
    
    
    if (android.os.Build.VERSION.SDK_INT > 9) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }
   
    init();
    AudioInfo.Audio_Play= new Handler(){
		public void handleMessage(android.os.Message msg) {
			
			
			if(msg.arg1==1){
			
			Toast.makeText(getApplicationContext(), "来电接起", 
					Toast.LENGTH_SHORT).show();	}
			
	
			if(G711Running) 
				return;	
			G711Running = true;
			G711.init();				
			G711_recored();
			track.play(); // 启动播放音频	
		super.handleMessage(msg);
		}
		}; 
     
    	AudioInfo.Audio_Off_2= new Handler(){
			public void handleMessage(android.os.Message msg) {
				
				
				if(msg.arg1==1){
				
				Toast.makeText(getApplicationContext(), "已经挂断", 
						Toast.LENGTH_SHORT).show();	}
				
				G711Running = false;
				if(!(rtpSession==null)){
				
				rtpSession.endSession();	
				rtpSession = null;
				}
				recRTPData = new ReceiveRTPData();
				
			    track.stop();
			    AudioCallActivity.this.finish();
					
			super.handleMessage(msg);
			}
			}; 

			InitRtp(AudioInfo.ChatIp, 5552, 5553,5552,5553);
            rtpSession.RTPSessionRegister(recRTPData, null, null);
    
            android.os.Message LaiDian=new android.os.Message();	//创建handler消息
			 LaiDian.arg1=1;
			AudioInfo.Audio_Play.sendMessage(LaiDian);
  
	}
	
	
	
public void init(){
		
		
		Intent intent=new Intent(AudioCallActivity.this,ChatService.class);
    	startService(intent);
       	bindService(intent, connection=new MyServiceConnection(), Context.BIND_AUTO_CREATE);
		
		
	}


private void sendMsg(UDPMessage msg){
	if(binder!=null){
		try {
				binder.sendMsg(msg, InetAddress.getByName(AudioInfo.ChatIp));
			if(Listener.RECEIVE_MSG==Integer.valueOf(msg.getType()))//如果是文本消息
				System.out.println("111");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}else{
		 unbindService(connection);
		 Intent intent=new Intent(AudioCallActivity.this,ChatService.class);
		 bindService(intent, connection=new MyServiceConnection(), Context.BIND_AUTO_CREATE);
		 Toast.makeText(this, "未发送出去,请重新发送", Toast.LENGTH_SHORT).show();
	}
}

	
	
public void over(View view){
	
	 UDPMessage message=WinChatApplication.mainInstance.getMyUdpMessage(MessageInfo.audio_off_2+"/"+"m", Listener.RECEIVE_MSG);
		sendMsg(message);
	
	
	
	
	
	G711Running = false;
	if(!(rtpSession==null)){
		
		rtpSession.endSession();	
		rtpSession = null;
		}
    recRTPData = new ReceiveRTPData();
	track.stop();
	
	Toast.makeText(getApplicationContext(), "挂断电话了", 
			Toast.LENGTH_SHORT).show();
	this.finish();
	
	
}	
	
public void InitRtp(String networkAddress, int localRtpPort, int localRtcpPort, int remoteRtpPort, int remoteRtcpPort) {
	try {
		rtpSocket = new DatagramSocket(localRtpPort);
		rtcpSocket = new DatagramSocket(localRtcpPort);
	} catch (Exception e) {
		System.out.println("RTPSession failed to obtain port");
	}
	
	rtpSession = new RTPSession(rtpSocket, rtcpSocket);
	Participant p = new Participant(networkAddress, remoteRtpPort, remoteRtcpPort);	
	rtpSession.addParticipant(p);
	rtpSession.naivePktReception(true);
}


// 4、RTP数据接收
//通过RTPAppIntf接口实现的，该接口有3个函数receiveData、userEvent、frameSize
public class ReceiveRTPData implements RTPAppIntf {	
	RTPSession rtpSession = null;	//rtpSession     新建RTP会话类
	public void receiveData(DataFrame frame, Participant p) {
	
		if(G711Running) {
			byte[] audioBuffer = new byte[frameSizeG711];
			short[] audioData = new short [frameSizeG711];
			audioBuffer = frame.getConcatenatedData();				
			//decode 解码, 将G.711、G.729数据还原成原始PCM数据
			G711.ulaw2linear(audioBuffer, audioData, frameSizeG711);
			//　写缓存,(播放音频数据),audioData存放播放数据的数组
			track.write(audioData, 0, frameSizeG711);
			
//			++count;
//			totalTimeAfter += System.currentTimeMillis();
//			Log.v("zlj","zzzzzzzzzzzz711"+"    packet:"+count+"    time"+totalTimeAfter);
			
			Log.v("zlj","zzzzzzzzzzzz711");
						}
		
    }  
	
    public void userEvent(int type, Participant[] participant) {
		//Do nothing
	}
	public int frameSize(int payloadType) {
		return 1;
	}			
}

/**
 * g711
 */
private void G711_recored(){
	new Thread (G711_encode).start();
}


/**
 * 编码线程
 */
Runnable G711_encode = new Runnable(){
	public void run() {			
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
		
		AudioRecord record = getAudioRecord();		
		//int frame_size = 160;
		short [] audioData = new short [frameSizeG711];
		byte  [] encodeData = new byte[frameSizeG711];
		int numRead = 0;
		
		while(G711Running) 
		{
			//读取 PCM(录音 数据）， 保存在 audioData
			numRead = record.read(audioData, 0, frameSizeG711);
			if(numRead<=0) continue;
			
			calc2(audioData,0,numRead);		// ?		
//进行pcmu编码
//linear2ulaw(short[] lin, int offset, byte[] ulaw, int frames)
//lin：存放PCM数据的数组（输入）,offset：PCM偏移量,ulaw：存放G.711数据的数组（输出）,frames：指示有多少个short数据送入编码
		G711.linear2ulaw(audioData, 0, encodeData, numRead);
		//RTP发送,调用jlibrtp库用RTP格式数据包将压缩后的G.711或G.729数据打包发送
		
		
		
		        
			rtpSession.sendData(encodeData);
			Log.v("zlj","G711_encodeing!");
		}
		
//		// 测试延迟，2012-10-09
//		for(int i = 1; i <= 500; ++i) {
//			numRead = record.read(audioData, 0, frameSizeG711);
//			if(numRead<=0) continue;
//			calc2(audioData,0,numRead);					
//			//进行pcmu编码
//			G711.linear2ulaw(audioData, 0, encodeData, numRead);
//			totalTimeBefore += System.currentTimeMillis();
//			rtpSession.sendData(encodeData);
//			Log.v("zlj","G711 sending..."+"    packet:"+i+"    time:"+totalTimeBefore);
//		}
		record.release();
		Log.v("zlj","G711_encode stopped!");
	}
};   



void calc2(short[] lin,int off,int len) {
	int i,j;
	
	for (i = 0; i < len; i++)
	{
		j = lin[i+off];
		lin[i+off] = (short)(j>>1); // ?
	}
}

/**
 * PCM数据采集部分
 */
    // 1、原始PCM数据采集，从手机硬件（麦克风）获取原始的PCM音频数据
private  AudioRecord getAudioRecord(){
	int samp_rate =  8000 ;
	// 获取最小缓存长度min
	int min = AudioRecord.getMinBufferSize(samp_rate, 
			AudioFormat.CHANNEL_CONFIGURATION_MONO, 
			AudioFormat.ENCODING_PCM_16BIT);
	Log.e("TAG", "min buffer size:"+min);
	
	// 创建一个新的AudioRecord类record
	AudioRecord record = null;
			record = new AudioRecord(
			MediaRecorder.AudioSource.MIC,          //音频源，MIC
			samp_rate,                              //采样频率，一般为8000hz/s 
			AudioFormat.CHANNEL_CONFIGURATION_MONO, // 声道，单声道
			AudioFormat.ENCODING_PCM_16BIT,         // PCM量化位数，16位
			min                                     // 最小缓存长度，min
			);
	//  开始采集原始PCM音频数据
	record.startRecording();
	
	return record;
}

 // 7、PCM数据播放,调用Android系统接口，播放PCM音频数据
	int samp_rate = 8000 ;
	int maxjitter = AudioTrack.getMinBufferSize(samp_rate, 
			AudioFormat.CHANNEL_CONFIGURATION_MONO, 
			AudioFormat.ENCODING_PCM_16BIT);
	// 播放类型，听筒播放
	AudioTrack track = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
			samp_rate,
			AudioFormat.CHANNEL_CONFIGURATION_MONO, 
			AudioFormat.ENCODING_PCM_16BIT,
			maxjitter, 
			AudioTrack.MODE_STREAM
			);	


public static byte[] shortArrayToByteArray(short[] values) {
	byte[] s = new byte[values.length*2];
	for(int q=0; q<values.length;q++) {
		byte[] bytes = shortToBytes(values[q]);
		s[2*q] = bytes[0];
		s[2*q+1] = bytes[1];
	}
	return s;
}
public static byte[] shortToBytes(int myInt) {
	byte[] bytes = new byte[2];
	int hexBase = 0xff;
	bytes[0] = (byte) (hexBase & myInt);
	bytes[1] = (byte) (((hexBase << 8)& myInt) >> 8);
	return bytes;
}

public byte[] shortArray2byteArray(short[] data, int items) { 
	byte[] a = new byte[items*2]; 
    for (int i = 0; i < items; i++) {   
        a[i * 2] = (byte) ((data[i] >> 8) & 0xFF);   
        a[i * 2 + 1] = (byte) (data[i]  & 0xff);   
    }  
	return a;    
	}

public short bytesToShort(byte byte1, byte byte2) {
	return (short)(0xffff&((0xff&byte1) | ((0xff&byte2)<<8)));
}

public short[] byteArrayToShortArray(byte[] bytes) {
	short[] s = new short[bytes.length/2];
	for(int q=0; q<s.length;q++) {
		s[q] = bytesToShort(bytes[2*q],bytes[2*q+1]);
	}
	return s;
}


public  class MyServiceConnection implements ServiceConnection{
	@Override
public void onServiceConnected(ComponentName name, IBinder service) {
	binder=(MyBinder) service;
	
}

	@Override
public void onServiceDisconnected(ComponentName name) {
}

}
protected void onDestroy() {
	super.onDestroy();
	unbindService(connection);
	
}  
		
		
}
