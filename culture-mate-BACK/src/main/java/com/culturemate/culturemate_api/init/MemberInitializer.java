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
public class MemberInitializer {

  private final MemberService memberService;
  private final MemberRepository memberRepository;

  @Autowired
  public MemberInitializer(MemberService memberService, MemberRepository memberRepository) {
    this.memberService = memberService;
    this.memberRepository = memberRepository;
  }

  public void memberInit(int memberNum) {
    try {

      for (int i = 0; i < memberNum; i++) {
        String loginId = "user" + (i + 1);

        if (memberRepository.existsByLoginId(loginId)) {
          System.out.println("사용자 " + loginId + "는 이미 존재합니다.");
          continue;
        }
        
        MemberDto.Register registerDto = MemberDto.Register.builder()
            .loginId(loginId)
            .password("user@1234")
            .nickname(loginId)
            .intro(loginId + "의 한줄 자기소개")
            .mbti("")
            .email(loginId + "@culturemate.com")
            .build();
        
        Member newMember = memberService.register(registerDto);
        System.out.println("계정 생성 완료: " + loginId);
      }
      
      System.out.println("회원 데이터 초기화 완료");
      
    } catch (Exception e) {
      System.out.println("에러 타입: " + e.getClass().getSimpleName());
      System.out.println("에러 메시지: " + e.getMessage());
      e.printStackTrace();
    }
  }

}
