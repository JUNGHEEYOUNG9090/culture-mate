package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.member.InterestTogethers;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.together.Together;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterestTogethersRepository extends JpaRepository<InterestTogethers, Long> {
    
    /**
     * 특정 회원과 동행에 대한 관심 등록 여부 확인
     */
    Optional<InterestTogethers> findByMemberAndTogether(Member member, Together together);
    
    /**
     * 특정 회원이 관심 등록한 모든 동행 조회
     */
    @Query("SELECT it.together FROM InterestTogethers it WHERE it.member = :member")
    List<Together> findTogethersByMember(@Param("member") Member member);
    
    /**
     * 특정 동행에 관심을 등록한 회원 수 조회
     */
    long countByTogether(Together together);
    
    /**
     * 특정 회원이 특정 동행에 관심을 등록했는지 확인
     */
    boolean existsByMemberAndTogether(Member member, Together together);
    
    /**
     * 여러 동행에 대한 특정 회원의 관심 상태를 배치로 조회
     */
    @Query("SELECT it.together.id FROM InterestTogethers it WHERE it.member.id = :memberId AND it.together.id IN :togetherIds")
    List<Long> findInterestedTogetherIdsByMemberIdAndTogetherIds(@Param("memberId") Long memberId, @Param("togetherIds") List<Long> togetherIds);

    /**
     * 특정 동행에 대한 모든 관심 등록 삭제
     */
    void deleteByTogetherId(Long togetherId);
}