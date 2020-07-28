package com.keyword.repository;

import com.keyword.entity.KeywordStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeywordStatRepository extends JpaRepository<KeywordStat, Long> {

}
