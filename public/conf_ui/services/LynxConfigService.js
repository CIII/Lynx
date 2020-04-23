(function () {
	'use strict';
	/*
	* @Service - CommonService
	* @Description
	* @property {Array} CommonService.$inject This injects the paramters properly for minimization.
	*/
	angular.module("lynxConfig").factory("lynxConfigService", LynxConfigService);

	LynxConfigService.$inject = ["$http", "$q"];

	function LynxConfigService($http, $q) {
    var factory = {};
    factory.configurations = {};
    factory.existing_urls = [];

    var collect_existing_urls = function() {
      factory.existing_urls.length = 0;
      for(var domain in factory.configurations["lynx"]["domains"]){
        var domain_config = factory.configurations["lynx"]["domains"][domain]
        for(var directory in domain_config){
          if(directory != "preview"){
            factory.existing_urls.push(
              {
                "url": domain_config[directory]["url"],
                "domain": domain,
                "folder": directory
              }
            );
          }
        }
      }
    };

    factory.load_current_configurations = function() {
      var deferred = $q.defer();

      $http({
        method: 'GET',
        url: "/pages/current_config.json"
      }).then(function successCallback(response) {
          Object.keys(factory.configurations).forEach(function(key) { delete factory.configurations[key]; });
          factory.configurations["lynx"] = response.data["lynx"];
          collect_existing_urls();
          deferred.resolve(response.data);
      }, function errorCallback(response) {
          deferred.reject(response);
      });

      return deferred;
    };

    factory.save_current_configuration = function(url, new_configuration){
      var current_configuration = factory.configurations["lynx"]["domains"][url.domain][url.folder]
      var homepage_configuration = factory.configurations["lynx"]["domains"][url.domain]["home_page"]
      var keys = Object.keys(new_configuration);
      for(var i=0; i<keys.length; i++){
        var key = keys[i];
        //If null we dont want to submit
        if(new_configuration[key] != null){
          current_configuration[key] = new_configuration[key];
          //If array we need to extract value
          var type = Object.prototype.toString.call(current_configuration[key]).replace("[object ","").replace("]","");
          if(type == "Array"){
            for(var j=0; j<current_configuration[key].length; j++){
              current_configuration[key][j] = current_configuration[key][j].value;
            }
          }
          if(current_configuration[key] == homepage_configuration[key] && url.folder != "home_page"){
            delete current_configuration[key]
          }else if(current_configuration[key].length == 0){
            delete current_configuration[key]
          }
        }
      }
    }

    factory.save_to_preview = function(url,new_configuration){
      var current_configuration = factory.configurations["lynx"]["domains"][url.domain]["preview"]
      var homepage_configuration = factory.configurations["lynx"]["domains"][url.domain]["home_page"]
      var keys = Object.keys(new_configuration);
      for(var i=0; i<keys.length; i++){
        var key = keys[i];
        //If null we dont want to submit
        if(new_configuration[key] != null){
          current_configuration[key] = new_configuration[key];
          //If array we need to extract value
          var type = Object.prototype.toString.call(current_configuration[key]).replace("[object ","").replace("]","");
          if(type == "Array"){
            for(var j=0; j<current_configuration[key].length; j++){
              current_configuration[key][j] = current_configuration[key][j].value;
            }
          }
          if(current_configuration[key] == homepage_configuration[key] && url.folder != "home_page"){
            delete current_configuration[key]
          }
        }
      }
    }

		return factory;
	}

})();
