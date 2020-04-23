(function () {
	'use strict';
	/*
	* @Service - CommonService
	* @Description
	* @property {Array} CommonService.$inject This injects the paramters properly for minimization.
	*/
	angular.module("solar").factory("formService", FormService);

	FormService.$inject = ["$localStorage", "BASE_CONSTS", "$rootScope"];

	function FormService($localStorage, BASE_CONSTS, $rootScope) {
		var factory = {};
		factory.form = {};
		factory.form.electric_bill = "$151-200";
		factory.form.hasValidAddress = false;
		factory.form.hasValidAddressFormat = false;

		$rootScope.form = factory.form;

		/*
		* @Method - localDataSave
		* @Description - Save obj on localstorage
		*/
		factory.localDataSave = function (obj) {			
			_.each(obj, function(val, key){
			  if(angular.isDefined(val)){
          $localStorage[key] = val;
				}
			});
		};

    $rootScope.$watch('form', function(newVal, oldVal){
      _.each(newVal, function(val, key){
        if(angular.isDefined(val) && val.length > 0){
          $localStorage[key] = val;
        }else if (val.constructor === {}.constructor){
          $localStorage[key] = val;
        }
      });
    }, true);
		
		factory.persistMaxmindValues = function(maxmindResp) {
      if (maxmindResp.hasOwnProperty("postal")) {
        localStorage['maxmind_zip'] = maxmindResp.postal.code;
        factory.form.zip = localStorage['maxmind_zip'];
        factory.form.maxmind_zip = factory.form.zip;
      }
      // gettign all input values from ping api
      if (maxmindResp.hasOwnProperty('subdivisions')) {
        localStorage['maxmind_state'] = maxmindResp.subdivisions[0].iso_code;
        factory.form.state = localStorage['maxmind_state'];
        factory.form.maxmind_state = factory.form.state;
      }
      if (maxmindResp.hasOwnProperty('city')){
        localStorage['maxmind_city'] = maxmindResp.city.names.en;
        factory.form.city = localStorage['maxmind_city'];
        factory.form.maxmind_city = factory.form.city;
      }
      if (maxmindResp.hasOwnProperty('country')){
        localStorage['maxmind_country'] = maxmindResp.country.iso_code;
        factory.form.country = localStorage['maxmind_country'];
        factory.form.maxmind_country = factory.form.country;
      }
      if (maxmindResp.hasOwnProperty('traits')) {
        factory.form.ip_address = (maxmindResp.hasOwnProperty("traits")) ? maxmindResp.traits.ip_address : "";
      }
		}

    /**
    *Translate google's address object into a more manageable/readable object
    */
		var parseGoogleAddress = function(addressObj) {
			var parsed_address = {};
			_.map(addressObj.address_components,function(obj){
				parsed_address[obj.types[0]] = [obj.short_name, obj.long_name];
			});

			var translated_address = {};

			if(addressObj.hasOwnProperty('formatted_address')){
			  translated_address.address = addressObj.formatted_address;
			}
      if(parsed_address.hasOwnProperty('administrative_area_level_1')){
        translated_address.state = parsed_address['administrative_area_level_1'][0];
      }
      if(parsed_address.hasOwnProperty('locality')){
        translated_address.city = parsed_address['locality'][0];
      }
      if(parsed_address.hasOwnProperty('postal_code')) {
        translated_address.zip = parsed_address['postal_code'][0];
      }
      if(parsed_address.hasOwnProperty('street_number')){
        translated_address.street_number = parsed_address['street_number'][1];
      }
      if(parsed_address.hasOwnProperty('route')){
        translated_address.street = parsed_address['route'][1];
      }

      return translated_address;
		}
		
		factory.validateGoogleAddress = function(address_info) {

      factory.form.lat = address_info.geometry.location.lat();
      factory.form.lng = address_info.geometry.location.lng();
      factory.form.address = address_info.formatted_address;

      var parsed_address = parseGoogleAddress(address_info);
      var addressValidate = function (address) {
        if(address.hasOwnProperty('address') && address.hasOwnProperty('state') && address.hasOwnProperty('city') && address.hasOwnProperty('zip') && address.hasOwnProperty('street_number') &&  address.hasOwnProperty('street')){
          if(address.hasOwnProperty('address') && address["state"].trim() != '' && address["city"].trim() != '' && address["zip"].trim() != '' && address["street_number"].trim() != '' && address["street"].trim() != ''){
            return true;
          }else{
            return false;
          }
        } else {
          return false;
        }
      };
      if(addressValidate(parsed_address)){
        factory.form.state = parsed_address.state;
        factory.form.city = parsed_address.city;
        factory.form.zip = parsed_address.zip;
        factory.form.street = parsed_address.street_number + " " + parsed_address.street;
        factory.form.hasValidAddress = true;
        factory.form.hasValidAddressFormat = true;
      }else{
        factory.form.address = parsed_address.address;
        if(factory.form.hasOwnProperty("street")){
          delete factory.form.street;
        }
        factory.form.hasValidAddress = false;
        factory.form.hasValidAddressFormat = false;
      }
    }

    factory.validateAddress = function(){
      var pattern = /^[\w-]+ [A-Za-z ]+[ #\w]+, [A-Za-z]+, [A-Z]{2} \d{5}(?:, ){0,1}[A-Za-z]*$/
      if(!factory.form.address.match(pattern)){
        factory.form.hasValidAddress = false;
        factory.form.hasValidAddressFormat = false;
      }
    }

    factory.validateCurrentAddressFormat = function(address) {
      var validateStreetFormat = function(street){
        var pattern = /^\d[\dA-Za-z]+ [A-Za-z ]+[ #\w]+$/g
        return street.match(pattern)
      }
      if(factory.form.hasOwnProperty('state') && factory.form.hasOwnProperty('city') && factory.form.hasOwnProperty('zip') && factory.form.hasOwnProperty('street')){
        if(factory.form["state"].trim() != '' && factory.form["city"].trim() != '' && factory.form["zip"].trim() != '' && factory.form["street"].trim() != ''){
          if(validateStreetFormat(factory.form.street)){
            factory.form.address = factory.form.street + ", " + factory.form.city + ", " + factory.form.state + ", " + factory.form.zip;
            factory.form.hasValidAddressFormat = true;
            return;
          }
        }
      }
      factory.form.hasValidAddressFormat = false;
    }

		return factory;
	}

})();
