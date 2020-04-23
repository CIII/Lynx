@(headerSettings: utils.templates.HeaderSettings)(implicit request: play.api.mvc.Request[AnyContent], configuration: play.api.Configuration, environment: play.api.Environment)
@import utils.utilities.construct_url
(function (global, factory) {
	typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports) :
	typeof define === 'function' && define.amd ? define(['exports'], factory) :
	(factory((global.lynx_report = global.lynx_report || {})));
}(this, (function (exports) { 'use strict';

  var version = "1.0.0";
  var server_url = "@{request.host}";

  const MAXMIND_FAIL = "Maxmind failure on page load";
  const PAGE_LOADED = "Page loaded";
  const PAGE_SCROLL = "Page Scroll";
  const PAGE_MOUSE_MOVEMENT = "Page Mouse Movement";
  const PAGE_MOUSE_CLICK = "Page Mouse Click";
  const PAGE_BLUR = "Page Blur";
  const PAGE_FOCUS = "Page Focus";
  const PAGE_VIEW = "Page View";

  var robot_id;

  var maxmind_results = {};
  var os_name = null;

  var getOSName = function(){
    if (navigator.appVersion.indexOf("Win")!=-1){ return "Windows";}
    else if (navigator.appVersion.indexOf("Mac")!=-1){ return "MacOS";}
    else if (navigator.appVersion.indexOf("X11")!=-1){ return "UNIX";}
    else if (navigator.appVersion.indexOf("Linux")!=-1){ return "Linux";}
    else { return null;}
  }

  var getElementAttr = function(element){
    var element_attr = {};
    if(element != null && typeof element.dataset != 'undefined'){
      if(element.dataset.hasOwnProperty("section")){
        element_attr.button_section = element.dataset.section;
      }
      if(element.dataset.hasOwnProperty("text")){
        element_attr.button_text = element.dataset.text;
      }else if(element.textContent.length > 0){
        element_attr.button_text = element.textContent;
      }
    }

    return element_attr;
  }

  var fireEvent = function(event, element, eventAttr){
    eventAttr = eventAttr || {};
    element = element || null;
    if(element != null){
      jQuery.extend(eventAttr, getElementAttr(element))
    }

    var url = "http://" + server_url + "/event/create";
    return jQuery.ajax({
      url: url,
      type: 'POST',
      contentType: 'application/x-www-form-urlencoded',
      xhrFields: {
        withCredentials: true
      },
      data: jQuery.extend(
        {
          browser_id: browserId,
          event: event,
          request_url: window.location.href,
          local_hour: (new Date()).getHours(),
          os: os_name,
        },eventAttr, maxmind_results),
      async: false,
    })
  }

  var clickEvent = function(event, element){
    return fireEvent(event, element);
  }

  var createEvent = function(event, eventAttr){
    return fireEvent(event, null, eventAttr);
  }

  var assignBrowserId = function(){
    if (jQuery("#browser_id").length) {
      jQuery("#browser_id").val(browserId);
    }
  }

  var check_robot_id = function(){
    jQuery.getJSON('@construct_url("javascripts/crawler-user-agents.json")', function(crawler_data) {
      crawler_data.forEach(function(obj) {
        if (user_agent.toLowerCase().indexOf(obj.pattern.toLowerCase()) != -1) {
          robot_id = obj.pattern;
        }
      });
    });
  }

  var onSuccess = function(maxmindResp) {
    if (maxmindResp.hasOwnProperty("postal")) {
      maxmind_results.maxmind_zip = maxmindResp.postal.code;
    }
    if (maxmindResp.hasOwnProperty('subdivisions')) {
      maxmind_results.maxmind_state = maxmindResp.subdivisions[0].iso_code;
    }
    if (maxmindResp.hasOwnProperty('city')){
      maxmind_results.maxmind_city = maxmindResp.city.names.en;
    }
    if (maxmindResp.hasOwnProperty('country')){
      maxmind_results.maxmind_country = maxmindResp.country.iso_code;
    }
    if (maxmindResp.hasOwnProperty('traits')) {
      maxmind_results.maxmind_ip_address = (maxmindResp.hasOwnProperty("traits")) ? maxmindResp.traits.ip_address : "";
    }
  };

  var onError = function(error) {
    createEvent(MAXMIND_FAIL);
  }

  var pageViewMonitor = function(){
    var scrollTop = -1;
    var scrollBottom = -1;
    try{
      scrollTop = window.pageYOffset || document.documentElement.scrollTop;
      scrollBottom = scrollTop + jQuery(window).height();
      if(scrollTop >= 0 && scrollBottom >= 0){
        var eventAttr = {
          'scroll_top': scrollTop,
          'scroll_bottom': scrollBottom
        }
        createEvent(PAGE_VIEW, eventAttr)
      }
    }catch(err){}

  }

  jQuery(document).ready(function(){
    try{
      assignBrowserId();
      geoip2.city(onSuccess, onError);
      os_name = getOSName();
      setInterval(pageViewMonitor, 1000);
    }finally{
      createEvent(PAGE_LOADED);
    }}
  )

  var lastScrollFireTime = 0;
  /**
  * Tracks and fires event on intervals for scrolls
  */
  jQuery(window).on('scroll', function() {
    var minScrollTime = 5000;
    var now = new Date().getTime();
    if (now - lastScrollFireTime > ( minScrollTime)) {

      createEvent(PAGE_SCROLL);
      lastScrollFireTime = now;
    }
  });

  var lastMouseFireTime = 0;
  /**
  * Tracks and fires event on intervals for mouse movements
  */
  jQuery(window).on('mousemove', function(event) {
    var minScrollTime = 5000;
    var now = new Date().getTime();
    if (now - lastMouseFireTime > ( minScrollTime)) {

        createEvent(PAGE_MOUSE_MOVEMENT);
        lastMouseFireTime = now;
    }
  });

  jQuery(window).on('mousedown', function(event) {
    var eventAttr = {
      'mouse_x': event.pageX,
      'mouse_y': event.pageY
    }

    createEvent(PAGE_MOUSE_CLICK, eventAttr);
  });

  jQuery(window).bind('focus', function() {
    createEvent(PAGE_FOCUS)
  }).bind('blur', function() {
    createEvent(PAGE_BLUR)
  });

  exports.createEvent = createEvent;
  exports.clickEvent = clickEvent;
  exports.location = maxmind_results;

  Object.defineProperty(exports, '__esModule', { value: true });

})));