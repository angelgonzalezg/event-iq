package com.eventiq.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EventServiceTests {

	private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

	@Mock
	private EventRepository events;

	@Mock
	private RegistrationRepository registrations;

	@InjectMocks
	private EventService service;

	@Test
	void createsAnEventFromTheRequestAndReturnsTheSavedData() {
		when(this.events.save(any(Event.class))).thenReturn(event(7L, "Java Day", DATE, 50));

		EventResponse response = this.service.create(request("Java Day", DATE, 50));

		ArgumentCaptor<Event> submitted = ArgumentCaptor.forClass(Event.class);
		verify(this.events).save(submitted.capture());
		assertThat(submitted.getValue().getName()).isEqualTo("Java Day");
		assertThat(submitted.getValue().getEventDate()).isEqualTo(DATE);
		assertThat(submitted.getValue().getCapacity()).isEqualTo(50);
		assertThat(response.eventId()).isEqualTo(7L);
		assertThat(response.name()).isEqualTo("Java Day");
	}

	@Test
	void updatesEveryFieldOfAnExistingEvent() {
		Event existing = event(7L, "Java Day", DATE, 50);
		when(this.events.findById(7L)).thenReturn(Optional.of(existing));

		EventResponse response = this.service.update(7L, request("Java Day 2", LocalDate.of(2026, 12, 1), 80));

		assertThat(existing.getName()).isEqualTo("Java Day 2");
		assertThat(existing.getEventDate()).isEqualTo(LocalDate.of(2026, 12, 1));
		assertThat(existing.getCapacity()).isEqualTo(80);
		assertThat(response).isEqualTo(EventResponse.from(existing));
	}

	@Test
	void rejectsUpdatingAnEventThatDoesNotExist() {
		when(this.events.findById(99L)).thenReturn(Optional.empty());

		assertNotFound(() -> this.service.update(99L, request("Java Day", DATE, 50)));
	}

	@Test
	void findsAnEventById() {
		when(this.events.findById(7L)).thenReturn(Optional.of(event(7L, "Java Day", DATE, 50)));

		assertThat(this.service.findById(7L)).isEqualTo(new EventResponse(7L, "Java Day", "Talks", DATE, "Online", 50));
	}

	@Test
	void rejectsFindingAnEventThatDoesNotExist() {
		when(this.events.findById(99L)).thenReturn(Optional.empty());

		assertNotFound(() -> this.service.findById(99L));
	}

	@Test
	void listsEveryEventSortedByDate() {
		when(this.events.findAll(any(Sort.class))).thenReturn(List.of(event(7L, "Java Day", DATE, 50),
				event(8L, "Data Day", DATE.plusDays(3), 120)));

		assertThat(this.service.findAll()).extracting(EventResponse::name).containsExactly("Java Day", "Data Day");
	}

	@Test
	void returnsAnEmptyListWhenNoEventsExist() {
		when(this.events.findAll(any(Sort.class))).thenReturn(List.of());

		assertThat(this.service.findAll()).isEmpty();
	}

	@Test
	void deletesAnEventThatHasNoRegistrations() {
		Event existing = event(7L, "Java Day", DATE, 50);
		when(this.events.findById(7L)).thenReturn(Optional.of(existing));
		when(this.registrations.existsByEventEventId(7L)).thenReturn(false);

		this.service.delete(7L);

		verify(this.events).delete(existing);
	}

	@Test
	void refusesToDeleteAnEventThatHasRegistrations() {
		when(this.events.findById(7L)).thenReturn(Optional.of(event(7L, "Java Day", DATE, 50)));
		when(this.registrations.existsByEventEventId(7L)).thenReturn(true);

		assertThatExceptionOfType(EventHasRegistrationsException.class)
			.isThrownBy(() -> this.service.delete(7L))
			.satisfies(error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		verify(this.events, never()).delete(any(Event.class));
	}

	@Test
	void rejectsDeletingAnEventThatDoesNotExist() {
		when(this.events.findById(99L)).thenReturn(Optional.empty());

		assertNotFound(() -> this.service.delete(99L));
		verifyNoInteractions(this.registrations);
	}

	private static void assertNotFound(Runnable action) {
		assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(action::run).satisfies(error -> {
			assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
			assertThat(error.getReason()).isEqualTo("Event not found.");
		});
	}

	private static EventRequest request(String name, LocalDate date, int capacity) {
		EventRequest request = new EventRequest();
		request.setName(name);
		request.setDescription("Talks");
		request.setEventDate(date);
		request.setLocation("Online");
		request.setCapacity(capacity);
		return request;
	}

	private static Event event(Long id, String name, LocalDate date, int capacity) {
		Event event = new Event(name, "Talks", date, "Online", capacity);
		ReflectionTestUtils.setField(event, "eventId", id);
		return event;
	}
}