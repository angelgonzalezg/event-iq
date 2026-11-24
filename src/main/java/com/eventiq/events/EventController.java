package com.eventiq.events;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf pages to list, create, edit and delete events. It follows the conventions in
 * {@code docs/view-conventions.md} and calls the same {@link EventService} as the REST API.
 */
@Controller
@RequestMapping("/events")
class EventController {

	private final EventService eventService;

	EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping
	String list(Model model) {
		model.addAttribute("events", this.eventService.findAll());
		return "events/list";
	}

	@GetMapping("/new")
	String newForm(Model model) {
		model.addAttribute("event", new EventRequest());
		return "events/form";
	}

	@PostMapping
	String create(@Valid @ModelAttribute("event") EventRequest form, BindingResult result,
			RedirectAttributes redirect) {
		if (result.hasErrors()) {
			return "events/form";
		}
		this.eventService.create(form);
		redirect.addFlashAttribute("successMessage", "events.created");
		return "redirect:/events";
	}

	@GetMapping("/{id}/edit")
	String editForm(@PathVariable Long id, Model model) {
		model.addAttribute("event", EventRequest.from(this.eventService.findById(id)));
		model.addAttribute("eventId", id);
		return "events/form";
	}

	@PostMapping("/{id}")
	String update(@PathVariable Long id, @Valid @ModelAttribute("event") EventRequest form, BindingResult result,
			Model model, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			model.addAttribute("eventId", id);
			return "events/form";
		}
		this.eventService.update(id, form);
		redirect.addFlashAttribute("successMessage", "events.updated");
		return "redirect:/events";
	}

	@PostMapping("/{id}/delete")
	String delete(@PathVariable Long id, RedirectAttributes redirect) {
		try {
			this.eventService.delete(id);
			redirect.addFlashAttribute("successMessage", "events.deleted");
		}
		catch (EventHasRegistrationsException ex) {
			redirect.addFlashAttribute("errorMessage", "events.delete.has-registrations");
		}
		catch (DataIntegrityViolationException ex) {
			redirect.addFlashAttribute("errorMessage", "events.delete.in-use");
		}
		return "redirect:/events";
	}
}
