package com.xbc.Video;




import android.util.Log;

import jlibrtp.DataFrame;
import jlibrtp.Participant;
import jlibrtp.RTPAppIntf;
import jlibrtp.RTPSession;
/**********************************************************************************
 * RTP��ʵ�ֹ��ܣ�
 *   ������Ҫʵ����Ƶ���Ľ��պͼ�����ķ��ͣ�����rtp�Ự����rtp���ݰ������û�������ݰ���������
 *����GetNal�������������ΪNal����洢��ָ���������ȴ����룻��ȡ���������Ƶת����������ַ��
 *���ͼ��������Ƶת��������������롣
 ***********************************************************************************/
public class RTP implements RTPAppIntf {
	 RTPSession rtpSession = null;	//rtpSession     �½�RTP�Ự��
	 byte pH264StreamHead[] = {0x00, 0x00, 0x00, 0x01};  //Nal����ǰ4���ֽ�{00,00,00,01}�����ڹ���һ��������Nal֡
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
	 static int flag ;    //���ڱ���Ƿ��յ���֡��0��ʾ�յ���1��ʾû���յ�����ֵ��ֵΪ1
	 int headseqnum;
	 int tailseqnum;
	 int subduction;
	 int AttachNum;
	 int count ;
	 
	 int throwFrameNumber=0;   //�����ĸ�֡��
	 
	 boolean bDisorder=false;
	 byte temp_nal[]=new byte[50000];    //���õ���ʱ�����С
	 StreamBuf m_pVFragmentBuffer;       //RTP������
	  
	 private int PreSeq;  //ǰһ���յ���RTP�������к�
	 private int state;	  //��¼Nal�����״̬�жϣ�0��ʾû�յ��װ���1��ʾ�յ��װ�
	 private int PutNum;  //��ǰָ��д��Nal�����λ��
	 private int first=0;
	 
	 /*************************************************************
		 * ���ܣ�RTP�Ĺ��캯�� 
		 * ��������
		 * ����ֵ����
		 * ��ϸ˵����
		 * 1��RTP�Ĺ��캯������ʼ��RTP�����ࡣ��ʼ���洢RTP���Ļ���Ϊ100��
		 * ����RTP������5��ʱ������GetNal�������������Nal���棬Ϊ�����NalBuf�ำ��ֵ��
		 * PreSeq���ϴν��ܵ�RTP�����кţ� ��ֵΪ-1��
		 * state��ʾ��Nal���Ƿ��յ�RTP�װ���0��ʾû�У�1��ʾ�У���ֵ��0��
		 * Info.PutNum��ʾNal�����ָ��λ�ã���ֵΪ0�����Ϊ199��
		 * **************************************************************/	
	public RTP() {	  
		PreSeq=-1;      //ǰһ��RTP���а�Ϊ-1����ʾû���յ�RTP��
		state=0;        //û���յ��װ�
		flag=1;         //û���յ���֡
		PutNum=0;       //ָ�򻺴�λ��Ϊ0
		headseqnum=0;
		tailseqnum=0;
		subduction=0;    //���ó�ʼ״̬
		
		Info.packet_got = 0;
		Info.main_suc = 0;
		Info.assis_suc = 0;
		
		//ÿ����Ԫ�൱��һ���ṹ�壬NALbufferÿ����Ԫ��С����Ϊ50000���������ȵ�
		for(int i=0;i<Info.NalBuffer.length;i++){
			Info.NalBuffer[i]=new NalBuffer();
		}
		
		//��ʼ���洢RTP���Ļ���Ϊ100������RTP������5��ʱ������GetNal���������
		m_pVFragmentBuffer=new StreamBuf(100,5);
		
		Log.d(TAG,"RTP has been opened!");
		
	}
	
	/*************************************************************
	 * ���ܣ�
	 * ��RTPSessionRegister�����е�AppCallerThread�߳��е��õġ�
	 * ��ʵ���˶���RTP�����ܣ�ȥ����ͷ����ȡ���ø��ء�
	 * ��RTPAppIntf�ӿڵĳ��󷽷�ʵ�ֵľ���ʵ�֡�
	 * ������
	 * frame �����ܵ���RTP������װ��Ϊһ��DataFram���֡
	 * Participant��һ��RTP�Ự�Ĳ�����
	 * ����ֵ����
	 * ��ϸ˵����
	 * 1)	����RTP����ȥ��RTP��ͷ����ȡ���ء�
	 * 2)	��RTP������pRtpFrameNode�ڵ㣬�ٰѽڵ����RTP����m_pVFragmentBuffer�С�
	 * 3)	��RTP�����жԽ��ܵ���RTP���������������ڽ��ܵ�����5����ʱ������GetNal��������Nal�����
	 * ע�⣺
	 *     �˴�pRtpFrameNode����pNode��m_pVFragmentBuffer����Nal
	 * **************************************************************/
	public void receiveData(DataFrame frame, Participant p) {//Rtp�ص�����
		int len=0;
		int seqnum[]=null;
		byte data[]=null;
		StreamBufNode frame_second = null;
		if(frame.payloadType()==98 |frame.payloadType()==96)//�жϽ��հ��ǲ�����Ƶ��
		{
			Info.packet_got ++;
			StreamBufNode pRtpFrameNode = new StreamBufNode(frame); //��RTP������pRtpFrameNode�ڵ�
			Log.d(TAG6,"new StreamBufNode"); 
			m_pVFragmentBuffer.AddToBufBySeq(pRtpFrameNode);        //���յ���RTP�������ٷ���Rtp����		
			
			if(m_pVFragmentBuffer.IsReady())                        //������RTP������5������ʼ����
			{
				frame_second= m_pVFragmentBuffer.GetFromBuf();      //��ȡ��ǰ�����еĽڵ�
				seqnum=frame_second.m_pData.sequenceNumbers();      //RTP�����к�
				data=frame_second.m_pData.getConcatenatedData();    //RTP��������
				
//				//�Զ�����SPS��PPS�������ֻ����ϴ���Ƶ���ݵĽ��ռ��
//				if(Info.sps_pps_got == false){
//					
//					//�жϽ��ղɼ��˷��͹�����sps
//					if((data[0] & 0x1f) == 7){
//						Info.sps = data;
//						System.out.println("sps--->" + byte2HexStr(Info.sps));
//						Info.sps_got = true;
//					}
//					//�жϽ��ղɼ��˷��͹�����pps
//					if((data[0] & 0x1f) == 8){
//						Info.pps = data;
//						System.out.println("pps--->" + byte2HexStr(Info.pps));
//						Info.pps_got = true;   //��־λ����ʾ�Ѿ����sps��pps
//					}
//					 //��SPS��PPS���յ�����Info.sps_pps_got��־λ��Ϊtrue
//					if(Info.sps_got && Info.pps_got){
//						Info.sps_pps_got = true;
//					}
//				}
				
				
				len=frame_second.m_pData.getTotalLength();          //RTP���صĳ���		
				index_RTP=Integer.toString(seqnum[0]);              //rtp�����ת����ʮ���ƴ�ӡ����
				info_RTP="Got RTP  "+index_RTP;
				Log.d(TAG,info_RTP);                                //��ӡ�õ���RTP�����к�
		
				index_now=seqnum[0];   //��ǰRTP�����к�
				index++;               //��¼�յ���RTP����
				Info.iRTP_num++;       //��¼�յ���RTP����
				byte[] ddd ={data[0],data[1],data[2]};
				byte[] dd={data[0],data[1]};
				Log.d("zztest","data[0],data[1],data[2]="+byte2HexStr(ddd));  
				Log.e("data", "data[0],data[1]="+byte2HexStr(dd));
				if(Info.media_type==4){
					GetNal5(data,seqnum[0],len);   //����Nal������ֻ��豸�������	
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
					GetNal4(data,seqnum[0],len);   //����Nal�����ʵ�����豸����Ӳ������		
					}
					}	
			}
		}
	}
	
	/*************************************************************
	 * ���ܣ���
	 * ��������
	 * ����ֵ����
	 * ��ϸ˵����
	 * 1)	��������RTPAppIntf�ӿڵĳ��󷽷���������û��ʵ��
	 **************************************************************/
	public void userEvent(int type, Participant[] participant) {
		//Do nothing
	}
	
	/*************************************************************
	 * ���ܣ���
	 * ������PayloadType��RTP���ĸ�������
	 * ����ֵ��1
	 * ��ϸ˵����
	 * 1��	��������RTPAppIntf�ӿڵĳ��󷽷�����ʵ�֣�return 1��ʾÿ��ÿһ��DataFrame֡��ָ����һ��RTP����
	 **************************************************************/
	public int frameSize(int payloadType) {
		return 1;  //��ʾÿ��ÿһ��DataFrame֡��ָ����һ��RTP����
	}

	/*************************************************************
	 * ���ܣ�ʵ�ַ���RTP�����������������
	 * ��������
	 * ����ֵ����
	 * ��ϸ˵����
	 * 1��	�������ĸ���������Ϊ0x7a
	 * 2��	ȡInfo.media_info_megic�ĺ�������ΪRTP��ͬ��Դ�루Ssrc����
	 * 3��	RTP������������Info.media_info_megic֮ǰ�ټ���0x00 0x01 0x00 0x10��
	 **************************************************************/
	public void SendActivePacket(){   //����RTP�����������������
		byte msg[]=new byte[20];
		long Ssrc=0;
		msg[0]=0x00;
		msg[1]=0x01;
		msg[2]=0x00;
		msg[3]=0x10;
		try{
			System.arraycopy(Info.media_info_magic, 0, msg, 4, 16);  //����RTP���������������Info.media_info_megic֮ǰ�ټ���0x00 0x01 0x00 0x10
		}catch(Exception e){
			Log.d(TAG,"System.arraycopy failed!");
		}
		this.rtpSession.payloadType(0x7a);	//�����������ĸ�������Ϊ0x7a	
		
		//ȡInfo.media_info_megic�ĺ�������ΪRTP��ͬ��Դ�루Ssrc��
		Ssrc=(long)((Info.media_info_magic[15]&0x000000ff)|((Info.media_info_magic[14]<<8)&0x0000ff00)|((Info.media_info_magic[13]<<16)&0x00ff0000)|((Info.media_info_magic[12]<<24)&0xff000000));
		this.rtpSession.setSsrc(Ssrc);	
		
		//������������RTP������
		for(int i=0;i<2;i++){		
		this.rtpSession.sendData(msg);
		}
	}

	
	private void addFirstRtpPacketToTemp(byte[] data,int seqnum,int len){
//		temp_nal=new byte[50000];			//�½�����	
		temp_nal[0]=pH264StreamHead[0];	
		temp_nal[1]=pH264StreamHead[1];
		temp_nal[2]=pH264StreamHead[2];
		temp_nal[3]=pH264StreamHead[3];  //���NALU��ͷ��00 00 00 01
		
//		temp_nal[4]=(byte)(data[0]&0xe0 + data[1]&0x1f);
		if(Info.dev_type_value.equals("1")){
			temp_nal[4]=(byte)(data[0]&0xe0 | data[1]&0x1f);
			try{    	
				System.arraycopy(data, 2, temp_nal, 5, len-2);    //��RTP��Ч������ӵ�NALUͷ����
			}
			catch(Exception e) {
				Log.d(TAG,"System.arraycopy failed!");
			}
			Info.NalBuffer[PutNum].setNalLen(len+3); //���õ�ǰָ��NAL����ĳ���
			
			
		}else{
			try{    	
				System.arraycopy(data, 2, temp_nal, 4, len-2);    //��RTP��Ч������ӵ�NALUͷ����
			}
			catch(Exception e) {
				Log.d(TAG,"System.arraycopy failed!");
			}
			Info.NalBuffer[PutNum].setNalLen(len+2); //���õ�ǰָ��NAL����ĳ���
		}  
		PreSeq=seqnum;                              //��ǰ���кŵ�ֵ��ʾǰһ���յ���RTP�����к�
		state=1;                                    // ��ʾ�װ��Ѿ��յ�
	}
	
	private void addLastRtpPacketToTemp(byte[] data,int seqnum,int len){
		try{    
			System.arraycopy(data, 2,temp_nal , Info.NalBuffer[PutNum].getNalLen(), len-2);
		}
		catch(Exception e){
			Log.d(TAG,"System.arraycopy failed!");
		} 		
		Info.NalBuffer[PutNum].addNalLen(len-2);   //���õ�ǰָ��NAL����ĳ���
		PreSeq=seqnum;
	}
	
	private void addMiddleRtpPacketToTemp(byte[] data,int seqnum,int len){
		try{    
			System.arraycopy(data, 2,temp_nal , Info.NalBuffer[PutNum].getNalLen(), len-2);    //��RTP������Ч���طŵ�������
		}
		catch(Exception e){
			Log.d(TAG,"System.arraycopy failed!");
		}					 
		Info.NalBuffer[PutNum].addNalLen(len-2);     //����ǰָ��Nal�����λ�üӡ�len-2��
		PreSeq=seqnum;  //���պã���ǰ���кű�Ϊǰһ�����к�
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
		Info.NalBuffer[PutNum].IsWriteAble();  //���Ƿ��д��������ȴ�
		try{				 				
			System.arraycopy(temp_nal,0,Info.NalBuffer[PutNum].getWriteable_Nalbuf(), 0,Info.NalBuffer[PutNum].getNalLen());					 			
		}catch(Exception e){		
			Log.d(TAG,"System.arraycopy failed!");					 			
		}
		info_NALU=Integer.toString(PutNum);
		info_NALU=info_NALU+"Write Nalu done!";
		Log.d(TAG1,info_NALU);      //��ӡ���ڼ���NAL��Ԫ��д��
		Info.NalBuffer[PutNum].WriteLock();	   //���û���ֻ�ɶ�������д
		PutNum++;
 		if(PutNum==200){
 			PutNum=0;
 		}
 		state=0;   //�ְ�������ϣ�״̬���㣬���¿�ʼ�����װ�
	}
	
	private void mainInit(int seqnum){
//		temp_nal=new byte[50000];               //���temp_nal����
	 	Info.NalBuffer[PutNum].setNalLen(0);    //Nal��������
	 	PreSeq=seqnum;                          //��¼�µ�ǰ�����к�
	 	flag=1;                                 //û�յ���֡
		state=0;	                            //û�յ��װ�
	}
	
	private void assistInit(int seqnum){
//		temp_nal=new byte[50000];               //���temp_nal����
	 	Info.NalBuffer[PutNum].setNalLen(0);    //Nal��������
	 	PreSeq=seqnum;                          //��¼�µ�ǰ�����к�
		state=0;	                            //û�յ��װ�
	}
	
	/** 
	 * bytesת����ʮ�������ַ��� 
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
	
	public enum FrameType{//ö�٣���ʾ֡������
		MainFrameComp,//��֡����
		MainFrameSliceFirst,//��֡��Ƭ�װ�
		MainFrameSliceMiddle,//��֡��Ƭ�а�
		MainFrameSliceLast,//��֡��Ƭĩ��
		AssistFrameComp,//��֡����
		AssistFrameSliceFirst,//��֡��Ƭ�װ�
		AssistFrameSliceMiddle,//��֡��Ƭ�а�
		AssistFrameSliceLast,//��֡��Ƭĩ��
		
		Comp,
		SliceFirst,
		SliceMiddle,
		SliceLast
		
		
	}
	
	public FrameType Frame_parse(byte[] data){
		if( (byte)(data[0]&0x1f) == 28){//���ж��Ƿ��Ƿ�Ƭ
			if( (byte)(data[0]&0xe0 | data[1]&0x1f) == 0x65){//�������֡��Ƭ
				if((data[1]&0xe0) == 0x80){
					return FrameType.MainFrameSliceFirst;//��֡��Ƭ�װ�
				}
				else if((data[1]&0xe0) == 0x00){
					return FrameType.MainFrameSliceMiddle;//��֡��Ƭ�а�
				}
				else{
					return FrameType.MainFrameSliceLast;//��֡��Ƭĩ��
				}
			}
			else{
				if(((byte)data[1]&(byte)0xe0) == (byte)0x80){
					return FrameType.AssistFrameSliceFirst;//��֡��Ƭ�װ�
				}
				else if(((byte)data[1]&(byte)0xe0) == (byte)0x00){
					return FrameType.AssistFrameSliceMiddle;//��֡��Ƭ�а�
				}
				else{
					return FrameType.AssistFrameSliceLast;//��֡��Ƭĩ��
				}				
			}
		}
		else{//���Ƿ�Ƭ
			if ( data[0] == 0x65){
				return FrameType.MainFrameComp;//��֡����
			}
			else{
				return FrameType.AssistFrameComp;//��֡����
			}
		}
	}
	public FrameType Frame_parseD1(byte[] data){
		if( (byte)(data[0]&0x1f) == 28){//���ж��Ƿ��Ƿ�Ƭ
			
				if((data[1]&0xe0) == 0x80){
					return FrameType.MainFrameSliceFirst;//��Ƭ�װ�
				}
				else if((data[1]&0xe0) == 0x00){
					return FrameType.MainFrameSliceMiddle;//��Ƭ�а�
				}
				else{
					return FrameType.MainFrameSliceLast;//��Ƭĩ��
				}
			}
		else{//���Ƿ�Ƭ	
				return FrameType.MainFrameComp;//����
		}
	}
	public void GetNalD1(byte[] data,int seqnum,int len){
		FrameType frameType = null;
		frameType = Frame_parseD1(data);
		switch( frameType ){
		case MainFrameComp://��֡����
			flag = 0;
			addCompleteRtpPacketToTemp(data,seqnum,len);
			copyFromTempToNal();
			break;
		case MainFrameSliceFirst://��֡��Ƭ�װ�
			Log.d("function","��֡�װ�"+seqnum);
			flag = 0;
			addFirstRtpPacketToTemp(data,seqnum,len);
			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case MainFrameSliceMiddle://��֡��Ƭ�а�
			Log.d("function","��֡�а�"+seqnum);
			if( flag == 1 ){
				Log.d("function3","��֡�а�����");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ�
				addMiddleRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{//֮ǰû���յ��װ������кŲ�����
				mainInit(seqnum);
//				assistInit(seqnum);
				Log.d("function","assistInit");
			}
			break;
		case MainFrameSliceLast://��֡��Ƭĩ��
			Log.d("function","��֡ĩ��"+seqnum);
			if( flag == 1 ){
				Log.d("function3","��֡ĩ������");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ�
				addLastRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addLastRtpPacketToTemp");
				copyFromTempToNal();
				Log.d("function","copyFromTempToNal");
				Info.main_suc++;
				Log.d("function2","��֡�ɹ�"+Info.main_suc);
			}
			else{//֮ǰû���յ��װ������кŲ�����
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
		case MainFrameComp://��֡����
			flag = 0;
			addCompleteRtpPacketToTemp(data,seqnum,len);
			Log.e("�յ���֡�װ�","�յ���֡����");
			copyFromTempToNal();
			break;
		case AssistFrameComp://��֡����
			if( flag == 1 ){
				Log.d("function3","��֡��������");
				break;
			}
			addCompleteRtpPacketToTemp(data,seqnum,len);
			copyFromTempToNal();
			break;
		case MainFrameSliceFirst://��֡��Ƭ�װ�
			Log.d("function","��֡�װ�"+seqnum);
			flag = 0;
			addFirstRtpPacketToTemp(data,seqnum,len);
			
			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case MainFrameSliceMiddle://��֡��Ƭ�а�
			Log.d("function","��֡�а�"+seqnum);
			if( flag == 1 ){
				Log.d("function3","��֡�а�����");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ�
				addMiddleRtpPacketToTemp(data,seqnum,len);				
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{//֮ǰû���յ��װ������кŲ�����
				mainInit(seqnum);
				//assistInit(seqnum);
				Log.d("function","assistInit");
			}
			break;
		case MainFrameSliceLast://��֡��Ƭĩ��
			Log.d("function","��֡ĩ��"+seqnum);
			if( flag == 1 ){
				Log.d("function3","��֡ĩ������");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ�
				addLastRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addLastRtpPacketToTemp");
				copyFromTempToNal();
				Log.d("function","copyFromTempToNal");
				Info.main_suc++;
				Log.d("function2","��֡�ɹ�"+Info.main_suc);
				Log.e("�յ���֡������","�յ���֡������");
			}
			else{//֮ǰû���յ��װ������кŲ�����
				mainInit(seqnum);
			}
			break;
		case AssistFrameSliceFirst://��֡��Ƭ�װ�
			Log.d("function","��֡�װ�"+seqnum);
			if( flag == 1 ){
				Log.d("function3","��֡�װ�����");
				break;
			}			
			addFirstRtpPacketToTemp(data,seqnum,len);
			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case AssistFrameSliceMiddle://��֡��Ƭ�а�
			Log.d("function","��֡�а�"+seqnum);
			if( flag == 1 ){
				Log.d("function3","��֡�а�����");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ�
				addMiddleRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{
				assistInit(seqnum);
				//mainInit(seqnum);
				Log.d("function","assistInit");
			}
			break;
		case AssistFrameSliceLast://��֡��Ƭĩ��
			Log.d("function","��֡ĩ��"+seqnum);
			if( flag == 1 ){
				Log.d("function3","��֡ĩ������");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ�
				addLastRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addLastRtpPacketToTemp");
				copyFromTempToNal();
				Log.d("function","copyFromTempToNal");
				Info.assis_suc++;
				Log.d("function2","��֡�ɹ�"+Info.assis_suc);
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
		if( (byte)(data[0]&0x1f) == 28||(byte)(data[0]&0x1f) == 29){//���ж��Ƿ��Ƿ�Ƭ
			
				if((data[1]&0xe0) == 0x80){
					return FrameType.SliceFirst;//��Ƭ�װ�
				}
				else if((data[1]&0xe0) == 0x00){
					return FrameType.SliceMiddle;//��Ƭ�а�
				}
				else{
					return FrameType.SliceLast;//��Ƭĩ��
				}
			
			
		}
		else{//���Ƿ�Ƭ
			
				return FrameType.Comp;//����
			
		}
	}
	
	public void GetNaldm365(byte[] data,int seqnum,int len){
		FrameType frameType = null;
		frameType = Frame_parsedm365(data);
		switch( frameType ){
		case Comp://����
//			flag = 0;
			if((data[0]&0xff) == 0x21 && (data[1]&0xff) == 0x88){
				first=1;
			}
			if(first==1){
			addCompleteRtpPacketToTemp(data,seqnum,len);
			copyFromTempToNal();
			}
			
			
			
			
			break;
		case SliceFirst://��Ƭ�װ�
//			Log.d("function","��֡�װ�"+seqnum);
//			flag = 0;

			
			
			addFirstRtpPacketToTemp(data,seqnum,len);
			
			state=1;
			
			
//			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case SliceMiddle://��֡��Ƭ�а�
//			Log.d("function","��֡�а�"+seqnum);
			
			
			if( state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ�  
				addMiddleRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{//֮ǰû���յ��װ������кŲ�����
//				mainInit(seqnum);
				assistInit(seqnum);
//				Log.d("function","assistInit");
			}
			break;
		case SliceLast://��֡��Ƭĩ��
//			Log.d("function","��֡ĩ��"+seqnum);
			
			
			
			if(state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ� 
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
//				Log.d("function2","��֡�ɹ�"+Info.main_suc);
			}
			else{//֮ǰû���յ��װ������кŲ�����
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
		case Comp://����
			
			addCompleteRtpPacketToTemp(data,seqnum,len);
			copyFromTempToNal();

			break;
		case SliceFirst://��Ƭ�װ�

			addFirstRtpPacketToTemp(data,seqnum,len);		
			state=1;
		
//			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case SliceMiddle://��֡��Ƭ�а�

			if( state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ�  
				addMiddleRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{//֮ǰû���յ��װ������кŲ�����
//				mainInit(seqnum);
				assistInit(seqnum);
//				Log.d("function","assistInit");
			}
			break;
		case SliceLast://��֡��Ƭĩ��
						
			if(state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ� 
				addLastRtpPacketToTemp(data,seqnum,len);
				copyFromTempToNal();				
				state=0;
				
//				Log.d("function","copyFromTempToNal");
//				Info.main_suc++;
//				Log.d("function2","��֡�ɹ�"+Info.main_suc);
			}
			else{//֮ǰû���յ��װ������кŲ�����
//				mainInit(seqnum);
				assistInit(seqnum);
			}
			break;
		
		}
	}
	
	public FrameType Frame_parse_soft_mobile(byte[] data){
		if( (byte)(data[0]&0x1f) == 28){//���ж��Ƿ��Ƿ�Ƭ
			if( (byte)(data[0]&0xe0 | data[1]&0x1f) == 0x25){//�������֡��Ƭ
				if((data[1]&0xe0) == 0x80){
					return FrameType.MainFrameSliceFirst;//��֡��Ƭ�װ�
				}
				else if((data[1]&0xe0) == 0x00){
					return FrameType.MainFrameSliceMiddle;//��֡��Ƭ�а�
				}
				else{
					return FrameType.MainFrameSliceLast;//��֡��Ƭĩ��
				}
			}
			else{
				if(((byte)data[1]&(byte)0xe0) == (byte)0x80){
					return FrameType.AssistFrameSliceFirst;//��֡��Ƭ�װ�
				}
				else if(((byte)data[1]&(byte)0xe0) == (byte)0x00){
					return FrameType.AssistFrameSliceMiddle;//��֡��Ƭ�а�
				}
				else{
					return FrameType.AssistFrameSliceLast;//��֡��Ƭĩ��
				}				
			}
		}
		else{//���Ƿ�Ƭ
			if ( data[0] == 0x25){
				return FrameType.MainFrameComp;//��֡����
			}
			else{
				return FrameType.AssistFrameComp;//��֡����
			}
		}
	}
	
	public void GetNal5(byte[] data,int seqnum,int len){
		FrameType frameType = null;
		frameType = Frame_parse_soft_mobile(data);
		switch( frameType ){
		case MainFrameComp://��֡����
			flag = 0;
			addCompleteRtpPacketToTemp(data,seqnum,len);
			copyFromTempToNal();
			break;
		case AssistFrameComp://��֡����
			if( flag == 1 ){
				Log.d("function3","��֡��������");
				break;
			}
			addCompleteRtpPacketToTemp(data,seqnum,len);
			copyFromTempToNal();
			break;
		case MainFrameSliceFirst://��֡��Ƭ�װ�
			Log.d("function","��֡�װ�"+seqnum);
			flag = 0;
			addFirstRtpPacketToTemp(data,seqnum,len);
			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case MainFrameSliceMiddle://��֡��Ƭ�а�
			Log.d("function","��֡�а�"+seqnum);
			if( flag == 1 ){
				Log.d("function3","��֡�а�����");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ�
				addMiddleRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{//֮ǰû���յ��װ������кŲ�����
				mainInit(seqnum);
				//assistInit(seqnum);
				Log.d("function","assistInit");
			}
			break;
		case MainFrameSliceLast://��֡��Ƭĩ��
			Log.d("function","��֡ĩ��"+seqnum);
			if( flag == 1 ){
				Log.d("function3","��֡ĩ������");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ�
				addLastRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addLastRtpPacketToTemp");
				copyFromTempToNal();
				Log.d("function","copyFromTempToNal");
				Info.main_suc++;
				Log.d("function2","��֡�ɹ�"+Info.main_suc);
			}
			else{//֮ǰû���յ��װ������кŲ�����
				mainInit(seqnum);
			}
			break;
		case AssistFrameSliceFirst://��֡��Ƭ�װ�
			Log.d("function","��֡�װ�"+seqnum);
			if( flag == 1 ){
				Log.d("function3","��֡�װ�����");
				break;
			}			
			addFirstRtpPacketToTemp(data,seqnum,len);
			Log.d("function","addFirstRtpPacketToTemp");
			break;
		case AssistFrameSliceMiddle://��֡��Ƭ�а�
			Log.d("function","��֡�а�"+seqnum);
			if( flag == 1 ){
				Log.d("function3","��֡�а�����");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ�
				addMiddleRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addMiddleRtpPacketToTemp");
			}
			else{
				assistInit(seqnum);
				//mainInit(seqnum);
				Log.d("function","assistInit");
			}
			break;
		case AssistFrameSliceLast://��֡��Ƭĩ��
			Log.d("function","��֡ĩ��"+seqnum);
			if( flag == 1 ){
				Log.d("function3","��֡ĩ������");
				break;
			}
			if( state == 1 && PreSeq+1 == seqnum ){//֮ǰ�Ѿ��յ��װ������к������Ļ�
				addLastRtpPacketToTemp(data,seqnum,len);
				Log.d("function","addLastRtpPacketToTemp");
				copyFromTempToNal();
				Log.d("function","copyFromTempToNal");
				Info.assis_suc++;
				Log.d("function2","��֡�ɹ�"+Info.assis_suc);
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






