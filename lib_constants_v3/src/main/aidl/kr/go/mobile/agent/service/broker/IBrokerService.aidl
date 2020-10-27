// IBrokerService.aidl
package kr.go.mobile.agent.service.broker;

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.BrokerTask;
import kr.go.mobile.agent.service.broker.IBrokerServiceCallback;
import kr.go.mobile.agent.service.broker.UserAuthentication;


interface IBrokerService {
    // 공통기반 통합인증 기능으로 사용자 인증 정보를 요청한다.
    UserAuthentication getUserAuth();
    // 공통기반의 기본 기능으로 행정서버에 데이터를 전달 / 요청한다.
    BrokerResponse execute(in BrokerTask task);
    oneway void enqueue(in BrokerTask task, in IBrokerServiceCallback callback);
}