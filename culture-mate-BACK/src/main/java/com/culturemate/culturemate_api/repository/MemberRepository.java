package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.member.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

  Optional<Member> findByLoginId(String loginId);
  
  boolean existsByLoginId(String loginId);
  
  List<Member> findByStatus(MemberStatus status);

  // nickname으로 Member 조회 (MemberDetail과 JOIN, 부분 검색)
  @Query("SELECT m FROM Member m LEFT JOIN m.memberDetail md WHERE LOWER(md.nickname) LIKE LOWER(CONCAT('%', :nickname, '%'))")
  List<Member> findByNicknameContaining(@Param("nickname") String nickname);

}
