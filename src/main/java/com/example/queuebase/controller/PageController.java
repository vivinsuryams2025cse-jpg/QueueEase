package com.example.queuebase.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

	@GetMapping({"/", "/index"})
	public String index() {
		return "index";
	}

	@GetMapping("/dashboard")
	public String dashboard() {
		return "dashboard";
	}

	@GetMapping("/ui/doctors")
	public String doctors() {
		return "doctors";
	}

	@GetMapping("/ui/patients")
	public String patients() {
		return "patients";
	}

	@GetMapping("/ui/token")
	public String token() {
		return "token";
	}

	@GetMapping("/ui/queue")
	public String queue() {
		return "queue";
	}

	@GetMapping("/ui/history")
	public String history() {
		return "history";
	}
}