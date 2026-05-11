package com.culturemate.culturemate_api.init;

import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.member.Role;
import com.culturemate.culturemate_api.dto.MemberDto;
import com.culturemate.culturemate_api.repository.MemberRepository;
import com.culturemate.culturemate_api.service.MemberService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class AdminInitializer {

  private final MemberService memberService;
  private final MemberRepository memberRepository;

  @Autowired
  public AdminInitializer(MemberService memberService, MemberRepository memberRepository) {
    this.memberService = memberService;
    this.memberRepository = memberRepository;
  }

  public void adminInit() {
    try(InputStream jsonFile = Thread.currentThread()
      .getContextClassLoader()
      .getResourceAsStream("db/AdminMember.json")) {
      
      if (jsonFile == null) { 
        throw new IllegalStateException("파일을 찾을 수 없음");
      }
      
      ObjectMapper mapper = new ObjectMapper();
      List<Map<String, String>> adminList = mapper.readValue(jsonFile, 
          new TypeReference<List<Map<String, String>>>() {});
      
      System.out.println("관리자 데이터 개수: " + adminList.size());
      
      for (Map<String, String> adminData : adminList) {
        String loginId = adminData.get("loginId");
        System.out.println("처리 중인 관리자: " + loginId);
        
        if (memberRepository.existsByLoginId(loginId)) {
          System.out.println("관리자 " + loginId + "는 이미 존재합니다.");
          continue;
        }
        
        MemberDto.Register registerDto = MemberDto.Register.builder()
            .loginId(loginId)
            .password(adminData.get("password"))
            .nickname(adminData.get("nickname"))
            .intro(adminData.get("intro"))
            .mbti(adminData.get("mbti"))
            .email(adminData.get("email"))
            .build();
        
        System.out.println("Register 생성 완료: " + loginId);
        
        Member newAdmin = memberService.register(registerDto);
        memberService.updateRole(newAdmin.getId(), Role.ADMIN);
        System.out.println("관리자 계정 생성 및 권한 설정 완료: " + loginId);
      }
      
      System.out.println("관리자 데이터 초기화 완료");
      
    } catch (Exception e) {
      System.out.println("관리자 데이터 불러오기 실패");
      System.out.println("에러 타입: " + e.getClass().getSimpleName());
      System.out.println("에러 메시지: " + e.getMessage());
      e.printStackTrace();
    }
  }

}
