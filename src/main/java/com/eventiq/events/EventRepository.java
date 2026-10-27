package com.eventiq.events;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

	List<Event> findByEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate eventDate);

}
