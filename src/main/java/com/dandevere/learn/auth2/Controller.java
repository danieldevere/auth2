package com.dandevere.learn.auth2;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class Controller {
	@GetMapping("/info")
	public String hello(Principal principal) {
		System.out.println(principal.getName());
		return "hello world";
	}
}
