(function(){
   'use strict';

   var $urlRouterProviderRef = null;
   var $stateProviderRef = null;

   /*
   * @@App - config
   * @@Description - Module initialization
   */
   angular.module('lynxConfig',[
      "ui.router",
      "ui.bootstrap",
      'ngDialog'
   ])
   .config(Config)
   .run(Run)
   .controller('MainCtrl',MainCtrl);

   /*
   * @@App Configuration
   * @@Description
   * @@property {Array} Config.$inject This injects the paramters properly for minimization.
   */

   Config.$inject = ['$stateProvider', '$urlRouterProvider', '$locationProvider', '$sceDelegateProvider'];
   function Config($stateProvider, $urlRouterProvider, $locationProvider, $sceDelegateProvider) {
      $stateProviderRef = $stateProvider;
      $urlRouterProviderRef = $urlRouterProvider;
      $sceDelegateProvider.resourceUrlWhitelist([
              'self',
              cdn_url + "**"
          ]);
      console.log( cdn_url + "conf_ui/static/config.html")
      $stateProvider
        .state('config', {
          views: {
            "function": {
              templateUrl: cdn_url + "conf_ui/static/config.html",
              controller: "ConfigCtrl",
              controllerAs: "ctrl",
              cache : true
            }
          },
          url: '/config',
        }).state(
          'home',
          { views: {
            "function": {
              templateUrl: cdn_url + "conf_ui/static/home.html",
              controller: "MainCtrl",
              controllerAs: "ctrl",
              cache : true
            }
          },
          url: '/home',
        }).state(
          'ab',
          { views: {
            "function": {
              templateUrl: cdn_url + "conf_ui/static/ab.html",
              controller: "ABCtrl",
              controllerAs: "ctrl",
              cache : true
            }
          },
          url: '/ab',
        });

      $urlRouterProvider.otherwise('/home');
     }

   Run.$inject = ["lynxConfigService"];
   function Run(lynxConfigService){
    lynxConfigService.load_current_configurations();
   }
   
   MainCtrl.$inject = ['$state'];
   function MainCtrl($state){
    this.$state = $state;
   }

   MainCtrl.prototype.loadState = function(state){
    console.log(state)
    this.$state.go(state);
   }


  angular.module('lynxConfig').controller('ConfigCtrl', ConfigCtrl);

   /*
   * @@Controller - ConfigCtrl
   * @@Description
   * @@property {Array} ConfigCtrl.$inject This injects the paramters properly for minimization.
   */
  ConfigCtrl.$inject = ["lynxConfigService", "$scope", "$http", "$interval", "$q", "$timeout", "$window"];
  function ConfigCtrl(lynxConfigService, $scope, $http, $interval, $q, $timeout, $window){

    var ctrl = this;
    this.lynxConfigService = lynxConfigService;
    this.$scope = $scope;
    this.$http = $http;
    this.$interval = $interval;
    this.$q = $q;
    this.$timeout = $timeout;
    this.$window = $window;
    this.configurations = this.lynxConfigService.configurations;
    this.homepage_configuration = {};
    this.existing_urls = this.lynxConfigService.existing_urls;
    this.CONSTANTS = ["capital_domain", "domain", "url"]
    this.cdn_url = cdn_url;
    this.current_url = {};
    this.current_configuration = {};
    $scope.current_configuration = this.current_configuration;
    this.show_image={};
    this.conf_var_types = {};
    this.homepage_keys = [];
    this.flags = {
      is_flushing: false,
      is_invalidating: false,
      is_generating_preview: false,
      preview_generated: false
    };
    this.available_images = [];
    getAvailableImages(this.$http, this.available_images);
    this.preview_url = "/preview";
    $scope.flags = this.flags;
    this.pending_invalidations = [];
    $scope.pending_invalidations = this.pending_invalidations;
    // used to check on invalidation
    function checkInvalidation() {
      if(ctrl.pending_invalidations.length > 0){
        ctrl.$http({
          method: 'GET',
          url: "/pages/invalidate-status",
          params: {invalidations: ctrl.pending_invalidations}
        }).then(function successCallback(response) {
          var data = response.data;
          var keys = Object.keys(data);
          for(var i=0; i<keys.length; i++){
            var key = keys[i];
            if(data[key]){
              ctrl.pending_invalidations.splice(ctrl.pending_invalidations.indexOf(key), 1);
            }
          }

          if(ctrl.pending_invalidations.length <= 0){
            ctrl.flags.is_invalidating = false;
            if(ctrl.flags.is_generating_preview){
              ctrl.flags.is_generating_preview = false;
              ctrl.flags.preview_generated = true;
              ctrl.preview_url = ctrl.configurations["lynx"]["domains"][ctrl.current_url.domain]["home_page"]["url"]
              if(subdomain.length > 0){
                ctrl.preview_url = "http://" + subdomain + "." + ctrl.preview_url + "/preview";
              }else{
                ctrl.preview_url = "http://" + ctrl.preview_url + "/preview"
              }
              ctrl.$window.open(ctrl.preview_url,"_blank")
            }
          }
        }, function errorCallback(response) {
          console.log("Could not check invalidations")
        });
      }
    }

    ctrl.stop_invalidation_check = null;
    //clean up invalidation check on app destroy
    $scope.$on('$destroy', function() {
      if(ctrl.stop_invalidation_check == null){
        $interval.cancel(ctrl.stop_invalidation_check);
      }
    });
    //set up invalidation check
    $scope.$watch("pending_invalidations",function(newVal,oldVal){
      var min_invalidation_time = 10000;
      if(newVal.length > 0 && oldVal.length == 0 && ctrl.stop_invalidation_check == null){
        ctrl.stop_invalidation_check = $interval(checkInvalidation, min_invalidation_time);
      }
    }, true)
    //stop invalidation check
    $scope.$watch("pending_invalidations",function(newVal,oldVal){
      if(oldVal.length > 0 && newVal.length == 0){
        ctrl.$interval.cancel(ctrl.stop_invalidation_check);
        ctrl.stop_invalidation_check = null;
      }
    }, true)

    $scope.$watch("current_configuration", function(newVal, oldVal){
      ctrl.flags.preview_generated = false;
    },true)
  }

  var getAvailableImages = function($http, available_images){
    $http({
      method: 'GET',
      url: "/pages/available_images"
    }).then(function successCallback(response) {
      angular.copy(response.data, available_images);
    }, function errorCallback(response) {
      console.log("Could not get available images for s3");
      available_images = [];
    });
  }

  var hasImageExtension = function(str) {
    var image_files_extensions = /(?:\.jpg|\.png)$/;
    return str.match(image_files_extensions)
  }

  var compile_configuration_variable_types = function(ctrl){
    ctrl.homepage_keys = Object.keys(ctrl.homepage_configuration);
    ctrl.conf_var_types = {};
    for(var i=0; i<ctrl.homepage_keys.length; i++){
      var key = ctrl.homepage_keys[i];
      var type = Object.prototype.toString.call(ctrl.homepage_configuration[key]).replace("[object ","").replace("]","");
      if(type == "Array"){
        //Differentiate between integer and string
        if(isNaN(ctrl.homepage_configuration[key][0])){
          type = "Array_String"
        }else{
          type = "Array_Number"
        }
      }
      if(type == "String" && hasImageExtension(ctrl.homepage_configuration[key])){
        type = "Image"
      }
      ctrl.conf_var_types[key] = type;
    }
  }

  ConfigCtrl.prototype.update_current_configuration = function(){
    if(this.current_url && Object.keys(this.current_url).length > 0){
      //populate existing values
      this.current_configuration = {};
      this.show_image = {};
      this.current_configuration = JSON.parse(JSON.stringify(this.configurations["lynx"]["domains"][this.current_url.domain][this.current_url.folder]));
      this.$scope.current_configuration = this.current_configuration;
      //homepage of domain should have all the configurable values
      this.homepage_configuration = JSON.parse(JSON.stringify(this.configurations["lynx"]["domains"][this.current_url.domain]["home_page"]));
      compile_configuration_variable_types(this);
      for(var i=0; i < this.homepage_keys.length; i++){
        var key = this.homepage_keys[i];
        if(!this.current_configuration.hasOwnProperty(key)){
          this.current_configuration[key] = this.homepage_configuration[key];
        }
      };
      //array needs to be array of object
      for(var i=0; i < this.homepage_keys.length; i++){
        var key = this.homepage_keys[i];
        if(this.conf_var_types[key].indexOf("Array") != -1){
          var shallow_copy = [];
          var current_values = this.current_configuration[key];
          for(var j=0; j< current_values.length; j++){
            shallow_copy.push({"value": current_values[j]});
          }
          this.current_configuration[key] = shallow_copy;
        }
      };
      //remove values that should not be changed
      for(var j=0; j<this.CONSTANTS.length; j++){
        var key = this.CONSTANTS[j];
        if(this.current_configuration.hasOwnProperty(key)){
          delete this.current_configuration[key];
        }
      }
    }
  }

  ConfigCtrl.prototype.save_current_configuration = function(){
    this.lynxConfigService.save_current_configuration(this.current_url, JSON.parse(JSON.stringify(this.current_configuration)));
  }

  ConfigCtrl.prototype.submit_configuration = function(){
    var url = "/pages/upload_config";
    var deferred = this.$q.defer();
    this.$http({
      method: 'POST',
      url: url,
      data: this.configurations
    }).then(function successCallback(response) {
      console.log("Value submitted")
      deferred.resolve(response);
    }, function errorCallback(response) {
      console.log("Value failed")
      deferred.reject(response);
    });

    return deferred.promise;
  }

  ConfigCtrl.prototype.flush_configuration = function(){
    var url = "/pages/flush";
    var deferred = this.$q.defer();
    var ctrl = this;
    this.flags.is_flushing = true;
    this.$http({
      method: 'GET',
      url: url
    }).then(function successCallback(response) {
      console.log("Flush sent")
      ctrl.flags.is_flushing = false;
      deferred.resolve(response)
    }, function errorCallback(response) {
      console.log("Flush failed")
      ctrl.flags.is_flushing = false;
      deferred.reject(response);
    });

    return deferred.promise;
  }

  ConfigCtrl.prototype.create_invalidation = function(paths){
    paths = paths || [];
    var url = "/pages/invalidate";
    var deferred = this.$q.defer();
    var ctrl = this;
    this.flags.is_invalidating = true;
    this.$http({
      method: 'GET',
      url: url,
      params: {paths: paths}
    }).then(function successCallback(response) {
      console.log("Invalidation created")
      var data = response.data
      if(angular.isDefined(data["invalidation"])){
        var invalidation_id = data["invalidation"]["id"];
        if(ctrl.pending_invalidations.indexOf(invalidation_id) == -1){
          ctrl.pending_invalidations.push(data["invalidation"]["id"]);
        }
      }
      deferred.resolve(response)
    }, function errorCallback(response) {
      console.log("Invalidation not created")
      ctrl.flags.is_invalidating = false;
      deferred.reject(response);
    });

    return deferred.promise;
  }

  ConfigCtrl.prototype.addNewInput = function(key) {
    this.current_configuration[key].push({"value":""});
  };

  ConfigCtrl.prototype.removeInput = function(key,index_to_remove) {
    this.current_configuration[key].splice(index_to_remove,1)
  };

  ConfigCtrl.prototype.preview = function(){
    var ctrl = this;
    this.lynxConfigService.save_to_preview(this.current_url,JSON.parse(JSON.stringify(this.current_configuration)));
    this.flags.is_generating_preview = true;
    this.submit_configuration().then(
      function(response){
        return ctrl.flush_configuration();
      },
      function(error){
        console.log("Unable to submit, cannot preview")
        console.log(error)
        ctrl.flags.is_generating_preview = false;
      }
    ).then(
      function(response){
        var path = "/" + ctrl.current_url.domain + "/static/preview/*"
        return ctrl.create_invalidation([path]);
      },
      function(error){
        console.log("Unable to flush, cannot preview")
        console.log(error)
        ctrl.flags.is_generating_preview = false;
      }
    ).then(
      function(response){
      },
      function(error){
        console.log("Unable to create invalidation")
        console.log(error)
      }
    )
  }

  angular.module('lynxConfig').controller('ABCtrl', ABCtrl);

  ABCtrl.$inject = ["lynxABService", "$q", "$http", "$scope", "ngDialog"];
  function ABCtrl(lynxABService, $q, $http, $scope, ngDialog){
    var ctrl = this;
    this.lynxABService = lynxABService;
    this.$scope = $scope;
    this.$q = $q;
    this.$http = $http;
    this.ngDialog = ngDialog;
    this.experiments = this.lynxABService.experiments;
    this.current_experiment = {};
    this.current_experiment_status = {};
    this.current_ab_test = {};
    this.current_condition = {};
    this.available_images = [];
    getAvailableImages(this.$http, this.available_images);

    this.flags = {
      is_submitting_experiments: false,
      is_flushing_ab_tests: false,
      is_loading_ab_from_server: false
    };

    this.lynxABService.load_current_experiments();
  }

  String.prototype.replaceAll=function(t,r){return this.split(t).join(r);};

  var compileActiveTest = function(ctrl){
    var test_statuses = {};
    var experiment = JSON.parse(ctrl.current_experiment["json"])
    for(var i=0; i < experiment.experiment_sequence.length; i++){

      var exp_log = experiment.experiment_sequence[i];
      var exp_name = exp_log.name;
      if(angular.isDefined(test_statuses[exp_name]) && exp_log.action == "remove"){
        delete test_statuses[exp_name];
      }else if(exp_log.action == "add"){
        test_statuses[exp_name] = {segments: exp_log.segments, definition: exp_log.definition, name: exp_name};
      }
    }
    if(Object.keys(test_statuses).length > 0){
      return test_statuses[Object.keys(test_statuses)[0]];
    }else{
      return null;
    }
  }

  ABCtrl.prototype.updateCurrentExperimentView = function(){
    var ctrl = this;
    this.current_ab_test = {};
    this.current_experiment_status = {test_definitions:[]};
    this.current_test_assignments = [];
    var experiment = JSON.parse(ctrl.current_experiment["json"])
    experiment.experiment_definitions.forEach(
      function(definition){
        ctrl.current_experiment_status.test_definitions.push(definition);
      }
    )

    ctrl.current_experiment_status.active_test = compileActiveTest(ctrl);
  }

  var evaluateIf = function(and){
    if(and.op == "and"){
      var conditions = [];
      for(var i=0; i<and.values.length; i++){
        conditions = conditions.concat(evaluateIf(and.values[i]));
      }
      return conditions;
    }else if(and.op == "equals"){
      return [{op:"equals", var:and.left.var, val:and.right}]
    }else if(and.op == "not"){
      return [{op:"not equals", var:and.value.left.var, val:and.value.right}]
    }
  }

  var evaluateThen = function(then){
    var vars = [];
    for(var i=0; i<then.seq.length; i++){
      vars.push(evaluateSet(then.seq[i]));
    }

    return vars;
  }

  var evaluateSet = function(set){
    var values = [];
    for(var i=0; i<set.value.choices.values.length; i++){
      var weight = 0;
      var choice = set.value.choices.values[i];
      if(set.value.op == "weightedChoice"){
        weight = set.value.weights.values[i];
      }
      if((typeof choice) == "object"){
        if(choice.op == "array"){
          values.push({value: choice.values, weight: weight})
        }
      }else{
        values.push({value: choice, weight: weight});
      }
    }

    return {
      var: set.var,
      choices: values,
      group: set.value.salt,
      op: set.value.op
    }
  }

  var assignVariableTypes = function(variable_set){
    variable_set.forEach(
      function(variable){
        console.log(variable)
        var type = Object.prototype.toString.call(variable.choices[0].value).replace("[object ","").replace("]","");
        if(type == "String" && hasImageExtension(variable.choices[0].value)){
          type = "Image"
        }else if(type == "Array"){
          if(isNaN(variable.choices[0].value[0])){
            type = "Array_String"
          }else{
            type = "Array_Number"
          }
        }
        variable.type = type;
      }
    )
  }

  ABCtrl.prototype.updateCurrentABTestView = function(){
    var ctrl = this;
    this.current_test_assignments = [];
    // If there's a value
    if(!angular.equals({},this.current_ab_test)){
      var assign_rule = ctrl.current_ab_test.assign.seq;
      for(var i=0; i<assign_rule.length;i++){
        var set_or_condition = assign_rule[i];
        //This is a variable being set
        if(set_or_condition.op == "set"){
          var variable_set = evaluateSet(set_or_condition);
          assignVariableTypes(variable_set);
          var conditional_set = {
            conditions: [],
            vars: [variable_set]
          };
          this.current_test_assignments.push(conditional_set);
        //This is a conditional
        }else if(set_or_condition.op == "cond"){
          var variable_set = evaluateThen(set_or_condition.cond[0].then);
          assignVariableTypes(variable_set);
          var conditional_set = {
            conditions: evaluateIf(set_or_condition.cond[0].if),
            vars: variable_set
          };
          this.current_test_assignments.push(conditional_set);
        }
      }
    }
  }

  ABCtrl.prototype.selectCondition = function(condition){
    this.current_condition = condition;
  }

  ABCtrl.prototype.addNewCondition = function(vars_ind) {
    console.log(this.current_condition)
    console.log(vars_ind)
    this.current_condition.conditions.push({op:"equals",var:"",val:""});
  };

  ABCtrl.prototype.removeCondition = function(vars_ind, var_ind) {
    this.current_condition.conditions.splice(var_ind,1)
  };

  ABCtrl.prototype.addChoice = function(vars_ind) {
    this.current_condition.vars[vars_ind].choices.push({value:"", weight: 0});
  };

  ABCtrl.prototype.removeChoice = function(vars_ind, var_ind) {
    this.current_condition.vars[vars_ind].choices.splice(var_ind,1)
  };

  ABCtrl.prototype.addVariable = function() {
    this.current_condition.vars.push({var:"",choices:[{value:null, weight:0}],op:"uniformChoice", type:""});
  };

  ABCtrl.prototype.removeVariable = function(varInd) {
    this.current_condition.vars.splice(varInd,1)
  };

  ABCtrl.prototype.addNewAssignment = function(){
    this.current_test_assignments.push({conditions:[{op:"equals",var:"",val:""}],vars:[{var:"",choices:[{value:null, weight:0}],op:"uniformChoice"}]})
  }

  ABCtrl.prototype.persistToYaml = function(){
    this.lynxABService.jsonToYaml(this.current_experiment.name, this.current_ab_test.definition, this.current_test_assignments);
  }

  ABCtrl.prototype.createNewExperiment = function(new_experiment_name){
    if(new_experiment_name.length > 0){
      this.lynxABService.createNewExperiment(new_experiment_name);
    }
  }

  ABCtrl.prototype.createNewExperimentModal = function(){
    var ctrl = this;
    this.$scope.new_experiment_name = "";
    var createDialog = this.ngDialog.openConfirm({
        template: '<p>New Experiment Name</p><p><input ng-model="new_experiment_name"/></p><p><button ng-click="confirm(new_experiment_name);">Submit</button></p>',
        plain: true,
        data: {
          new_experiment_name: this.$scope.new_experiment_name
        }
    });

    createDialog.then(function(data){
      ctrl.createNewExperiment(data)
    }, function(e){});
  }

  ABCtrl.prototype.createNewAbTest = function(ctrl, new_ab_test_name){
    if(new_ab_test_name.length > 0){
      var new_test_definition = {"definition":new_ab_test_name,"assign":{"op":"seq","seq":[{"op":"cond","cond":[{"if":{"op":"equals","left":{"op":"get","var":""},"right":""},"then":{"op":"seq","seq":[{"op":"set","var":"","value":{"choices":{"op":"array","values":[""]},"unit":{"op":"get","var":"session_id"},"op":"uniformChoice"}}]}}]}]}};
      ctrl.current_experiment_status.test_definitions.push(new_test_definition)
    }
  }
  
  ABCtrl.prototype.createNewABTestModal = function(){
    var ctrl = this;
    this.$scope.new_ab_test_name = "";
    var createDialog = this.ngDialog.openConfirm({
        template: '<p>New Experiment Name</p><p><input ng-model="new_ab_test_name"/></p><p><button ng-click="confirm(new_ab_test_name);">Submit</button></p>',
        plain: true,
        data: {
          new_ab_test_name: this.$scope.new_ab_test_name
        }
    });

    createDialog.then(function(data){
      ctrl.createNewAbTest(ctrl,data)
    }, function(e){});
  }

  ABCtrl.prototype.previewImageModal = function(image_path){
    var ctrl = this;
    var newScope = this.$scope.$new();
    newScope.image_path = cdn_url + image_path;
    var createDialog = this.ngDialog.open({
        template: '<img src="{{image_path}}" style="max-width:100%; max-height: 100%;"/>',
        plain: true,
        scope: newScope
    });
  }

  ABCtrl.prototype.flush_ab_tests = function(){
    var deferred = this.$q.defer();
    this.$http({
      method: 'GET',
      url: "/ab/flush"
    }).then(function successCallback(response) {
      console.log("Ab test flushed")
      deferred.resolve(response);
    }, function errorCallback(response) {
      console.log("Could not flush ab tests");
      console.log(response);
      deferred.reject(response);
    });

    return deferred;
  }

  ABCtrl.prototype.submitExperiments = function(){
    var ctrl = this;
    this.flags.is_submitting_experiments = true;
    this.lynxABService.submitExperiments().then(
      function(response){
        ctrl.flags.is_submitting_experiments = false;
        ctrl.flags.is_flushing_ab_tests = true;
        return ctrl.flush_ab_tests();
      },
      function(error){
        console.log("Unable to submit experiments")
        console.log(error)
        ctrl.flags.is_submitting_experiments = false;
      }
    ).then(
      function(response){
        ctrl.flags.is_flushing_ab_tests = false;
        ctrl.flags.is_loading_ab_from_server = true;
        return ctrl.lynxABService.load_current_experiments();
      },
      function(error){
        console.log(error)
        ctrl.flags.is_flushing_ab_tests = false;
      }
    ).then(
      function(response){
        ctrl.flags.is_loading_ab_from_server = false;
      },
      function(error){
        console.log(error)
        ctrl.flags.is_loading_ab_from_server = false;
      }
    )
  }

  ABCtrl.prototype.setActiveTest = function(new_active_test){
    var ctrl = this;
    var old_active_test = this.current_experiment_status.active_test;
    var new_test = this.lynxABService.activateNewTest(this.current_experiment.name, old_active_test, new_active_test);
    this.current_experiment_status.active_test = new_test;
  }

})();