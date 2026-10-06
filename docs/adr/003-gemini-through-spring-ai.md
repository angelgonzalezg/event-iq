# ADR-003: Gemini through Spring AI, with Ollama as the local alternative

- **Status:** Accepted
- **Date:** 2026-10-05
- **Related:** [#30](https://github.com/angelgonzalezg/event-iq/issues/30), [#33](https://github.com/angelgonzalezg/event-iq/issues/33), [ADR-001](001-java-25-spring-boot-4-and-spring-ai-2.md), [ADR-005](005-sql-retrieval-as-rag-baseline.md)

## Context

The assistant in the Events module answers questions with a language model (ADR-005). The model has to:

- Cost nothing, without a credit card, for two engineers and for anyone who clones the portfolio.
- Be called from Java in a way that survives a change of model or provider, because both change often. While the project was being planned, Google limited the Gemini 2.5 models to accounts that already used them, and legacy *standard* API keys stopped working in September 2026.
- Fail without breaking the application: an exhausted quota or an invalid key must not take down the rest of the pages.

## Decision

The assistant uses the Google Gemini API through Spring AI's `ChatClient` and the `spring-ai-starter-model-google-genai` starter:

- The key is an *auth key* from Google AI Studio, read from `AI_API_KEY` in `.env`.
- The model is configuration, not code: `spring.ai.google.genai.chat.model` defaults to `gemini-3.8-flash`, and `AI_MODEL` overrides it.
- Only the `ai` package calls the model. It handles failures there and reports them as a notice, which views show with the `serviceWarning` alert while the rest of the page keeps working.

Ollama is the documented local alternative. Replacing the starter with `spring-ai-starter-model-ollama`, and the `spring.ai.google.genai.*` properties with `spring.ai.ollama.*`, switches providers without changing the assistant's code, because `ChatClient` is the same for every provider.

## Alternatives considered

- **OpenAI or Anthropic APIs:** no sustained free tier for students when the project was planned.
- **Direct HTTP calls to the Gemini API:** more code for requests, responses and errors, tied to one provider, and less representative of how Java teams integrate models.

## Consequences

- No cost, and changing the model or the provider is a configuration change.
- The free tier has per-minute and per-day quotas: an HTTP 429 means the quota is exhausted, not a bug. Demos should avoid bursts of questions.
- Under the [Gemini API terms](https://ai.google.dev/gemini-api/terms), Google may use prompts and responses sent to unpaid services to improve its products, and human reviewers may read them. That is acceptable here because the sample data is synthetic; a deployment with real attendee data would need a paid tier or a local model.
- The assistant needs internet access. CI has neither Oracle nor a Gemini key, so the test that starts the full application context (`contextLoads`) is disabled, and tests of the assistant mock the model.
- Model names must be checked in Google AI Studio before the assistant work starts, and again before the demo.
