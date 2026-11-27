package com.eventiq.events;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/registrations")
class RegistrationController {

	private final RegistrationService registrationService;

	private final EventService eventService;

	private final AttendeeService attendeeService;

	RegistrationController(RegistrationService registrationService, EventService eventService,
			AttendeeService attendeeService) {
		this.registrationService = registrationService;
		this.eventService = eventService;
		this.attendeeService = attendeeService;
	}

	@GetMapping("/new")
	String newForm(@RequestParam(required = false) Long eventId, Model model) {
		RegistrationForm form = new RegistrationForm();
		form.setEventId(eventId);
		model.addAttribute("registration", form);
		model.addAttribute("events", this.eventService.findAll());
		return "registrations/form";
	}

	@PostMapping
	String create(@Valid @ModelAttribute("registration") RegistrationForm form, BindingResult result, Model model,
			RedirectAttributes redirect) {
		AttendeeResponse attendee = null;
		if (!result.hasErrors()) {
			try {
				attendee = this.attendeeService.findByEmail(form.getEmail());
			}
			catch (ResponseStatusException ex) {
				result.rejectValue("email", "registrations.form.email.unknown");
			}
		}
		if (result.hasErrors()) {
			model.addAttribute("events", this.eventService.findAll());
			return "registrations/form";
		}
		try {
			this.registrationService.register(new CreateRegistrationRequest(form.getEventId(), attendee.attendeeId()));
		}
		catch (EventFullException ex) {
			redirect.addFlashAttribute("errorMessage", "registrations.event-full");
			return "redirect:/registrations/new?eventId=" + form.getEventId();
		}
		catch (AlreadyRegisteredException ex) {
			redirect.addFlashAttribute("errorMessage", "registrations.already-registered");
			return "redirect:/registrations/new?eventId=" + form.getEventId();
		}
		redirect.addFlashAttribute("successMessage", "registrations.created");
		return "redirect:/events/" + form.getEventId();
	}

	@PostMapping("/{id}/cancel")
	String cancel(@PathVariable Long id, RedirectAttributes redirect) {
		RegistrationResponse cancelled = this.registrationService.cancel(id);
		redirect.addFlashAttribute("successMessage", "registrations.cancelled");
		return "redirect:/events/" + cancelled.eventId();
	}

}
