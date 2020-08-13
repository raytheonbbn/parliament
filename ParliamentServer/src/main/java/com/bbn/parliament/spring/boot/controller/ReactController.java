package com.bbn.parliament.spring.boot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ReactController {

	@RequestMapping(value = {"/", "/query/**", "/update/**", "/graphstore/**", "/error/**"})
	public String index() {
		return "index.html";
	}
}
