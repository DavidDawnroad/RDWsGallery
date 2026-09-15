package com.test.prac.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.test.prac.controller.PageRenderController;

// @RestController를 제외한 나머지 Controller 에서 발생하는 Exception 을 처리
// assignableTypes = 특정 Controller 를 선택하는 옵션
@ControllerAdvice(assignableTypes = PageRenderController.class)
public class PracPageExceptionHandler {
	
	@ExceptionHandler(UserNotFoundException.class)
	public String handleUserNotFoundException(UserNotFoundException e, RedirectAttributes redirectAttributes) {
		
	    redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
	    
	    return "redirect:/login";
	}
}
