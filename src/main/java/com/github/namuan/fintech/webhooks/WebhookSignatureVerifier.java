package com.github.namuan.fintech.webhooks;
@FunctionalInterface public interface WebhookSignatureVerifier { boolean verify(WebhookEnvelope envelope); static WebhookSignatureVerifier trustForTests(){ return e -> true; } }
