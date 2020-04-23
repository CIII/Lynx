@(landingSettings: utils.templates.LandingSettings)(implicit configuration: play.api.Configuration)

(function(){
    'use strict';

    /*
	* @@Controller - HomeCtrl
	* @@Description
	* @@property {Array} HomeCtrl.$inject This injects the paramters properly for minimization.
	*/
	angular.module('solar').controller('HomeCtrl', HomeCtrl);

	HomeCtrl.$inject = ['$scope', '$rootScope', '$sce', '$http', '$state', 'Socialshare', 'NgMap', '$localStorage', 'CommonService', 'formService', 'abTestService', '$timeout', "BASE_CONSTS", "$uibModal", "SweetAlert","$q", "$filter","$window", "$location", "$document"];
		function HomeCtrl($scope, $rootScope, $sce, $http, $state, Socialshare, NgMap, $localStorage, CommonService, formService, abTestService, $timeout, BASE_CONSTS, $uibModal, SweetAlert,$q, $filter,$window, $location, $document) {
		    var ctrl = this;
		    $rootScope.stateName = $state.current.name;
		    this.places = null;
		    this.boolChangeClass = false;
		    this.$document = $document;
		    this.$rootScope = $rootScope;
		    this.$scope = $scope;
		    this.$sce = $sce;
		    this.$q = $q;
		    this.$http = $http;
		    this.$state = $state;
		    this.$filter = $filter;
		    this.Socialshare = Socialshare;
		    this.NgMap = NgMap;
		    this.$localStorage = $localStorage;
		    this.SweetAlert = SweetAlert;
		    this.CommonService = CommonService;
		    this.formService = formService;
		    this.abTestService = abTestService;
		    this.form = formService.form;
		    this.ab = abTestService.ab;
		    this.addrErr = true;
		    this.nameErr = false;
		    this.phoneVerify = false;
		    this.isValidAddress = true;   
		    this.speed  = 500;
		    this.IsCompanyChange = (this.$localStorage.electric_company) ? true : false;
		    this.$timeout = $timeout; 
		    this.$rootScope.loading = false;
		    this.BASE_CONSTS = BASE_CONSTS;
		    this.$modal = $uibModal;
		    this.CompanyList = [];
		    this.stateList = $state.get();
		    this.nextStateName = '';
		    this.prevStateName = '';
		    this.$window = $window;
		    this.electricBillData = [{"id":"$0-50", "label":"$0-50"}, {"id":"$51-100", "label":"$51-100"}, {"id":"$101-150", "label":"$101-150"}, {"id":"$151-200", "label":"$151-200"}, {"id":"$201-300", "label":"$201-300"}, {"id":"$301-400", "label":"$301-400"}, {"id":"$401-500", "label":"$401-500"}, {"id":"$501-600", "label":"$501-600"}, {"id":"$601-700", "label":"$601-700"}, {"id":"$701-800", "label":"$701-800"}, {"id":"$801+", "label":"$801+"}];
		    this.oldAddress = '';
		    this.creditRangeData = [{"id":"Under 550", "label":"Under 550"}, {"id":"550-649", "label":"550-649"}, {"id":"650-699", "label":"650-699"}, {"id":"700-750", "label":"700-750"}, {"id":"Above 750", "label":"Above 750"}];
		    this.savingTooltipTxt = "Based on what you've told us so far, this is our forecast for your total solar savings over 25 years. Savings are based on our recommended loan product, using average system prices including incentives, and forecasts for future energy prices and system yield. Your solar advisor will complete a site survey to provide a more accurate forecast.";
		    this.purchaseTypeTxt = "A low interest, zero down path to system ownership. Lower your monthly payment, plus get huge upside later.";
		    this.nameTxt = "Help us personalize your energy savings plan and build a smart forecast. We need your name to pull usage statistics from your electric company.";
		    this.creditTxt = "Your credit score unlocks info on the best rates, deals and products available to you from hundreds of providers. Know more so you can save more.";
		    //this.$rootScope.purchaseType = "LOAN";
		    this.rotateSpinner_1 = true;
		    this.rotateSpinner_2 = true;
		    this.rotateSpinner_3 = true;
		    $rootScope.social_url = BASE_CONSTS.SERVER_URL;
		    $scope.solar_calculator_total_years = BASE_CONSTS.SOLAR_CALCULATOR_TOTAL_YEARS;
		    $scope.full_landing = true;
		    this.$location = $location;
		    this.outputs = {};
		    this.abTest = {};
        this.flags = ctrl.CommonService.flags;

		    // this.list = [];
		    // this.userInfo = [];
		    this.$localStorage.selectedAnalytic = (angular.isDefined(this.$localStorage.selectedAnalytic)) ? this.$localStorage.selectedAnalytic : 'CURRENCY';

		    // initialize email dictionary
		    this.emailObj = {};
        this.power_options = ['Eversource','NationalGrid','BGE','Other']
		    // Watcher on state name
		    /*$scope.$watch(function(){
			    return $state.$current.name
			}, function(newVal, oldVal){
			    $rootScope.stateName = newVal;  
			});*/

			$scope.$watch(function(){
			    return ctrl.form.phone_home;
			}, function(newVal, oldVal){
			   	ctrl.checkValidPhone(ctrl.form.phone_home);
			});

			if(this.$state.current.name == 'home'  || (!this.$localStorage.hasOwnProperty('zip'))){
		    	this.initialize();
		    }

		    // getting width of screen
		    
		    if(/android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(navigator.userAgent.toLowerCase())){
		    	this.tooltipPosition = "bottom";
       			this.tooltipPositionCredit = "bottom";			    
			}else{
				this.tooltipPosition = ($window.innerWidth >= 770) ? "right":"bottom";
			    this.tooltipPositionCredit = ($window.innerWidth >= 770) ? "right":"left";
			}
	       	angular.element($window).bind('resize', function(){
	       		if(/android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(navigator.userAgent.toLowerCase())){	       			
	       			ctrl.tooltipPosition = "bottom";
	       			ctrl.tooltipPositionCredit = "bottom";
		       		
	       		}else{
	       			if ($window.innerWidth >= 770) {
		       			ctrl.tooltipPosition = "right";
		       			ctrl.tooltipPositionCredit = "right";
		       		}else{
		       			ctrl.tooltipPosition = "bottom";
		       			ctrl.tooltipPositionCredit = "left";
		       		}
	       		}	       		
	       	});
	       	this.selectedIndex = -1;

	       	this.autocompleteOptions = {
                componentRestrictions: { country: 'us' },
                types: ['address']
            }

          this.graph = true;

		   	$scope.$on('$viewContentLoaded', function(event) {
				angular.element($window).bind('orientationchange', function () {
					angular.element('.tooltipCustom').toggle();
				});
				if($state.current.name == 'home' ){
					angular.element($window).bind("scroll", function() {
						var btn = angular.element(".scrollBtn");						
	                    if (angular.element($window).scrollTop()  > 0) {
	                        $rootScope.boolChangeClass = true;
	                    } else {
	                        $rootScope.boolChangeClass = false;
	                    }
	                    $scope.$apply();
	                });
				}
			});
      this.nextStateName = this.getNextState();
			this.prevStateName = this.getPrevState();

      if(this.$state.current.name == "form1"){
        ctrl.NgMap.getMap().then(function(map){
          ctrl.$timeout(function(){
            google.maps.event.trigger(map, 'resize');
            map.setCenter(new google.maps.LatLng(ctrl.form.lat,ctrl.form.lng));
            map.setOptions({draggable: false, zoomControl: false, scrollwheel: false, disableDoubleClickZoom: true});
          }, 100)
        });
      }
		};



		/*
		* @@Method - getNextState
		* @@Description - Get next state name for redirect on another state
		*/
	    HomeCtrl.prototype.getNextState = function() {
			var ctrl = this;
			var db = this.stateList;
			var state =  this.$state.current.name;
			var next = function(db, val) {
			  var i = _.pluck(db, 'name').indexOf(val);
			  return i !== -1 && db[i + 1] && db[i + 1].name;
			};
			return next(db, state);
	   	};


	   	/*
		* @@Method - getPrevState
		* @@Description - Get prev state name for redirect on another state
		*/
	    HomeCtrl.prototype.getPrevState = function(v) {
			var ctrl = this;
			var db = this.stateList;
			var state =  this.$state.current.name;
			var prev = function(db, val) {
			  var i = _.pluck(db, 'name').indexOf(val);
			  return i !== -1 && db[i - 1] && db[i - 1].name;
			};
			return prev(db, state);
	   	};

	   	/*
		* @@Method - initialize
		* @@Description -
		*/
	   	HomeCtrl.prototype.initialize = function() {
	   		var ctrl = this;
        ctrl.initializePing();
	   	};

	   	/*
		* @@Method - initializeChart
		* @@Description - Generate Chart on landing page
		*/
	   	HomeCtrl.prototype.initializeChart = function(currentSelectedAnalytic, gridAnalytic) {
	   		var ctrl = this;
	   		var cols = [];
	   		var rows = [];
	   		//console.log(currentSelectedAnalytic.cost);
	   		angular.forEach(currentSelectedAnalytic.cost, function(value, key){
	   			var rowsData = {};
	   			key = key + 1
	   			rowsData = {
	   				"c":[
		   				{ 'v': key + 1, 'f': 'Year ' + (key + 1)},
		   				{ 'v': currentSelectedAnalytic.cost_cumulative[key], 'f': '$'+currentSelectedAnalytic.cost_cumulative[key]},
		   				{ 'v': gridAnalytic.cost_cumulative[key], 'f': '$'+gridAnalytic.cost_cumulative[key]},
		   				{ 'v': currentSelectedAnalytic.savings_cumulative[key], 'f': '$'+currentSelectedAnalytic.savings_cumulative[key] }
	   				]
	   			}
	   			cols.push({"id": value.toString(), "label": value.toString(),"type": "number"});
	   			rows.push(rowsData);
	   		});

	   		cols = [{
                    "id": "Year",
                    "label": "Year",
                    "type": "number"
                },
                {
                    "id": "Solar Cost Cumulative",
                    "label": "Solar Cost",
                    "type": "number"
                },
                {
                    "id": "Grid Cost Cumulative",
                    "label": "Grid Cost",
                    "type": "number"
                },
                {
                    "id":  "Solar Savings Cumulative",
                    "label": "Solar Savings",
                    "type": "number"
            }]
           ctrl.myChartObject = {
			  "type": "ComboChart",
			  "data": {
			  	"cols":cols,
			    "rows":rows,
			  },
			  "options": {
			    "title": "Grid Cost and Solar Savings",
			    'chartArea': {'width': '80%', 'height': '80%'},
			    "legend": {"position": 'bottom'},
			    "vAxis": {
			      "title": "Dollars (Cumulative)",
			      "minValue": 0,
			      "gridlines": {
			        "count": 10
			      },
			      format: 'short'
			    },
			    "hAxis": {
			      "title": "Years"
			    },
			    series: {
			        0: {type: 'line'},
			        1: {type: 'line'},
			        2: {type: 'bars'}
			    },
			    colors: ['#3c7ac5', '#535353', '#bce37d']
			  }
			}

		 	ctrl.cssStyle = "height:400px; padding:20px;";
	   	};

	   	/*
		* @@Method - initializePing
		* @@Description - Call ping api on intialize
		*/
	   	HomeCtrl.prototype.initializePing = function() {

	   		var ctrl = this;
	   		ctrl.$localStorage['formProgression'] = 0
	   		function strip(str) {
                          return str.replace(/^\s+|\s+$/g, '');
                        }
	   		if (angular.isDefined(ctrl.form.zip) && strip(ctrl.form.zip) !== "" && ctrl.form.zip.length <= ctrl.BASE_CONSTS.ZIP_LENGTH && ctrl.form.zip.length >= ctrl.BASE_CONSTS.ZIP_LENGTH) {
		    	this.solar_calc().then(function(response){
		    		if(response && angular.isDefined(response.outputs)){
                        // initialize chart from here
                        ctrl.initializeChart(response.outputs.loan, response.outputs.grid);

		    		}else{
		    			ctrl.graph = false;
		    		}		    		
		    	})
	   		}else{
    			ctrl.graph = false;
    		}
	   	};

	   	/*
		* @@Method - storeResponseInLocalstorage
		* @@Description -
		*/

		HomeCtrl.prototype.storeResponseInLocalstorage = function(response) {
			var ctrl = this;
      ctrl.CommonService.storeResponseInLocalstorage(response);
		};

   		/*
		* @@Method - showModal
		* @@Description - Display modal popup for email and phone
		*/
	   	HomeCtrl.prototype.showModal = function(val) {       
	   		var ctrl = this; 
	        this.opts = {
		       	backdrop: true,
		        backdropClick: true,
		        dialogFade: false,
		        keyboard: true,
		        templateUrl : '../views/forms/emailModal.html',
		        controller : ModalInstanceCtrl,
		        resolve: {} // empty storage
	        };
		          
        
	        this.opts.resolve.item = function() {
	            return angular.copy({name: (val == 'email') ? "Update Email":"Update Phone", email: ctrl.form.email, phone_home: ctrl.form.phone_home, type : val}); // pass name to Dialog
	        }
        
          	var modalInstance = this.$modal.open(ctrl.opts);
          
          	modalInstance.result.then(function(response){
            	if(val == 'email'){
            		ctrl.CommonService.eventCreate(ctrl.BASE_CONSTS.EMAIL_MODAL_COMPLETED);
            		ctrl.form.email = response.email;
            		ctrl.$localStorage.email = response.email;
            	}else{
            		ctrl.form.phone_home = response.phone_home;
            		ctrl.$localStorage.phone_home = response.phone_home;
            	}


          	},function(){
            	//on cancel button press
            	console.log("Modal Closed");
          	});
      	};


	   	/*
		* @@Method - getCompanyList
		* @@Description - Get list of electric companies
		*/
	    HomeCtrl.prototype.getCompanyList = function() {
	    	var ctrl = this;
			var state = this.$localStorage.state;
			this.CommonService.getCompanyList(state).then(function(res) {
	            console.log("Get power suppliers...");
	            ctrl.CompanyList = ctrl.$localStorage.CompanyList;
	        },function(err){
	            console.log(err);
	            ctrl.nextStateName = ctrl.getNextState();
	            ctrl.form.electric_company = 'Other';
                ctrl.$localStorage['electric_company'] = 'Other';
	            ctrl.submitPowerCompanyForm(ctrl.nextStateName);
           	});
	   	};

	   	/*
		* @@Method - getAddress
		* @@Description - Getting address from google API
		*/
	   	HomeCtrl.prototype.getAddress = function(address) {
	   		var ctrl = this;
	   		this.addrErr = false;
	   		this.flags.isPlaceChange = false;
	   		ctrl.CommonService.getNearByAddress(address, ctrl.$state.current.name).then(function(response){
	   			// console.log(response);
	   			ctrl.places = response;
	   		},function(err){
	   			console.log(err);
	   		})
	    };

	    HomeCtrl.prototype.validateCurrentAddress = function(){
	      var ctrl = this;
        ctrl.formService.validateCurrentAddressFormat();
	    }

      HomeCtrl.prototype.validateAddress = function(){
	      var ctrl = this;
        ctrl.formService.validateAddress();
	    }

	    /*
		* @@Method - changePurchaseType
		* @@Description - changing calculation on purchase type change
		*/
	   	HomeCtrl.prototype.changePurchaseType = function(type) {
	   		var ctrl = this; var saving = 0;
	   		this.$rootScope.purchaseType = type;
	   		this.chartAnalytic = {};	   		

	   		if(type=='LOAN'){
	   			this.chartAnalytic = ctrl.outputs.loan;
	   			this.purchaseTypeTxt = "A low interest, zero down path to system ownership.Lower your monthly payment, plus get huge upside later.";
   			 	saving = localStorage.getItem('ngStorage-loanTotalSavings');
	   		}else if(type=='PPA'){
	   			this.chartAnalytic = ctrl.outputs.ppa;
	   			this.purchaseTypeTxt = "Just pay for power, your installer owns the panels, you just get clean power for less than you pay now.";
	   			saving = localStorage.getItem('ngStorage-ppaTotalSavings');
	   		}else if(type=='CASH'){
	   			this.chartAnalytic = ctrl.outputs.cash;
	   			this.purchaseTypeTxt = "Own your system free and clear from day one.";
	   			saving = localStorage.getItem('ngStorage-cashTotalSavings');
	   		}
	   		saving = saving/ctrl.BASE_CONSTS.SOLAR_CALCULATOR_TOTAL_YEARS;
	   		saving = this.$filter('number')(saving, 2);
	   		this.$rootScope.saving = saving;
	   		this.$rootScope.twitterText = "I'll pay $"+this.$rootScope.saving+" less a year with #solar. You can too. @@easiersolar calc took 30sec, best thing I’ve done all day.";
     		this.$rootScope.fbText = "My power costs will drop $"+this.$rootScope.saving+" a year with solar. Yours can to. EasierSolar’s calculator took me 30 seconds, best thing I’ve done all day. Get personalized results fast. Compare local and national installers on your terms. Unbiased info, skilled craftsman, and the biggest change you’ll make in the fight against climate change.";

	   		// initialize chart again on 
	   		ctrl.initializeChart(this.chartAnalytic, ctrl.outputs.grid);

    };

		/*
		* @@Method - onKeyupAddress
		* @@Description - Select address from an google API
		*/
	    HomeCtrl.prototype.onKeyupAddress = function(address) {
    		var ctrl = this;
    		ctrl.flags.isPlaceChange = false;
	    }

	    /*
		* @@Method - resetPlaces
		* @@Description -
		*/
	    HomeCtrl.prototype.resetPlaces = function() {
	    	var ctrl = this;
	    	this.$timeout(function(){
    		 	ctrl.places = null;
	    	}, 1000);	       
	    }

        HomeCtrl.prototype.formPageLoaded = function(val) {
            var ctrl = this;
            ctrl.CommonService.eventCreate(val + " Loaded");
        }

    /*
    * @@Method - submitInfoForm
    * @@Description -
    */
    HomeCtrl.prototype.submitInfoForm = function(events) {
      events = events || [];
      var ctrl = this;
      updateLeadIdAndTrustedForm(ctrl)
      if(events.length > 0){
        for (var i = 0; i < events.length; i++) {
          ctrl.CommonService.eventCreate(events[i]);
        }
      }
      this.solar_calc();
      this.ping(ctrl.form);
      if(ctrl.BASE_CONSTS.stateNames.indexOf(this.$state.current.name)==ctrl.BASE_CONSTS.stateNames.length - 2){
        this.callPost();
      }

      this.nextState();
    }

	    /*
		* @@Method - submitRentalForm
		* @@Description -
		*/
	    HomeCtrl.prototype.submitRentalForm = function(val) {
		    var ctrl = this;
		    updateLeadIdAndTrustedForm(ctrl);
		    this.solar_calc();
    		this.ping();
            ctrl.CommonService.eventCreate(ctrl.BASE_CONSTS.OWNERSHIP_COMPLETED);
            this.nextState(val);
	    }

	    /*
		* @@Method - submitRentalForm
		* @@Description -
		*/
	    HomeCtrl.prototype.submitPowerCompanyForm = function(val) {
		    var ctrl = this;
		    updateLeadIdAndTrustedForm(ctrl);
            this.solar_calc();
    		this.ping(val);
            ctrl.CommonService.eventCreate(ctrl.BASE_CONSTS.POWER_COMPANY_COMPLETED);
		    this.nextState(val);
	    }

	    /*
		* @@Method - submitAddressForm
		* @@Description -
		*/
	    HomeCtrl.prototype.submitAddressForm = function(val,isValidAddress, button_location, button_text) {
	    	var ctrl = this;
	    	updateLeadIdAndTrustedForm(ctrl);
	    	if(isValidAddress == true || isValidAddress == null){
	    	    this.solar_calc();
	    		this.ping(val,true);

	    		var button_info = {};
	    		if(angular.isDefined(button_location)) button_info['button_location'] = button_location;
	    		if(angular.isDefined(button_text)) button_info['button_text'] = button_text;

	    		ctrl.CommonService.eventCreate(ctrl.BASE_CONSTS.ADDRESS_COMPLETED, button_info);
	    		this.nextState(val,true)
	    		this.addrErr = false; 
    		}else{
	    		this.addrErr = true; 
	    	}	
	    }

        HomeCtrl.prototype.submitLandingPage = function(val , button_location, button_text) {

            var ctrl = this;
            updateLeadIdAndTrustedForm(ctrl);

            var button_info = {};
            if(angular.isDefined(button_location)) button_info['button_location'] = button_location;
            if(angular.isDefined(button_text)) button_info['button_text'] = button_text;

            ctrl.CommonService.eventCreate(ctrl.BASE_CONSTS.LANDING_COMPLETED, button_info)

            ctrl.nextState(val);
        }

	    /*
		* @@Method - submitNameForm
		* @@Description -
		*/
	    HomeCtrl.prototype.submitNameForm = function(val) {
	    	var ctrl = this;
	    	updateLeadIdAndTrustedForm(ctrl);
	    	if(this.form.first_name.length >= 2 && this.form.last_name.length >= 2){
    		    this.solar_calc();
	    		this.ping(val);
	    		ctrl.CommonService.eventCreate(ctrl.BASE_CONSTS.NAME_COMPLETED);
	    		this.nextState(val);
    		}else{
    			this.nameErr = true;
    		}
	    }

	    /*
		* @@Method - submitEmailForm
		* @@Description -
		*/
     	HomeCtrl.prototype.submitEmailForm = function(val) {
     		var ctrl = this;
     		updateLeadIdAndTrustedForm(ctrl);
	    	if(this.form.email && this.form.phone_home && !this.emailErr && !this.phoneErr){
    		  this.solar_calc();
	    		ctrl.CommonService.eventCreate(ctrl.BASE_CONSTS.EMAIL_COMPLETED);
	    		this.callPost();
	    		this.nextState(val);
	    	}else{
	    		return false;
	    	}
	    }

	    /*
		* @@Method - skip
		* @@Description - skip credit score page & redirect on next state
		*/
	    HomeCtrl.prototype.skip = function(val, formElements) {
	    	var ctrl = this;
	    	angular.forEach(formElements, function(value, key){
	    		ctrl.form[value] = null;
	    		ctrl.$localStorage[value] = null;
	    	});
	    	ctrl.nextState(val);
	    }

	    /*
		* @@Method - submitElectricFrom
		* @@Description -
		*/
	    HomeCtrl.prototype.submitElectricFrom = function(val) {
	    	var ctrl = this;
	    	updateLeadIdAndTrustedForm(ctrl);
		    this.solar_calc();
	    	this.ping(val);
            ctrl.CommonService.eventCreate(ctrl.BASE_CONSTS.POWER_BILL_COMPLETED);
    		this.nextState(val);
	    }


	    /*
		* @@Method - creditScoreForm
		* @@Description -
		*/
	    HomeCtrl.prototype.creditScoreForm = function(val) {
	    	var ctrl = this;
	    	updateLeadIdAndTrustedForm(ctrl);
		    this.solar_calc();
	    	this.ping(val);
            ctrl.CommonService.eventCreate(ctrl.BASE_CONSTS.CREDIT_SCORE_COMPLETED);
            this.nextState(val);
	    }

//      HomeCtrl.prototype.form_init = function() {
//        var ctrl = this;
//
//        this.form_ab_test();
//      }
//
//      HomeCtrl.prototype.form_ab_test = function(form) {
//        var ctrl = this;
//
//        var ab = {};
//        if(angular.isDefined(ctrl.$localStorage['ab'])){
//            ab = JSON.parse(ctrl.$localStorage['ab']);
//        }
//
//        if(angular.isDefined(ab['submit_logic'])){
//
//        }
//      }

      HomeCtrl.prototype.home_init = function() {
        var ctrl = this;
        this.CommonService.eventCreate(this.BASE_CONSTS.PAGE_LOADED);
        this.home_ab_test();
        var header = angular.element(".home_title").html()
        if (header != null) {
        	angular.element(".home_title").html(ctrl.process_str(header))
        }
        var section_two_header = angular.element("#compare_header").html()
        if (section_two_header != null) {
        	angular.element("#compare_header").html(ctrl.process_str(section_two_header))
        }
      }

      HomeCtrl.prototype.hero_image_path = function() {
        var ctrl = this;

        if(angular.isDefined(ctrl.ab['hero_image_path'])){
          ctrl.BASE_CONSTS.HERO_IMAGE = ctrl.BASE_CONSTS.CDN_URL + ctrl.ab['hero_image_path'];
        }
        return ctrl.BASE_CONSTS.HERO_IMAGE;
      }

      HomeCtrl.prototype.home_ab_test = function() {
        var ctrl = this;

        if(angular.isDefined(ctrl.ab['state_header']) && angular.isDefined(localStorage['maxmind_state'])){
            angular.element('.home_title').html(ctrl.ab['state_header'])
        }else if(angular.isDefined(ctrl.ab['default_header'])){
            angular.element(".home_title").html(ctrl.ab['default_header'])
        }

        if(angular.isDefined(ctrl.ab['full_landing']) & ctrl.ab['full_landing'] === "0"){
            ctrl.$scope.full_landing = false;
        }else{
          ctrl.$scope.full_landing = ctrl.BASE_CONSTS.FULL_LANDING;
        }

        if(angular.isDefined(ctrl.ab['main_cta'])){
            angular.element('.main-cta').html(ctrl.ab['main_cta'])
        }
      }

      HomeCtrl.prototype.process_str = function(str) {
        var ctrl = this;
        var new_str = str.replace('%s', localStorage['maxmind_state']).replace("%n", "<br />")
        if(angular.isDefined(target_city)){new_str = new_str.replace("%target_city",target_city)}
        if(angular.isDefined(target_brand)){new_str = new_str.replace("%target_brand",target_brand)}
        if(angular.isDefined(target_state)){new_str = new_str.replace("%target_state",target_state)}
        if(angular.isDefined(target_state_full)){new_str = new_str.replace("%full_target_state",target_state_full)}
        return new_str
      }

	    /*
		* @@Method - ping
		* @@Description - Call PING API & Store data in localstorage
		*/
	    HomeCtrl.prototype.ping = function(isFromHome) {

	    	var ctrl = this;
	    	var deferred = ctrl.$q.defer();
    		this.$rootScope.loading = true;
    		updateLeadIdAndTrustedForm(ctrl);

    		this.CommonService.ping(ctrl.form).then(function(response){
	    		deferred.resolve(response);
    		},function(err){
	    		deferred.reject(err);
    		});

    		return deferred.promise;	
	    };

        HomeCtrl.prototype.solar_calc = function() {

	    	var ctrl = this;
	    	var deferred = ctrl.$q.defer();
            var calc_visible = true;
            if(this.$state.current.name !== "home"){
                var scrollElement = "#sidebar";
                if(angular.element(scrollElement).is(':visible')){
                    calc_visible = true;
                }else{
                    calc_visible = false;
                }
            } else if (!angular.element('#section_4').length) {
            	calc_visible = false;
            }else{
                calc_visible = true;
            }

            if(calc_visible){
                this.$rootScope.loading = true;
                this.CommonService.solar_calc(ctrl.form).then(function(response){
                    ctrl.outputs = response.outputs;
                    ctrl.storeResponseInLocalstorage(response);
                    deferred.resolve(response);
                },function(err){
                    console.log(err)
                    deferred.reject(err);
                });
    		}else{
    		    deferred.resolve("No need to fire, no solar calculator");
    		}

    		return deferred.promise;
	    };

	    HomeCtrl.prototype.apply_logic = function(rule){
	      var ctrl = this;
        var next_step = jsonLogic.apply(rule,ctrl.form);
        if(next_step != null){
          next_step = JSON.parse(next_step)
          switch(next_step['op']){
            case "redirect":
              ctrl.$window.location.href = next_step['form'];
              break;
            case "form":
              return "form" + next_step['form'];
          }
        }
        return null;
	    }

      HomeCtrl.prototype.pageLoadLogic = function(loading_step) {
        var ctrl = this;

        if(angular.isDefined(ctrl.ab[loading_step + '_load_rule'])){
          var load_rule = JSON.parse(ctrl.ab[loading_step + '_load_rule'])
          return ctrl.apply_logic(load_rule) || loading_step;
        }else if(ctrl.BASE_CONSTS.loadRules.hasOwnProperty(loading_step)){
          var load_rule = ctrl.BASE_CONSTS.loadRules[loading_step];
          return ctrl.apply_logic(load_rule) || loading_step;
        }else{
          return loading_step;
        }
      }

      HomeCtrl.prototype.pageSubmitLogic = function(current, original_next_step) {
        var ctrl = this;

        if(angular.isDefined(ctrl.ab[current + '_submit_rule'])){
          var submit_rule = JSON.parse(ctrl.ab[current + '_submit_rule'])
          return ctrl.apply_logic(submit_rule) || original_next_step;
        }else if(ctrl.BASE_CONSTS.submitRules.hasOwnProperty(current)){
          var submit_rule = ctrl.BASE_CONSTS.submitRules[current];
          return ctrl.apply_logic(submit_rule) || original_next_step;
        }else{
          return original_next_step;
        }
      }
	    /*
		* @@Method - nextState
		* @@Description - Redirect on next state
		*/
	    HomeCtrl.prototype.nextState = function(val, isFromHome) {
	    	var ctrl = this;
	    	ctrl.$localStorage.formProgression += 1;
	    	/*if(ctrl.form.address){
	    		this.$localStorage.progressBarWidth = this.$localStorage.progressBarWidth + (100/(ctrl.BASE_CONSTS.STATE_COUNT-1));
    			this.$rootScope.progressBarWidth = this.$localStorage.progressBarWidth;
	    	}*/

	    	var proceed = function(ctrl,val){
          //Push to gtm dataLayer

          var step = ctrl.$state.get(val).url;

          if(step.indexOf("index") !== -1){
            step = "lp";
          } else if (step.indexOf("thanks") !== -1) {
            step = "ty";
          } else {
            try{
              var regex = /\/step\/(\d)+/g;
              var matches, output = [];
              while (matches = regex.exec(step)) {
                  output.push(matches[1]);
              }
              if(output.length > 0){
                step = output[0];
              }
            }catch(ignore){}
          }

          dataLayer.push({
            'previous_form': ctrl.$state.current.name,
            'current_form': val,
            'step': step
          })

          ctrl.$localStorage.loadedStates = (angular.isDefined(ctrl.$localStorage.loadedStates)) ? ctrl.$localStorage.loadedStates : [];

          ctrl.$localStorage.loadedStates.push(val);
          //	    	if(isFromHome){
          //	    		this.$localStorage.loadedStates.push("form1");
          //	    	}
          ctrl.flags.attemptedToContinue = false;
          ctrl.$timeout(function(){
              ctrl.$state.go(val);
          }, 200);
	    	}
	    	var val = ctrl.getNextState();
	    	val = ctrl.pageSubmitLogic(ctrl.$state.current.name, val);
        ctrl.ab_test(val).then(
          function(resp){
            val = ctrl.pageLoadLogic(val);
            proceed(ctrl,val);
          },
          function(err){
            val = ctrl.pageLoadLogic(val);
            proceed(ctrl,val);
          }
        )
	    }


	    /*
		* @@Method - prevState
		* @@Description - Redirect on previous state
		*/
	    HomeCtrl.prototype.prevState = function(val, width) {
	    	var ctrl = this;
        ctrl.$localStorage.formProgression -= 1;
        ctrl.solar_calc();
		    //ctrl.reduceProgressBarWidth(width);
	    	/*if (ctrl.$state.$current.name == _.first(ctrl.$localStorage.loadedStates)) {
		        ctrl.$state.go("home");
	    	}else{
		        ctrl.$state.go(val);
	    	}*/
	    	var val = ctrl.getPrevState();
	    	ctrl.ab_test(val);
	    	ctrl.$state.go(val);
	    }

	    HomeCtrl.prototype.ab_test = function(val) {
	      var ctrl = this;
	      var deferred = ctrl.$q.defer();
	    	ctrl.abTestService.ab_test(val,directory).then(
           function(response){
               deferred.resolve(response);
           },
           function(err){
               console.log(err);
               deferred.reject(err);
           });
        return deferred.promise;
	    }


	     /*
		* @@Method - reduceProgressBarWidth
		* @@Description - Reduce progress bar width on click on redirect on previous state
		*/
	    /*HomeCtrl.prototype.reduceProgressBarWidth = function(width) {
	    	var ctrl = this;
	    	if(width){
	    		this.$localStorage.progressBarWidth = this.$localStorage.progressBarWidth - (100/(ctrl.BASE_CONSTS.STATE_COUNT-1)) - (100/(ctrl.BASE_CONSTS.STATE_COUNT-1));
	    	}else{
	    		this.$localStorage.progressBarWidth = this.$localStorage.progressBarWidth - (100/(ctrl.BASE_CONSTS.STATE_COUNT-1));
	    	}
    		
    		this.$rootScope.progressBarWidth = this.$localStorage.progressBarWidth;
	    }*/

		/*
		* @@Method - companyValidator
		* @@Description - Address field validation
		*/
	    HomeCtrl.prototype.companyValidator = function(e){
	    	if(e.keyCode != 13){
	          this.IsCompanyChange = false;
	        }
	  	}

	 	/*
		* @@Method - onCompanySelect
		* @@Description - Address field validation
		*/
	    HomeCtrl.prototype.onCompanySelect = function(e){
    	 	if(this.form.electric_company){
	        	this.IsCompanyChange = true;
	        }
	  	}
		
		/*
		* @@Method - checkValidEmail
		* @@Description - checking email validation
		*/
	    HomeCtrl.prototype.checkValidPhone = function(phone_home){
	    	var ctrl = this;
	    	ctrl.phoneVerify = false;
	    	if(phone_home){
	    		ctrl.CommonService.validatePhone(phone_home).then(function(response){	    			
	    			ctrl.phoneVerify = true;
	    		},function(err){
	    			ctrl.phoneVerify = false;
	    		});
	    	}else{
	    		ctrl.phoneVerify = false;
	    	}			
	  	}

	  	/*
		* @@Method - resetForms
		* @@Description - Reset all forms
		*/
	    HomeCtrl.prototype.resetForms = function() {
	    	var ctrl = this;

	    	this.opts = {
		       	backdrop: true,
		        backdropClick: true,
		        dialogFade: false,
		        keyboard: true,
		        templateUrl : '../views/forms/resetForms.html',
		        controller : resetFormsCtrl,
		        resolve: {} // empty storage
	        };
        
          var modalResetInstance = this.$modal.open(ctrl.opts);
          
          modalResetInstance.result.then(function(){
            ctrl.form = {};
            ctrl.$localStorage.progressBarWidth = 0;
            ctrl.$rootScope.progressBarWidth = 0;
            ctrl.initialize();
            ctrl.$timeout(function(){
              ctrl.$localStorage.selectedAnalytic = (angular.isDefined(ctrl.$localStorage.selectedAnalytic)) ? ctrl.$localStorage.selectedAnalytic : 'CURRENCY';
              var home_ind = ctrl.BASE_CONSTS.stateNames.indexOf('home')
              ctrl.$state.go(ctrl.BASE_CONSTS.stateNames[home_ind+1]);
            }, 100);
          	},function(){
            	//on cancel button press
            	console.log("Reset Modal Closed");
          	});
	   	};

	   	/*
		* @@Method - replaceSpinner
		* @@Description - Get list of electric companies
		*/
	    HomeCtrl.prototype.replaceSpinner = function() {
	    	var ctrl = this;
	    	var timer = 1000;
	    	ctrl.$timeout(function(){
	    		ctrl.rotateSpinner_1 = false;
	    		ctrl.$timeout(function(){
		    		ctrl.rotateSpinner_2 = false;	
		    		ctrl.$timeout(function(){
			    		ctrl.rotateSpinner_3 = false;	
			    		ctrl.nextStateName = ctrl.getNextState();
		    			ctrl.nextState(ctrl.nextStateName);
			    	}, timer);
		    	}, timer);
	    	}, timer);
	   	};

	   	
	   	/*
		* @@Method - selectedAnalytics
		* @@Description -
		*/
	   	HomeCtrl.prototype.selectedAnalytics = function(selectedAnalytic) {
	   		var ctrl = this;
	   		ctrl.$localStorage.selectedAnalytic = selectedAnalytic;
	   	};

	   	HomeCtrl.prototype.getListings = function() {
	   		var ctrl = this;
	   		
	   		//Need the two top tiles to display by default
	   		ctrl.userInfo = [
          {"desc":"It uses federal data on solar yield, typical costs, and applicable rebates and incentives to project your savings. When you get competitive quotes, your savings can be even greater.",
            "image_url":"/images/thanks-001.png",
            "section_type":"Email",
            "title":"We just emailed your solar savings report."},
          {"desc":"We’ve selected 3 reputable installers that will provide more info and customized quotes. Comparing multiple offers yields the best results. In the meantime, see options below.",
            "image_url":"/images/thanks-002.png",
            "section_type":"Phone",
            "title":"We’ll be calling shortly with free quotes."}
        ];

	   		var lead_id_listener = ctrl.$scope.$watch("ctrl.$localStorage.lead_id", function() {
	   			if(typeof ctrl.$scope.ctrl.$localStorage.lead_id !== 'undefined' && ctrl.$scope.ctrl.$localStorage.lead_id != null) {
	   				ctrl.CommonService.getListings(ctrl.$scope.ctrl.$localStorage.lead_id, function(response){
	   					ctrl.$scope.$apply(function() {
	   						ctrl.list = response.listing_id;
	   						ctrl.userInfo = response.user_info;
              });
	   				},function (err) {
	   					console.log(err);
	   				});
	   			
	   				lead_id_listener();
	   			}
	   		});
	   		
	   	}

	   	/*
		* @@Method - callPost
		* @@Description -
		*/
	   	HomeCtrl.prototype.callPost = function() {
	   		console.log("Call Post API...");
	   		var ctrl = this;
	   		var deferred = ctrl.$q.defer();
	   		
	   		ctrl.CommonService.post(ctrl.form).then(function(resp){
	   		    ctrl.CommonService.eventCreate(ctrl.BASE_CONSTS.FORM_COMPLETE);
	   		    ctrl.$localStorage.lead_id = resp.lead_id;
	   		},function(err){
	   			console.log(err);
	   			deferred.reject(err);
	   		})

	   		return deferred.promise;
	   	};

	   	// Update email & phone controller
	   	var ModalInstanceCtrl = function($scope, $uibModalInstance, CommonService, item) {
	     	$scope.item = item;
	     	$scope.phoneErr = false;
	     	$scope.emailErr = false;
	     	$scope.phoneVerify = false;

	     	$scope.$watch(function(){
			    return item.phone_home;
			}, function(newVal, oldVal){
			   	$scope.checkValidPhone(item.phone_home);
			});

		    $scope.checkValidEmail = function(email){
		    	if(email){
	    			var re = /^(([^<>()[\]\\.,;:\s@@\"]+(\.[^<>()[\]\\.,;:\s@@\"]+)*)|(\".+\"))@@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
					if(re.test(email)){
						$scope.emailErr = false;
					}else{
						$scope.emailErr = true;
					}
		    	}else{
		    		$scope.emailErr = false;
		    	}	    		
		  	}
		    $scope.checkValidPhone = function(phone_home){
		    	if(phone_home){
		    		CommonService.validatePhone(phone_home).then(function(response){	
		    			$scope.phoneErr = false;
		    			$scope.phoneVerify = true;	
		    		},function(err){
		    			$scope.phoneErr = true;
		    			$scope.phoneVerify = false;	
		    		});
		    	}else{
		    		$scope.phoneErr = false;
		    	}				
		  	}
	      	$scope.ok = function () {
		        $uibModalInstance.close($scope.item);
	      	};
	      	$scope.cancel = function () {
		        $uibModalInstance.dismiss('cancel');
	      	};
		}

		// Reset form controller
		var resetFormsCtrl = function($scope, $uibModalInstance) {
	      	$scope.ok = function () {
		        $uibModalInstance.close();
	      	};
	      	$scope.cancel = function () {
		        $uibModalInstance.dismiss('cancel');
	      	};
		}
    
		/**
		 * updateLeadIdAndTrustedForm
		 *
		 * This function performs the necessary updates to interact with Jornaya's LeadID product. Because Angular does
		 * not support 2-way binding on hidden fields and the LeadID API (loaded on index.scala.html) is pre-set to
		 * update a hidden field with the id=leadid_token, the Angular model must be updated manually.
		 *
		 */
		var updateLeadIdAndTrustedForm = function(ctrl) {
			ctrl.form[ctrl.BASE_CONSTS.LEADID_TOKEN] = ctrl.$document.find('input[name="'+ctrl.BASE_CONSTS.LEADID_TOKEN+'"]').slice(-1)[0].value || ctrl.form[ctrl.BASE_CONSTS.LEADID_TOKEN];
			ctrl.form[ctrl.BASE_CONSTS.TRUSTEDFORM_TOKEN] = ctrl.$document.find('input[name="'+ctrl.BASE_CONSTS.TRUSTEDFORM_TOKEN+'"]').slice(-1)[0].value || ctrl.form[ctrl.BASE_CONSTS.TRUSTEDFORM_TOKEN];
			ctrl.form[ctrl.BASE_CONSTS.TRUSTEDFORM_CERT] = ctrl.$document.find('input[name="'+ctrl.BASE_CONSTS.TRUSTEDFORM_CERT+'"]').slice(-1)[0].value || ctrl.form[ctrl.BASE_CONSTS.TRUSTEDFORM_CERT];
		}
})();
