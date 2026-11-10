package com.eventiq.events;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {

	// Spring Data generates the query; an unknown email returns Optional.empty().
	Optional<Attendee> findByEmail(String email);

}
