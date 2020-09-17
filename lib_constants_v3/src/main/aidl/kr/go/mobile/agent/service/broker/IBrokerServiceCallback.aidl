// IBrokerServiceCallback.aidl
package kr.go.mobile.agent.service.broker;

import kr.go.mobile.agent.service.broker.BrokerResponse;

interface IBrokerServiceCallback {
    oneway void onResponse(in BrokerResponse response);
    oneway void onFailure(int code, String msg);
}