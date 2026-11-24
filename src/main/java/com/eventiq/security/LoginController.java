package com.eventiq.security;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Returns a Thymeleaf view name for the web login page. */
@Controller
class LoginController {

	/** Shows the sign-in form and the result of the previous login or logout. */
	@GetMapping("/login")
	String login(@RequestParam Map<String, String> parameters, Model model) {
		if (parameters.containsKey("error")) {
			model.addAttribute("errorMessage", "login.invalid-credentials");
		}
		else if (parameters.containsKey("logout")) {
			model.addAttribute("successMessage", "login.signed-out");
		}

		return "security/login";
	}

}
