package com.test.prac.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.test.prac.config.UserDetailsCustom;
import com.test.prac.dto.BoardCreateRequestDTO;
import com.test.prac.dto.BoardIncreaseViewsRequestDTO;
import com.test.prac.dto.BoardUpdateRequestDTO;
import com.test.prac.dto.ReplyCreateRequestDTO;
import com.test.prac.service.BoardService;

import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardAPIController {

	private final BoardService boardService;
	
	@PostMapping("/{boardSeq}/increase-views")
	public ResponseEntity<Void> increaseViews(@PathVariable("boardSeq") Long boardSeq, @RequestBody BoardIncreaseViewsRequestDTO viewsDTO) {
		
		boardService.increaseViews(boardSeq, viewsDTO);
		
		return ResponseEntity.ok().build(); // body 없이 상태 코드와 헤더만 반환할 때(ex-DELETE나 UPDATE 작업) build()를 사용
	}

	// 게시글 등록
	@PreAuthorize("isAuthenticated() and principal.isNormalStatus()")
	@PostMapping("/post")
	public ResponseEntity<Map<String, String>> createPost(@Valid @RequestBody BoardCreateRequestDTO createDTO, @AuthenticationPrincipal UserDetailsCustom userDetails) {
		
		// UserDetailsCustom에 있는 로그인 사용자 정보 중 @id 전달
		boardService.createPost(createDTO, userDetails.getSessionDTO().seq());
		
		return ResponseEntity.ok(Map.of("status", "SUCCESS"));
	}
	
	// 게시글 수정
	@PreAuthorize("hasRole('ADMIN') or @boardService.getUserAccountId(#boardSeq) == authentication.name")
	@PutMapping("/{boardSeq}/update")
	public ResponseEntity<Map<String, String>> updatePost(@PathVariable("boardSeq") Long boardSeq,
														  @Valid @RequestBody BoardUpdateRequestDTO updateDTO,
														  @AuthenticationPrincipal UserDetailsCustom userDetails) {
		
		boardService.updatePost(boardSeq, updateDTO, userDetails);
		
		return ResponseEntity.ok(Map.of("status", "SUCCESS"));
	}

	// 게시글 삭제
	@PreAuthorize("hasRole('ADMIN') or @boardService.getUserAccountId(#boardSeq) == authentication.name")
	@PostMapping("/{boardSeq}/delete")
	public ResponseEntity<Void> deletePost(@PathVariable("boardSeq") Long boardSeq, @AuthenticationPrincipal UserDetailsCustom userDetails) {
		
		boardService.deletePost(boardSeq, userDetails);
		
		return ResponseEntity.ok().build();
	}
	
	//게시글 추천
	@PreAuthorize("isAuthenticated() and principal.isNormalStatus()")
	@PostMapping("/{boardSeq}/like")
	public ResponseEntity<Long> increaseLikes(@PathVariable("boardSeq") Long boardSeq, @AuthenticationPrincipal UserDetailsCustom userDetails) {
		
		Long likes = boardService.increaseLikes(boardSeq, userDetails.getSessionDTO().seq());
		
		return ResponseEntity.ok(likes);
		
	}
	
	//게시글 비추천
	@PreAuthorize("isAuthenticated() and principal.isNormalStatus()")
	@PostMapping("/{boardSeq}/dislike")
	public ResponseEntity<Long> increaseDislikes(@PathVariable("boardSeq") Long boardSeq, @AuthenticationPrincipal UserDetailsCustom userDetails) {
		
		Long dislikes = boardService.increaseDislikes(boardSeq, userDetails.getSessionDTO().seq());
		
		return ResponseEntity.ok(dislikes);
		
	}
	
	// 댓글 등록
	@PreAuthorize("isAuthenticated() and principal.isNormalStatus()")
	@PostMapping("/{boardSeq}/reply/create")
	public ResponseEntity<Map<String, String>> createReply(@PathVariable("boardSeq") Long boardSeq,
														   @Valid @RequestBody ReplyCreateRequestDTO replyDTO,
														   @AuthenticationPrincipal UserDetailsCustom userDetails) {
		
		boardService.createReply(boardSeq, replyDTO, userDetails.getSessionDTO().seq());
		
		return ResponseEntity.ok(Map.of("status", "SUCCESS"));
	}
	
	// 댓글 삭제
	@PreAuthorize("hasRole('ADMIN') or @boardService.getUserAccountIdForReply(#boardSeq, #replySeq) == authentication.name")
	@PostMapping("/{boardSeq}/reply/{replySeq}/delete")
	public ResponseEntity<Void> deleteReply(@P("boardSeq") @PathVariable("boardSeq") Long boardSeq,
											@P("replySeq") @PathVariable("replySeq") Long replySeq,
			   								@AuthenticationPrincipal UserDetailsCustom userDetails) {
		
		boardService.deleteReply(boardSeq, replySeq, userDetails);
		
		return ResponseEntity.ok().build();
		
	}
}
