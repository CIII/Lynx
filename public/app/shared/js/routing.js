(function() {
   'use strict';
   angular.module('app').config(routeConfig);
   routeConfig.$inject = ['$urlRouterProvider', '$stateProvider'];
   function routeConfig($urlRouterProvider, $stateProvider) {
      $urlRouterProvider.otherwise('/');

      $stateProvider
         // base template for content
         .state('index', {
            abstract: true,
            templateUrl: '/assets/app/shared/templates/index.html',
         })
         .state('index.home', {
            url: '/',
            views: {
               "@index": {
                  templateUrl: '/assets/app/views/home/home.html',
                  controller: "HomeController as vm"
               }
            }
         })
         .state('index.about', {
            url: '/about',
            views: {
               "@index": {
                  templateUrl: '/assets/app/views/about/about.html',
                  controller: "AboutController as vm"
               }
            }
         });
   }
})();