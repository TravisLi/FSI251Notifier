package com.kohang.fsi251notifier.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthenicationController {

	@GetMapping("/login")
	public String getLogin() {
		return "login";
	}

	@GetMapping("/manual")
	public String getManual(@AuthenticationPrincipal User user, Model model) {
		model.addAttribute("user",user);
		model.addAttribute("msg", "Please select an action");
		return "manual";
	}
		
}
