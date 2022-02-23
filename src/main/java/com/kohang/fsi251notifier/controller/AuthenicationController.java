package com.kohang.fsi251notifier.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthenicationController {

	@GetMapping("/login")
	public String getLogin() {
		return "login";
	}
		
}
