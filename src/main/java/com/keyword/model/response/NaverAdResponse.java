package com.keyword.model.response;

import com.keyword.model.RelateKeywordStatModel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NaverAdResponse {
    String token;
    List<RelateKeywordStatModel.Keyword> keywordList;

    public static NaverAdResponse of(List<RelateKeywordStatModel.Keyword> keywordList) {
        return NaverAdResponse.builder()
                .keywordList(keywordList)
                .build();
    }
}
