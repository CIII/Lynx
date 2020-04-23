(function() {
    'use strict';

    /*
    * @Directive - pageScroll
    * @Description - Add class when scroll page
    */
    angular.module('solar')
        /*.directive("pageScroll", function ($window) {
            return function(scope, element, attrs) {
                angular.element($window).bind("scroll", function() {
                    if (this.pageYOffset > 0) {
                        scope.boolChangeClass = true;
                    } else {
                        scope.boolChangeClass = false;
                    }
                    scope.$apply();
                });
            };
        })*/
        .directive('restrictInput', [function(){
            return {
                restrict: 'A',
                link: function (scope, element, attrs) {
                    var ele = element[0];
                    var regex = RegExp(attrs.restrictInput);
                    var value = ele.value;

                    ele.addEventListener('keyup',function(e){
                        if (regex.test(ele.value)){
                            value = ele.value;
                        }else{
                            ele.value = value;
                        }
                    });
                }
            };
        }]);
})();