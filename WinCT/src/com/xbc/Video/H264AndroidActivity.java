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
	
	/**播放画面*/
	SurfaceView sfv;
	/**播放画面SurfaceHolder*/
	SurfaceHolder sfh;
	/**预览画面*/
	SurfaceView 	m_surface;
	/**预览画面SurfaceHolder*/
	SurfaceHolder   m_surfaceHolder; 
	
	
	
	SdCardStore Sd;
	
	
	public  RTPSending rtpsending=null;
	NativeH264Encoder encode;
	/**打包发送的数组大小定义*/
	byte[] rtppkt = new byte[VideoInfo.divide_length + 2];
	
	 boolean Decoding=true; 	
	   
	    /**解码播放数组*/
	    int[] frame = new int[VideoInfo.width*VideoInfo.height];   

		//Bitmap VideoBit;   
	
	
	/**手机摄像头的个数*/
	private int numCamera;       
	/**前置摄像头的Id*/
    private int cameraId_front;   
    /**后置摄像头的Id*/
    private int cameraId_back;
    /**判断前置摄像头是否存在的标志位*/
    private boolean frontExist = false;  
    
    /**音频开关*/
	Button audioSwitch;
	/**结束视频聊天按钮*/
	Button exitSwitch;
    
    /**摄像头引用*/
   	private Camera mCamera ;
   	/**演示帧率*/
       private final int PreviewFrameRate = 10; 
       /**水平像素*/
       private final int PreviewWidth = 176; 
       /**垂直像素*/
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
	        
		 
		  WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);  //获取一个windowmanager对象
	        
	        //自动获取屏幕分辨率
		    Display display = manager.getDefaultDisplay();   //获取默认演示对象
		    Point screenResolution = new Point(display.getWidth(), display.getHeight());  //取得屏幕分辨率
		    
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
	 * 进行初始化控件的相关操作 
	 */
	public void init(){
		
		
		
		
		 GetNum=0;
	       	int i = mPixel.length;//ffmpeg
	    	
	        for(i=0; i<mPixel.length; i++){
	        	mPixel[i]=(byte)0x00;
	        }
	        
	  
	        
	        
	        
		//按钮初始化
		audioSwitch = (Button)findViewById(R.id.audio_switch);
		exitSwitch = (Button)findViewById(R.id.shutdown_meeting);
		//预览画面初始化
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
		   
        // 得到SurfaceHolder对象
        SurfaceHolder holder= m_surface.getHolder();  //返回m_surface的表示符（句柄）
        //设置回调函数
		holder.addCallback(H264AndroidActivity.this);   //添加回调接口
        //设置风格
      	holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	/**
     * 检测到surface有变化时调用此函数，打开摄像头，预览视频
     */
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.e("", "surfaceChanged");
		NativeH264Encoder.InitEncoder(PreviewWidth, PreviewHeight, PreviewFrameRate);
		
		//获取手机摄像头的个数
				numCamera = Camera.getNumberOfCameras();   
		        CameraInfo info = new CameraInfo();
		        for(int i = 0;i< numCamera;i++){
		        	Camera.getCameraInfo(i, info);
		        	if(info.facing == CameraInfo.CAMERA_FACING_BACK){
		        		cameraId_back = i;     //获取后置摄像头的Id
		        	}
		        	if(info.facing == CameraInfo.CAMERA_FACING_FRONT){
		        		cameraId_front = i;    //获取前置摄像头的Id
		        		frontExist = true;
		        	}
		        }
//		        if(frontExist){
//		        	mCamera = Camera.open(cameraId_front);
//					DisplayToast("打开前置摄像头");
//				}else{
					mCamera = Camera.open(cameraId_back);
					DisplayToast("打开后置摄像头");
				//}  
				try {
					mCamera.setPreviewDisplay(m_surfaceHolder);//设置surface进行实时演示，m_surfaceHolder表示在surface上演示的位置，null表示清楚
					//设置回叫函数，每个演示帧的时候都被呼叫
					 //参数this是回叫信号对象接收每个演示帧的拷贝，null则停止接收回叫信号。
					mCamera.setPreviewCallback(this);  
					mCamera.setDisplayOrientation(90);
					//set camera 
					Camera.Parameters parame = mCamera.getParameters();    //获取配置参数对象
					parame.setPreviewFrameRate(PreviewFrameRate);    //设置Camera的演示帧率
					parame.setPreviewSize(PreviewWidth, PreviewHeight);    //设置屏幕分辨率
					parame.set("orientation", "portrait");
					//android2.3.3以后无需下步
					
					mCamera.setParameters(parame);
					
					
					
					//通过SurfaceView显示取景画面
					//开始对演示帧进行捕获和绘图到surface
					mCamera.startPreview();   	
					// 自动对焦
					mCamera.autoFocus(null);
				} catch (IOException e) {
					e.printStackTrace();
				}
	}
	
	/**
	 * 此函数在surface第一次创建时立即执行，可对surface的一些参数进行设置
	 */
	public void surfaceCreated(SurfaceHolder holder) {  
		Log.e("", "surfaceCreated");   
		m_surfaceHolder = holder;   //参数设置
	}

	/**
	 * 在一个surface被销毁之前调用
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {  
		Log.e("", "surfaceDestroyed");
		if(mCamera != null){  //没有背面摄像头的情况
			mCamera.setPreviewCallback(null);//must do this，停止接收回叫信号
			mCamera.stopPreview();   //停止捕获和绘图
			mCamera.release();   //断开与摄像头的连接，并释放摄像头资源
		}
		
		this.Sd.Close();    //关闭此输出流，并释放与此流有关的所有系统资源
		NativeH264Encoder.DeinitEncoder();
	
		rtpsending=null;
		try {
			m_h264File.close();
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		Uninit_Decoder();
		
	}
	final byte[] head = {0x00,0x00,0x00,0x01};
	static long time = System.currentTimeMillis(); 
	/**
	 * 摄像头预览回调函数，在此进行视频数据H.264编码
	 */
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		System.out.println("xbc...is...sb");
		
		
		byte[]encodeResult = NativeH264Encoder.EncodeFrame(data, time);  //进行编码，将编码结果存放进数组
		time += 1000/PreviewFrameRate;    //计算出一帧所耗费的时间，单位为毫秒
		int encodeState = NativeH264Encoder.getLastEncodeStatus();    //获取最后的编码状态，0——表示成功！！
		
		if(VideoInfo.endView== true){    //收到BYE命令，关闭当前视频采集功能，重新回到注册之后的等待邀请界面
			
			VideoInfo.endView = false;
          	this.finish();
		}
		
		if(encodeState ==0 && encodeResult.length>0)
		{	
			Log.e("TAG","encode len:"+encodeResult.length);//打印编码结果的长度
			
			DivideAndSendNal(encodeResult);		
		}
		Sd.Write(head);   //将head中的字节写入输出流，即写入此路径下的文件中
		Sd.Write(encodeResult);  	
		
	}
		/**分片、发送方法*/
		public void DivideAndSendNal(byte[] h264){
			
			if(h264.length > 0){  //有数据才进行分片发送操作
				if(h264.length > VideoInfo.divide_length){
					VideoInfo.dividingFrame = true;
					VideoInfo.status = true;
					VideoInfo.firstPktReceived = false;
					VideoInfo.pktflag = 0;

					while(VideoInfo.status){
						if(!VideoInfo.firstPktReceived){  //首包
							sendFirstPacket(h264);
						}
						else{
							if(h264.length - VideoInfo.pktflag > VideoInfo.divide_length){  //中包
								sendMiddlePacket(h264);
							}
							else{   //末包
								sendLastPacket(h264);
							}
						} //end of 首包
					}//end of while
				}
				else{   //不分片包
					sendCompletePacket(h264);
				}
			}
		}
		
		/**发送首包*/
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
			//设置RTP包的负载类型为0x62
			VideoInfo.rtpRecv.rtpSession.payloadType(0x62);
			//发送打包数据
			VideoInfo.rtpRecv.rtpSession.sendData(rtppkt);   //发送打包数据
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		/**发送中包*/
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
			//设置RTP包的负载类型为0x62
			VideoInfo.rtpRecv.rtpSession.payloadType(0x62);
			//发送打包数据
			VideoInfo.rtpRecv.rtpSession.sendData(rtppkt);   //发送打包数据   //发送打包数据
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		/**发送末包*/
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
			//设置RTP包的负载类型为0x62
			VideoInfo.rtpRecv.rtpSession.payloadType(0x62);	
			//发送打包数据
			VideoInfo.rtpRecv.rtpSession.sendData(rtppktLast);   //发送打包数据  //发送打包数据
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			VideoInfo.status = false;  //打包组包结束，下一步进行解码
			VideoInfo.dividingFrame = false;  //一帧分片打包完毕，时间戳改下一帧
		}
		
		/**发送完整包*/
		public void sendCompletePacket(byte[] h264){
			//设置RTP包的负载类型为0x62
			VideoInfo.rtpRecv.rtpSession.payloadType(0x62);
			//发送打包数据
			VideoInfo.rtpRecv.rtpSession.sendData(h264);   //发送打包数据   //发送打包数据
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
		/**
	     * 开启视频解码线程
	     */
		private void playVideo() {
			new Thread(Video).start();
		}
		
		private boolean flagIframe=false;
		private int GetNum=0;
		/**
		 * 视频解码播放线程:opencore
		 */
		
		 
		 byte [] mPixel = new byte[176*144*2];  
	    ByteBuffer buffer = ByteBuffer.wrap( mPixel );//ffmpeg
	    ffmpeg codec = new ffmpeg();//good ffmpeg
	    
	    Bitmap VideoBit = Bitmap.createBitmap(176,144, Config.RGB_565); 
//	    /**
//	     * 开启视频解码线程
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
			    
			    iTemp=codec.DecoderNal(pps_soft,13,mPixel); //设置解码器
			    iTemp=codec.DecoderNal(sps_soft,9,mPixel);
		    	
			    
//			    NativeH264Decoder.DecodeAndConvert(pps_soft, frame);
//		    	NativeH264Decoder.DecodeAndConvert(sps_soft, frame);
				while(Decoding) {
					Info.NalBuffer[GetNum].IsReadAble();  
					Buffer_length=Info.NalBuffer[GetNum].getNalLen();  //ffmpeg  
					nal=Info.NalBuffer[GetNum].getReadable_NalBuf();
					//nal=VideoInfo.rtpRecv.NalBuffer[GetNum].getReadable_NalBuf2();
			    
			    	//判断送入解码器第一帧是不是主帧，是主帧才开始解码，否则丢弃
			    	//手机设备软件编码时，25：主帧；21：辅帧
					if(!flagIframe){   //没有收到主帧
			    		if((nal[4]&0xff) == 0x25){    //收到主帧，进行置位；更改flag标志位，下次将不再进行主帧判断，直接进行解码
			    			flagIframe=true;
			    		}
			    		else{    //没有收到主帧，清空缓存，继续接收
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
			    		//dosurfacedraw();   //将图绘制到SurfaceView中
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
	     * 绘制图像到屏幕上
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
	        matrix.setRotate(90);  //只针对有前置摄像头的手机录制的视频进行旋转
	        //实验室视频缩放规则
//	        if(surfaceViewWidth > surfaceViewHeight){
//	        	scalewh = (float)surfaceViewWidth/bmpWidth;
//	        	scalehh = (float)surfaceViewWidth/bmpWidth;
//	        }else{
//	            scalewh = (float)surfaceViewHeight/bmpHeight;
//	            scalehh = (float)surfaceViewHeight/bmpHeight;
//	        }
	        //手机摄像头视频缩放规则（因为旋转过了，所以缩放规则不一样）
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
//	        	matrix.setRotate(90);  //设置播放画面旋转90度（小米2），270度（G13）
//		    }
//		    else{
//		    	matrix.setRotate(270);  //设置播放画面旋转90度（小米2），270度（G13）
//		    }
		
		    //手机摄像头视频缩放规则（因为旋转过了，所以缩放规则不一样）
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
		 * 结束解码设置
		 */
	    public void Uninit_Decoder(){
	    	this.Decoding=false;
	    }	
		
	
	
	/**
 	 * 显示toast
 	 */
	public void DisplayToast(String str){
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}
	

}
