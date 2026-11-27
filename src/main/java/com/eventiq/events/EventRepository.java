package com.eventiq.events;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EventRepository extends JpaRepository<Event, Long> {

	List<Event> findByEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate eventDate);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Event> findWithLockByEventId(Long eventId);

}
