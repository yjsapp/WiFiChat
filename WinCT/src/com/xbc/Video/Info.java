package com.xbc.Video;

public class Info {
	
	
	
	/**��¼��һ��REGISTER�յ���Ӧ*/
	public static boolean surfaceviewout = false;
	public static boolean first_register_step_succeccful = false;
//	public static boolean sipup = false;
	public static int total=0;//���豸����
	public static int live=0;//�����豸����

	public static boolean dlgshow = false;

	public static String seed;
	public static String time;
	public static String password;
	public static String userid;  //XML����ʱ��ֵ
	public static String salt;

	public static boolean login_info_incorrect=false; //�û����������Ƿ����
	public static boolean  reg_success=false;  //�Ƿ�ע��ɹ�
	public static boolean  timeout=false;      //�Ƿ����糬ʱ
	public static boolean  reg_state=false;	   //�Ƿ��Ѿ�ע���¼
	public static boolean  login_replace=false;	//
	public static boolean on_media_state=false;   //�Ƿ��Ѿ�������Ƶ
	public static boolean list_update=false;  //���devname��־
	public static boolean devlist=false;
	
	
	public static String media_info_resolution;
	public static String media_info_video;
	public static String media_info_audio;
	public static String media_info_kbps;
	public static String media_info_peer;
	public static String media_info_mode;	
	public static byte [] media_info_magic=new byte[16];
	public static String media_info_ip;
	public static String media_info_port;
	public static String query_resolution;
	public static boolean query_done=false;
//	public static List<String> devname = new ArrayList<String>();
//	public static List<String> devstatus = new ArrayList<String>();
//	public static List<String> devid = new ArrayList<String>();
//	public static HashMap<String,String> dev_type  = new HashMap<String,String>();//�¼ӵ�dev����
	public static String PPS;//�¼�
	public static String SPS;//�¼�
	public static String dev_type_value="2";//�¼�
	public static int media_type=0;//QCIF or CIF
	public static int iRTP_num=0;	
	 
	public static NalBuffer[] NalBuffer=new NalBuffer[200];
	public static int iLost_num;
	public static int width=176;
	public static int height=144;
	public static int result=-1;/////////////////////////change cif 0r qcif success =0;
	public static String CIFOrQCIF;
	//������IP
	public static String serverid;
	
	//RTPͳ��
	public static int packet_got;
	
	//��֡�ɹ�ͳ��
	public static int main_suc;
	//��֡�ɹ�ͳ��
	public static int assis_suc;
	
	//�Ƿ���������б��־
	public static boolean isupdate = true;
	
	//�ֻ��豸��SPS��PPS
		public static byte[] sps;
		public static byte[] pps;
		/**
		 * SPS���ձ�־λ���յ�����Ϊtrue
		 */
		public static boolean sps_got = false;  
		/**
		 * PPS���ձ�־λ���յ�����Ϊtrue
		 */
		public static boolean pps_got = false;  
		
		/**
		 * SPS��PPS���ձ�־λ��ȫ���յ�����Ϊtrue
		 */
		public static boolean sps_pps_got = false;  
		
		/**
		 * �ж��Ƿ����ڽ���SPS��PPS
		 */
		public static boolean receiving_sps_pps = true;
		public static String dev_name_value;//�¼�

}
