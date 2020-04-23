@()(implicit request: play.api.mvc.Request[AnyContent])

const MAXMIND_FAIL = "Maxmind failure on page load";
const PAGE_LOADED = "Page loaded";
const PAGE_SCROLL = "Page Scroll";
const PAGE_MOUSE_MOVEMENT = "Page Mouse Movement";
const PAGE_BLUR = "Page Blur";
const PAGE_FOCUS = "Page Focus";
var robot_id;

function sendPing() {
  jQuery.ajax({
    url: "/ping",
    type: 'POST',
    contentType: 'application/x-www-form-urlencoded',
    data: jQuery("#leadform").serializeArray(),
    async: true,
    success: function() {}
  });
}

function createEvent(event, eventAttr) {
  eventAttr = eventAttr || {};
  jQuery.ajax({
    url: "/event/create",
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
        local_hour: (new Date()).getHours()
      },eventAttr),
    async: false,
    success: function() {}
  });
}

function getParameterByName(name, url) {
  if (!url) {
    url = window.location.href;
  }
  name = name.replace(/[\\[\\]]/g, "\\jQuery&");
  var regex = new RegExp("[?&]" + name + "(=([^&#]*)&#  jQuery)"),
    results = regex.exec(url);
  if (!results) return null;
  if (!results[2]) return '';
  return decodeURIComponent(results[2].replace(/\\+/g, " "));
}

function escapeSelector(s) {
  return s.replace(/(:\\.\\[\\])/g, "\\ jQuery1");
}

function callNow() {
  createEvent(LP_CTC);
}
window.onload = function() {
  if (jQuery("#browser_id").length) {
    jQuery("#browser_id").val(browserId);
  }
  sendPing();

  function check_robot_id() {
    jQuery.getJSON('${configuration.getString("cloudfront.cdn.url")}javascripts/crawler-user-agents.json', function(crawler_data) {
      crawler_data.forEach(function(obj) {
        if (user_agent.toLowerCase().indexOf(obj.pattern.toLowerCase()) != -1) {
          robot_id = obj.pattern;
        }
      });
    });
  }
  var onSuccess_maxmind = function(location) {
    ip_address = location.traits.ip_address;
    if (jQuery('#ip').length > 0) {
      jQuery("#ip").val(ip_address);
      sessionStorage['ip_address'] = ip_address
    }
    zip = location.postal.code;
    if (jQuery('#zip').length > 0) {
      jQuery("#zip").val(zip);
      sessionStorage['ip_based_zip'] = zip
    }
    check_robot_id();
    if (jQuery('#leadform').length > 0) {
      jQuery.ajax({
        url: "https://maps.googleapis.com/maps/api/geocode/json?address=" + zip + "&key=AIzaSyDJPoJBBnxlePYGmFz5MEmiHtWcKm0jSNY",
        type: 'GET',
        async: false,
        success: function(res) {
          var city = res.results[0]["address_components"].filter(function(component) {
            return component.types.indexOf('locality') > -1;
          })[0].long_name;
          var state = res.results[0]["address_components"].filter(function(component) {
            return component.types.indexOf('administrative_area_level_1') > -1;
          })[0].short_name;
          sessionStorage['ip_based_iso_code'] = location.subdivisions.iso_code
          jQuery('.compare_header').html(location.subdivisions.iso_code + " Homeowners Save 20% with Solar.<br/>100% Free, No Obligation Quotes");
          if (jQuery('#zip').length > 0) {
            jQuery("#zip").val(location.postal.code);
          }
          if (jQuery('#ip').length > 0) {
            jQuery("#ip").val(ip_address);
          }
          jQuery('#city').val(city);
          jQuery('#state').val(state.toUpperCase());
        }
      });
    }
  };

  function onSuccess() {
    jQuery("#ip").val(sessionStorage['ip_address']);
    jQuery("#zip").val(sessionStorage['ip_based_zip']);
    check_robot_id();
    zip = sessionStorage['ip_based_zip']
    if (jQuery('#leadform').length > 0) {
      jQuery.ajax({
        url: "https://maps.googleapis.com/maps/api/geocode/json?address=" + zip + "&key=AIzaSyDJPoJBBnxlePYGmFz5MEmiHtWcKm0jSNY",
        type: 'GET',
        async: false,
        success: function(res) {
          var city = res.results[0]["address_components"].filter(function(component) {
            return component.types.indexOf('locality') > -1;
          })[0].long_name;
          var state = res.results[0]["address_components"].filter(function(component) {
            return component.types.indexOf('administrative_area_level_1') > -1;
          })[0].short_name;
          jQuery('.compare_header').html(sessionStorage['ip_based_iso_code'] + " Homeowners Save 20% with Solar.<br/>100% Free, No Obligation Quotes");
          if (jQuery('#zip').length > 0) {
            jQuery("#zip").val(sessionStorage['ip_based_zip']);
          }
          if (jQuery('#ip').length > 0) {
            jQuery("#ip").val(sessionStorage['ip_address']);
          }
          jQuery('#city').val(city);
          jQuery('#state').val(state.toUpperCase());
        }
      });
    }
  }
  var onError = function(error) {
    createEvent(MAXMIND_FAIL);
  }

  String.prototype.toProperCase = function() {
    return this.replace(/\\w\\S*/g, function(txt) {
      return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
    });
  };

  function validateLeadId() {
    // If the leadid_token does not exist, re-initialize the form.
    var form_value = jQuery("#leadid_token");
    if (form_value.val() == null || form_value.val() == "") {
      LeadiD.formcapture.init();
    }
  } //Call back function to either take result from redis(if successful) or json

  var lastScrollFireTime = 0;
  jQuery(window).on('scroll', function() {
      var minScrollTime = 500;
      var now = new Date().getTime();
      if (now - lastScrollFireTime > ( minScrollTime)) {

        var eventAttr = {};

        var scrollTop = -1;
        var scrollBottom = -1;
        try{
          scrollTop = window.pageYOffset || document.documentElement.scrollTop;
          scrollBottom = scrollTop + $(window).height();
        }catch(err){}

        if(scrollTop != -1 && scrollBottom != -1){
          eventAttr['scroll_top'] = scrollTop;
          eventAttr['scroll_bottom'] = scrollBottom;
        }

        createEvent(PAGE_SCROLL, eventAttr);
        lastScrollFireTime = now;
      }
  });

  var lastMouseFireTime = 0;
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

    createEvent(PAGE_MOUSE_MOVEMENT, eventAttr);
  });

  jQuery(window).bind('focus', function() {
    createEvent(PAGE_FOCUS)
  }).bind('blur', function() {
    createEvent(PAGE_BLUR)
  });

  createEvent(PAGE_LOADED);
}