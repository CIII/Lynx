$( document ).ready(function() {
    function populate_power_supplier_array() {
        var powerArray = [];
        var state = $("#state").val();
        var powerSuppliersHandler = function() {
            populatePowerSuppliers(state,powerArray);
        };

        //Routine used to fill powerArray with the names of utility providers
        function getPowerSuppliers(){
          query_power_suppliers(state, powerSuppliersHandler);
        }

        getPowerSuppliers();

        $('#powerSuppliers').autocomplete({
            'source': powerArray,
            'minLength': 0
        }).on('focus', function () {
            $(this).keydown();
        });
    }

    $("#continue-1").click(function(e) {
        e.preventDefault();
        validateLeadId();
        var errors = [];
        is_street_and_zip_valid(errors)

        if (errors.length > 0) {
            return alert("Please correct the following errors:\n\n- " + errors.join("\n- "));
        }
        var current_step = $(".step.current");
        current_step.next().addClass("current");
        current_step.removeClass("current");
        evaluate_city_state_from_address_routine();
        populate_power_supplier_array();
        createEvent(FORM_STEP2);
        location.hash = 'step-2';
    });

    $("#continue-2").click(function(e) {
    	validateLeadId();
        var errors = [];

        is_electric_bill_valid(errors);
        is_power_supplier_valid(errors);

        if (errors.length > 0) {
            e.preventDefault();
            var error_text = "Please correct the following errors:\n\n- " + errors.join("\n- ");
            return alert(error_text);
        }

        var current_step = $(".step.current");
        current_step.next().addClass("current");
        current_step.removeClass("current");

        createEvent(FORM_STEP3);
        location.hash = 'step-3';
    });


    $("#leadform").submit(function(e) {
    	validateLeadId();
        var errors = [];
        is_first_last_name_valid(errors);
        is_email_valid(errors);
        is_phone_valid(errors)

        if (errors.length > 0) {
            e.preventDefault();
            var error_text = "Please correct the following errors:\n\n- " + errors.join("\n- ");
            return alert(error_text);
        }

        createEvent(FORM_COMPLETE);
    });

    $(".cta-btn").click(function() {
        $('html, body').animate({
            scrollTop: $("#leadform").offset().top - 100
        }, 500);

    });

    try {
        $("#leadform").setAttribute("autocomplete", "off");
        $("#powerSuppliers").setAttribute("autocomplete", "off");
    } catch (e) {}
    $('.phoneLoading').hide();

    sendPing();

    $("#leadform").keyup(function(event) {
        if (event.keyCode == 13) {
            if ($(".current").attr("id") == "step1") {
                $("#continue-1").click();
            } else if ($(".current").attr("id") == "step2") {
                $("#continue-2").click();
            } else if ($(".current").attr("id") == "step3") {
                if ($("#step4").length) {
                    $("#continue-3").click();
                } else {
                    $("#submit").click();
                }
            } else if ($(".current").attr("id") == "step4") {
                $("#submit").click();
            }
        }
    });

    $(window).bind('hashchange', function() {
        var newHash = location.hash.replace('#', '');
        var autocompleteUI = $('#ui-id-1');

        if (newHash == 'step-1' || newHash == '' || newHash == ' ' || !newHash) {
            sendPing();
            var target = $('#step1');
            $('.step').not(target).removeClass('current');
            target.addClass('current');
            $(autocompleteUI).hide();
        }

        if (newHash == 'step-2') {
            sendPing();
            var target = $('#step2');
            $('.step').not(target).removeClass('current');
            target.addClass('current');
            $(autocompleteUI).hide();
        }

        if (newHash == 'step-3') {
            sendPing();
            var target = $('#step3');
            $('.step').not(target).removeClass('current');
            target.addClass('current');
            $('#powerSuppliers').trigger('focus');

            setTimeout(function() {
                var dropdownWidth = $('#powerSuppliers').outerWidth();
                $(autocompleteUI).css('width', dropdownWidth).css('max-width', dropdownWidth);
            }, 100);
        }

        if (newHash == 'step-4') {
            sendPing();
            var target = $('#step4');
            $('.step').not(target).removeClass('current');
            target.addClass('current');
            $(autocompleteUI).hide();
        }

    });

    $('body').on('click', '#form .form-group .radio:first-child', function(e) {

        if ($('#property_ownership-OWN').is(':checked')) {
            $('#step2 > div.form-group.cf > div').removeClass('radioactive');
            $('#property_ownership-OWN').prop('checked', false);
        } else {
            $('#step2 > div.form-group.cf > div').addClass('radioactive');
            $('#property_ownership-OWN').prop('checked', true);
        }
        e.preventDefault();
    });
})