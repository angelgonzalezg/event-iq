/**
 * Events and attendees: event management, attendee registration and the capacity rules
 * that govern registrations.
 *
 * <p>The same service layer is exposed through Thymeleaf controllers and through REST
 * controllers under {@code /api}. Registrations are the link with
 * {@code com.eventiq.payments}, which may depend on this package; this package must not
 * depend on it.
 */
package com.eventiq.events;
