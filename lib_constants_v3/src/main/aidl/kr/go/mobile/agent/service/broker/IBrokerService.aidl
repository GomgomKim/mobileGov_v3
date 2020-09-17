// IBrokerService.aidl
package kr.go.mobile.agent.service.broker;

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.BrokerTask;
//import kr.go.mobile.agent.service.broker.DocImage;
//import kr.go.mobile.agent.service.broker.DocTask;
//import kr.go.mobile.agent.service.broker.FileResponse;
//import kr.go.mobile.agent.service.broker.FileTask;
import kr.go.mobile.agent.service.broker.IBrokerServiceCallback;
import kr.go.mobile.agent.service.broker.UserAuthentication;


interface IBrokerService {
    // 공통기반 통합인증 기능으로 사용자 인증 정보를 요청한다.
    UserAuthentication getUserAuth();
    // 공통기반의 기본 기능으로 행정서버에 데이터를 전달 / 요청한다.
    BrokerResponse execute(in BrokerTask task);
    void enqueue(in BrokerTask task, in IBrokerServiceCallback callback);
//    oneway void enqueue(in BrokerTask task);
    // 공통기반 문서뷰어 기능으로 문서 정보(이미지)를 요청한다.
//    DocImage getDocImage(in DocTask task);
//    // 행정서버와 파일을 주고받는 기능을 제공한다.
//    FileResponse doDownload(in FileTask task);
//    FileResponse doUpload(in FileTask task);
}