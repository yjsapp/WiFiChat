package com.xbc.Video;

import android.util.Log;
import jlibrtp.DataFrame;

class StreamBufNode
{
	public DataFrame m_pData;
	public int   m_nSequs[];
	public int m_nPts;
	public StreamBufNode m_pNext;
StreamBufNode()
{
	m_pData=null;
	m_nSequs=null;
	m_nPts=0;
	m_pNext=null;
}
StreamBufNode(DataFrame pData)
{
	if(pData!=null)
	{
		m_pData=pData;
		m_nSequs=pData.sequenceNumbers();
		m_nPts=m_nSequs[0];
	}
}

DataFrame GetData()
{
	return m_pData;
}
int GetLen()
{
	return m_pData.getTotalLength();
}
int GetPts()
{
	return m_nPts;
}
StreamBufNode GetNext()
{
	return m_pNext;
}
}

class StreamBuf
{
	private int   m_nLen;//整个缓存的长度
	private int   m_nCurLen;//缓存中已有RTP包的缓存的个数
	private int   m_nReadyLen;//开始组包的最小RTP的个数
	private int   m_nMinLen;//缓存的最小长度
	private boolean  m_bReady;//标志位，为true时开始调用GetNal函数
	private StreamBufNode m_pHead;//首部节点
	private StreamBufNode m_pTail;//尾部节点
	private String TAG6="AddBufBySeq";
	private String TAG7="step";
	
StreamBuf ( int nLen,  int nReadyLen)
{
    m_nLen =nLen;
    m_nCurLen = 0;
    m_nReadyLen =nReadyLen;
    m_pHead = null;
    m_pTail = null;
    m_bReady = false;
}


StreamBufNode GetFromBuf()
{
	if(IsEmpty())
	{
		return null;
	}
	StreamBufNode pNode = m_pHead;
	m_pHead = m_pHead.m_pNext;
	m_nCurLen --;
	if(m_nCurLen < m_nReadyLen)
	{
		m_bReady = false; 
	}
	return pNode;
}


boolean IsEmpty()
{
	return m_nCurLen == 0;
}
boolean IsReady()
{
	return m_bReady == true;
}
int GetCurLen()
{
	return m_nCurLen;
}
/*
bool StreamBuf::IsReady()
{
	bool bFlag;
	bFlag = m_bReady;
	return bFlag;
}
*/
void SetReadyLen(int nDelay)
{
	if(nDelay > 0)
	{
		if(nDelay > m_nReadyLen && m_bReady)
			m_bReady = false;
		m_nReadyLen = nDelay;
		while(m_nCurLen >= m_nReadyLen)
		{
			StreamBufNode pNode = m_pHead;
			if(m_pHead!=null)
				m_pHead = m_pHead.GetNext();
			m_nCurLen --;
		}
	}
}

void ClearBuf()
{
	StreamBufNode pNode = m_pHead;
	if(m_pHead!=null)
		m_pHead = m_pHead.m_pNext;
	while(pNode!=null)
	{
		m_nCurLen--;
		pNode = m_pHead;
		if(m_pHead!=null)
			m_pHead = m_pHead.m_pNext;
		
	}
	if(m_nCurLen!=0)
	{
		m_nCurLen = 0;
	}
		
	m_nCurLen = 0;
	m_pHead = m_pTail = null;
	m_bReady = false;
}

/*
this function is ued to sort the sequence number of RTP packet,
it maitains m_nReadyLen number of packet , if the number is less then
m_nReadyLen ,do the same as the function of AddToBuf(); 


*/
boolean AddToBufBySeq(StreamBufNode pNode)
{	
	if(m_nCurLen  > m_nLen)
	{
		return false;
	}
	
	
	if(m_nCurLen == 0)
	{
		m_pHead = m_pTail = pNode;
		m_nCurLen ++;
		m_bReady = false;
		return true;
	}

	else if(m_nCurLen < m_nReadyLen && m_nCurLen > 0)
	{
		m_pTail.m_pNext = pNode;
		m_pTail = pNode;
		m_bReady = false;
		m_nCurLen ++;
		return true;
	}

	
	else if(m_nCurLen >= m_nReadyLen)
	{
		StreamBufNode pTemp;
		m_bReady = true;

		if(pNode.m_nPts > m_pTail.m_nPts)          //arrived by sequence,insert after the tail ,
		{
			m_pTail.m_pNext = pNode;
			m_pTail = pNode;
			pNode.m_pNext = null;
			m_nCurLen ++;
			return true;
		}
	
		else                                                          //arrived not by sequence                                 
		{

			if(pNode.m_nPts < m_pHead.m_nPts)
			{ 	
				
				pNode.m_pNext = m_pHead ;   //insert before the head
				m_pHead = pNode;
				m_nCurLen ++;

				Log.d(TAG6,"insert before the head---- ---------------------");	
				return true;
			
			}

			else
			{
				pTemp = m_pHead;
				while(pTemp.m_pNext != null)   //insert in the middle
				{
					if(pNode.m_nPts < pTemp.m_pNext.m_nPts)
					{
						pNode.m_pNext = pTemp.m_pNext;
						pTemp.m_pNext = pNode;
						m_nCurLen ++;
						
						Log.d(TAG6,"insert in the middle---- ------------------");	
						return true;
						//break;
					
					}
					else
					{	
						pTemp = pTemp.m_pNext;
					}	
				}

				if(pTemp == m_pTail)
				{
					return false; //this should not happen
				}
			
			
			
			}
		}

	}	
	return true;
		
}
}
