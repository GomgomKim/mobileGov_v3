// IBrokerServiceCallback.aidl
package kr.go.mobile.agent.service.broker;

import kr.go.mobile.agent.service.broker.DocumentResponse;

interface IDocumentCallback {
    oneway void onResponse(in DocumentResponse response);
    oneway void onFailure(int code, String msg);
}