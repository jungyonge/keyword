package com.keyword.service;

import com.keyword.model.request.NaverAdRequest;
import com.keyword.repository.KeywordStatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class NaverAdService {

    private final KeywordStatRepository keywordStatRepository;


    public NaverAdService(KeywordStatRepository keywordStatRepository) {
        this.keywordStatRepository = keywordStatRepository;
    }

    @Transactional
    public String naverAdKeywordStat( NaverAdRequest request) {

        return "";
    }


}
