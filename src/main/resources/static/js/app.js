document.addEventListener('DOMContentLoaded', function () {
  document.documentElement.dataset.js = 'enabled';

  document.querySelectorAll('form[data-confirm]').forEach(function (form) {
    form.addEventListener('submit', function (event) {
      const message = form.dataset.confirm;
      if (message && !window.confirm(message)) {
        event.preventDefault();
      }
    });
  });

  document.querySelectorAll('.alert--dismissible').forEach(function (alertNode) {
    window.setTimeout(function () {
      alertNode.style.opacity = '0';
      alertNode.style.transition = 'opacity 180ms ease-out';
      window.setTimeout(function () {
        alertNode.remove();
      }, 200);
    }, 5000);
  });
});
