console.log("START to load CommonBaseAdapter.3.0.0.js");

var CommonBaseAPI = new function () {

    this._constructors = [];
    this.init = function () {
		console.log("CommonBaseAPI.init()");
	};
	
    //native코드 호출
	this.exec = function () {
		console.log("exec() is called");
		var callMethod = arguments[0];
		var callbackID = callMethod + this.CallbackManager.callbackId++;

		if (typeof arguments[1] != "function" || typeof arguments[2] != "function") {
		    console.log("exec() param type error : is not a function");
			return
		}

		if (arguments[1] || arguments[2]) {
			this.CallbackManager.callbacks[callbackID] = {
				success: arguments[1],
				error: arguments[2]
			}
		}

        if (arguments.length > 3) {
			CBHybridPluginManager.exec(callbackID, callMethod, JSON.stringify(arguments[3]));
		}else{
		    CBHybridPluginManager.exec(callbackID, callMethod);
		}
	};

	this.CallbackManager = {};
	this.CallbackManager.callbackId = 0;
	this.CallbackManager.callbacks = {};
	this.CallbackManager.callbackStatus = {
        HYBRID_ERROR_NONE : 9000,
        HYBRID_ERROR_NATIVE_CALL_PARAMETER : 9001,
        /**
         * 요청하는 플러그인을 찾을 수 없음
         */
        HYBRID_ERROR_PLUGIN_NOT_FOUND : 9002,
        /**
         * 호출 객제 생성 에러
         */
        HYBRID_ERROR_NEW_INSTANCE : 9003,
        /**
         * 함수 호출 에러
         */
        HYBRID_ERROR_METHOD_CALL : 9004,

        HYBRID_ERROR_JSON_EXPR : 9005,

        HYBRID_ERROR_PERMISSION_DENIED : 9005,

        HYBRID_ERROR_AIRPLANE_MODE : 9006,

        HYBRID_ERROR_NO_SIM : 9007,

        HYBRID_ERROR_INVALID_PARAMETER : 9008,

        HYBRID_ERROR_UNKNOWN : 9009,

        HYBRID_ERROR_GPS_IS_NOT_AVAILABLE : 9010,
        HYBRID_ERROR_GPS_TIMEOUT : 9011,
        HYBRID_ERROR_GENERIC_FAILURE : 9012,
        HYBRID_ERROR_RADIO_OFF : 9013,
        HYBRID_ERROR_SMS_NULL_PDU : 9014,
        HYBRID_ERROR_NO_SMS_SERVICE : 9015,
        HYBRID_ERROR_GPS_NO_DATA : 9016,
        HYBRID_ERROR_DELETE_CALLBACK : 9018,
	};
	this.CallbackManager.onCallback = function (id, pluginResult) {
		if (CommonBaseAPI.CallbackManager.callbacks[id]) {
		    switch(pluginResult.status){
		        case CommonBaseAPI.CallbackManager.callbackStatus.OK:{
                    try {
					    if (CommonBaseAPI.CallbackManager.callbacks[id].success) {
					        if(pluginResult.retMsg){
					           CommonBaseAPI.CallbackManager.callbacks[id].success(pluginResult.retMsg);
					        }else{
					           CommonBaseAPI.CallbackManager.callbacks[id].success();
					        }
					    }
				    } catch (exception) {
					    console.log("Error in success callback: " + id + " = " + exception);
				    }
                }
		        break;
		        case CommonBaseAPI.CallbackManager.callbackStatus.CALLBACK_DELETE:{

		        }
		        break;
		        default:{
                    try {
					    if (CommonBaseAPI.CallbackManager.callbacks[id].error) {
					        if(pluginResult.retMsg){
					            CommonBaseAPI.CallbackManager.callbacks[id].error(pluginResult.status, pluginResult.retMsg);
					        }else{
					            CommonBaseAPI.CallbackManager.callbacks[id].error();
					        }
					    }
				    } catch (exception) {
					    console.log("Error in error callback: " + id + " = " + exception);
				    }
		        }
		    }

			if (pluginResult.keepCallback == false) {
				delete CommonBaseAPI.CallbackManager.callbacks[id];
			}
		}
	};

	this.Browser = new function () {
	    //로딩바 시작
		this.startLoadingBar = function (param) {
		    if(param == undefined){
		        CBHybridPluginManager.exec('noID', 'Browser.startLoadingBar');
		    }else{
		        CBHybridPluginManager.exec('noID', 'Browser.startLoadingBar', JSON.stringify(param));
		    }

		};
		//로딩바 종료
		this.stopLoadingBar = function () {
			CBHybridPluginManager.exec('noID', 'Browser.stopLoadingBar');
		};
		//앱종료
		this.terminateApp = function () {
		    CBHybridPluginManager.exec('noID', 'Browser.terminateApp');
		};
        //BackKey Event 리스너 등록
		this.addBackKeyListener = function (listener) {
		    //callbackID를 전달하기 위해 사용자 플러그인 호출 구조로 부름
	         CommonBaseAPI.exec('Browser.addAsyncBackKeyListener', listener, function(){}) ;
		};
		//BackKey Event 리스너 삭제
		this.removeBackKeyListener = function () {
            CBHybridPluginManager.exec('noID', 'Browser.removeBackKeyListener');
		};
	};

    //구버전과 호출 형식이 다름
	this.Telephony = new function () {

		this.call = function (succCallback, errorCallback, param) {
			CommonBaseAPI.exec('Telephony.call',succCallback, errorCallback, param);
		};
		this.dial = function (succCallback, errorCallback, param) {
			CommonBaseAPI.exec('Telephony.dial',succCallback, errorCallback, param);
		};

		this.sendSMS = function (succCallback, errorCallback, param) {
			CommonBaseAPI.exec('Telephony.sendSMSAsync',succCallback, errorCallback, param);
		};

		this.launchSMS = function (succCallback, errorCallback, param) {
			CommonBaseAPI.exec('Telephony.launchSMS',succCallback, errorCallback, param);
		};
	};

	this.Location = new function () {
		this.getCurrentPosition = function (succCallback, errorCallback, param) {
			CommonBaseAPI.exec('Location.getCurrentPositionAsync',succCallback, errorCallback, param);
		};
	};

    this.Broker = new function () {
        this.getSSO = function (succCallback, errorCallback) {
            CommonBaseAPI.exec('Broker.getSSO',succCallback, errorCallback);
        };
        this.startDefaultDocumentView = function(succCallback, errorCallback, docUrl, docName, docCreatedDate) {
            var jsonParams = new Object();
            jsonParams.docUrl = docUrl;
            jsonParams.docName = docName;
            jsonParams.docCreatedDate = docCreatedDate;
            CommonBaseAPI.exec('Broker.startDefaultDocumentView',succCallback, errorCallback, JSON.stringify(jsonParams));
        };
        this.enqueue = function (succCallback, errorCallback, serviceId, serviceParams) {
            var jsonParams = new Object();
            jsonParams.serviceId = serviceId;
            jsonParams.serviceParams = serviceParams;
            CommonBaseAPI.exec('Broker.asyncRequest',succCallback, errorCallback, JSON.stringify(jsonParams));
        }
    }
};

var URLParams = new function() {
    this._constructors = [];
    this.parse = function () {
        var params = {};
        window.location.search.replace(/[?&]+([^=&]+)=([^&]*)/gi, function(str, key, value) { params[key] = decodeURIComponent(value); });
        return params;
	};
};

CommonBaseAPI.init();