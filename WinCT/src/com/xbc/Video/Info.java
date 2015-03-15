package com.xbc.Video;

public class Info {
	
	
	
	/**登录第一次REGISTER收到回应*/
	public static boolean surfaceviewout = false;
	public static boolean first_register_step_succeccful = false;
//	public static boolean sipup = false;
	public static int total=0;//总设备数量
	public static int live=0;//在线设备数量

	public static boolean dlgshow = false;

	public static String seed;
	public static String time;
	public static String password;
	public static String userid;  //XML解析时赋值
	public static String salt;

	public static boolean login_info_incorrect=false; //用户名、密码是否错误
	public static boolean  reg_success=false;  //是否注册成功
	public static boolean  timeout=false;      //是否网络超时
	public static boolean  reg_state=false;	   //是否已经注册登录
	public static boolean  login_replace=false;	//
	public static boolean on_media_state=false;   //是否已经邀请视频
	public static boolean list_update=false;  //清空devname标志
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
//	public static HashMap<String,String> dev_type  = new HashMap<String,String>();//新加的dev属性
	public static String PPS;//新加
	public static String SPS;//新加
	public static String dev_type_value="2";//新加
	public static int media_type=0;//QCIF or CIF
	public static int iRTP_num=0;	
	 
	public static NalBuffer[] NalBuffer=new NalBuffer[200];
	public static int iLost_num;
	public static int width=176;
	public static int height=144;
	public static int result=-1;/////////////////////////change cif 0r qcif success =0;
	public static String CIFOrQCIF;
	//服务器IP
	public static String serverid;
	
	//RTP统计
	public static int packet_got;
	
	//主帧成功统计
	public static int main_suc;
	//辅帧成功统计
	public static int assis_suc;
	
	//是否继续更新列表标志
	public static boolean isupdate = true;
	
	//手机设备的SPS和PPS
		public static byte[] sps;
		public static byte[] pps;
		/**
		 * SPS接收标志位，收到则置为true
		 */
		public static boolean sps_got = false;  
		/**
		 * PPS接收标志位，收到则置为true
		 */
		public static boolean pps_got = false;  
		
		/**
		 * SPS和PPS接收标志位，全部收到则置为true
		 */
		public static boolean sps_pps_got = false;  
		
		/**
		 * 判断是否正在接收SPS和PPS
		 */
		public static boolean receiving_sps_pps = true;
		public static String dev_name_value;//新加

}
