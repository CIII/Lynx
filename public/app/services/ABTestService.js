(function () {
	'use strict';
	/*
	* @Service - CommonService
	* @Description
	* @property {Array} CommonService.$inject This injects the paramters properly for minimization.
	*/
	angular.module("solar").factory("abTestService", ABTestService);

	ABTestService.$inject = ["$localStorage", "BASE_CONSTS", "$rootScope", "$q", "$http"];

	function ABTestService($localStorage, BASE_CONSTS, $rootScope, $q, $http) {
		var factory = {};
//		factory.requireParams = {
//     'home': ["default_header","state_header", "full_landing", "main_cta", "hero_image_path"],
//     'form1': ["form1_load_rule"],
//     'form13': ["form13_submit_rule"]
//    }
    factory.requireParams = {
     'home': ["default_header","state_header", "full_landing", "main_cta", "hero_image_path"]
    }
    factory.ab = {};
    $rootScope.ab = factory.ab;

    $rootScope.$watch('ab', function(newVal, oldVal){
      $localStorage['ab'] = JSON.stringify(newVal);
    }, true);

		/*
		* @Method - ab/test
		* @Description - Ping API Call
		*/
		factory.ab_test = function (state, directory, paramArr, queryParam) {
      paramArr = paramArr || [];
      queryParam = queryParam || {};

      var deferred = $q.defer();

      if(state != null && angular.isDefined(factory.requireParams[state])){
          paramArr = paramArr.concat(factory.requireParams[state]);
      }
      if(!angular.isDefined(BASE_CONSTS.ORIGINAL_QUERY['syscheck'])){

        var should_run = false;
        var known_values = {};
        for (var i = 0; i < paramArr.length; i++) {
          if(factory.ab.hasOwnProperty(paramArr[i])){
              known_values[paramArr[i]] = factory.ab[paramArr[i]];
          }else{
              should_run = true;
              break;
          }
        }

        if (should_run && paramArr.length > 0){
            var url = BASE_CONSTS.SERVER_URL+'/ab/get_params?';
            var params = (typeof ab_override !== 'undefined' && angular.isDefined(ab_override)) ? { req_param: paramArr, directory: directory, ab_override: ab_override} : { req_param: paramArr, directory: directory};
            params = Object.assign(params, params, queryParam)
            $http({
              method: 'GET',
              url: url,
              params: params

            }).then(function successCallback(response) {
                var data = response.data;
                var keys = Object.keys(data);
                for (var i = 0; i < keys.length; i++) {
                    factory.ab[keys[i]] = data[keys[i]];
                }
                deferred.resolve(data);
            }, function errorCallback(response) {
                deferred.reject(response);
            });
        }else{
            deferred.resolve(known_values);
        }
      }else{
          deferred.reject("syscheck, skipping");
      }

      return deferred.promise;
		};

		return factory;
	}

})();
