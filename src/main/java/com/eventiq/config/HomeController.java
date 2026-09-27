package com.eventiq.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the home page, the entry point to both modules.
 */
@Controller
class HomeController {

	@GetMapping("/")
	String home() {
		return "home";
	}

}
