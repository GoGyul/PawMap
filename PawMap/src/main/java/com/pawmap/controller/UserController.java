package com.pawmap.controller;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.websocket.server.PathParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.pawmap.VO.Criteria;
import com.pawmap.VO.FileVO;
import com.pawmap.VO.PageVO;
import com.pawmap.VO.ShelterVO;
import com.pawmap.VO.UserVO;
import com.pawmap.configuration.auth.PrincipalDetails;
import com.pawmap.configuration.auth.PrincipalDetailsService;
import com.pawmap.mapper.ShelterMapper;
import com.pawmap.mapper.UserMapper;
import com.pawmap.service.BoardService;
import com.pawmap.service.FileService;
import com.pawmap.service.UserService;
import com.pawmap.util.CookieUtil;
import com.pawmap.util.FileUtils;





@Controller
public class UserController {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private PrincipalDetailsService principalDetailsService;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	

	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired 
	private UserMapper userMapper;
	
	
	@Autowired
	private FileService fileService;
	
	
	@Autowired
	private ShelterMapper shelterMapper;
	
	@Autowired
	private BoardService boardService;
	
	 
	/*
	 * 페이지 이동 관련 메소드
	 * 
	 * */
	//공백 및 / 요청시 메인페이지로 이동

//	@GetMapping({"","/"})
//	public String index() {
//		return "index2";
//	}
	
	//공백 및 / 요청시 메인페이지로 이동, 하단부 shelter정보와 매핑되는 file(이미지)정보 갖고옴
	@RequestMapping({"","/"})
	public String index(ShelterVO vo, Model model) {
		
		System.out.println("index 통과===================");
		

		//!참고 : list가져오는것은 조건이 따로 필요없어서 vo를 매개변수로 넣지않아도 해당 list를 DB에서 뽑아올 수 있습니다!
		List<ShelterVO> shelter = shelterMapper.getShelterList(null);
		System.out.println("index - shelter에 담긴값 출력===========" + shelter);
		
		List<HashMap<String,Object>> latelyShelterBoardListForMain = boardService.getLatelyBoardListForShelterBoardMain();
		

		//** VO에 담긴 값이 없으므로 의미 없는 코드임
//		System.out.println("index의 getShelterSeq값 =============" + vo.getShelterSeq());
		
		//103번라인에서 똑같은 코드를 shelter 변수에 저장했기때문에 shelter 변수를 사용하겠습니다.
//		 model.addAttribute("shelter", shelterMapper.getShelterList(vo));
		 model.addAttribute("shelter", shelter);
		 model.addAttribute("shelterPic", latelyShelterBoardListForMain);
		 
		 System.out.println("shelterPic==========" + latelyShelterBoardListForMain);
		 
		
		return "index2";
	}
	


	//회사 소개페이지로 이동
	@GetMapping("/about")
	public String about() {
		return "about";
	}
	
	
	//관리자 페이지 이동하는 메소드
	@GetMapping("/admin")
	public  String admin() {
		return "admin_index";
	}
	
	//마이페이지 이동하는 메소드
	@GetMapping("/mypage")
	public  String mypageIndex() {
		return "my_page_main";
	}
	
	//마이페이지-> 회원정보 수정으로 이동하는 메소드
	@GetMapping("/mypage/userInfo")
	public  String userInfo(UserVO vo, Model model) {
		UserVO user = userService.getUser(vo);
		System.out.println("user 정보 출력== " + user);
		
		 model.addAttribute("user", userService.getUser(vo));
		
		return "my_account_update";
	}
	

	
	//스프링 시큐리티가 해당 주소를 낚아채감 추후 설정 필요
	//SecurityConfig파일 생성 후 활성화안됨 (스프링 필터가 가로채기때문)
	@GetMapping("/loginForm")
	public String loginForm() {
		return "login-form";
	}

	@GetMapping("/joinForm")
	public String joinForm() {
		return "join-form";
	}
	
	//일반 유저 회원가입으로 이동
	@GetMapping("/userJoinForm")
	public String userJoinForm() {
		return "user-join-form";
	}
	//병원 유저 회원가입으로 이동
	@GetMapping("/hospitalJoinForm")
	public String hospitalJoinForm() {
		return "hospital-join-form";
	}

				
	// 비밀번호를 잊어버렸습니까? 클릭시 forgotPW 
		@GetMapping("/searchIdPw")
		public String showFindLoginPasswd() {
			return "searchIdPw";
		}
	
	//관리자페이지 -> 회원정보 관리로 이동
	@GetMapping("/admin/userInfo")
	public String userInfoForm() {
		return "admin_user";
	}
	
	

	
	
	
	
	
	
	/*
	 *로직관련 메서드
	 * 
	 * */
	//마이페이지 -> 유저 정보 업데이트
	@PostMapping("/mypage/updateUser")
	public String updateUser(UserVO vo, @AuthenticationPrincipal PrincipalDetails principal, HttpSession session
							,HttpServletRequest request, MultipartHttpServletRequest mhsr) throws IOException {
		System.out.println("updateUser 호출 !! ");
		System.out.println("UserVO getNickname ====="+ vo.getUserNickname());
		System.out.println("UserVO getUserId====="+ vo.getUserId());
		System.out.println("UserVO getpassword====="+ vo.getUserPassword());
		
		
		if(vo.getUserPassword() != null && !(vo.getUserPassword().equals(""))) {
			String rawPassword = vo.getUserPassword();
			String encPassword = bCryptPasswordEncoder.encode(rawPassword);
			vo.setUserPassword(encPassword);
		} else {
			vo.setUserPassword(null);
		}
		
		//  유저 프로필 업데이트를 위한 로직
		String userId = vo.getUserId();
		String userType = vo.getUserType();
		int userSeq = vo.getUserSeq();
		
		FileUtils fileUtils = new FileUtils();
		List<FileVO> fileList = fileUtils.parseFileInfo(userSeq, request, mhsr,userId);

		if(CollectionUtils.isEmpty(fileList) == false) {

			fileList.get(0).setBoardType(userType);
			System.out.println("유저타입은  N 입니다.");
			System.out.println("fileList ======" + fileList);
			fileService.insertUserProfile(fileList);	
			vo.setUserProfile(fileList.get(0).getOriginalFileName());

		}
		
		
		
		userService.updateUser(vo);
		
		// user에 직접 들어갈 수 있도록 여기서 데이터 입력해줌
		UserDetails userDetails = principalDetailsService.loadUserByUsername(vo.getUserId());
		
		//세션 등록
		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		SecurityContext securityContext = SecurityContextHolder.getContext();
		securityContext.setAuthentication(authentication);
		
		session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
		
//		Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(vo.getUserId(), vo.getUserPassword(),principal.getAuthorities()));
//		SecurityContextHolder.getContext().setAuthentication(authentication);
		
		return "redirect:/";
	}
	
	//마이페이지-> 회원탈퇴 로직 (미완성)
	@GetMapping("/mypage/deleteUser")
	public  String deleteUser(UserVO vo, HttpSession session) {
		System.out.println("deleteUser 호출 !!!");
		System.out.println("vo 출력 === " + vo);
		session.invalidate();
		userService.deleteUser(vo);
		 
		 return "redirect:/";
	}
	
	
	
	//OAuth 로그인을해도 PrincipalDetails로 받을수 있고, userDetails로 로그인해도 PrincipalDetails로 받을 수 있음
	@GetMapping("/user")
	public @ResponseBody String user(@AuthenticationPrincipal PrincipalDetails principalDetails) {
		System.out.println("principalDetails : "+principalDetails.getUser());
		return "user";
	}
	

	
	@GetMapping("/manager")
	public @ResponseBody String manager() {
		return "manager";
	}
	

	//로그인 시 아이디 비밀번호 확인 메소드 
	//cookieUtil에 setAttribute
	@RequestMapping("/doLogin")
	@ResponseBody
	public String doLogin(HttpServletResponse response, @RequestParam Map<String, Object> param) {
		Map<String, Object> rs = userService.loginV2(param);
	 
		String resultCode = (String) rs.get("resultCode");
		UserVO userId = (UserVO) rs.get("User");
	 
		if (resultCode.startsWith("S-")) {
			 CookieUtil.setAttribute(response, "uerId", userId.getUserId() + "");
		 	}
		return (String) rs.get("msg");
	 }

	//회원 가입 메소드
	@PostMapping("/join")
	public String join(UserVO vo) {
		System.out.println(vo);
		vo.setRole("ROLE_USER");
		vo.setUserType("N");
		String rawPassword = vo.getUserPassword();
		String encPassword = bCryptPasswordEncoder.encode(rawPassword);
		vo.setUserPassword(encPassword);
		
		System.out.println("vo출력 : "+vo);
		userService.insertUser(vo);
		
		return "redirect:/loginForm";
	}
	//병원 회원 가입 메소드
	@PostMapping("/hospitalJoin")
	public String hospitalJoin(UserVO vo) {
		System.out.println(vo);
		vo.setRole("ROLE_USER");
		vo.setUserType("H");
		String rawPassword = vo.getUserPassword();
		String encPassword = bCryptPasswordEncoder.encode(rawPassword);
		vo.setUserPassword(encPassword);
		userService.insertHospitalUser(vo);
		
		userService.insertHospitalData(vo);
		
		return "redirect:/loginForm";
	}

	
	// 아이디 중복 검사 => 회원 가입 페이지에서 아이디 중복 메세지 안뜸
	@RequestMapping(value = "/userIdChk", method = RequestMethod.POST)
	@ResponseBody
	public String userIdChk(String userId) throws Exception{
		int result = userService.idCheck(userId);
		if(result != 0) {
			return "fail";	// 중복 아이디가 존재
		} else {
			return "success";	// 중복 아이디 x
		}	
		
	} // memberIdChkPOST() 종료	
	
		

	@PreAuthorize("hasRole('ROLE_ADMIN')") //하기 메서드가 실행하기 직전에 실행됨
	@GetMapping("/data")
	public @ResponseBody String data() {

		return "login";
	}
	

	// 비밀번호 찾기 화면에서 데이터 받기 
	@RequestMapping("/doForgotPw")
	@ResponseBody
	public String doFindLoginPasswd(@RequestParam Map<String, Object> param) {
		Map<String, Object> findLoginIdRs = userService.findLoginPasswd(param);
		
		return (String) findLoginIdRs.get("msg");
	}
	
	//일반 유저 목록 표출
	@GetMapping("/getUserList")
	public String getUserList(UserVO vo, Model model, Criteria cri) {
		System.out.println("getUserList 호출 !!");
		
		cri.setStartNum((cri.getPageNum()-1) *cri.getAmount());
		//리스트에 담긴 값 확인용 코드
		List<UserVO> list = userMapper.getUserListWithPaging(cri);
		System.out.println("UserList 표출=="+list);
		
		int total = userMapper.getUserCount();
		
		if(list.size()!=0) {
			System.out.println("userList 담긴거 = " +list.get(0));
			System.out.println("userList 총 갯수 = " +list.size());
		}
		
		
		
		model.addAttribute("userList", list);
		model.addAttribute("pageMaker", new PageVO(cri,total));
		
		return "admin_user";
	}
	
	//특정 유저 정보 출력
	@GetMapping("/getUser")
	public String getUser(UserVO vo, Model model) {
		System.out.println("getUser 호출 !!");
		System.out.println("vo.getSeq() ==== "+ vo.getUserSeq());
		
		UserVO user = userService.getUser(vo);
		System.out.println("user 정보 출력== " + user);
		
		 model.addAttribute("user", userService.getUser(vo));
		
		return "admin_user_detail";
	}
	
	//병원 유저 목록 표출
	@GetMapping("/getHospitalList")
	public String getHospitalList(UserVO vo, Model model, Criteria cri) {
		System.out.println("getHospitalList 호출 !!");
		cri.setStartNum((cri.getPageNum()-1)*cri.getAmount());
		
		//리스트에 담긴 값 확인용 코드
		List<UserVO> list = userMapper.getHospitalUserListWithPaging(cri);
		
		if(list.size()!=0) {
			System.out.println("userList 담긴거 = " +list.get(0));
			System.out.println("userList 총 갯수 = " +list.size());
		}
		int total = userMapper.getHospitalUserCount();
		
		model.addAttribute("userList",list);
		model.addAttribute("pageMaker", new PageVO(cri,total));
		
		return "admin_user";
		
	}
	
	
	
	
	//관리자 페이지 -> 유저 정보 업데이트
	@PostMapping("/admin/updateUser")
	public String adminUpdateUser(UserVO vo) {
		System.out.println("Admin updateUser 호출 !! ");
		System.out.println("UserVO getNickname ====="+ vo.getUserId());
		userService.updateUserAdmin(vo);
		
		return "redirect:/admin";
	}
	
	//관리자페이지-> 회원삭제 로직 (미완성)
		@GetMapping("/admin/deleteUser")
		public  String adminDeleteUser(UserVO vo) {
			System.out.println("Admin deleteUser 호출 !!!");
			userService.deleteUser(vo);
			 
			 return "redirect:/admin";
		}


		
	//시큐리티 세션 참고용 메서드
	@GetMapping("/check")
	public @ResponseBody String check(@AuthenticationPrincipal PrincipalDetails principal) {
		String name = principal.toString();
		return name;
	}
	



	// 아이디 찾기

			@RequestMapping(value = "/pawmap/searchIdPw", method = RequestMethod.POST)
			@ResponseBody
			public String searchId(@RequestParam("userName") String userName, 
				@RequestParam("userTelNum") String userTelNum) {
				System.out.println("사용자 전화번호 : "+userTelNum);
				String result = userMapper.searchId(userName, userTelNum);
			    System.out.println("결과: "+result);
			
			
			
				return result;
			}
			


	// 비밀번호 찾기 화면에서 데이터 받기 

		@RequestMapping("/searchPw")
		@ResponseBody
		public String doFindLoginPasswd(@RequestParam Map<String, Object> param , HttpServletResponse response) throws IOException {
//			String msg= (String) findLoginIdRs.get("msg");
			
			
			String userId = (String) param.get("userId");
			String userName = (String) param.get("userName");
			String userEmail = (String) param.get("userEmail");
			
			UserVO user  = userMapper.searchPwd(userId, userName);
//			UserVO userWrongEmail = 
//					(user.getUserId() ==(String) param.get("userId"))
//					&& (user.getUserEmail() != (String) param.get("userEmail"));
			
			// 입력한 아이디 정보는 회원과 일치하지만 이메일정보는 일치하지 않을 때
				
			
			PrintWriter out = response.getWriter();
			 if(user == null) {
				response.setContentType("text/html; charset=UTF-8");
				
				// 입력한 정보가 일치하지 않을 때
				out.println("<script>alert('일치하는 회원이 없습니다'); location.href='searchIdPw';</script>");
				
				out.flush();
			 }else if (!user.getUserEmail().equals(userEmail)){
					response.setContentType("text/html; charset=UTF-8");
					
					out.println("<script>alert('이메일 정보가 일치하지 않습니다'); location.href='searchIdPw';</script>");
					
					out.flush();
			} else {
				response.setContentType("text/html; charset=UTF-8");
				
				// 입력한 정보와 회원정보가 일치할 때 
				out.println("<script>alert('입력하신 메일로 임시 패스워드가 발송되었습니다'); location.href='searchIdPw';</script>");
				
				out.flush();
				Map<String, Object> findLoginIdRs = userService.findLoginPasswd(param);
			
			}
			 
			return "loginForm";
			
		}
		
		


		// 아이디 중복 체크
		@RequestMapping("idCheck")
		@ResponseBody
		public String idCheck(@RequestParam("id") String id) throws Exception {
			int result = userService.idCheck(id);
			
			if(result > 0) {
				return "fail";
			} else {
				return "ok";
			}
		}
		
		// 닉네임 중복 체크
		@RequestMapping("nickCheck")
		@ResponseBody
		public String nickCheck(@RequestParam("nickname") String nickname) throws Exception {
			int result = userService.nickCheck(nickname);
			
			if(result > 0) {
				return "fail";
			} else {
				return "ok";
			}
		}
		
		// 회원가입 닉네임중복
		@RequestMapping("/mypage/mnickCheck")
		@ResponseBody
		public String mnickCheck(@RequestParam("nickname") String nickname) throws Exception {
			int result = userService.nickCheck(nickname);
			
			if(result > 0) {
				return "fail";
			} else {
				return "ok";
			}
		}
		
		// 이메일 중복 체크
		@RequestMapping("/mypage/emailCheck")
		@ResponseBody
		public String emailCheck(@RequestParam("email") String email) throws Exception {
			int result = userService.emailCheck(email);
			
			if(result > 0) {
				return "fail";
			} else {
				return "ok";
			}
		}
		
		// 관리자 닉네임 중복 체크
		@RequestMapping("/admin/anickCheck")
		@ResponseBody
		public String anickCheck(@RequestParam("nickname") String nickname) throws Exception {
			int result = userService.nickCheck(nickname);
			
			if(result > 0) {
				return "fail";
			} else {
				return "ok";
			}
		}
		

		// 사업자등록번호 중복 체크
		@RequestMapping("comCheck")
		@ResponseBody
		public String comCheck(@RequestParam("comnum") String comnum) throws Exception {
			System.out.print(comnum);
			System.out.print("comcheck 들어옴!!");
			
			int result = userService.comCheck(comnum);

			System.out.print(result);
			
			if(result > 0) {
				return "fail";
			} else {
				return "ok";
			}
		}
		



		   //   프로필 삭제 메서드
	      @RequestMapping("/mypage/deleteProfile")
	      public String deleteProfile(int userSeq, String userType, String userId) throws IOException {
	         
	         System.out.println("userSeq === "+userSeq);
	         System.out.println("userType === "+userType);
	         System.out.println("userId === "+userId);
	         fileService.deleteProfile(userSeq,userType,userId);
	         userService.updateUserProfileNull(userSeq,userType,userId);
	         String encodedParam = URLEncoder.encode(userId, "UTF-8");

	         return "redirect:/mypage/userInfo?userId="+encodedParam;
	      }

		@GetMapping("/getUserByJson")
		@ResponseBody
		public Map<String,Object> getUserByJson(@PathParam("search_value")String value, Model model) {
			
			System.out.println("받은 데이터 == "+ value);
			
			List<UserVO> userList= userService.getUserList(null);
			Map<String, Object> userMap = new HashMap<>();
					
			userMap.put("userList", userList);	
			
			return userMap;
		}
		@GetMapping("/getHospitalByJson")
		@ResponseBody
		public Map<String,Object> getHospitalByJson(@PathParam("search_value")String value, Model model) {
			
			System.out.println("받은 데이터 == "+ value);
			
			List<UserVO> userList= userService.getHospitalUserList(null);
			Map<String, Object> userMap = new HashMap<>();
			
			userMap.put("userList", userList);	
			
			return userMap;
		}
		
		/// Below controllers' methods were created by thomas lee on Dec 3rd 20:31pm
				/// he created methods the methods "shelter information" for admin management. 
				
				@Autowired
				private UserService shelterService; // the UserService interface was declared as shelterService for admin management...
				
				
				//관리자페이지 -> 보호소정보 관리로 이동
				@GetMapping("/admin/getShelterList")
				public String shelterInfoForm() {
					return "admin_shelter"; // this leads user to go onadmin_shetler.jsp.....
				}
				
				// 보호소 정보 출력 (관리자 페이지 -> 보호소정보 관리 )
		/*		@RequestMapping("/admin/getShelterList")
				@ResponseBody
				public String getShelterList(ShelterVO vo, Model model) {
					System.out.println("getShelterList 메소드가 호출 되었습니다==========."+vo);
					System.out.println("getShelterList 메소드가 호출 되었습니다.");
					
					//리스트에 담긴 값 확인용 코드.
					List<ShelterVO> list = shelterService.getShelterList(vo);
					System.out.println("ShelterList 표출 ==" + list);
					
					model.addAttribute("ShelterList", shelterService.getShelterList(vo));
				
					return "admin_shelter"; 
				}// Dec 3rd 현재 query가 작동안함.. vo를 따로 만들어 줄필요 없으며. query를 이용하여 list를 부르면 됨...
		*/	
}