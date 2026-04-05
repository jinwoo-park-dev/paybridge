document.addEventListener('DOMContentLoaded', async () => {
  const config = document.getElementById('stripe-config');
  const form = document.getElementById('stripe-payment-form');
  const messageNode = document.getElementById('stripe-payment-message');
  const confirmButton = document.getElementById('stripe-confirm-button');

  if (!config || !form || typeof Stripe === 'undefined') {
    return;
  }

  const publishableKey = config.dataset.publishableKey;
  const clientSecret = config.dataset.clientSecret;
  const returnUrl = config.dataset.returnUrl;

  if (!publishableKey || !clientSecret || !returnUrl) {
    if (messageNode) {
      messageNode.textContent = 'Stripe page is missing configuration. Check local env vars and server-created PaymentIntent state.';
      messageNode.classList.add('stripe-message--error');
    }
    return;
  }

  const stripe = Stripe(publishableKey);
  const elements = stripe.elements({ clientSecret });
  const paymentElement = elements.create('payment');
  paymentElement.mount('#payment-element');

  const setMessage = (message, isError) => {
    if (!messageNode) {
      return;
    }
    messageNode.textContent = message || '';
    messageNode.classList.toggle('stripe-message--error', Boolean(isError));
  };

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    confirmButton.disabled = true;
    setMessage('Submitting payment to Stripe test mode...', false);

    const result = await stripe.confirmPayment({
      elements,
      confirmParams: {
        return_url: returnUrl,
      },
      redirect: 'if_required',
    });

    if (result.error) {
      confirmButton.disabled = false;
      setMessage(result.error.message || 'Stripe confirmation failed.', true);
      return;
    }

    if (result.paymentIntent) {
      const target = new URL(returnUrl, window.location.origin);
      target.searchParams.set('payment_intent', result.paymentIntent.id);
      target.searchParams.set('redirect_status', result.paymentIntent.status);
      window.location.assign(target.toString());
      return;
    }

    confirmButton.disabled = false;
    setMessage('Stripe confirmation finished without a payment intent response. Please reload and inspect the payment status.', true);
  });
});
