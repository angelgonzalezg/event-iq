package com.eventiq.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.eventiq.OracleTestDatabase;
import com.eventiq.events.Attendee;
import com.eventiq.events.Event;
import com.eventiq.events.Registration;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OracleTestDatabase.class)
class CommentRepositoryIT {

	@Autowired
	private CommentRepository comments;

	@Autowired
	private TestEntityManager entityManager;

	private Event event;

	private Attendee attendee;

	@BeforeEach
	void saveARegistration() {
		this.event = this.entityManager.persist(new Event("Java Day", null, LocalDate.of(2026, 11, 10), "Online", 50));
		this.attendee = this.entityManager.persist(new Attendee("Ana Lopez", "ana.lopez@example.com", null));
		this.entityManager.persist(new Registration(this.event, this.attendee));
	}

	@Test
	void storesTheContentRelationsAndOracleCreationDate() {
		Comment comment = this.comments.saveAndFlush(new Comment(this.attendee, this.event, "Great event!"));

		assertThat(comment.getCommentId()).isNotNull();
		assertThat(comment.getCreatedAt()).isNotNull();
		this.entityManager.clear();

		Comment stored = this.comments.findById(comment.getCommentId()).orElseThrow();
		assertThat(stored.getContent()).isEqualTo("Great event!");
		assertThat(stored.getCreatedAt()).isEqualTo(comment.getCreatedAt());
		assertThat(Hibernate.isInitialized(stored.getEvent())).isFalse();
		assertThat(Hibernate.isInitialized(stored.getAttendee())).isFalse();
		assertThat(stored.getEvent().getEventId()).isEqualTo(this.event.getEventId());
		assertThat(stored.getAttendee().getAttendeeId()).isEqualTo(this.attendee.getAttendeeId());
	}

	@Test
	void rejectsASecondCommentByTheSameAttendeeForTheSameEvent() {
		this.comments.saveAndFlush(new Comment(this.attendee, this.event, "Great event!"));

		assertThatThrownBy(() -> this.comments.saveAndFlush(new Comment(this.attendee, this.event, "Another comment")))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("UQ_COMMENTS_REGISTRATION");
	}

	@Test
	void allowsTheSameAttendeeAtAnotherEventAndAnotherAttendeeAtTheSameEvent() {
		Event otherEvent = this.entityManager.persist(new Event("Python Day", null, LocalDate.of(2026, 11, 11), "Online", 50));
		Attendee otherAttendee = this.entityManager.persist(new Attendee("Luis Perez", "luis.perez@example.com", null));
		this.entityManager.persist(new Registration(otherEvent, this.attendee));
		this.entityManager.persist(new Registration(this.event, otherAttendee));

		this.comments.saveAndFlush(new Comment(this.attendee, this.event, "Great event!"));
		this.comments.saveAndFlush(new Comment(this.attendee, otherEvent, "Useful talks!"));
		this.comments.saveAndFlush(new Comment(otherAttendee, this.event, "Well organized!"));
		this.entityManager.clear();

		assertThat(this.comments.count()).isEqualTo(3);
	}

	@Test
	void rejectsACommentWithoutARegistrationForThatEvent() {
		Event otherEvent = this.entityManager.persist(new Event("Python Day", null, LocalDate.of(2026, 11, 11), "Online", 50));

		assertThatThrownBy(() -> this.comments.saveAndFlush(new Comment(this.attendee, otherEvent, "Great event!")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
