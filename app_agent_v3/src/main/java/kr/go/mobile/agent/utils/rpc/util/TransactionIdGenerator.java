/**
 * Filename : SessionKeyGenerator.java
 * Comment  : Session Key??? ?????밸? ?닷???????깅?. <br>
 */
package kr.go.mobile.agent.utils.rpc.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


/**
 * Transaction ID ???; '??? UTIL
 * 
 * @author Administrator
 * 
 */

public class TransactionIdGenerator {
	private static TransactionIdGenerator generator;

	private long currTimeMillis;
	private long currSeq;

	/**
	 * ??????????????
	 */
	private TransactionIdGenerator() {
		currTimeMillis = System.currentTimeMillis();
		currSeq = 0;
	}

	/**
	 * ??깃????? ??⑥?몄
	 * 
	 * @return SessionKeyGenerator ???泥?
	 */
	public static TransactionIdGenerator getInstance() {
		/*synchronized(generator){
			if (generator == null) {
				generator = new TransactionIdGenerator();
			}
			return generator;
		}*/
		
		if (generator == null) {
			generator = new TransactionIdGenerator();
		}
		
		synchronized(generator){
			return generator;
		}
	}

	/**
	 * ?????占?ession Key??? ?????쇰?.
	 * 
	 * @return ????占?ession Key String ???泥?
	 */
	public String nextKey() {
		synchronized (generator) {
			long tempTimeMillis = System.nanoTime();
			if (currTimeMillis == tempTimeMillis) {
				currSeq++; // ?????? ??怨ㅼ??? ?????? seq??? count
			} else {
				currTimeMillis = tempTimeMillis;
				currSeq = 1;
			}
		}
		return makeTransactionKey(currTimeMillis, currSeq);
	}

	// transaction ID ?????占? ???????ID(2) + System.currentTimeMillis(13) + ??????(3)
	// ex)01+1042517111350+001 = 011042517111350001

	

	
	
	/**
	 * <PRE>
	 * MethodName : makeTransactionKey
	 * ClassName  : TransactionIdGenerator
	 * Comment   : 
	 * 占쎈?苑?옙占?  : master
	 * 占쎈?苑?옙占?  : 2012. 2. 22. 占썬쎌 1:02:29
	 * </PRE>
	 *
	 * @param millis
	 * @param seq
	 * @return String
	 */
	private String makeTransactionKey(long millis, long seq) {
		String sSeq = "00000" + Long.toString(seq);
		sSeq = millis + sSeq;
		if (sSeq.length() > 12) {
			sSeq = sSeq.substring(sSeq.length() - 12, sSeq.length());
		}
		// IConfig config =
		// ConfigurationManager.getInstance().getConfiguration();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA);
		
		return sdf.format(Calendar.getInstance().getTime()) + "-" + sSeq;
	}

	
	public static void main(String[] args) {

//		for (int i = 0; i < 10000; i++){
//			LogUtil.log_d(TransactionIdGenerator.getInstance().nextKey());
//		}
	}
}