/**
 * Client for the Python analytics service (FastAPI), consumed through {@code RestClient}:
 * exploratory analysis, revenue prediction and comment sentiment.
 *
 * <p>This package only calls the service and maps its responses; the analysis itself
 * lives in {@code python-service/}. Service failures are handled here so views can show
 * a notice instead of failing.
 */
package com.eventiq.analytics;
