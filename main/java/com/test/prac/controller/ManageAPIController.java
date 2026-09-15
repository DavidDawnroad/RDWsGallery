package com.test.prac.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.test.prac.config.UserDetailsCustom;
import com.test.prac.dto.ManageChangeUserStatusRequestDTO;
import com.test.prac.dto.ManageChangeNicknameRequestDTO;
import com.test.prac.service.ManageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manage")
@RequiredArgsConstructor
public class ManageAPIController {

	private final ManageService manageService;
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/my/change-nickname")
	public ResponseEntity<Map<String, String>> changeMyNickname(@Valid @RequestBody ManageChangeNicknameRequestDTO nicknameDTO,
															    @AuthenticationPrincipal UserDetailsCustom userDetails) {
				
		String newNickname = manageService.changeMyNickname(userDetails.getSessionDTO().seq(), nicknameDTO.getNickname());
		
		return ResponseEntity.ok(Map.of("status", "SUCCESS", "newNickname", newNickname));
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/admin/change-nickname/{userAccountId}")
	public ResponseEntity<Map<String, String>> changeNicknameByAdmin(@PathVariable("userAccountId") String userAccountId,
																	 @Valid @RequestBody ManageChangeNicknameRequestDTO nicknameDTO) {
		
		String newNickname = manageService.changeNicknameByAdmin(userAccountId, nicknameDTO.getNickname());
		
		return ResponseEntity.ok(Map.of("status", "SUCCESS", "newNickname", newNickname));
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/admin/change-status/{userAccountId}")
	public ResponseEntity<Map<String, String>> changeUserStatus(@PathVariable("userAccountId") String userAccountId,
																@Valid @RequestBody ManageChangeUserStatusRequestDTO uStatDTO) {
		
		manageService.changeUserStatusByAdmin(userAccountId, uStatDTO);
		
		return ResponseEntity.ok(Map.of("status", "SUCCESS"));
	}
}
