package com.eventiq.events;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

	boolean existsByEventEventId(Long eventId);

	long countByEventEventIdAndStatusNot(Long eventId, RegistrationStatus status);

	Optional<Registration> findByEventEventIdAndAttendeeAttendeeId(Long eventId, Long attendeeId);

	@EntityGraph(attributePaths = "attendee")
	List<Registration> findByEventEventIdAndStatusNotOrderByRegisteredAtAscRegistrationIdAsc(Long eventId,
			RegistrationStatus status);

}