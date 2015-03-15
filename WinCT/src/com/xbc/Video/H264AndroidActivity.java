package com.xbc.Video;



import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Calendar;

import org.opencore.avch264.NativeH264Decoder;
import org.opencore.avch264.NativeH264Encoder;

import com.FFmpeg.ffmpeg;


import com.ty.winchat.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.widget.Button;
import android.widget.Toast;

public class H264AndroidActivity extends Activity implements Callback, PreviewCallback{
	
	/**���Ż���*/
	SurfaceView sfv;
	/**���Ż���SurfaceHolder*/
	SurfaceHolder sfh;
	/**Ԥ������*/
	SurfaceView 	m_surface;
	/**Ԥ������SurfaceHolder*/
	SurfaceHolder   m_surfaceHolder; 
	
	
	
	SdCardStore Sd;
	
	
	public  RTPSending rtpsending=null;
	NativeH264Encoder encode;
	/**������͵������С����*/
	byte[] rtppkt = new byte[VideoInfo.divide_length + 2];
	
	 boolean Decoding=true; 	
	   
	    /**���벥������*/
	    int[] frame = new int[VideoInfo.width*VideoInfo.height];   

		//Bitmap VideoBit;   
	
	
	/**�ֻ�����ͷ�ĸ���*/
	private int numCamera;       
	/**ǰ������ͷ��Id*/
    private int cameraId_front;   
    /**��������ͷ��Id*/
    private int cameraId_back;
    /**�ж�ǰ������ͷ�Ƿ���ڵı�־λ*/
    private boolean frontExist = false;  
    
    /**��Ƶ����*/
	Button audioSwitch;
	/**������Ƶ���찴ť*/
	Button exitSwitch;
    
    /**����ͷ����*/
   	private Camera mCamera ;
   	/**��ʾ֡��*/
       private final int PreviewFrameRate = 10; 
       /**ˮƽ����*/
       private final int PreviewWidth = 176; 
       /**��ֱ����*/
       private final int PreviewHeight = 144;    
	
       FileOutputStream m_h264File;
	@Override
	public  void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);
		
		
		
		
		 if (android.os.Build.VERSION.SDK_INT > 9) {
	            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	            StrictMode.setThreadPolicy(policy);
	        }
	        
		 
		  WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);  //��ȡһ��windowmanager����
	        
	        //�Զ���ȡ��Ļ�ֱ���
		    Display display = manager.getDefaultDisplay();   //��ȡĬ����ʾ����
		    Point screenResolution = new Point(display.getWidth(), display.getHeight());  //ȡ����Ļ�ֱ���
		    
		    Log.e("TAG", "Screen resolution: " + screenResolution);
		    
		    
		  
		    
		    
		    
		 
	       
	            rtpsending = new RTPSending(); 
	           
		init();
		playVideo();
		
		
		Sd();
	}
	
	public void Sd(){
		
		  Sd=new SdCardStore();
			Sd.createSDCardDir();
			Sd.Creat();
		
		
		
		
		
		
	}
	
	/**
	 * ���г�ʼ���ؼ�����ز��� 
	 */
	public void init(){
		
		
		
		
		 GetNum=0;
	       	int i = mPixel.length;//ffmpeg
	    	
	        for(i=0; i<mPixel.length; i++){
	        	mPixel[i]=(byte)0x00;
	        }
	        
	  
	        
	        
	        
		//��ť��ʼ��
		audioSwitch = (Button)findViewById(R.id.audio_switch);
		exitSwitch = (Button)findViewById(R.id.shutdown_meeting);
		//Ԥ�������ʼ��
		sfv = (SurfaceView)findViewById(R.id.surfaceView);
		m_surface = (SurfaceView)this.findViewById(R.id.surface_preview);
		
		sfh = sfv.getHolder();
		sfh.addCallback(new Callback(){
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){
				System.out.println("surfaceChanged");
			}
			@Override
			public void surfaceCreated(SurfaceHolder holder){
				System.out.println("surfaceCreated");
			}
			@Override
			public void surfaceDestroyed(SurfaceHolder holder){
				System.out.println("surfaceDestroyed");
			}		
		});
		sfh.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);		
		   
        // �õ�SurfaceHolder����
        SurfaceHolder holder= m_surface.getHolder();  //����m_surface�ı�ʾ���������
        //���ûص�����
		holder.addCallback(H264AndroidActivity.this);   //��ӻص��ӿ�
        //���÷��
      	holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	/**
     * ��⵽surface�б仯ʱ���ô˺�����������ͷ��Ԥ����Ƶ
     */
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.e("", "surfaceChanged");
		NativeH264Encoder.InitEncoder(PreviewWidth, PreviewHeight, PreviewFrameRate);
		
		//��ȡ�ֻ�����ͷ�ĸ���
				numCamera = Camera.getNumberOfCameras();   
		        CameraInfo info = new CameraInfo();
		        for(int i = 0;i< numCamera;i++){
		        	Camera.getCameraInfo(i, info);
		        	if(info.facing == CameraInfo.CAMERA_FACING_BACK){
		        		cameraId_back = i;     //��ȡ��������ͷ��Id
		        	}
		        	if(info.facing == CameraInfo.CAMERA_FACING_FRONT){
		        		cameraId_front = i;    //��ȡǰ������ͷ��Id
		        		frontExist = true;
		        	}
		        }
//		        if(frontExist){
//		        	mCamera = Camera.open(cameraId_front);
//					DisplayToast("��ǰ������ͷ");
//				}else{
					mCamera = Camera.open(cameraId_back);
					DisplayToast("�򿪺�������ͷ");
				//}  
				try {
					mCamera.setPreviewDisplay(m_surfaceHolder);//����surface����ʵʱ��ʾ��m_surfaceHolder��ʾ��surface����ʾ��λ�ã�null��ʾ���
					//���ûؽк�����ÿ����ʾ֡��ʱ�򶼱�����
					 //����this�ǻؽ��źŶ������ÿ����ʾ֡�Ŀ�����null��ֹͣ���ջؽ��źš�
					mCamera.setPreviewCallback(this);  
					mCamera.setDisplayOrientation(90);
					//set camera 
					Camera.Parameters parame = mCamera.getParameters();    //��ȡ���ò�������
					parame.setPreviewFrameRate(PreviewFrameRate);    //����Camera����ʾ֡��
					parame.setPreviewSize(PreviewWidth, PreviewHeight);    //������Ļ�ֱ���
					parame.set("orientation", "portrait");
					//android2.3.3�Ժ������²�
					
					mCamera.setParameters(parame);
					
					
					
					//ͨ��SurfaceView��ʾȡ������
					//��ʼ����ʾ֡���в���ͻ�ͼ��surface
					mCamera.startPreview();   	
					// �Զ��Խ�
					mCamera.autoFocus(null);
				} catch (IOException e) {
					e.printStackTrace();
				}
	}
	
	/**
	 * �˺�����surface��һ�δ���ʱ����ִ�У��ɶ�surface��һЩ������������
	 */
	public void surfaceCreated(SurfaceHolder holder) {  
		Log.e("", "surfaceCreated");   
		m_surfaceHolder = holder;   //��������
	}

	/**
	 * ��һ��surface������֮ǰ����
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {  
		Log.e("", "surfaceDestroyed");
		if(mCamera != null){  //û�б�������ͷ�����
			mCamera.setPreviewCallback(null);//must do this��ֹͣ���ջؽ��ź�
			mCamera.stopPreview();   //ֹͣ����ͻ�ͼ
			mCamera.release();   //�Ͽ�������ͷ�����ӣ����ͷ�����ͷ��Դ
		}
		
		this.Sd.Close();    //�رմ�����������ͷ�������йص�����ϵͳ��Դ
		NativeH264Encoder.DeinitEncoder();
	
		rtpsending=null;
		try {
			m_h264File.close();
		} catch (IOException e) {
			// TODO �Զ����ɵ� catch ��
			e.printStackTrace();
		}
		Uninit_Decoder();
		
	}
	final byte[] head = {0x00,0x00,0x00,0x01};
	static long time = System.currentTimeMillis(); 
	/**
	 * ����ͷԤ���ص��������ڴ˽�����Ƶ����H.264����
	 */
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		System.out.println("xbc...is...sb");
		
		
		byte[]encodeResult = NativeH264Encoder.EncodeFrame(data, time);  //���б��룬����������Ž�����
		time += 1000/PreviewFrameRate;    //�����һ֡���ķѵ�ʱ�䣬��λΪ����
		int encodeState = NativeH264Encoder.getLastEncodeStatus();    //��ȡ���ı���״̬��0������ʾ�ɹ�����
		
		if(VideoInfo.endView== true){    //�յ�BYE����رյ�ǰ��Ƶ�ɼ����ܣ����»ص�ע��֮��ĵȴ��������
			
			VideoInfo.endView = false;
          	this.finish();
		}
		
		if(encodeState ==0 && encodeResult.length>0)
		{	
			Log.e("TAG","encode len:"+encodeResult.length);//��ӡ�������ĳ���
			
			DivideAndSendNal(encodeResult);		
		}
		Sd.Write(head);   //��head�е��ֽ�д�����������д���·���µ��ļ���
		Sd.Write(encodeResult);  	
		
	}
		/**��Ƭ�����ͷ���*/
		public void DivideAndSendNal(byte[] h264){
			
			if(h264.length > 0){  //�����ݲŽ��з�Ƭ���Ͳ���
				if(h264.length > VideoInfo.divide_length){
					VideoInfo.dividingFrame = true;
					VideoInfo.status = true;
					VideoInfo.firstPktReceived = false;
					VideoInfo.pktflag = 0;

					while(VideoInfo.status){
						if(!VideoInfo.firstPktReceived){  //�װ�
							sendFirstPacket(h264);
						}
						else{
							if(h264.length - VideoInfo.pktflag > VideoInfo.divide_length){  //�а�
								sendMiddlePacket(h264);
							}
							else{   //ĩ��
								sendLastPacket(h264);
							}
						} //end of �װ�
					}//end of while
				}
				else{   //����Ƭ��
					sendCompletePacket(h264);
				}
			}
		}
		
		/**�����װ�*/
		public void sendFirstPacket(byte[] h264){

			rtppkt[0] = (byte) (h264[0] & 0xe0);
			rtppkt[0] = (byte) (rtppkt[0] + 0x1c);
			rtppkt[1] = (byte) (0x80 + (h264[0]&0x1f));
			try{
				System.arraycopy(h264,0,rtppkt, 2,VideoInfo.divide_length);	
			}catch(Exception e){
				e.printStackTrace();
			}
			VideoInfo.pktflag = VideoInfo.pktflag + VideoInfo.divide_length;
			VideoInfo.firstPktReceived = true;					
			//����RTP���ĸ�������Ϊ0x62
			VideoInfo.rtpRecv.rtpSession.payloadType(0x62);
			//���ʹ������
			VideoInfo.rtpRecv.rtpSession.sendData(rtppkt);   //���ʹ������
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		/**�����а�*/
		public void sendMiddlePacket(byte[] h264){

			rtppkt[0] = (byte) (h264[0] & 0xe0);
			rtppkt[0] = (byte) (rtppkt[0] + 0x1c);
			rtppkt[1] = (byte) (0x00 + (h264[0]&0x1f));
			
			try{
				System.arraycopy(h264,VideoInfo.pktflag,rtppkt, 2,VideoInfo.divide_length);	
			}catch(Exception e){
				e.printStackTrace();
			}
			VideoInfo.pktflag = VideoInfo.pktflag + VideoInfo.divide_length;
			//����RTP���ĸ�������Ϊ0x62
			VideoInfo.rtpRecv.rtpSession.payloadType(0x62);
			//���ʹ������
			VideoInfo.rtpRecv.rtpSession.sendData(rtppkt);   //���ʹ������   //���ʹ������
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		/**����ĩ��*/
		public static void sendLastPacket(byte[] h264){
			byte[] rtppktLast = new byte[h264.length - VideoInfo.pktflag + 2];
			rtppktLast[0] = (byte) (h264[0] & 0xe0);
			rtppktLast[0] = (byte) (rtppktLast[0] + 0x1c);
			rtppktLast[1] = (byte) (0x40 + (h264[0]&0x1f));
			try{
				System.arraycopy(h264,VideoInfo.pktflag,rtppktLast, 2,h264.length - VideoInfo.pktflag);	
			}catch(Exception e){
				e.printStackTrace();
			}
			//����RTP���ĸ�������Ϊ0x62
			VideoInfo.rtpRecv.rtpSession.payloadType(0x62);	
			//���ʹ������
			VideoInfo.rtpRecv.rtpSession.sendData(rtppktLast);   //���ʹ������  //���ʹ������
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			VideoInfo.status = false;  //��������������һ�����н���
			VideoInfo.dividingFrame = false;  //һ֡��Ƭ�����ϣ�ʱ�������һ֡
		}
		
		/**����������*/
		public void sendCompletePacket(byte[] h264){
			//����RTP���ĸ�������Ϊ0x62
			VideoInfo.rtpRecv.rtpSession.payloadType(0x62);
			//���ʹ������
			VideoInfo.rtpRecv.rtpSession.sendData(h264);   //���ʹ������   //���ʹ������
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
		/**
	     * ������Ƶ�����߳�
	     */
		private void playVideo() {
			new Thread(Video).start();
		}
		
		private boolean flagIframe=false;
		private int GetNum=0;
		/**
		 * ��Ƶ���벥���߳�:opencore
		 */
		
		 
		 byte [] mPixel = new byte[176*144*2];  
	    ByteBuffer buffer = ByteBuffer.wrap( mPixel );//ffmpeg
	    ffmpeg codec = new ffmpeg();//good ffmpeg
	    
	    Bitmap VideoBit = Bitmap.createBitmap(176,144, Config.RGB_565); 
//	    /**
//	     * ������Ƶ�����߳�
//	     */
//		private void playVideo() {
//			new Thread(Video).start();
//		}
//		
//		private boolean flagIframe=false;    
	    Runnable Video = new Runnable(){
			public void run(){
				 int iTemp=0;
				byte nal[]=new byte[50000];
				codec.init(176,144);
				int Buffer_length = 0;
				//NativeH264Decoder.InitDecoder(VideoInfo.width, VideoInfo.height);
//				byte pps_soft[] = {0x27,0x42,0x10,0x09,(byte) 0x96,0x35,0x05,(byte) 0x89,(byte) 0xc8};
//			    byte sps_soft[] = {0x28,(byte) 0xce,0x02,(byte) 0xfc,(byte) 0x80}; 
			    
			    byte pps_soft[] = {0x00, 0x00, 0x00, 0x01 ,0x27,0x42,0x10,0x09,(byte) 0x96,0x35,0x05,(byte) 0x89,(byte) 0xc8};
				byte sps_soft[] = {0x00, 0x00, 0x00, 0x01 ,0x28,(byte) 0xce,0x02,(byte) 0xfc,(byte) 0x80};  
			    
			    iTemp=codec.DecoderNal(pps_soft,13,mPixel); //���ý�����
			    iTemp=codec.DecoderNal(sps_soft,9,mPixel);
		    	
			    
//			    NativeH264Decoder.DecodeAndConvert(pps_soft, frame);
//		    	NativeH264Decoder.DecodeAndConvert(sps_soft, frame);
				while(Decoding) {
					Info.NalBuffer[GetNum].IsReadAble();  
					Buffer_length=Info.NalBuffer[GetNum].getNalLen();  //ffmpeg  
					nal=Info.NalBuffer[GetNum].getReadable_NalBuf();
					//nal=VideoInfo.rtpRecv.NalBuffer[GetNum].getReadable_NalBuf2();
			    
			    	//�ж������������һ֡�ǲ�����֡������֡�ſ�ʼ���룬������
			    	//�ֻ��豸�������ʱ��25����֡��21����֡
					if(!flagIframe){   //û���յ���֡
			    		if((nal[4]&0xff) == 0x25){    //�յ���֡��������λ������flag��־λ���´ν����ٽ�����֡�жϣ�ֱ�ӽ��н���
			    			flagIframe=true;
			    		}
			    		else{    //û���յ���֡����ջ��棬��������
			    			Log.d("zzframe","else");
			    			Info.NalBuffer[GetNum].cleanNalBuf();
			    			Info.NalBuffer[GetNum].setNalLen(0);
			    			Info.NalBuffer[GetNum].ReadLock();
		    	         	GetNum++;
		    	         	if(GetNum==200)
		    	         	{GetNum=0; }
		    	         	continue;
		    	        }
			    	}
		         	try{	
			    		//NativeH264Decoder.DecodeAndConvert(nal, frame);
		         		iTemp=codec.DecoderNal(nal,Buffer_length,mPixel);//good ffmpeg
		         		System.out.println("is>>>>>jiema");
		         		if(iTemp > 0){
				    		Log.e("qqqqqq", "I'm coming too");
				    		dosurfacedraw1();}
			    		//dosurfacedraw();   //��ͼ���Ƶ�SurfaceView��
		         		Info.NalBuffer[GetNum].cleanNalBuf();
		         		Info.NalBuffer[GetNum].setNalLen(0);
		         		Info.NalBuffer[GetNum].ReadLock();
		     			GetNum++;
		     			if(GetNum==200)
		     				GetNum=0;   
					    }
				    catch(Exception e){
				    	e.printStackTrace();
				    }			
	 		    }
				Uninit_Decoder();	}
			 
		};
		
	    /**
	     * ����ͼ����Ļ��
	     */
		private void dosurfacedraw() { 
			VideoBit = Bitmap.createBitmap(frame,176,144, Bitmap.Config.RGB_565);
			int surfaceViewWidth = sfv.getWidth();
			int surfaceViewHeight = sfv.getHeight();
			int bmpWidth = VideoBit.getWidth();
			int bmpHeight = VideoBit.getHeight();
	        Matrix matrix = new Matrix();
	        float scalewh;
	        float scalehh;
	        matrix.setRotate(90);  //ֻ�����ǰ������ͷ���ֻ�¼�Ƶ���Ƶ������ת
	        //ʵ������Ƶ���Ź���
//	        if(surfaceViewWidth > surfaceViewHeight){
//	        	scalewh = (float)surfaceViewWidth/bmpWidth;
//	        	scalehh = (float)surfaceViewWidth/bmpWidth;
//	        }else{
//	            scalewh = (float)surfaceViewHeight/bmpHeight;
//	            scalehh = (float)surfaceViewHeight/bmpHeight;
//	        }
	        //�ֻ�����ͷ��Ƶ���Ź�����Ϊ��ת���ˣ��������Ź���һ����
	        if(surfaceViewWidth > surfaceViewHeight){
	        	scalewh = (float)surfaceViewWidth/bmpHeight;
	        	scalehh = (float)surfaceViewWidth/bmpHeight;
	        }else{
	            scalewh = (float)surfaceViewHeight/bmpWidth;
	            scalehh = (float)surfaceViewHeight/bmpWidth;
	        }
	        
	        
	        matrix.postScale(scalewh, scalehh);
	        
	        Bitmap resizeBmp = Bitmap.createBitmap(VideoBit,0 ,0 ,bmpWidth,bmpHeight,matrix,true); 
	        Canvas canvas = sfh.lockCanvas();
	        canvas.drawBitmap(resizeBmp, 0, 0, null); 	
	        sfh.unlockCanvasAndPost(canvas);		
		}	
		
		
		private void dosurfacedraw1() { 
		
			VideoBit.copyPixelsFromBuffer(buffer);//makeBuffer(data565, N));
	        
			int surfaceViewWidth = sfv.getWidth();
			int surfaceViewHeight = sfv.getHeight();
			int bmpWidth = VideoBit.getWidth();
			int bmpHeight = VideoBit.getHeight();
	        Matrix matrix = new Matrix();
	        float scalewh;
	        float scalehh;
	        matrix.setRotate(270);
//	        if(frontExist){
//	        	matrix.setRotate(90);  //���ò��Ż�����ת90�ȣ�С��2����270�ȣ�G13��
//		    }
//		    else{
//		    	matrix.setRotate(270);  //���ò��Ż�����ת90�ȣ�С��2����270�ȣ�G13��
//		    }
		
		    //�ֻ�����ͷ��Ƶ���Ź�����Ϊ��ת���ˣ��������Ź���һ����
		    if(surfaceViewWidth > surfaceViewHeight){
		    	scalewh = (float)surfaceViewWidth/bmpHeight;
		    	scalehh = (float)surfaceViewWidth/bmpHeight;
		    }else{
		        scalewh = (float)surfaceViewHeight/bmpWidth;
		        scalehh = (float)surfaceViewHeight/bmpWidth;
		    }
	       
	        matrix.postScale(scalewh, scalehh);
	        
	        Bitmap resizeBmp = Bitmap.createBitmap(VideoBit,0 ,0 ,bmpWidth,bmpHeight,matrix,true);
	        Canvas canvas = sfh.lockCanvas();
	        canvas.drawBitmap( resizeBmp, 0, 0, null);  	
	        sfh.unlockCanvasAndPost(canvas);		
				}
	
		
		/**
		 * ������������
		 */
	    public void Uninit_Decoder(){
	    	this.Decoding=false;
	    }	
		
	
	
	/**
 	 * ��ʾtoast
 	 */
	public void DisplayToast(String str){
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}
	

}
