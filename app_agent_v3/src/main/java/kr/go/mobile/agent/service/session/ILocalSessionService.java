package kr.go.mobile.agent.service.session;


import kr.go.mobile.agent.service.broker.UserAuthentication;

public interface ILocalSessionService {
    String getUserID();
    String getUserDN();
    void registerSigned(UserSigned signed);
    void registerAuthentication(UserAuthentication authentication);
    UserSigned getUserSigned();
    UserAuthentication getUserAuthentication();
    void clear();
}
