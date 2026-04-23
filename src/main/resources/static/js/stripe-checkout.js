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
    const paymentStatus = (config.dataset.paymentStatus || '').toLowerCase();

    const setMessage = (message, isError) => {
        if (!messageNode) {
            return;
        }
        messageNode.textContent = message || '';
        messageNode.classList.toggle('stripe-message--error', Boolean(isError));
    };

    const restoreButton = () => {
        if (confirmButton) {
            confirmButton.disabled = false;
        }
    };

    const redirectToReturnPage = (paymentIntentId, status) => {
        const target = new URL(returnUrl, window.location.origin);
        target.searchParams.set('payment_intent', paymentIntentId);
        if (status) {
            target.searchParams.set('redirect_status', status);
        }
        window.location.assign(target.toString());
    };

    if (!publishableKey || !clientSecret || !returnUrl) {
        setMessage('Stripe page is missing configuration. Check the active deployment settings and the server created PaymentIntent state.', true);
        return;
    }

    if (paymentStatus && !['requires_payment_method', 'requires_confirmation', 'requires_action'].includes(paymentStatus)) {
        setMessage(`This PaymentIntent is not ready for browser confirmation. Current status: ${paymentStatus}.`, true);
        return;
    }

    let stripe;
    let elements;
    try {
        stripe = Stripe(publishableKey);
        elements = stripe.elements({ clientSecret });
        const paymentElement = elements.create('payment');
        paymentElement.mount('#payment-element');
    } catch (error) {
        setMessage(error && error.message ? error.message : 'Stripe Payment Element could not be initialized.', true);
        restoreButton();
        return;
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!confirmButton || confirmButton.disabled) {
            return;
        }

        confirmButton.disabled = true;
        setMessage('Submitting payment to Stripe test mode...', false);

        try {
            const result = await stripe.confirmPayment({
                elements,
                confirmParams: {
                    return_url: returnUrl,
                },
                redirect: 'if_required',
            });

            if (result.error) {
                setMessage(result.error.message || 'Stripe confirmation failed.', true);
                restoreButton();
                return;
            }

            if (result.paymentIntent) {
                const status = result.paymentIntent.status || '';
                if (status === 'succeeded') {
                    setMessage('Stripe confirmed the payment. Recording it in PayBridge...', false);
                } else {
                    setMessage(`Stripe returned status ${status || 'unknown'}. Opening the result page for verification.`, status !== 'processing');
                }
                redirectToReturnPage(result.paymentIntent.id, status);
                return;
            }

            setMessage('Stripe confirmation finished without a payment intent response. Reload the page and inspect the payment status.', true);
            restoreButton();
        } catch (error) {
            setMessage(error && error.message ? error.message : 'Unexpected Stripe confirmation error. Try again with a fresh demo order ID.', true);
            restoreButton();
        }
    });
});
