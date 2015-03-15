package com.xbc.Video;




import android.util.Log;

import jlibrtp.DataFrame;
import jlibrtp.Participant;
import jlibrtp.RTPAppIntf;
import jlibrtp.RTPSession;
/**********************************************************************************
 * RTP类实现功能：
 *   此类主要实现视频包的接收和激活包的发送：创建rtp会话接收rtp数据包，设置缓存对数据包进行排序，
 *调用GetNal函数进行组包成为Nal包后存储到指定缓存区等待解码；获取激活码和视频转发服务器地址，
 *发送激活包到视频转发服务器激活代码。
 ***********************************************************************************/
public class RTP implements RTPAppIntf {
	 RTPSession rtpSession = null;	//rtpSession     新建RTP会话类
	 byte pH264StreamHead[] = {0x00, 0x00, 0x00, 0x01};  //Nal流的前4个字节{00,00,00,01}，用于构成一个完整的Nal帧
	 static int index=0;
//	 private static final String TAG3="RTPlost";
	 private static final String TAG="RTPPackets";
	 private static final String TAG1="NALUWrite";
//	 private static final String TAG4="RTPdisorder";
	 private static final String TAG6="step";
	 private static final String TAG7="GetNal";
	 
	 String index_RTP;
	 String info_RTP;
	 String info_NALU;
	 int index_now=0;
	 int packet_lost=0;
	 static int flag ;    //用于标记是否收到主帧，0表示收到，1表示没有收到，初值赋值为1
	 int headseqnum;
	 int tailseqnum;
	 int subduction;
	 int AttachNum;
	 int count ;
	 
	 int throwFrameNumber=0;   //丢弃的辅帧数
	 
	 boolean bDisorder=false;
	 byte temp_nal[]=new byte[50000];    //设置的临时缓存大小
	 StreamBuf m_pVFragmentBuffer;       //RTP缓存类
	  
	 private int PreSeq;  //前一个收到的RTP包的序列号
	 private int state;	  //记录Nal组包的状态判断，0表示没收到首包，1表示收到首包
	 private int PutNum;  //当前指向写入Nal缓存的位置
	 private int first=0;
	 
	 /*************************************************************
		 * 功能：RTP的构造函数 
		 * 参数：无
		 * 返回值：无
		 * 详细说明：
		 * 1）RTP的构造函数，初始化RTP接受类。初始化存储RTP包的缓存为100，
		 * 设置RTP包超过5个时，调用GetNal函数组包。设置Nal缓存，为构造的NalBuf类赋初值。
		 * PreSeq是上次接受的RTP包序列号， 初值为-1。
		 * state表示本Nal包是否收到RTP首包，0表示没有，1表示有，初值赋0。
		 * Info.PutNum表示Nal缓存的指向位置，初值为0，最大为199。
		 * **************************************************************/	
	public RTP() {	  
		PreSeq=-1;      //前一个RTP序列包为-1，表示没有收到RTP包
		state=0;        //没有收到首包
		flag=1;         //没有收到主帧
		PutNum=0;       //指向缓存位置为0
		headseqnum=0;
		tailseqnum=0;
		subduction=0;    //设置初始状态
		
		Info.packet_got = 0;
		Info.main_suc = 0;
		Info.assis_suc = 0;
		
		//每个单元相当于一个结构体，NALbuffer每个单元大小设置为50000，。。。等等
		for(int i=0;i<Info.NalBuffer.length;i++){
			Info.NalBuffer[i]=new NalBuffer();
		}
		
		//初始化存储RTP包的缓存为100，设置RTP包超过5个时，调用GetNal函数组包。
		m_pVFragmentBuffer=new StreamBuf(100,5);
		
		Log.d(TAG,"RTP has been opened!");
		
	}
	
	/*************************************************************
	 * 功能：
	 * 从RTPSessionRegister方法中的AppCallerThread线程中调用的。
	 * 它实现了对于RTP包接受，去掉包头，提取有用负载。
	 * 是RTPAppIntf接口的抽象方法实现的具体实现。
	 * 参数：
	 * frame ：接受到的RTP包，封装成为一个DataFram类的帧
	 * Participant：一次RTP会话的参与者
	 * 返回值：无
	 * 详细说明：
	 * 1)	接受RTP包，去掉RTP包头，提取负载。
	 * 2)	把RTP包存入pRtpFrameNode节点，再把节点存入RTP缓存m_pVFragmentBuffer中。
	 * 3)	在RTP缓存中对接受到的RTP包进行排序，设置在接受到超过5个包时，调用GetNal函数进行Nal组包。
	 * 注解：
	 *     此处pRtpFrameNode即是pNode，m_pVFragmentBuffer即是Nal
	 * **************************************************************/
	public void receiveData(DataFrame frame, Participant p) {//Rtp回调函数
		int len=0;
		int seqnum[]=null;
		byte data[]=null;
		StreamBufNode frame_second = null;
		if(frame.payloadType()==98 |frame.payloadType()==96)//判断接收包是不是视频包
		{
			Info.packet_got ++;
			StreamBufNode pRtpFrameNode = new StreamBufNode(frame); //把RTP包存入pRtpFrameNode节点
			Log.d(TAG6,"new StreamBufNode"); 
			m_pVFragmentBuffer.AddToBufBySeq(pRtpFrameNode);        //接收到的RTP包排序，再放入Rtp缓存		
			
			if(m_pVFragmentBuffer.IsReady())                        //若缓存RTP包超过5个，则开始解码
			{
				frame_second= m_pVFragmentBuffer.GetFromBuf();      //读取当前缓存中的节点
				seqnum=frame_second.m_pData.sequenceNumbers();      //RTP包序列号
				data=frame_second.m_pData.getConcatenatedData();    //RTP包中数据
				
//				//自动接收SPS和PPS：用于手机端上传视频数据的接收监控
//				if(Info.sps_pps_got == false){
//					
//					//判断接收采集端发送过来的sps
//					if((data[0] & 0x1f) == 7){
//						Info.sps = data;
//						System.out.println("sps--->" + byte2HexStr(Info.sps));
//						Info.sps_got = true;
//					}
//					//判断接收采集端发送过来的pps
//					if((data[0] & 0x1f) == 8){
//						Info.pps = data;
//						System.out.println("pps--->" + byte2HexStr(Info.pps));
//						Info.pps_got = true;   //标志位，提示已经获得sps和pps
//					}
//					 //若SPS和PPS都收到，则将Info.sps_pps_got标志位置为true
//					if(Info.sps_got && Info.pps_got){
//						Info.sps_pps_got = true;
//					}
//				}
				
				
				len=frame_second.m_pData.getTotalLength();          //RTP负载的长度		
				index_RTP=Integer.toString(seqnum[0]);              //rtp包序号转换成十进制打印出来
				info_RTP="Got RTP  "+index_RTP;
				Log.d(TAG,info_RTP);                                //打印得到的RTP包序列号
		
				index_now=seqnum[0];   //当前RTP包序列号
				index++;               //记录收到的RTP包数
				Info.iRTP_num++;       //记录收到的RTP包数
				byte[] ddd ={data[0],data[1],data[2]};
				byte[] dd={data[0],data[1]};
				Log.d("zztest","data[0],data[1],data[2]="+byte2HexStr(ddd));  
				Log.e("data", "data[0],data[1]="+byte2HexStr(dd));
				if(Info.media_type==4){
					GetNal5(data,seqnum[0],len);   //进行Nal组包：手机设备软件编码	
				}
				else if (Info.media_type==5){
					GetNalD1(data,seqnum[0],len);
				}
				else{
					if(Info.dev_type_value.equals("2")){
						GetNaldm365_2(data,seqnum[0],len); 
					}else if(Info.dev_type_value.equals("1")){
						GetNaldm365_2(data,seqnum[0],len);
					}
					else{
					GetNal4(data,seqnum[0],len);   //进行Nal组包：实验室设备或者硬件编码		
					}
					}	
			}
		}
	}
	
	/*************************************************************
	 * 功能：无
	 * 参数：无
	 * 返回值：无
	 * 详细说明：
	 * 1)	本函数是RTPAppIntf接口的抽象方法，本类中没有实现
	 **************************************************************/
	public void userEvent(int type, Participant[] participant) {
		//Do nothing
	}
	
	/*************************************************************
	 * 功能：无
	 * 参数：PayloadType：RTP包的负载类型
	 * 返回值：1
	 * 详细说明：
	 * 1）	本函数是RTPAppIntf接口的抽象方法具体实现，return 1表示每次每一个DataFrame帧是指包含一个RTP包。
	 **************************************************************/
	public int frameSize(int payloadType) {
		return 1;  //表示每次每一个DataFrame帧是指包含一个RTP包。
	}

	/*************************************************************
	 * 功能：实现发送RTP心跳保活包到服务器
	 * 参数：无
	 * 返回值：无
	 * 详细说明：
	 * 1）	心跳包的负载类型设为0x7a
	 * 2）	取Info.media_info_megic的后四组设为RTP的同步源码（Ssrc）；
	 * 3）	RTP包的内容是在Info.media_info_megic之前再加上0x00 0x01 0x00 0x10；
	 **************************************************************/
	public void SendActivePacket(){   //发送RTP心跳保活包到服务器
		byte msg[]=new byte[20];
		long Ssrc=0;
		msg[0]=0x00;
		msg[1]=0x01;
		msg[2]=0x00;
		msg[3]=0x10;
		try{
			System.arraycopy(Info.media_info_magic, 0, msg, 4, 16);  //生成RTP心跳保活包，即在Info.media_info_megic之前再加上0x00 0x01 0x00 0x10
		}catch(Exception e){
			Log.d(TAG,"System.arraycopy failed!");
		}
		this.rtpSession.payloadType(0x7a);	//设置心跳包的负载类型为0x7a	
		
		//取Info.media_info_megic的后四组设为RTP的同步源码（Ssrc）
		Ssrc=(long)((Info.media_info_magic[15]&0x000000ff)|((Info.media_info_magic[14]<<8)&0x0000ff00)|((Info.media_info_magic[13]<<16)&0x00ff0000)|((Info.media_info_magic[12]<<24)&0xff000000));
		this.rtpSession.setSsrc(Ssrc);	
		
		//连续发送两次RTP心跳包
		for(int i=0;i<2;i++){		
		this.rtpSession.sendData(msg);
		}
	}

	
	private void addFirstRtpPacketToTemp(byte[] data,int seqnum,int len){
//		temp_nal=new byte[50000];			//新建缓存	
		temp_nal[0]=pH264StreamHead[0];	
		temp_nal[1]=pH264StreamHead[1];
		temp_nal[2]=pH264StreamHead[2];
		temp_nal[3]=pH264StreamHead[3];  //添加NALU的头：00 00 00 01
		
//		temp_nal[4]=(byte)(data[0]&0xe0 + data[1]&0x1f);
		if(Info.dev_type_value.equals("1")){
			temp_nal[4]=(byte)(data[0]&0xe0 | data[1]&0x1f);
			try{    	
				System.arraycopy(data, 2, temp_nal, 5, len-2);    //将RTP有效负载添加到NALU头后面
			}
			catch(Exception e) {
				Log.d(TAG,"System.arraycopy failed!");
			}
			Info.NalBuffer[PutNum].setNalLen(len+3); //设置当前指向NAL缓存的长度
			
			
		}else{
			try{    	
				System.arraycopy(data, 2, temp_nal, 4, len-2);    //将RTP有效负载添加到NALU头后面
			}
			catch(Exception e) {
				Log.d(TAG,"System.arraycopy failed!");
			}
			Info.NalBuffer[PutNum].setNalLen(len+2); //设置当前指向NAL缓存的长度
		}  
		PreSeq=seqnum;                              //当前序列号的值表示前一个收到的RTP包序列号
		state=1;                                    // 表示首包已经收到
	}
	
	private void addLastRtpPacketToTemp(byte[] data,int seqnum,int len){
		try{    
			System.arraycopy(data, 2,temp_nal , Info.NalBuffer[PutNum].getNalLen(), len-2);
		}
		catch(Exception e){
			Log.d(TAG,"System.arraycopy failed!");
		} 		
		Info.NalBuffer[PutNum].addNalLen(len-2);   //设置当前指向NAL缓存的长度
		PreSeq=seqnum;
	}
	
	private void addMiddleRtpPacketToTemp(byte[] data,int seqnum,int len){
		try{    
			System.arraycopy(data, 2,temp_nal , Info.NalBuffer[PutNum].getNalLen(), len-2);    //将RTP包的有效负载放到缓存中
		}
		catch(Exception e){
			Log.d(TAG,"System.arraycopy failed!");
		}					 
		Info.NalBuffer[PutNum].addNalLen(len-2);     //将当前指向Nal缓存的位置加“len-2”
		PreSeq=seqnum;  //接收好，当前序列号变为前一个序列号
	}
	
	private void addCompleteRtpPacketToTemp(byte[] data,int seqnum,int len){
//		temp_nal=new byte[50000];			
		temp_nal[0]=pH264StreamHead[0];	
		temp_nal[1]=pH264StreamHead[1];
		temp_nal[2]=pH264StreamHead[2];
		temp_nal[3]=pH264StreamHead[3];
		try{    
			System.arraycopy(data, 0, temp_nal, 4, len);
		}
		catch(Exception e){
			Log.d(TAG,"System.arraycopy failed!");
		}					
		Info.NalBuffer[PutNum].setNalLen(len+4);
		PreSeq=seqnum;
	}
	
	private void copyFromTempToNal(){
		Info.NalBuffer[PutNum].IsWriteAble();  //看是否可写，不可则等待
		try{				 				
			System.arraycopy(temp_nal,0,Info.NalBuffer[PutNum].getWriteable_Nalbuf(), 0,Info.NalBuffer[PutNum].getNalLen());					 			
		}catch(Exception e){		
			Log.d(TAG,"System.arraycopy failed!");					 			
		}
		info_NALU=Integer.toString(PutNum);
		info_NALU=info_NALU+"Write Nalu done!";
		Log.d(TAG1,info_NALU);      //打印出第几个NAL单元被写入
		Info.NalBuffer[PutNum].WriteLock();	   //设置缓存只可读，不可写
		PutNum++;
 		if(PutNum==200){
 			PutNum=0;
 		}
 		state=0;   //分包接收完毕，状态清零，重新开始接受首包
	}
	
	private void mainInit(int seqnum){
//		temp_nal=new byte[50000];               //清空temp_nal数组
	 	Info.NalBuffer[PutNum].setNalLen(0);    //Nal长度清零
	 	PreSeq=seqnum;                          //记录下当前的序列号
	 	flag=1;                                 //没收到主帧
		state=0;	                            //没收到首包
	}
	
	private void assistInit(int seqnum){
//		temp_nal=new byte[50000];               //清空temp_nal数组
	 	Info.NalBuffer[PutNum].setNalLen(0);    //Nal长度清零
	 	PreSeq=seqnum;                          //记录下当前的序列号
		state=0;	                            //没收到首包
	}
	
	/** 
	 * bytes转换成十六进制字符串 
	 */  
	public static String byte2HexStr(byte[] b) {  
	    String hs = "";  
	    String stmp = "";  
	    for (int n = 0; n < b.length; n++) {  
	        stmp = (Integer.toHexString(b[n] & 0XFF));  
	        if (stmp.length() == 1)  
	            hs = hs + "0" + stmp;  
	        else  
	            hs = hs + stmp;  
	        // if (n<b.length-1) hs=hs+":";  
	    }  
	    return hs.toUpperCase();  
	}  
	
	public enum FrameType{//枚举：表示帧的类型
		MainFrameComp,//主帧单包
		MainFrameSliceFirst,//主帧分片首包
		MainFrameSliceMiddle,//主帧分片中包
		MainFrameSliceLast,//主帧分片末包
		AssistFrameComp,//辅帧单包
		AssistFrameSliceFirst,//辅帧分片首包
		AssistFrameSliceMiddle,//辅帧分片中包
		AssistFrameSliceLast,//辅帧分片末包
		
		Comp,
		SliceFirst,
		SliceMiddle,
		SliceLast
		
		
	}
	
	public FrameType Frame_parse(byte[] data){
		if( (byte)(data[0]&0x1f) == 28){//先判断是否是分片
			if( (byte)(data[0]&0xe0 | data[1]&0x1f) == 0x65){//如果是主帧分片
				if((data[1]&0xe0) == 0x80){
					return FrameType.MainFrameSliceFirst;//主帧分片首包
				}
				else if((data[1]&0xe0) == 0x00){
					return FrameType.MainFrameSliceMiddle;//主帧分片中包
				}
				else{
					return FrameType.MainFrameSliceLast;//主帧分片末包
				}
			}
			else{
				if(((byte)data[1]&(byte)0xe0) == (byte)0x80){
					return FrameType.AssistFrameSliceFirst;//辅帧分片首包
				}
				else if(((byte)data[1]&(byte)0xe0) == (byte)0x00){
					return FrameType.AssistFrameSliceMiddle;//辅帧分片中包
				}
				else{
					return FrameType.AssistFrameSliceLast;//辅帧分片末包
				}				
			}
		}
		else{//不是分片
			if ( data[0] == 0x65){
				return FrameType.MainFrameComp;//主帧单包
			}
			else{
				return FrameType.AssistFrameComp;//辅帧单包
			}
		}
	}
	public FrameType Frame_parseD1(byte[] data){
		if( (byte)(data[0]&0x1f) == 28){//先判断是否是分片
			
				if((data[1]&0xe0) == 0x80){
					return FrameType.MainFrameSliceFirst;//分片首包
				}
				else if((data[1]&0xe0) == 0x00){
					return FrameType.MainFrameSliceMiddle;//分片中包
				}
				else{
					return FrameType.MainFrameSliceLast;//分片末包
				}
			}
		else{//不是分片	
				return FrameType.MainFrameComp;//单包
		}
	}
	public void GetNalD1(byte[] data,int seqnum,int len){
		FrameType frameType = null;
		frameType = Frame_parseD1(data);
		switch( frameType ){
		case MainFrameComp://主帧单包
			flag = 0;
			addCompleteRtpPacketToTemp(data,seqnum,len);
			copyFromTempToNal();
			break;
		case MainFrameSliceFirst://主帧分片首包
			Log.d("function","主帧首包"+seqnum);
			flag = 0;
			addFirstRtpPacketToTemp(data,seqnum,len);
			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case MainFrameSliceMiddle://主帧分片中包
			Log.d("function","主帧中包"+seqnum);
			if( flag == 1 ){
				Log.d("function3","主帧中包忽略");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话
				addMiddleRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{//之前没有收到首包或序列号不连续
				mainInit(seqnum);
//				assistInit(seqnum);
				Log.d("function","assistInit");
			}
			break;
		case MainFrameSliceLast://主帧分片末包
			Log.d("function","主帧末包"+seqnum);
			if( flag == 1 ){
				Log.d("function3","主帧末包忽略");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话
				addLastRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addLastRtpPacketToTemp");
				copyFromTempToNal();
				Log.d("function","copyFromTempToNal");
				Info.main_suc++;
				Log.d("function2","主帧成功"+Info.main_suc);
			}
			else{//之前没有收到首包或序列号不连续
				mainInit(seqnum);
//				assistInit(seqnum);
			}
			break;
		
		}
	}
	public void GetNal4(byte[] data,int seqnum,int len){
		FrameType frameType = null;
		frameType = Frame_parse(data);
		switch( frameType ){
		case MainFrameComp://主帧单包
			flag = 0;
			addCompleteRtpPacketToTemp(data,seqnum,len);
			Log.e("收到主帧首包","收到主帧单包");
			copyFromTempToNal();
			break;
		case AssistFrameComp://辅帧单包
			if( flag == 1 ){
				Log.d("function3","辅帧单包忽略");
				break;
			}
			addCompleteRtpPacketToTemp(data,seqnum,len);
			copyFromTempToNal();
			break;
		case MainFrameSliceFirst://主帧分片首包
			Log.d("function","主帧首包"+seqnum);
			flag = 0;
			addFirstRtpPacketToTemp(data,seqnum,len);
			
			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case MainFrameSliceMiddle://主帧分片中包
			Log.d("function","主帧中包"+seqnum);
			if( flag == 1 ){
				Log.d("function3","主帧中包忽略");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话
				addMiddleRtpPacketToTemp(data,seqnum,len);				
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{//之前没有收到首包或序列号不连续
				mainInit(seqnum);
				//assistInit(seqnum);
				Log.d("function","assistInit");
			}
			break;
		case MainFrameSliceLast://主帧分片末包
			Log.d("function","主帧末包"+seqnum);
			if( flag == 1 ){
				Log.d("function3","主帧末包忽略");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话
				addLastRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addLastRtpPacketToTemp");
				copyFromTempToNal();
				Log.d("function","copyFromTempToNal");
				Info.main_suc++;
				Log.d("function2","主帧成功"+Info.main_suc);
				Log.e("收到主帧完整包","收到主帧完整包");
			}
			else{//之前没有收到首包或序列号不连续
				mainInit(seqnum);
			}
			break;
		case AssistFrameSliceFirst://辅帧分片首包
			Log.d("function","辅帧首包"+seqnum);
			if( flag == 1 ){
				Log.d("function3","辅帧首包忽略");
				break;
			}			
			addFirstRtpPacketToTemp(data,seqnum,len);
			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case AssistFrameSliceMiddle://辅帧分片中包
			Log.d("function","辅帧中包"+seqnum);
			if( flag == 1 ){
				Log.d("function3","辅帧中包忽略");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话
				addMiddleRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{
				assistInit(seqnum);
				//mainInit(seqnum);
				Log.d("function","assistInit");
			}
			break;
		case AssistFrameSliceLast://辅帧分片末包
			Log.d("function","辅帧末包"+seqnum);
			if( flag == 1 ){
				Log.d("function3","辅帧末包忽略");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话
				addLastRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addLastRtpPacketToTemp");
				copyFromTempToNal();
				Log.d("function","copyFromTempToNal");
				Info.assis_suc++;
				Log.d("function2","辅帧成功"+Info.assis_suc);
			}
			else{
				assistInit(seqnum);
				//mainInit(seqnum);
				Log.d("function","assistInit");
			}
			break;
		}
	}
	
	public FrameType Frame_parsedm365(byte[] data){
		if( (byte)(data[0]&0x1f) == 28||(byte)(data[0]&0x1f) == 29){//先判断是否是分片
			
				if((data[1]&0xe0) == 0x80){
					return FrameType.SliceFirst;//分片首包
				}
				else if((data[1]&0xe0) == 0x00){
					return FrameType.SliceMiddle;//分片中包
				}
				else{
					return FrameType.SliceLast;//分片末包
				}
			
			
		}
		else{//不是分片
			
				return FrameType.Comp;//单包
			
		}
	}
	
	public void GetNaldm365(byte[] data,int seqnum,int len){
		FrameType frameType = null;
		frameType = Frame_parsedm365(data);
		switch( frameType ){
		case Comp://单包
//			flag = 0;
			if((data[0]&0xff) == 0x21 && (data[1]&0xff) == 0x88){
				first=1;
			}
			if(first==1){
			addCompleteRtpPacketToTemp(data,seqnum,len);
			copyFromTempToNal();
			}
			
			
			
			
			break;
		case SliceFirst://分片首包
//			Log.d("function","主帧首包"+seqnum);
//			flag = 0;

			
			
			addFirstRtpPacketToTemp(data,seqnum,len);
			
			state=1;
			
			
//			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case SliceMiddle://主帧分片中包
//			Log.d("function","主帧中包"+seqnum);
			
			
			if( state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话  
				addMiddleRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{//之前没有收到首包或序列号不连续
//				mainInit(seqnum);
				assistInit(seqnum);
//				Log.d("function","assistInit");
			}
			break;
		case SliceLast://主帧分片末包
//			Log.d("function","主帧末包"+seqnum);
			
			
			
			if(state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话 
				addLastRtpPacketToTemp(data,seqnum,len);
//				Log.d("function","addLastRtpPacketToTemp");
				copyFromTempToNal();
				
				state=0;
				
				if(first==0){
				if(((data[0]&0xff) == 0x21 && (data[1]&0xff) == 0x88)){
					first=1;
				}else{
					assistInit(seqnum);
				}
				}
//				Log.d("function","copyFromTempToNal");
//				Info.main_suc++;
//				Log.d("function2","主帧成功"+Info.main_suc);
			}
			else{//之前没有收到首包或序列号不连续
//				mainInit(seqnum);
				assistInit(seqnum);
			}
			break;
		
		}
	}
	
	public void GetNaldm365_2(byte[] data,int seqnum,int len){
		FrameType frameType = null;
		frameType = Frame_parsedm365(data);
		switch( frameType ){
		case Comp://单包
			
			addCompleteRtpPacketToTemp(data,seqnum,len);
			copyFromTempToNal();

			break;
		case SliceFirst://分片首包

			addFirstRtpPacketToTemp(data,seqnum,len);		
			state=1;
		
//			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case SliceMiddle://主帧分片中包

			if( state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话  
				addMiddleRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{//之前没有收到首包或序列号不连续
//				mainInit(seqnum);
				assistInit(seqnum);
//				Log.d("function","assistInit");
			}
			break;
		case SliceLast://主帧分片末包
						
			if(state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话 
				addLastRtpPacketToTemp(data,seqnum,len);
				copyFromTempToNal();				
				state=0;
				
//				Log.d("function","copyFromTempToNal");
//				Info.main_suc++;
//				Log.d("function2","主帧成功"+Info.main_suc);
			}
			else{//之前没有收到首包或序列号不连续
//				mainInit(seqnum);
				assistInit(seqnum);
			}
			break;
		
		}
	}
	
	public FrameType Frame_parse_soft_mobile(byte[] data){
		if( (byte)(data[0]&0x1f) == 28){//先判断是否是分片
			if( (byte)(data[0]&0xe0 | data[1]&0x1f) == 0x25){//如果是主帧分片
				if((data[1]&0xe0) == 0x80){
					return FrameType.MainFrameSliceFirst;//主帧分片首包
				}
				else if((data[1]&0xe0) == 0x00){
					return FrameType.MainFrameSliceMiddle;//主帧分片中包
				}
				else{
					return FrameType.MainFrameSliceLast;//主帧分片末包
				}
			}
			else{
				if(((byte)data[1]&(byte)0xe0) == (byte)0x80){
					return FrameType.AssistFrameSliceFirst;//辅帧分片首包
				}
				else if(((byte)data[1]&(byte)0xe0) == (byte)0x00){
					return FrameType.AssistFrameSliceMiddle;//辅帧分片中包
				}
				else{
					return FrameType.AssistFrameSliceLast;//辅帧分片末包
				}				
			}
		}
		else{//不是分片
			if ( data[0] == 0x25){
				return FrameType.MainFrameComp;//主帧单包
			}
			else{
				return FrameType.AssistFrameComp;//辅帧单包
			}
		}
	}
	
	public void GetNal5(byte[] data,int seqnum,int len){
		FrameType frameType = null;
		frameType = Frame_parse_soft_mobile(data);
		switch( frameType ){
		case MainFrameComp://主帧单包
			flag = 0;
			addCompleteRtpPacketToTemp(data,seqnum,len);
			copyFromTempToNal();
			break;
		case AssistFrameComp://辅帧单包
			if( flag == 1 ){
				Log.d("function3","辅帧单包忽略");
				break;
			}
			addCompleteRtpPacketToTemp(data,seqnum,len);
			copyFromTempToNal();
			break;
		case MainFrameSliceFirst://主帧分片首包
			Log.d("function","主帧首包"+seqnum);
			flag = 0;
			addFirstRtpPacketToTemp(data,seqnum,len);
			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case MainFrameSliceMiddle://主帧分片中包
			Log.d("function","主帧中包"+seqnum);
			if( flag == 1 ){
				Log.d("function3","主帧中包忽略");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话
				addMiddleRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{//之前没有收到首包或序列号不连续
				mainInit(seqnum);
				//assistInit(seqnum);
				Log.d("function","assistInit");
			}
			break;
		case MainFrameSliceLast://主帧分片末包
			Log.d("function","主帧末包"+seqnum);
			if( flag == 1 ){
				Log.d("function3","主帧末包忽略");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话
				addLastRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addLastRtpPacketToTemp");
				copyFromTempToNal();
				Log.d("function","copyFromTempToNal");
				Info.main_suc++;
				Log.d("function2","主帧成功"+Info.main_suc);
			}
			else{//之前没有收到首包或序列号不连续
				mainInit(seqnum);
			}
			break;
		case AssistFrameSliceFirst://辅帧分片首包
			Log.d("function","辅帧首包"+seqnum);
			if( flag == 1 ){
				Log.d("function3","辅帧首包忽略");
				break;
			}			
			addFirstRtpPacketToTemp(data,seqnum,len);
			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case AssistFrameSliceMiddle://辅帧分片中包
			Log.d("function","辅帧中包"+seqnum);
			if( flag == 1 ){
				Log.d("function3","辅帧中包忽略");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话
				addMiddleRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{
				assistInit(seqnum);
				//mainInit(seqnum);
				Log.d("function","assistInit");
			}
			break;
		case AssistFrameSliceLast://辅帧分片末包
			Log.d("function","辅帧末包"+seqnum);
			if( flag == 1 ){
				Log.d("function3","辅帧末包忽略");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//之前已经收到首包且序列号连续的话
				addLastRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addLastRtpPacketToTemp");
				copyFromTempToNal();
				Log.d("function","copyFromTempToNal");
				Info.assis_suc++;
				Log.d("function2","辅帧成功"+Info.assis_suc);
			}
			else{
				assistInit(seqnum);
				//mainInit(seqnum);
				Log.d("function","assistInit");
			}
			break;
		}
	}
}//end of class






