(function () {
	'use strict';
	/*
	* @Service - CommonService
	* @Description
	* @property {Array} CommonService.$inject This injects the paramters properly for minimization.
	*/
	angular.module("lynxConfig").factory("lynxABService", LynxABService);

	LynxABService.$inject = ["$http", "$q"];

	function LynxABService($http, $q) {
    var factory = {};
    factory.experiments = {};

    factory.load_current_experiments = function() {
      var deferred = $q.defer();

      $http({
        method: 'GET',
        url: "/pages/current_ab_tests.json"
      }).then(function successCallback(response) {
          Object.keys(response.data).forEach(
            function(key) { factory.experiments[key] = response.data[key]; factory.experiments[key]['name'] = key;}
          );
          deferred.resolve(response.data);
      }, function errorCallback(response) {
          deferred.reject(response);
      });

      return deferred.promise;
    };

    factory.submitExperiments = function(){
      var deferred = $q.defer();

      var experiments_to_upload = [];
      Object.keys(factory.experiments).forEach(
        function(key){
          experiments_to_upload.push({name: key, yaml: factory.experiments[key].yaml})
        }
      )
      $http({
        method: 'POST',
        url: "/pages/upload_ab_tests",
        data: experiments_to_upload
      }).then(function successCallback(response){
          deferred.resolve(response)
        }, function errorCallback(response){
          deferred.reject(response);
        }
      )

      return deferred.promise;
    }

    var conditionToString = function(condition){
      var operator = "";
      if(condition.op == "equals"){
        operator = "==";
      }else if(condition.op = "not equals"){
        operator = "!=";
      }

      return condition.var + operator + "\"" + condition.val + "\"";
    }

    var varToString = function(variable){
      var right = "";
      if(variable.op == "uniformChoice"){
        right += "uniformChoice(";
      }else if(variable.op =="weightedChoice"){
        right += "weightedChoice(weights=[";
        right += variable.choices.map(function(x){
          return x.weight
        }).join(",") + "],";
      }

      var choices = "choices=[" + variable.choices.map(function(x){
        if(variable.type == "String" || variable.type == "Image"){
          return '"' + x.value + '"';
        }else if(variable.type == "Array_Number"){
          return "[" + x.value + "]";
        }else if(variable.type == "Array_String"){
          return "[" + x.value.split(",").map(function(y){return '"' + y + '"'}).join(",") + "]"
        }else{
          return x.value;
        }
      }).join(",") + "]";

      right += choices + ", unit=session_id";
      if(angular.isDefined(variable.group) && variable.group.length > 0){
        right += ", salt=\"" + variable.group + "\"";
      }

      right += ");"

      return variable.var + "=" + right;
    }

    //TODO Need to remove empty options
    var testAssignmentToYml = function(test_assignments){
      var testDefinition = []
      test_assignments.forEach(
        function(testAssignment) {

          var conditionals = [];
          conditionals.push("      if(");
          for(var i=0; i<testAssignment.conditions.length; i++){
            conditionals.push(conditionToString(testAssignment.conditions[i]));
            if(i != testAssignment.conditions.length-1){
              conditionals.push(" && ")
            }
          }
          conditionals.push("){");
          testDefinition.push(conditionals.join(""));

          for(var i=0; i<testAssignment.vars.length; i++){
            testDefinition.push("        " + varToString(testAssignment.vars[i]));
          }

          testDefinition.push("      }")
        }
      )
      return testDefinition;
    }

    //TODO this is a terrible way to do this
    factory.jsonToYaml = function(experiment_name, test_name, test_assignments){

      var yml_split = factory.experiments[experiment_name].yaml.split("\n")
      console.log(yml_split.join("\n"))
      var test_assignment_yml = ["  - definition: " + test_name, "    assign: !planout |"].concat(testAssignmentToYml(test_assignments));
      console.log(test_assignment_yml.join(""));
      //Replace Test Definition
      var test_definition_start = -1;
      var test_definition_section_stop = -1;
      for(var i=0; i<yml_split.length; i++){
        //Find test definition
        if(yml_split[i].indexOf(test_name) != -1){
          test_definition_start = i;
          break;
        }
        //This is the end of the test definition
        if(yml_split[i].indexOf("default_experiment") != -1){
          test_definition_section_stop = i;
          break;
        }
      }
      //Test found
      if(test_definition_start != -1){
        var test_definition_end = -1;
        for(var i=test_definition_start+1; i<yml_split.length; i++){
          if(yml_split[i].indexOf("- definition:") != -1){
            test_definition_end = i - 1;
            break;
          }
          if(yml_split[i].indexOf("default_experiment") != -1){
            test_definition_end = i - 1 ;
            break;
          }
        }
        yml_split = yml_split.slice(0,test_definition_start).concat(test_assignment_yml).concat(yml_split.slice(test_definition_end + 1));
      }else{
      //Test not found just add
        yml_split = yml_split.slice(0,test_definition_section_stop).concat(test_assignment_yml).concat(yml_split.slice(test_definition_section_stop));
      }

      console.log(yml_split)
      console.log(yml_split.join("\n"))
      factory.experiments[experiment_name].yaml = yml_split.join("\n")
    }

    factory.createNewExperiment = function(new_experiment_name){
      factory.experiments[new_experiment_name] = {
        name: new_experiment_name,
        yaml: factory.experiments['dummy_test'].yaml.replace("dummy_test_name", new_experiment_name),
        json: factory.experiments['dummy_test'].json.replace("dummy_test_name", new_experiment_name)
      }
    }

    var stopTestYaml = function(old_active_test){
      return "\n  - action: remove\n    name: old_test_name".replace('old_test_name', old_active_test.name);
    }

    var startTestYaml = function(new_test_definition_name){
      var new_test_name = (Date.now() + "_" + new_test_definition_name.toLowerCase());
      return {
        yaml: "\n  - action: add\n    definition: new_test_definition_name\n    name: new_test_name  # must be unique within experiment_sequence\n    segments: 100".replace('new_test_definition_name',new_test_definition_name).replace('new_test_name', new_test_name),
        name: new_test_name,
        segments: 100,
        definition: new_test_definition_name
      };
    }

    factory.activateNewTest = function(experiment_name, old_active_test, new_test_definition_name){

      if(old_active_test != null){
        this.experiments[experiment_name].yaml += stopTestYaml(old_active_test);
      }

      var new_test_definition = startTestYaml(new_test_definition_name);
      this.experiments[experiment_name].yaml += new_test_definition.yaml;

      return new_test_definition;
    }

		return factory;
	}

})();
