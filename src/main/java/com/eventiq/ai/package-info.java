/**
 * Conversational assistant based on retrieval-augmented generation (RAG): retrieves
 * event data from Oracle and generates answers with Google Gemini through Spring AI.
 *
 * <p>Calls to the language model go through this package only, so no other module
 * depends on Spring AI directly. Model failures are handled here and reported to the
 * caller as a notice, so the rest of the application keeps working.
 */
package com.eventiq.ai;
