package kr.go.mobile.iff;

import kr.go.mobile.iff.service.ISessionManagerService;
import kr.go.mobile.iff.service.ISessionManagerService.ISessionManagerEventListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;


/**
 * SessionManagerSerivce의 bind를 확인하기 위하여  500ms 주기로 확인한다. 
 * 서비스의 바인딩이 이루어지면, 정상동작을 실행한다. 
 * 
 * @author 윤기현
 *
 */
public class ServiceWaitHandler extends Handler {
	public static final int MESSAGE_CATEGORY_LOADING_ACTIVITY = 1;
	public static final int READY_BIND_SERVICE = 101;
	public static final int CHANGE_SERVICE_EVENT = 102;
	public static final int TIMEOUT_BIND_SERVICE = 103;
	
	public static final int MESSAGE_CATEGORY_ADMINISTRATOR_BROADCAST = 2;
	
	private final int TIMEOUT_MILLIS = 60000; // 1 min 
	private final int RETRY_DELAY_MILLIS = 300; 
	private ISessionManagerEventListener m = null;
	
	private static ServiceWaitHandler INSTANCE;
	public static final ServiceWaitHandler getInstance() {
		if (INSTANCE == null) 
			INSTANCE = new ServiceWaitHandler();
		return INSTANCE;
	}
	
	private ServiceWaitHandler() { /*hide*/ }
	
	public boolean existListener() {
		return m != null ? true : false; 
	}
	
	synchronized void addEventListener(ISessionManagerEventListener listener) {
		m = listener;
	}
	
	synchronized void removeEventListener() {
		m = null;
	}
	
	@Override
	public void handleMessage(Message msg) {
		ISessionManagerService service = SAGTApplication.getSessionManagerService();
		
		if (service == null) {
			long delayTime = msg.arg2;
			if ( delayTime == TIMEOUT_MILLIS) {
				// 서비스 바인딩 TIMEOUT 이벤트 발생. 
				switch (msg.what) {
				case MESSAGE_CATEGORY_LOADING_ACTIVITY:
					msg.arg1 = TIMEOUT_BIND_SERVICE;
					handleActivityMessage(service, msg);
					break;
				default:
					break;
				}
			} else {
				msg.arg2 += RETRY_DELAY_MILLIS;
				Message m = Message.obtain(msg);
				sendMessageDelayed(m, RETRY_DELAY_MILLIS);
			}
		} else {
			switch (msg.what) {
			case MESSAGE_CATEGORY_LOADING_ACTIVITY:
				handleActivityMessage(service, msg);
				break;
			case MESSAGE_CATEGORY_ADMINISTRATOR_BROADCAST:
				handleAdminstratorBroadcast(service, msg);
				break;
			}
		}
	}
	
	synchronized private void handleActivityMessage(ISessionManagerService service, Message msg) {
		final ISessionManagerEventListener main = m;
		if (main == null) {
			throw new RuntimeException("ISessionManagerEventListener 가 등록되지 않았습니다. 앱을 종료합니다.");
		}
		switch (msg.arg1) {
		case READY_BIND_SERVICE:
			main.ready();
			break;
		case TIMEOUT_BIND_SERVICE:
			main.timeout();
			break;
		case CHANGE_SERVICE_EVENT:
			main.onChangedStatus(msg.arg2, (Bundle)msg.obj);
			break;
		default:
			break;
		}
	}
	
	private void handleAdminstratorBroadcast(ISessionManagerService service, Message msg) {
		String packageName = (String)msg.obj;
		switch (msg.arg1) {
		case 0: // 실행
			service.addMonitorPackage(packageName);
			break;
		case 1:
		case 2: // 비정상 종료
			service.removeMonitorPackage(packageName);
			break;
		case 3: // 정상 종료
			service.removeMonitorPackage(packageName, false);
			break;
		default:
			break;
		}
	}
}
