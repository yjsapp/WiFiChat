package com.xbc.Audio;


import java.util.Timer;
import java.util.TimerTask;

import com.ty.winchat.R;
import com.ty.winchat.service.ChatService;
import com.xbc.Video.H264AndroidActivity;
import com.xbc.Video.VideoInfo;
import com.xbc.Video.VideoReceiver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CallOutActivity extends Activity{
	
	/**结束邀请按钮*/
	Button hang_up;
	/**显示当前邀请所用时间*/
	TextView timeTextView;
	/**显示当前邀请所用时间*/
	TextView callTextView;
	/** 定时器*/
	private Timer timer = new Timer();
	/** 记录邀请连接所用时间*/
	private int connectTime;
	/**连接时间文字更新句柄*/
	Handler handler;
	/**向此好友发起请求（通过intent传递）*/
	String friendName;
	/**请求媒体类型（通过intent传递）*/
	String mediaType;
	
	
	
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.call_out);
		init();
		
		
		
		AudioInfo.Audio_Listen= new Handler(){
			public void handleMessage(android.os.Message msg) {
				Intent intent=new Intent(CallOutActivity.this,AudioCallActivity.class);
			          startActivity(intent);
				CallOutActivity.this.finish();
				super.handleMessage(msg);
			}
		};	
		
		 AudioInfo.Audio_Off_1= new Handler(){
				public void handleMessage(android.os.Message msg) {
					DisplayToast("对方直接挂断");
					endAudio();
					//关闭定时器
					stopTimer();
					//返回上层界面
					CallOutActivity.this.finish();
					
					super.handleMessage(msg);
				}
			};	
		
			VideoInfo.Video_Play= new Handler(){
				public void handleMessage(android.os.Message msg) {
					DisplayToast("对方同意视频");
					
					
					Intent intent = new Intent();
				     intent.setClass(CallOutActivity.this,H264AndroidActivity.class);
				     startActivity(intent);  
				     CallOutActivity.this.finish();
					
					super.handleMessage(msg);
				}
			};			
			
			
			
			
			
			
			
		//设置挂断按钮点击监听事件
		hang_up.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				endAudio();
				//关闭定时器
				stopTimer();
				//返回上层界面
				CallOutActivity.this.finish();
			}
			
		});
		
		
//		AudioInfo.AudioinviteResponseHandler = new Handler(){
//        	@Override
//    		public void handleMessage(android.os.Message msg) {
//        		super.handleMessage(msg);
//        		if(Info.invite_response.equals("yes")){
//        			DisplayToast("跳转到通话界面");	
//					if(mediaType.equals("audio")){
//        				Intent intent=new Intent(CallOutActivity.this,AudioCallActivity.class);
//        				startActivity(intent);
//        				//关闭定时器
//        				stopTimer();
//        				CallOutActivity.this.finish();
//        			}
//        			else{
////        				Intent intent=new Intent(CallOutActivity.this,H264AndroidActivity.class);
//        				Intent intent=new Intent(CallOutActivity.this,H264AndroidActivity_video.class);
//        				startActivity(intent);
//        				//关闭定时器
//        				stopTimer();
//        				CallOutActivity.this.finish();
//        			}
//        		}
//        		else{
//        			DisplayToast("返回之前界面");
//        			//发送BYE
//        			endMedia();
//        			//关闭定时器
//    				stopTimer();
//    				//返回上层界面
//    				CallOutActivity.this.finish();
//        		}
//    		}
//        };
		
		//用于更新显示邀请持续时间
		handler = new Handler(){
			public void handleMessage(Message msg){
				switch (msg.what){
					case 0x123:
						if(connectTime < 10){
							timeTextView.setText("00：0" + connectTime);
						}
						else{
							timeTextView.setText("00：" + connectTime);
						}
						connectTime++;
						//时间超过一分钟则自动的挂断此次通话
						if (connectTime == 60){
							endAudio();
							//关闭定时器
							stopTimer();
							//返回上层界面
							CallOutActivity.this.finish();
						}
						break;
				}
			}
		};
		
		//开启定时器
		startTimer();
		
	}
	
	/**控件初始化*/
	public void init(){
		hang_up = (Button)findViewById(R.id.call_out_hangup);
		timeTextView = (TextView)findViewById(R.id.call_out_timeTextView);
		callTextView = (TextView)findViewById(R.id.call_out_text);
		
		Intent intent = getIntent();
		Bundle bd = intent.getExtras();
		//friendName = bd.getString("name");
		mediaType = bd.getString("mediatype");
		if(mediaType.equals("audio")){
			callTextView.setText("向" +"sb"+ "发起语音通话邀请..." );
		}
		else{
			callTextView.setText("向" +"xb"+ "发起视频通话邀请..." );
		}
	}
	
	

	
	
	
	/**挂断电话，不再呼叫*/
	public void endAudio(){
		
		//发送挂断电话的信令
		
		
		
		
		
	}
	
	
	/**开启定时器*/
	private void startTimer(){
		this.timer.schedule(new TimerTask(){
			public void run(){
				handler.sendEmptyMessage(0x123);
			}
		}, 0, 1000);
	}
	
	/**停止定时器*/
	private void stopTimer(){
		if(this.timer != null){
			this.timer.cancel();
			this.timer = null;
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

	}
	
	/** 显示Toast  */
	public void DisplayToast(String str){
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}
}

