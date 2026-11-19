package com.eventiq.events;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {

	Optional<Attendee> findByEmail(String email);

}
