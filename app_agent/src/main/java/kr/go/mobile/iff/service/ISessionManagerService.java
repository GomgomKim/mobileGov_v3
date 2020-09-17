package kr.go.mobile.iff.service;

import kr.go.mobile.iff.util.AdminState;
import android.os.Bundle;

public interface ISessionManagerService {
	
public static interface ISessionManagerEventListener {
		
		int SIGN_EVENT = 1;
		int VPN_EVENT = 2;
		int CERT_EVENT = 3;
		
		String EXTRA_SIGNED_DN = "extra_signed_dn";
		String EXTRA_IS_CERT = "extra_cert";
		String EXTRA_RESULT = "extra_result";
		
		/**
		 * SessionManagerService 의 바인딩됨
		 */
		void ready();
		/**
		 * SessionManagerService 가 지정된 시간까지 바인딩되지 않음
		 */
		void timeout();
		/**
		 * SessionManagerService 와 관련된 솔루션의 상태가 변경됨
		 */
		public void onChangedStatus(int eventType, Bundle data);
	}
	/**
	 * 서명값이 존재하는지 여부를 알려준다. 
	 * 
	 * @return 서명값이 존재하면 true, 만약 존재하지 않거나 유지시간이 자났으면 false을 리턴한다. 
	 */
	public boolean existSigned();
	
	/**
	 * 현재 VPN이 활성화 와 서명값이 유효한지를 알려준다. 
	 * 
	 * @return 
	 */
	public boolean enableVpn();

	/**
	 * 행정앱이 런처에게 서명정보를 요청할때 호출한다 <br>
	 * VPN이 연결되어 있다면, 기존 연결을 해제하고 다시 연결한다. 만약 VPN이 연결되어 있지 않다면 새로 연결한다. 
	 */
	public void startVPN(AdminState adminState);

	public void stopVPN();
	
	/**
	 * 새로 시작되는 행정앱의 패키지명을 추가한다.
	 * 
	 * @param packageName
	 */
	public void addMonitorPackage(String packageName);
	/**
	 *  종료되는 행정앱의 패키명을 삭제한다.
	 * 
	 * @param packageName
	 */
	public void removeMonitorPackage(String packageName);
	/**
	 *  종료되는 행정앱의 패키명을 삭제한다.
	 * 
	 * @param packageName
	 * @param immediately 
	 */
	public void removeMonitorPackage(String packageName, boolean immediately);
	
}
