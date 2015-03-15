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
	
	/**�������밴ť*/
	Button hang_up;
	/**��ʾ��ǰ��������ʱ��*/
	TextView timeTextView;
	/**��ʾ��ǰ��������ʱ��*/
	TextView callTextView;
	/** ��ʱ��*/
	private Timer timer = new Timer();
	/** ��¼������������ʱ��*/
	private int connectTime;
	/**����ʱ�����ָ��¾��*/
	Handler handler;
	/**��˺��ѷ�������ͨ��intent���ݣ�*/
	String friendName;
	/**����ý�����ͣ�ͨ��intent���ݣ�*/
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
					DisplayToast("�Է�ֱ�ӹҶ�");
					endAudio();
					//�رն�ʱ��
					stopTimer();
					//�����ϲ����
					CallOutActivity.this.finish();
					
					super.handleMessage(msg);
				}
			};	
		
			VideoInfo.Video_Play= new Handler(){
				public void handleMessage(android.os.Message msg) {
					DisplayToast("�Է�ͬ����Ƶ");
					
					
					Intent intent = new Intent();
				     intent.setClass(CallOutActivity.this,H264AndroidActivity.class);
				     startActivity(intent);  
				     CallOutActivity.this.finish();
					
					super.handleMessage(msg);
				}
			};			
			
			
			
			
			
			
			
		//���ùҶϰ�ť��������¼�
		hang_up.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				endAudio();
				//�رն�ʱ��
				stopTimer();
				//�����ϲ����
				CallOutActivity.this.finish();
			}
			
		});
		
		
//		AudioInfo.AudioinviteResponseHandler = new Handler(){
//        	@Override
//    		public void handleMessage(android.os.Message msg) {
//        		super.handleMessage(msg);
//        		if(Info.invite_response.equals("yes")){
//        			DisplayToast("��ת��ͨ������");	
//					if(mediaType.equals("audio")){
//        				Intent intent=new Intent(CallOutActivity.this,AudioCallActivity.class);
//        				startActivity(intent);
//        				//�رն�ʱ��
//        				stopTimer();
//        				CallOutActivity.this.finish();
//        			}
//        			else{
////        				Intent intent=new Intent(CallOutActivity.this,H264AndroidActivity.class);
//        				Intent intent=new Intent(CallOutActivity.this,H264AndroidActivity_video.class);
//        				startActivity(intent);
//        				//�رն�ʱ��
//        				stopTimer();
//        				CallOutActivity.this.finish();
//        			}
//        		}
//        		else{
//        			DisplayToast("����֮ǰ����");
//        			//����BYE
//        			endMedia();
//        			//�رն�ʱ��
//    				stopTimer();
//    				//�����ϲ����
//    				CallOutActivity.this.finish();
//        		}
//    		}
//        };
		
		//���ڸ�����ʾ�������ʱ��
		handler = new Handler(){
			public void handleMessage(Message msg){
				switch (msg.what){
					case 0x123:
						if(connectTime < 10){
							timeTextView.setText("00��0" + connectTime);
						}
						else{
							timeTextView.setText("00��" + connectTime);
						}
						connectTime++;
						//ʱ�䳬��һ�������Զ��ĹҶϴ˴�ͨ��
						if (connectTime == 60){
							endAudio();
							//�رն�ʱ��
							stopTimer();
							//�����ϲ����
							CallOutActivity.this.finish();
						}
						break;
				}
			}
		};
		
		//������ʱ��
		startTimer();
		
	}
	
	/**�ؼ���ʼ��*/
	public void init(){
		hang_up = (Button)findViewById(R.id.call_out_hangup);
		timeTextView = (TextView)findViewById(R.id.call_out_timeTextView);
		callTextView = (TextView)findViewById(R.id.call_out_text);
		
		Intent intent = getIntent();
		Bundle bd = intent.getExtras();
		//friendName = bd.getString("name");
		mediaType = bd.getString("mediatype");
		if(mediaType.equals("audio")){
			callTextView.setText("��" +"sb"+ "��������ͨ������..." );
		}
		else{
			callTextView.setText("��" +"xb"+ "������Ƶͨ������..." );
		}
	}
	
	

	
	
	
	/**�Ҷϵ绰�����ٺ���*/
	public void endAudio(){
		
		//���͹Ҷϵ绰������
		
		
		
		
		
	}
	
	
	/**������ʱ��*/
	private void startTimer(){
		this.timer.schedule(new TimerTask(){
			public void run(){
				handler.sendEmptyMessage(0x123);
			}
		}, 0, 1000);
	}
	
	/**ֹͣ��ʱ��*/
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
	
	/** ��ʾToast  */
	public void DisplayToast(String str){
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}
}

