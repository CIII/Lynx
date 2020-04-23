@(landingSettings: utils.templates.LandingSettings, siteSettings: utils.templates.SiteSettings)(implicit configuration: play.api.Configuration, environment: play.api.Environment)
@import utils.utilities.{construct_url,construct_static_url}
(function(){
   'use strict';

   var SERVER_URL = window.location.protocol + "//" + window.location.host;
   var ORIGINAL_QUERY = {};
   var $urlRouterProviderRef = null;
   var $stateProviderRef = null;
   var STATE_COUNT = 0;

   // States default name array
   var stateNamesArr = []
   /*
   * @@App - solar
   * @@Description - Module initialization
   */
   angular.module('solar',[
      "ngAnimate",
      "ui.router",
      "ui.bootstrap",
      "720kb.socialshare",
      "ngMap",
      "ngStorage",
      "angular-underscore",
      "oitozero.ngSweetAlert",
      "anim-in-out",
      "googlechart",
      'ui.mask',
      'angucomplete-alt',
      'ngSanitize',
      'google.places'
   ])
   .config(Config)
   .run(Run)
   .controller('MainCtrl',MainCtrl)
   .constant('BASE_CONSTS',{ 
      'SERVER_URL': SERVER_URL,
      'STATE_COUNT': STATE_COUNT,
      'ORIGINAL_QUERY': ORIGINAL_QUERY,
      'FULL_LANDING': full_landing,
      'CDN_URL': cdn_url,
      'SITE_NAME': site_name,
      'US_STATES': [ "AK",
"AL",
"AR",
"AS",
"AZ",
"CA",
"CO",
"CT",
"DC",
"DE",
"FL",
"GA",
"GU",
"HI",
"IA",
"ID",
"IL",
"IN",
"KS",
"KY",
"LA",
"MA",
"MD",
"ME",
"MI",
"MN",
"MO",
"MS",
"MT",
"NC",
"ND",
"NE",
"NH",
"NJ",
"NM",
"NV",
"NY",
"OH",
"OK",
"OR",
"PA",
"PR",
"RI",
"SC",
"SD",
"TN",
"TX",
"UT",
"VA",
"VI",
"VT",
"WA",
"WI",
"WV",
"WY"],
      'HERO_IMAGE': "@construct_url(utils.templates.TemplateSettings.get_hero_image_path(landingSettings))",
      'DEFAULT_HEADER': "@utils.templates.TemplateSettings.get_default_header(landingSettings)",
      'PAGE_LOADED' : "Page loaded",
      'FORM_STEP1' : "Form Step 1",
      'FORM_STEP2' : "Form Step 2",
      'FORM_STEP3' : "Form Step 3",
      'FORM_STEP4' : "Form Step 4",
      'FORM_STEP5' : "Form Step 5",
      'FORM_STEP6' : "Form Step 6",
      'FORM_STEP7' : "Form Step 7",
      'FORM_STEP8' : "Form Step 8",
      'FORM_STEP_LOADED' : 'Form Step Loaded',
      'LP_CTC' : "LP CTC",
      'PAGE_CLOSED': "Page Closed",
      'MAXMIND_FAIL' : "Maxmind failure on page load",
      'LANDING_COMPLETED' : "Landing Page Completed",
      'ADDRESS_COMPLETED' : "Address Completed",
      'OWNERSHIP_COMPLETED' : "Ownership Completed",
      'POWER_BILL_COMPLETED' : "Power Bill Completed",
      'POWER_COMPANY_COMPLETED' : "Power Company Completed",
      'NAME_COMPLETED' : "Name Completed",
      'EMAIL_COMPLETED' : "Email Completed",
      'PHONE_COMPLETED' : "Phone Completed",
      'EMAIL_MODAL_COMPLETED' : "Email Modal Completed",
      'CREDIT_SCORE_COMPLETED' : "Credit Score Completed",
      'FORM_COMPLETE' : "Form Complete",
      'PAGE_BLUR' : "Page Blur",
      'PAGE_FOCUS' : "Page Focus",
      'PAGE_SCROLL' : "Page Scroll",
      'PAGE_MOUSE_MOVEMENT': "Page Mouse Movement",
      'ZIP_LENGTH' : 5,
      'MAP_KEY' : "AIzaSyD6bFBQXlwD2d2lpb0zVyku2nhdhR9nD3A",
      'LOCATION_RADIUS' : 10000,
      'TRUSTEDFORM_TOKEN': trusted_form_token_name,
      'TRUSTEDFORM_CERT': trusted_form_cert_name,
      'SOLAR_CALCULATOR_TOTAL_YEARS': 25,
      'LEADID_TOKEN': "leadid_token"
   });

   /*
   * @@App Configuration
   * @@Description
   * @@property {Array} Config.$inject This injects the paramters properly for minimization.
   */

   Config.$inject = ['$stateProvider', '$urlRouterProvider', '$locationProvider', '$sceDelegateProvider'];

   function Config($stateProvider, $urlRouterProvider, $locationProvider, $sceDelegateProvider) {
      $stateProviderRef = $stateProvider;
      $urlRouterProviderRef = $urlRouterProvider;
      $sceDelegateProvider = $sceDelegateProvider;
      $sceDelegateProvider.resourceUrlWhitelist([
              'self',
              "@configuration.getString("cloudfront.cdn.url")**"
          ]);

      $stateProvider
         .state('home', {
             views: {
                'header': {
                    "templateUrl": "@construct_static_url("views/header/header.html",siteSettings)",
                    "controller" : "HomeCtrl",
                    "controllerAs": "ctrl",
                    "cache" : true,
                    },
                  form: {
                     templateUrl: "@construct_static_url("views/home/home.html",siteSettings)",
                     controller: 'HomeCtrl',
                     cache : false,
                     controllerAs:'ctrl',
                     resolve: {
                        progressBarWidth: function($rootScope, $localStorage){
                           $rootScope.progressBarWidth = 0;
                           $localStorage.progressBarWidth = $rootScope.progressBarWidth;
                           return true;
                        }
                     }
                  }
                },
            url: '/index.html',
      });
      stateNamesArr.push('home');
      $urlRouterProvider.deferIntercept();
      $urlRouterProvider.otherwise('/index.html');

      //$locationProvider.html5Mode(true);
   }

   Run.$inject = ['$state', '$rootScope', '$timeout', "$q", "$http", "BASE_CONSTS", "$urlRouter", "$location", "$localStorage", 'CommonService', 'abTestService'];

   function Run($state, $rootScope, $timeout, $q, $http, BASE_CONSTS, $urlRouter, $location, $localStorage, CommonService, abTestService){
        try{
            if(window.location.search.length > 0){
                ORIGINAL_QUERY = JSON.parse('{"' + decodeURI(window.location.search.replace(/&/g, "\",\"").replace(/=/g,"\":\"").replace('?',"")) + '"}');
                BASE_CONSTS.ORIGINAL_QUERY = ORIGINAL_QUERY;
            }
        }catch(e){}

        function loadStates(sequence){
         var count = 0;
         var totalLength = sequence.length;
         angular.forEach(sequence, function (value, key){
            var formUrl = "@construct_url(utils.templates.TemplateSettings.get_site_name(siteSettings)+"/static/views/forms/form<value>.html")";
            var state = {
                "views": {
                'header': {
                    "templateUrl": "@construct_static_url("views/header/header.html",siteSettings)",
                    "controller" : "HomeCtrl",
                    "controllerAs": "ctrl",
                    "cache" : true,
                    },
                    @if(utils.templates.TemplateSettings.show_solar_cal(siteSettings)) {
                    	'sidebar': {
                            "templateUrl": "@construct_url(utils.templates.TemplateSettings.get_site_name(siteSettings)+"/static/views/sidebar/sidebar.html")",
                            "controller" : "HomeCtrl",
                            "controllerAs": "ctrl",
                            "cache" : true,
                        },	
                    }
                  'form': {
                     "templateUrl": formUrl.replace("<value>",value),
                     "controller" : "HomeCtrl",
                     "controllerAs": "ctrl",
                     "cache" : false,
                     "resolve": {
                        progressBarWidth: function($rootScope, $localStorage){
                           var width = 100/(totalLength-1);
                           $rootScope.progressBarWidth = width*(key+1);
                           $localStorage.progressBarWidth = $rootScope.progressBarWidth;
                           return true;
                        }
                     }
                  }
                },
               "url": (value == 9) ? "/thanks" : "/step/"+(key+1)
            };
            var state_name = (value == 9) ? "thanks" : 'form'+value;
            $stateProviderRef.state(state_name, state);
            stateNamesArr.push(state_name)
            count++;
         });
         BASE_CONSTS.stateNames = stateNamesArr
         BASE_CONSTS.STATE_COUNT = count;
         $urlRouter.sync();
         $urlRouter.listen();
        }

        var desiredParams = ['sequence']
        abTestService.ab_test(null, directory, desiredParams, BASE_CONSTS.ORIGINAL_QUERY).then(
            function(response){
                var sequence = [];
                if(angular.isDefined(abTestService.ab['sequence'])){
                    for (var i = 0, len = abTestService.ab.sequence.length; i < len; i++) {
                        sequence.push(abTestService.ab.sequence[i] | 0)
                    }
                }else{
                  if(BASE_CONSTS.FULL_LANDING){
                    sequence = [@(utils.templates.TemplateSettings.get_full_form_sequence(siteSettings).mkString(","))]
                  }else{
                    sequence = [@(utils.templates.TemplateSettings.get_short_form_sequence(siteSettings).mkString(","))]
                  }
                }
                BASE_CONSTS.form_sequence = sequence;
                loadStates(sequence);
            },function(error){
                var sequence = [];
                if(BASE_CONSTS.FULL_LANDING){
                  sequence = [@(utils.templates.TemplateSettings.get_full_form_sequence(siteSettings).mkString(","))]
                }else{
                  sequence = [@(utils.templates.TemplateSettings.get_short_form_sequence(siteSettings).mkString(","))]
                }
                BASE_CONSTS.form_sequence = sequence;
                loadStates(sequence);
            }
        )

        abTestService.ab_test('home',directory, null, BASE_CONSTS.ORIGINAL_QUERY).then(
            function(response){
                BASE_CONSTS.home_queried = true;
            },
            function(err){
                BASE_CONSTS.home_queried = true;
                console.log(err);
            });

        // States redirection dynamically
        $localStorage.loadedStates = (angular.isDefined($localStorage.loadedStates) && $localStorage.loadedStates.length > 0) ? $localStorage.loadedStates : [];

        $rootScope.$on('$stateChangeSuccess', function(e, toState, toParams, fromState, fromParams) {
          if(toState.name == 'home'){
            $state.go(toState.name);
          }else if(toState.name == 'thanks'){
            $state.go(toState.name);
          }else{
            var val = _.contains($localStorage.loadedStates, toState.name);
            if(!val && angular.isDefined($localStorage.loadedStates) && angular.isDefined(_.last($localStorage.loadedStates))){
               $state.go(_.last($localStorage.loadedStates));
            }else if(!val && !angular.isDefined(_.last($localStorage.loadedStates))){
               var home_ind = BASE_CONSTS.stateNames.indexOf('home')
               $state.go(BASE_CONSTS.stateNames[home_ind+1]);
            }
          }
        });
   }

    MainCtrl.prototype.initializeSolarCalcValues = function(){
      try{
       localStorage.setItem('ngStorage-prevLoanTotalSavings', 0);
       localStorage.setItem('ngStorage-prevLoanSavingsPercentage', 0);
       localStorage.setItem('ngStorage-prevLoanFirstYearSaving', 0);
       localStorage.setItem('ngStorage-prevLoanYear1SavingsPercentage', 0);
      }catch(err){
        console.error("local storage not available")
      }
    }

   /*
   * @@Controller - MainCtrl
   * @@Description
   * @@property {Array} MainCtrl.$inject This injects the paramters properly for minimization.
   */
   MainCtrl.$inject = ['$rootScope', '$scope', '$window', '$state', '$timeout', 'BASE_CONSTS', "Socialshare","$uibModal", "$filter", "$localStorage", "CommonService", "formService", "abTestService", '$location'];
   
   function MainCtrl($rootScope, $scope, $window, $state, $timeout, BASE_CONSTS, Socialshare,$uibModal, $filter, $localStorage, CommonService, formService, abTestService, $location){
        var ctrl = this;
        this.CommonService = CommonService;
        this.formService = formService;
        ctrl.form = formService.form;
        this.$localStorage = $localStorage;
        this.flags = ctrl.CommonService.flags;
        $rootScope.stateName = "home";
        $rootScope.scrollWidth = 0;
        $rootScope.loading = false;
        $rootScope.purchaseType = 'LOAN';
        $rootScope.$localStorage = $localStorage;
        $rootScope.formService = formService;

        //ctrl.$localStorage.$reset();

        this.initializeSolarCalcValues();

        //BASE_CONSTS.loadRules = {"form1":{"if":[{"missing":"address"},undefined,'{"op":"form","step":"4"}']}};
        BASE_CONSTS.loadRules = @JavaScript(utils.templates.TemplateSettings.get_load_rules(siteSettings))
        BASE_CONSTS.submitRules = @JavaScript(utils.templates.TemplateSettings.get_submit_rules(siteSettings))
        BASE_CONSTS.topSuppliers = ["National Grid","Eversource/NSTAR","Other"]

        try{
          var saving = $localStorage['loanTotalSavings'];
          saving = saving/BASE_CONSTS.SOLAR_CALCULATOR_TOTAL_YEARS;
          saving = $filter('number')(saving, 2);
          $rootScope.saving = saving;
        }catch(err){
          $rootScope.saving = 0.00;
        }

        $localStorage.selectedAnalytic = (angular.isDefined($localStorage.selectedAnalytic)) ? $localStorage.selectedAnalytic : 'CURRENCY';

        $rootScope.twitterText = "I'll pay $"+$rootScope.saving+" less a year with #solar. You can too. @@easiersolar calc took 30sec, best thing I’ve done all day.";
        $rootScope.fbText = "My power costs will drop $"+$rootScope.saving+" a year with solar. Yours can to. EasierSolar’s calculator took me 30 seconds, best thing I’ve done all day. Get personalized results fast. Compare local and national installers on your terms. Unbiased info, skilled craftsman, and the biggest change you’ll make in the fight against climate change.";
        /*
        * @@Method - social_share
        * @@Description
        */
        $rootScope.social_share = function(provider) {
            var saving = 0;
                if($rootScope.purchaseType == 'PPA'){
                     saving = $localStorage['ppaTotalSavings'];
                }else if($rootScope.purchaseType == 'CASH'){
                     saving = $localStorage['cashTotalSavings'];
                }else if($rootScope.purchaseType == 'LOAN'){
                     saving = $localStorage['loanTotalSavings'];
                }
            saving = saving/BASE_CONSTS.SOLAR_CALCULATOR_TOTAL_YEARS;
            saving = $filter('number')(saving, 2);
            $rootScope.saving = saving;

            $rootScope.twitterText = "I'll pay $"+$rootScope.saving+" less a year with #solar. You can too. @@easiersolar calc took 30sec, best thing I’ve done all day.";
            $rootScope.fbText = "My power costs will drop $"+$rootScope.saving+" a year with solar. Yours can to. EasierSolar’s calculator took me 30 seconds, best thing I’ve done all day. Get personalized results fast. Compare local and national installers on your terms. Unbiased info, skilled craftsman, and the biggest change you’ll make in the fight against climate change.";
            if(provider == 'facebook'){
                Socialshare.share({
                    'provider': provider,
                    'attrs': {
                       "socialshareType":"feed",
                       "socialshareVia":'145634995501895',
                       "socialsharePopupHeight":400,
                       "socialsharePopupWidth":600,
                       "socialshareText":$rootScope.fbText,
                       "socialshareUrl":BASE_CONSTS.SERVER_URL
                    }
                });
            }else{
                Socialshare.share({
                    'provider': provider,
                    'attrs': {
                        "socialshareType":"feed",
                        "socialsharePopupHeight":400,
                        "socialsharePopupWidth":600,
                        "socialshareText":$rootScope.twitterText,
                        "socialshareUrl":BASE_CONSTS.SERVER_URL
                    }
                });
            }
        };

      /*
      * @@Method - sendEmail
      * @@Description -  Send email popup
      */
      $rootScope.sendEmail = function() {        
         $scope.opts = {
            backdrop: true,
            backdropClick: true,
            dialogFade: false,
            keyboard: true,
            templateUrl : '/assets/app/views/forms/sendEmailPopup.html',
            controller : sendEmailCtrl,
            resolve: {} // empty storage
         };
     
         var modalInstance = $uibModal.open($scope.opts);
         modalInstance.result.then(function(response){
            
         },function(){
            //on cancel button press
            console.log("Modal Closed");
         });
      };

      var sendEmailCtrl = function($scope, $uibModalInstance) {
         $scope.ok = function () {
           $uibModalInstance.close();
         };

         $scope.cancel = function () {
           $uibModalInstance.dismiss('cancel');
         };
      }

      //Fire event for when user closes the page
      var pageCloseEvent = function () {
          var browser_id = $localStorage.browser_id;
          if(angular.isDefined(browser_id)){
              if(browser_id == null || browser_id.trim().length <= 0){
                  browser_id = browserId;
              }
          }else{
              browser_id = browserId;
          }
          var data = {
              event:BASE_CONSTS.PAGE_CLOSED,
              browser_id:browser_id,
              request_url: $location.absUrl()}

          $.ajax({
            type: "POST",
            url: BASE_CONSTS.SERVER_URL+"/event/create",
            contentType: "application/x-www-form-urlencoded",
            data: $.param(data),
            async: false
          })
      };

      $window.onbeforeunload = function(){
        $localStorage.ab = "{}";
        pageCloseEvent();
      }
      
			$scope.$watch(function () { return $localStorage.zip },function(newVal,oldVal){
        if(angular.isDefined($localStorage.state) && $localStorage.state == "MA"){
          CommonService.bestGuessSupplier($localStorage['zip']).then(
            function(resp){
              BASE_CONSTS.topSuppliers[2] = resp;
              BASE_CONSTS.topSuppliers[3] = 'Other';
            },
            function(err){
              console.log("unsuccessful")
            }
          )
        }
      })

      $scope.$watch(function() {return $localStorage.property_ownership}, function(newVal, oldVal) {
        if(angular.isDefined($localStorage.property_ownership)){
          dataLayer.push({"property_ownership":newVal});
        }
      })

      $scope.$on('g-places-autocomplete:select', function (event, param) {
        ctrl.flags.isPlaceChange = true;
        ctrl.formService.validateGoogleAddress(param);
      });

      dataLayer.push({"current_form":"home", "step":"lp"})

      ctrl.getMyLocationDetails();
      ctrl.getLeadInfo();
   }

   MainCtrl.prototype.eventCreate = function(event){
        var ctrl = this;
        ctrl.CommonService.eventCreate(event);
   }

   MainCtrl.prototype.getMyLocationDetails = function(){
        var ctrl = this;
        ctrl.CommonService.getMyLocationDetails().then(function(resp){
          ctrl.formService.persistMaxmindValues(resp);
        },function(err){
            ctrl.CommonService.eventCreate(ctrl.BASE_CONSTS.MAXMIND_FAIL);
        });
    }

    MainCtrl.prototype.getLeadInfo = function(){
      var ctrl = this;
      ctrl.CommonService.getLeadInfo().then(
        function(response){
          ctrl.$localStorage.lead_id = response;
        },
        function(err){
          //ignore
        }
      )
    }

})();
