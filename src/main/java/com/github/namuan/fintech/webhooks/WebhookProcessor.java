package com.github.namuan.fintech.webhooks;
@FunctionalInterface public interface WebhookProcessor { void process(WebhookEnvelope envelope) throws Exception; }
