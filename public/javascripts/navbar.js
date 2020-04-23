$('body').on('click', '.save-button, .save-cta', function(e) {
    $('body, html').animate({
        scrollTop: 0
    }, "slow");
    $('#electric_bill').trigger('focus');
    $('#form').addClass('attention');
    setTimeout(function() {
        $('#form').removeClass('attention');
    }, 2000);
    e.preventDefault();
});

// Nav Modals

$('body').on('click', '.incentives-nav h1, .business-nav h1, .renters-nav h1', function(e) {

    $('div[class$="modal"]').fadeOut();

    if ($(this).is('.renters-nav h1')) {
        $('.renters-nav-modal').fadeIn();
    }
    if ($(this).is('.business-nav h1')) {
        $('.business-nav-modal').fadeIn();
    }
    if ($(this).is('.incentives-nav h1')) {
        $('.incentives-nav-modal').fadeIn();
    }

    $('.modal-bg').fadeIn();
    e.preventDefault();
});

$('body').on('click', '.modal-bg', function(e) {

    $('.modal-bg, .email-capture-modal-container').fadeOut();
    $('div[class$="modal"]').fadeOut(500);
    e.preventDefault();
});

$('body').on('click', '.modal-cta h4', function(e) {

    $('.modal-bg, div[class$="modal"]').fadeOut();
    $('#electric_bill').trigger('focus');
    $('#form').addClass('attention');
    // $('#form').delay(5000).removeClass('attention');

    setTimeout(function() {
        $('#form').removeClass('attention');
    }, 2000);

    e.preventDefault();
});


$('body').on('click', '#ui-id-1 li', function(e) {
    if ($(window).width() < 762) {
        $(this).trigger('touchstart, touchend');
        $('#continue-3').trigger('focus');
    }
});

try {
    //PHONE NUMBER MASK
    $('#phone_home').mask('(000)000-0000');
} catch (e) {}