package com.xbc.Video;

import java.io.IOException;
import java.net.InetAddress;

import com.ty.winchat.R;
import com.ty.winchat.WinChatApplication;
import com.ty.winchat.listener.Listener;
import com.ty.winchat.model.UDPMessage;
import com.ty.winchat.service.ChatService;
import com.ty.winchat.service.ChatService.MyBinder;
import com.xbc.Audio.AudioInfo;
import com.xbc.Audio.AudioReceiver;
import com.xbc.Audio.AudioReceiver.MyServiceConnection;
import com.xbc.control_message.MessageInfo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class VideoReceiver extends Activity {
	//private MyDialog dialog;
	private LinearLayout layout;
	private TextView video_name;
	
	
	public static MyBinder binder;
	MyServiceConnection connection;
	
	MediaPlayer mMediaPlayer=new MediaPlayer();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.videoreceiver);
		init();
		VideoAlerm();
		video_name=(TextView)findViewById(R.id.confirm_video_name);
		
//		if(!Info.IsFirstCast){
//			
//			
//			zhujianame.setText(SipInfo.zhujianame+"大妈"+" 你要进行道路\n维护吗？确定要去吗？");
//			
//			
//		}
		
		
		
		//dialog=new MyDialog(this);
		layout=(LinearLayout)findViewById(R.id.videoreceiver_layout);
		layout.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "提示：点击窗口外部关闭窗口！", 
						Toast.LENGTH_SHORT).show();	
			}
		});
	}

	
	
public void init(){
		
		
		Intent intent=new Intent(VideoReceiver.this,ChatService.class);
    	startService(intent);
       	bindService(intent, connection=new MyServiceConnection(), Context.BIND_AUTO_CREATE);
		
		
	}
	
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event){
		
		mMediaPlayer.stop();
		
		//等同于 拒绝的
		finish();
		return true;
	}
	
	public void button_video_yes(View v) {
		
		
		 mMediaPlayer.stop();
		 
		 
		 UDPMessage message=WinChatApplication.mainInstance.getMyUdpMessage(MessageInfo.video_ok+"/"+"m", Listener.RECEIVE_MSG);
			sendMsg(message);
     	
     	Intent intent = new Intent();
     intent.setClass(VideoReceiver.this,H264AndroidActivity.class);
   
     	
     	startActivity(intent);      
		
    	this.finish();    	
      }  
	public void button_video_no(View v) {  
    	this.finish();
    

    	mMediaPlayer.stop();
    	
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
			 Intent intent=new Intent(VideoReceiver.this,ChatService.class);
			 bindService(intent, connection=new MyServiceConnection(), Context.BIND_AUTO_CREATE);
			 Toast.makeText(this, "未发送出去,请重新发送", Toast.LENGTH_SHORT).show();
		}
	}
	
	
	public void VideoAlerm(){
		
		Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		  mMediaPlayer = new MediaPlayer();
		     try {
		         mMediaPlayer.setDataSource(this, alert);
		     } catch (Exception e) {
		         e.printStackTrace();
		     }
		     final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		     if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
		                 mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
		                 mMediaPlayer.setLooping(true);
		                 try {
		                     mMediaPlayer.prepare();
		                 } catch (IllegalStateException e) {
		                     e.printStackTrace();
		                 } catch (IOException e) {
		                     e.printStackTrace();
		                 }
		               //  isStart = true ;
		                 mMediaPlayer.start();
		       }
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


	@Override
	protected void onDestroy() {
		// TODO 自动生成的方法存根
		super.onDestroy();
		 unbindService(connection);
		
		
		
	}	
	
	
	
	
}
