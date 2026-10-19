package com.eventiq.payments;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.eventiq.events.Attendee;
import com.eventiq.events.Event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "comments")
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "comment_id")
	private Long commentId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "attendee_id", nullable = false)
	private Attendee attendee;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@Column(nullable = false, length = 1000)
	private String content;

	// Oracle supplies the current UTC date and time on insertion.
	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", nullable = false, insertable = false, columnDefinition = "DATE")
	private LocalDateTime createdAt;

	protected Comment() {
	}

	public Comment(Attendee attendee, Event event, String content) {
		this.attendee = attendee;
		this.event = event;
		this.content = content;
	}

	public Long getCommentId() {
		return this.commentId;
	}

	public Attendee getAttendee() {
		return this.attendee;
	}

	public void setAttendee(Attendee attendee) {
		this.attendee = attendee;
	}

	public Event getEvent() {
		return this.event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public String getContent() {
		return this.content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

}
