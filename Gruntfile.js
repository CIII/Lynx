'use strict';
module.exports = function (grunt) {
  require('load-grunt-tasks')(grunt);
  grunt.initConfig({
    concat: {
      bowerjs: {
        src: [
          './public/bower_components/jquery/dist/jquery.min.js',
          './public/bower_components/underscore/underscore-min.js',
          './public/bower_components/angular/angular.min.js',
          './public/bower_components/bootstrap/dist/js/bootstrap.min.js',
          './public/bower_components/angular-bootstrap/ui-bootstrap.min.js',
          './public/bower_components/angular-bootstrap/ui-bootstrap-tpls.min.js',
          './public/bower_components/angular-animate/angular-animate.min.js',
          './public/bower_components/angular-ui-router/release/angular-ui-router.min.js',
          './public/bower_components/ngmap/build/scripts/ng-map.min.js',
          './public/bower_components/ngstorage/ngStorage.min.js',
          './public/bower_components/angular-underscore/angular-underscore.min.js',
          './public/bower_components/angular-socialshare/dist/angular-socialshare.min.js',
          './public/bower_components/sweetalert/dist/sweetalert.min.js',
          './public/bower_components/ngSweetAlert/SweetAlert.min.js',
          './public/bower_components/angular-ui-router-anim-in-out/anim-in-out.js',
          './public/bower_components/angular-ui-mask/dist/mask.min.js',
          './public/bower_components/angucomplete-alt/dist/angucomplete-alt.min.js',
          './public/bower_components/angular-google-chart/ng-google-chart.min.js',
          './public/bower_components/angular-sanitize/angular-sanitize.min.js',
          './public/bower_components/countUp.js/dist/countUp.min.js',
          './public/bower_components/countUp.js/dist/angular-countUp.min.js',
          './public/bower_components/json-logic-js/logic.js',
          './public/bower_components/fingerprintjs2/fingerprint2.js'
        ],
        dest: './public/app/build/js/bower.js'
      },
      scriptsjs: {
        src: [
          './public/app/directives/directives.js',
          './public/app/services/CommonService.js',
          './public/app/services/FormService.js',
          './public/app/services/ABTestService.js'
        ],
        dest: './public/app/build/js/scripts.js'
      },
      bowercss: {
        src: [
            './public/bower_components/bootstrap/dist/css/bootstrap.min.css',
            './public/bower_components/angular-ui-router-anim-in-out/css/anim-in-out.css',
            './public/bower_components/sweetalert/dist/sweetalert.css',
            './public/bower_components/angular-google-places-autocomplete/src/autocomplete.css',
            './public/bower_components/angucomplete-alt/angucomplete-alt.css'
          ],
        dest: './public/app/build/css/bower.css'
      },
      mutualofomaha: {
    	src: [
    	  './public/app/css/mutualofomaha/mutualofomaha.css',
    	  './public/app/css/mutualofomaha/Index.css'
    	],
    	dest: './public/app/build/css/mutualofomaha.css'
      }
    },
    uglify: {
      scripts: {
        files: {
          './public/app/build/js/bower.min.js': ['./public/app/build/js/bower.js'],
          './public/app/build/js/scripts.min.js': ['./public/app/build/js/scripts.js'],
        }
      }
    },
    cssmin: {
      options: {
        mergeIntoShorthands: false,
        roundingPrecision: -1
      },
      target: {
        files: {
          './public/app/build/css/bower.min.css': ['./public/app/build/css/bower.css'],
          './public/app/css/style.min.css': ['./public/app/css/style.css'],
          './public/app/css/style_hs.min.css': ['./public/app/css/style_hs.css'],
          './public/app/css/font/stylesheet.min.css': ['./public/app/css/font/stylesheet.css']
        }
      }
    },
    concurrent: {
      dev: ['start', 'dev'],
      publish: ['start', 'publish'],
      options: {
        logConcurrentOutput: true
      }

    },
    watch: {
      scripts: {
        files: ['./public/app/css/**/*', './public/bower_components/**/*', './public/app/services/**/*', './public/app/directives/**/*', '!./public/**/*.min.js', '!./public/**/*.min.css'],
        tasks: ['concat','uglify','cssmin']
      }
    }
  });

  //Loading NPM tasks
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-contrib-cssmin');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-concurrent');
  grunt.registerTask('default', ['concurrent:dev']);
  grunt.registerTask('dev', ['concat', 'uglify', "cssmin"]);
  grunt.registerTask('deploy', ['concat']);
};
