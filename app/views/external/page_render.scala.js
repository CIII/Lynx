// Write browser_id.js tag
(function() {
	var initFunc = function(fingerprintParam) {
		var scripts = document.getElementsByTagName("script");
		var scriptSrc = scripts[scripts.length - 1].src
		var browserIdTag = document.createElement('script');
		browserIdTag.type = "text/javascript";
		browserIdTag.async = true;
		browserIdTag.src = scriptSrc + "/../../../browser_id.js?" + fingerprintParam;

		var lynxReportingTag = document.createElement('script');
		lynxReportingTag.type = "text/javascript";
		lynxReportingTag.asynx = true;
		lynxReportingTag.src = scriptSrc + "/../../lynx_reporting.js?" + fingerprintParam;

		var body = document.getElementsByTagName("body")[0];
		body.appendChild(browserIdTag);
		body.appendChild(lynxReportingTag);
		// Call to page_loading
		qr=new XMLHttpRequest();
		qr.open('get',scriptSrc + '/../../../page_loading?' + fingerprintParam);
		qr.send();

		// TODO: Write third-part browser.js tag
	};
	if (typeof(Fingerprint2) !== "undefined") {
		new Fingerprint2().get(function(result, components) {
			initFunc("fpt=" + result);
		})
	} else {
		initFunc("");
	}
})();