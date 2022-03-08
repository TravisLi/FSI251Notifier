package com.kohang.fsi251notifier.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.FSI251Repository;

@Controller
@RequestMapping("/fsi251")
public class FSI251Controller {
	
	private final FSI251Repository repo;
	
	@Autowired
	public FSI251Controller(FSI251Repository r) {
		this.repo = r;
	}
	
	@GetMapping
	public String getFullList(@AuthenticationPrincipal User user, Model model){
		List<FSI251Data> list = repo.findAll();
		model.addAttribute("fsi251List", list);
		model.addAttribute("user",user);
		return "fsi251List";
		
	}
	
}
