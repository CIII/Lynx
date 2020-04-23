(function() {
   'use strict';
   angular.module('app').config(config);
   config.$inject = ['$locationProvider'];
   function config($locationProvider) {
      //enable html5Mode
      $locationProvider.html5Mode(true).hashPrefix('!');
   }
})();