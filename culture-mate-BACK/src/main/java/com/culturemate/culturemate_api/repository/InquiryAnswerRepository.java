package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.inquiry.InquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, Long> {
}
