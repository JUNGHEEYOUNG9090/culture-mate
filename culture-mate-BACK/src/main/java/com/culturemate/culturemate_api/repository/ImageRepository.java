package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.Image;
import com.culturemate.culturemate_api.domain.ImageTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

  List<Image> findByTargetTypeAndTargetId(ImageTarget targetType, Long targetId);
  
  Optional<Image> findByPath(String path);
  
  List<Image> findByTargetType(ImageTarget targetType);
  
  @Modifying
  @Transactional
  @Query("DELETE FROM Image i WHERE i.targetType = :targetType AND i.targetId = :targetId")
  void deleteByTargetTypeAndTargetId(@Param("targetType") ImageTarget targetType, 
                                    @Param("targetId") Long targetId);

}
