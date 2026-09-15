package com.test.prac.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.test.prac.config.UserDetailsCustom;
import com.test.prac.dto.BoardShowListResponseDTO;
import com.test.prac.dto.BoardShowPostResponseDTO;
import com.test.prac.dto.ManageShowUserDataResponseDTO;
import com.test.prac.dto.ManageShowUserListResponseDTO;
import com.test.prac.dto.ReplyShowListResponseDTO;
import com.test.prac.entity.BoardEntity;
import com.test.prac.service.BoardService;
import com.test.prac.service.ManageService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
//@RequestMapping("/prac") // yml 파일에 server.servlet.context-path 옵션으로 Controller 공통 context path 주소 /prac 를 지정했기 때문에 주석 처리
public class PageRenderController {

	private final BoardService boardService;
	private final ManageService manageService;
	
	// 페이지 블록 하나 당 페이지 개수
	final int pageBlockSize = 10;

	/*
	 * 브라우저 URL 접속은 prac/login 으로 하되,
	 * Controller 의 return 주소는 templates 폴더가 default 이기 때문에 하위 폴더인 user 도 return 값에 적어줘야 한다.
	 */
	// 아이디 저장하기 체크박스 기능과 관련하여 @CookieValue로 파라미터 전달
	// Spring Security 적용 이후 @AuthenticationPrincipal로 로그인한 사용자에 대한 정보 이용 가능. expression 키워드로 특정 필드만 가져올 수도 있음
	@GetMapping("/login")
	public String login(@CookieValue(name = "savedId", required = false)String savedId, Model model, @AuthenticationPrincipal Object principal) {

		// 로그인한 사용자가 다시 로그인 창에 접근할 경우
		if (principal != null && !"anonymousUser".equals(principal)) {
			return "redirect:/board";
		}

		boolean isSaved = (savedId != null);

		// API에서 넘어온 savedId라는 이름의 쿠키에 id 데이터가 존재할 경우 프론트엔드에 해당 데이터 전달, 없으면 빈 String 전달
		model.addAttribute("savedId", isSaved ? savedId : "");
		// savedId가 있으면 true 이므로 체크박스를 체크 상태로 두라고 true 전달
		model.addAttribute("rememberIdChecked", isSaved);

		return "user/login";
	}

	@GetMapping("/signup")
	public String signup() {

		return "user/signup";
	}

	@GetMapping("/verify-user")
	public String verifyUser() {

		return "user/verify-user";
	}

	@GetMapping("/update-password")
	public String updatePassword() {

		return "user/update-password";
	}
	
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/manage/{userAccountId}")
	public String mypage(@PathVariable("userAccountId") String userAccountId, Model model) {
		
		ManageShowUserDataResponseDTO manageData = manageService.getManageDatas(userAccountId);
		
		model.addAttribute("manageData", manageData);
		
		return "manage/mypage";
	}
	
	@GetMapping("/portal/manage/admin")
	public String admin(Model model, @RequestParam(defaultValue = "0", name = "page")Integer page) {
		
		int postsByPage = 50;
		
		// page 변수에 음수가 들어와서 IllegalArgumentException이 발생하는 상황 방지
		if (page < 0) { page = 0; }
		
		Pageable pageable = PageRequest.of(page, postsByPage);
		
		Page<ManageShowUserListResponseDTO> userList = manageService.getUserList(pageable);
		
		int totalPage = userList.getTotalPages();
		
		if (page >= totalPage && totalPage > 0) {
			return "redirect:/portal/manage/admin?page=" + (totalPage - 1);
		}
		
		int currentPageBlock = page / pageBlockSize;
		int startPage = currentPageBlock * pageBlockSize + 1;
		int endPage = Math.min(startPage + pageBlockSize - 1, totalPage);
		
		if (endPage == 0) {
			endPage = 1;
		}
		
		model.addAttribute("userList", userList);
		model.addAttribute("startPage", startPage);
		model.addAttribute("endPage", endPage);
		
		return "manage/adminpage";
	}

	@GetMapping("/board")
	public String board(Model model, @RequestParam(defaultValue = "0", name = "page")Integer page) {

		// 이전 방식: JDBC식 수동 페이징(boot-jpa/AddressController.m29())
		// 지금 방식: Pageable Page<T> 객체 생성 방식을 사용한 JPA식 findAll(pageable)
		// org.springframework.data.domain 의 라이브러리를 import 해야 한다.
		// JPA에서는 1페이지 = page 0 이다.
		
		int postsByPage = 10;
		
		// page 변수에 음수가 들어와서 IllegalArgumentException이 발생하는 상황 방지
		if (page < 0) { page = 0; }
		
		// of()는 Java 라이브러리 표준 method 로, List.of()나 Map.of() 형태로 쓰인다면 객체를 new 선언 없이 한 줄로 간단하게 만들 때 사용한다.
		// PageRequest.of() : 백엔드 페이징 설정을 위한 method
		Pageable pageable = PageRequest.of(page, postsByPage);
		// ::(Method Reference) : Lambda Expression 이 단 하나의 method 만 호출하며, 그 method 도 추가작업 없이 다른 method 에 값을 넘겨주기만 할 때 사용하는 축약 문법
		Page<BoardShowListResponseDTO> boardPage = boardService.readBoardList(pageable);
		int totalPage = boardPage.getTotalPages();
		
		// 실제 존재하는 페이지 수보다 큰 요청이 들어올 경우 마지막 페이지로 고정
		if (page >= totalPage && totalPage > 0) {
			return "redirect:/board?page=" + (totalPage - 1);
		}
		
		int currentPageBlock = page / pageBlockSize; // 현재 페이지가 속한 페이지블록 (4페이지면 0번째 블록, 23페이지면 2번째 블록)
		int startPage = currentPageBlock * pageBlockSize + 1; // 페이지블록에서 시작할 페이지
		int endPage = Math.min(startPage + pageBlockSize - 1, totalPage); // 페이지블록에서 끝날 페이지
		
		// 게시글 데이터가 없을 경우 1페이지로 고정
		if (endPage == 0) {
			endPage = 1;
		}
		
		model.addAttribute("boardPage", boardPage);
		model.addAttribute("startPage", startPage);
		model.addAttribute("endPage", endPage);

		return "board/board";
	}
	
	// URL 경로 일부를 Query String 대신 변수로 사용하여 RESTFul 하게 나타내고 싶을 때 @PathVariable 사용
	@GetMapping("/board/{boardSeq}")
	public String boardReadDetailPage(@PathVariable("boardSeq") Long boardSeq,
									  @RequestParam(defaultValue = "0", name = "reply-page")Integer page,
									  Model model,
									  @AuthenticationPrincipal UserDetailsCustom userDetails) {
		
		// 실시간 조회수 데이터가 포함된 게시글 엔티티 단건 조회
		BoardShowPostResponseDTO post = boardService.readPost(boardSeq);
		
		int repliesByPage = 100;
		
		// page 변수에 음수가 들어와서 IllegalArgumentException이 발생하는 상황 방지
		if (page < 0) { page = 0; }
		
		Pageable pageable = PageRequest.of(page, repliesByPage);
		
		Page<ReplyShowListResponseDTO> replyPage = boardService.readReplyList(boardSeq, pageable);

		int totalPage = replyPage.getTotalPages();
		
		// 실제 존재하는 페이지 수보다 큰 요청이 들어올 경우 첫 댓글페이지로 고정
		if (page >= totalPage && totalPage > 0) {
			return "redirect:/board/" + boardSeq + "?reply-page=" + (totalPage - 1);
		}
		
		int currentPageBlock = page / pageBlockSize;
		int startPage = currentPageBlock * pageBlockSize + 1;
		int endPage = Math.min(startPage + pageBlockSize - 1, totalPage);
		
		// 댓글 데이터가 없을 경우 1페이지로 고정
		if (endPage == 0) {
			endPage = 1;
		}
		
		// 비로그인 상태일 경우 비회원을, 로그인 상태일 경우 로그인 사용자의 닉네임을 프론트엔드로 전송
		String replyDisplayNickname = (userDetails != null) ? userDetails.getSessionDTO().nickname() : "비회원";

		model.addAttribute("board", post);
		model.addAttribute("startPage", startPage);
		model.addAttribute("endPage", endPage);
		model.addAttribute("replyPage", replyPage);
		model.addAttribute("replyDisplayNickname", replyDisplayNickname);
		
		return "board/board-detail";
	}
	
	@PreAuthorize("isAuthenticated() and principal.isNormalStatus()")
	@GetMapping("/board/write")
	public String boardInsert() {
		
		return "board/board-write";
	}
	
	@PreAuthorize("hasRole('ADMIN') or @boardService.getUserAccountId(#boardSeq) == authentication.name")
	@GetMapping("/board/{boardSeq}/update")
	public String boardDisplayDetailPageForUpdate(@PathVariable("boardSeq") Long boardSeq, Model model) {
		
		// Spring Security 가 @PreAuthorize를 보고 boardUpdate() 진입 전에 조건 검사를 먼저 시행함
		// 로그인한 사용자가 ADMIN 권한을 가지고 있거나 작성자일 경우 method 가 실행되며, 조건을 통과하지 못 했을 경우 403 Forbidden 에러 발생
		// @Secure를 사용하는 방법도 있지만, 작성자 체크 조건을 걸 수 없기 때문에 여기서는 사용하지 않음		
		BoardEntity boardEntity = boardService.getPostedData(boardSeq);
		
		model.addAttribute("board", boardEntity);
		
		return "board/board-update";
	}
	
/*	board-detail 페이지에서 삭제 기능을 직접 담당함에 따라 delete 페이지 연결 제거
	@GetMapping("/board/{boardSeq}/delete")
	public String boardDelete(@PathVariable("boardSeq") Long boardSeq) {
		
		BoardEntity boardEntity = boardRepository.findById(boardSeq)
												 .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 게시글입니다."));
		
		
		
		return "board/board-delete";
	}
*/
}