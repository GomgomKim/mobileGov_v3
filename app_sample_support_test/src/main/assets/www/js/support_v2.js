MDHBasic = new function () {
	this.constructor = function () {};

	this.SSO = new function () {
		this.getInfo = function (a, c, b) {
		    /*
            a : 성공
            c : 실패
            */
		    CommonBaseAPI.Broker.getSSO(a, c);
		};
	};

	this.SEMP = new function () {
		this.request = function (a, b, c) {
            /*
            a : 성공
            b : 실패
            c : 중계 시스템 연계 요청 정보 [ sCode, parameter ]
            */
		    CommonBaseAPI.Broker.enqueue(a, b, c.sCode, c.parameter);
		};
	};

	this.Attachment = new function () {
		this.load = function (a, b, c) {
            /*
            a : 성공
            b : 실패
            c : 문서 정보 [ attachID, file_name ]
            */
	        CommonBaseAPI.Broker.startDefaultDocumentView(a, b, c.attachID, c.file_name, "");
		};
	};
};

MDHUtil = new function () {
    this.Browser = new function () {
        // 로딩바 시작
        this.startLoadingBar = function (a) {
            CommonBaseAPI.Browser.startLoadingBar(a);
        };
        // 로딩바 종료
        this.stopLoadingBar = function () {
            CommonBaseAPI.Browser.stopLoadingBar();
        };
        // 앱종료
        this.terminateApp = function (a) {
            CommonBaseAPI.Browser.terminateApp();
        };
        // BackKey Event 리스너 등록
        this.addBackKeyListener = function (listener) {
            //callbackID를 전달하기 위해 사용자 플러그인 호출 구조로 부름
             CommonBaseAPI.exec('Browser.addAsyncBackKeyListener', listener, function(){}) ;
        };
        // BackKey Event 리스너 삭제
        this.removeBackKeyListener = function () {
            CBHybridPluginManager.exec('noID', 'Browser.removeBackKeyListener');
        };
    };
};