(function () {
	'use strict';
	/*
	* @Service - CommonService
	* @Description
	* @property {Array} CommonService.$inject This injects the paramters properly for minimization.
	*/
	angular.module("solar").factory("CommonService", CommonService);

	CommonService.$inject = ["$localStorage", "$q", "BASE_CONSTS", "$http","$sce","$timeout", "$location"];

	function CommonService($localStorage, $q, BASE_CONSTS, $http,$sce,$timeout, $location) {
		var factory = {};
		factory.ip_address = null;
		factory.CompanyList = [];
		factory.flags = {'isPlaceChange': false, 'attemptedToContinue': false}

    factory.storeResponseInLocalstorage = function (response) {
        // for PPA
        $localStorage.ppaTotalSavings = (response.outputs.ppa.savings_total !== undefined) ? response.outputs.ppa.savings_total : 0;
        $localStorage.ppaUpfrontCost = (response.outputs.ppa['upfront-cost'] !== undefined) ? response.outputs.ppa['upfront-cost'] : 0 ;
        $localStorage.ppaFirstYearSaving = (response.outputs.ppa.savings[0] !== undefined) ? response.outputs.ppa.savings[0] : 0;

        // for CASH
        $localStorage.cashTotalSavings = (response.outputs.cash.savings_total !== undefined) ? response.outputs.cash.savings_total : 0;
        $localStorage.cashUpfrontCost = (response.outputs.cash['upfront-cost'] !== undefined) ? response.outputs.cash['upfront-cost'] : 0;
        $localStorage.cashFirstYearSaving = (response.outputs.cash.savings[0] !== undefined) ? response.outputs.cash.savings[0] : 0;

        // for LOAN
        $localStorage.prevLoanTotalSavings = $localStorage.loanTotalSavings;
        $localStorage.loanTotalSavings = (response.outputs.loan.savings_total !== undefined)? response.outputs.loan.savings_total : 0;
        $localStorage.loanUpfrontCost = (response.outputs.loan['upfront-cost'] !== undefined) ? response.outputs.loan['upfront-cost'] : 0;
        $localStorage.prevLoanFirstYearSaving = $localStorage.loanFirstYearSaving;
        $localStorage.loanFirstYearSaving = (response.outputs.loan.savings[0] !== undefined && response.outputs.loan.savings[0] !== null && response.outputs.loan.savings[0] !== false) ? response.outputs.loan.savings[0] : 0;
        $localStorage.prevLoanSavingsPercentage = $localStorage.loanSavingsPercentage;
        $localStorage.loanSavingsPercentage = (response.outputs.loan['savings-percentage'] !== undefined && response.outputs.loan['savings-percentage'] !== null) ? response.outputs.loan['savings-percentage'] * 100 : 0;

        $localStorage.prevLoanYear1SavingsPercentage = $localStorage.loanYear1SavingsPercentage;
        $localStorage.loanYear1SavingsPercentage = (response.outputs.loan['year-1-savings-percentage'] !== undefined && response.outputs.loan['year-1-savings-percentage'] !== null) ? response.outputs.loan['year-1-savings-percentage'] * 100 : 0;

        //confidence
        $localStorage.confidence = (response.confidence !== undefined && response.confidence!== false) ? response.confidence * 100 : 0;
    }

    var _createFormObj = function(pingPostObj){
      pingPostObj = pingPostObj || {};
      var params = ["electric_bill",
        "state",
        "maxmind_state",
        "zip",
        "maxmind_zip",
        "city",
        "maxmind_city",
        "country",
        "maxmind_country",
        "leadid_token",
        "property_ownership",
        "address",
        "lat",
        "lng",
        "street",
        "electric_company",
        "first_name",
        "last_name",
        "full_name",
        "email",
        "phone_home",
        "dob"]
      
      // append form string as a prefix on all keys
      var formObj = new Object();
      for (var i = 0, len = params.length; i < len; i++) {
        var param = params[i];
        if(angular.isDefined(pingPostObj[param])){
          formObj["form."+param] = pingPostObj[param];
        }else if(angular.isDefined($localStorage[param])){
          formObj["form."+param] = $localStorage[param];
        }
      }
      if(angular.isDefined(formObj["form.electric_company"]) && formObj["form.electric_company"] == "Eversource/NSTAR"){
        formObj["form.electric_company"] = "Eversource Energy"
      }

      // conditional wise required variables
      if(angular.isDefined(pingPostObj[BASE_CONSTS.TRUSTEDFORM_CERT]) || angular.isDefined($localStorage[BASE_CONSTS.TRUSTEDFORM_CERT])){
        formObj[BASE_CONSTS.TRUSTEDFORM_CERT] = pingPostObj[BASE_CONSTS.TRUSTEDFORM_CERT] || $localStorage[BASE_CONSTS.TRUSTEDFORM_CERT];
      }
      if(angular.isDefined(pingPostObj[BASE_CONSTS.TRUSTEDFORM_TOKEN]) || angular.isDefined($localStorage[BASE_CONSTS.TRUSTEDFORM_TOKEN])){
        formObj[BASE_CONSTS.TRUSTEDFORM_TOKEN] = pingPostObj[BASE_CONSTS.TRUSTEDFORM_TOKEN] || $localStorage[BASE_CONSTS.TRUSTEDFORM_TOKEN];
      }
      formObj.user_agent = navigator.userAgent;
      formObj.local_hour = (new Date()).getHours();
      formObj.ip = (pingPostObj.ip_address) ? pingPostObj.ip_address : $localStorage.ip_address;

      return formObj;
    } 

		/*
		* @Method - ping
		* @Description - Ping API Call
		*/
		factory.ping = function (pingObj) {	
      var deferred = $q.defer();
			pingObj = pingObj || {};
      var formObj = _createFormObj(pingObj);

		 	var url = BASE_CONSTS.SERVER_URL+'/ping';
      $http({
			  method: 'POST',
			  url: url,
			  data: $.param(formObj),
    			headers: {'Content-Type': 'application/x-www-form-urlencoded'}

			}).then(function successCallback(response) {
				deferred.resolve(response.data);
			}, function errorCallback(response) {
				deferred.reject(response);
			});

      return deferred.promise;
		};

    var serialize = function(obj) {
      var str = [];
      for(var p in obj)
         str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
      return str.join("&");
    }
        /*
		* @Method - solar calc
		* @Description - solar calc call API Call
		*/
		factory.solar_calc = function (solarObj) {
            var deferred = $q.defer();
			solarObj = _.pick(solarObj, _.identity);

			// append form string as a prefix on all keys
			var formObj = new Object();
			angular.forEach(solarObj, function(value, key){
				formObj["form."+key] = value;
			});

			// temporary variavles
			if (formObj['form.zip'] == "452001") {
				formObj['form.zip'] = "02145";
			}

		 	// conditional wise required variables
			formObj.ip = (factory.ip_address) ?factory.ip_address : $localStorage.ip_address;
			formObj.user_agent = navigator.userAgent;

			formObj['form.street'] = $localStorage.street;
			formObj['form.city'] = $localStorage.city;
			formObj['form.state'] = $localStorage.state;

			//trusted form
      formObj['form_progression'] = $localStorage.formProgression;
		 	var url = BASE_CONSTS.SERVER_URL+'/solar/calc/estimate';

            $http({
			  method: 'POST',
			  url: url,
			  data: $.param(formObj),
    			headers: {'Content-Type': 'application/x-www-form-urlencoded'}

			}).then(function successCallback(response) {
				deferred.resolve(response.data);
			}, function errorCallback(response) {
				deferred.reject(response);
			});

            return deferred.promise;
		};

		/*
		* @Method - post
		* @Description - Post API Call
		*/
		factory.post = function (postObj) {	
      var deferred = $q.defer();
			postObj = postObj || {};
      var formObj = _createFormObj(postObj);

		 	var url = BASE_CONSTS.SERVER_URL+'/post';
	 		$http({
			  method: 'POST',
			  url: url,
			  data: $.param(formObj),
    			headers: {'Content-Type': 'application/x-www-form-urlencoded'}

			}).then(function successCallback(response) {
				deferred.resolve(response.data);
			}, function errorCallback(response) {
				deferred.reject(response);
			});

      return deferred.promise;
		};

		/*
		* @Method - getCompanyList
		* @Description
		*/
		factory.getCompanyList = function (state) {
		 	var url = BASE_CONSTS.SERVER_URL+'/query/power_suppliers/'+state;
            var deferred = $q.defer();
            $http.get(url).then(function (result) {
            	factory.CompanyList = result.data;
            	$localStorage['CompanyList'] = result.data;
            	factory.CompanyList.push('Other');
                deferred.resolve(factory);
            }, function (error) {
                deferred.reject(error);
            });

            return deferred.promise;
		};

		factory.bestGuessSupplier = function(zip) {
		 	var url = BASE_CONSTS.SERVER_URL+'/query/best_guess_supplier/'+zip;
      var deferred = $q.defer();
      $http.get(url).then(function (result) {
          deferred.resolve(result.data.supplier);
      }, function (error) {
          deferred.reject(error);
      });

      return deferred.promise;
		};

		/*
		* @Method - validatePhone
		* @Description - Checking validate phone number
		*/
		factory.validatePhone = function(number){
			var url = BASE_CONSTS.SERVER_URL+'/validate_phone/'+number;
            var deferred = $q.defer();
            $http.get(url).then(function (result) {
                deferred.resolve(result);
            }, function (error) {
                deferred.reject(error);
            });

            return deferred.promise;
		}

		/*
		* @Method - addressValidate
		* @Description - Checking address is valid or not
		*/
		factory.eventCreate = function (event, eventAttr) {
		        eventAttr = eventAttr || {};

      var deferred = $q.defer();
      lynx_report.createEvent(event, eventAttr).then(
        function successCallback(response) {
          deferred.resolve(response.data);
        }, function errorCallback(response) {
          deferred.reject(response);
        });
      return deferred.promise;
		};

		/*
		* @Method - getListings
		* @Description - 
		*/
	   	factory.getListings = function(lead_id, callback, errorCallback) {
	   		var deferred = $q.defer();
	   		var getSocketUrl = function(server_url,lead_id) {
          if(server_url.indexOf(":9000") > -1){
            return server_url.replace(":9000",":9998").replace("http", "ws") + "/nexus/listings/"+lead_id;
          }else{
            return server_url.replace("http", "ws") + ":9998/nexus/listings/"+lead_id;
          }
	   		}

	   		var url = getSocketUrl(BASE_CONSTS.SERVER_URL, lead_id);
	   		if(angular.isDefined(BASE_CONSTS.ORIGINAL_QUERY["user_id"])){
	   		  var request_url = BASE_CONSTS.SERVER_URL + "/lead-disposition/" + lead_id;
	   		  $http.get(request_url);
	   		}
			  var solar_socket = new WebSocket(url);
		    solar_socket.onmessage = function(evt) {
		    	if (angular.isDefined(evt.data)) {
		    		callback(JSON.parse(evt.data));
		    	}else{
		    		errorCallback("Err");
		    	}
		    };

		    return deferred.promise;	
	   	};

	   	factory.getNearByAddress = function(address, currentState) {
   			var deferred = $q.defer();
   			if ($localStorage.hasOwnProperty("currentlat") && $localStorage.hasOwnProperty("currentlng") && address!=="") {
   				var latLng = $localStorage.currentlat+","+$localStorage.currentlng;
	   			var params = {
	   				address: address,
	   				latlng: latLng,
	   				radius: BASE_CONSTS.LOCATION_RADIUS,
	   				keyword: address,
	   				key: BASE_CONSTS.MAP_KEY
	   			}

		        var url = "https://maps.googleapis.com/maps/api/geocode/json";
		        url =  $sce.trustAsResourceUrl(url);
		        $http({
		          method: 'GET',
		          url:url,
		          params:params
		        }).then(function success(response) {
		            deferred.resolve(response.data.results);
		        }, function error(err) {
		            deferred.reject(err);
		        });

   			}else{
				deferred.reject('Something went wrong with latitude, longitude')   				
   			}

	        return deferred.promise;
	   	}

		/*
		* @Method - getMyLocationDetails
		* @Description - getting all geo details of my location(Maxmind)
		*/
	   	factory.getMyLocationDetails = function(){
	   		var deferred  = $q.defer();

            if(!angular.isDefined(BASE_CONSTS.ORIGINAL_QUERY['syscheck'])){
                geoip2.city(function(resp){
                    $localStorage.currentlat = resp.location.latitude;
                    $localStorage.currentlng = resp.location.longitude;

                    if (resp.hasOwnProperty('postal')) {
                        deferred.resolve(resp);
                    }else{
                        deferred.resolve("not-found");
                    }
                }, function(err){
                    deferred.resolve("not-found");
                })
            }else{
                deferred.resolve("syscheck: skip");
            }
	      	return deferred.promise;
	   	}

	   	factory.getLeadInfo = function() {
	   	  var deferred = $q.defer();

	   	  if(angular.isDefined(BASE_CONSTS.ORIGINAL_QUERY["user_id"])){
          var url = BASE_CONSTS.SERVER_URL + "/nexus/lead-info/" + BASE_CONSTS.ORIGINAL_QUERY["user_id"];
          $http({
            method: 'GET',
            url: url
          }).then(function successCallback(response) {
              var data = response.data;
              if(angular.isDefined(data["lead_id"])){
                deferred.resolve(data["lead_id"]);
              }else{
                deferred.reject("No lead id in response");
              }
              deferred.resolve(data);
          }, function errorCallback(response) {
              deferred.reject(response);
          });
	   	  }else{
	   	    deferred.reject("No user id")
	   	  }

	   	  return deferred.promise;
	   	}
		return factory;
	}

})();
