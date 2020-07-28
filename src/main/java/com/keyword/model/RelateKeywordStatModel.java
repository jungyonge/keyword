package com.keyword.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class RelateKeywordStatModel {

    public List<Keyword> keywordList;

    @Data
    public class Keyword {
        private String relKeyword;
        private String monthlyPcQcCnt;
        private String monthlyMobileQcCnt;
        private String monthlyAvePcClkCnt;
        private String monthlyAveMobileClkCnt;
        private String monthlyAvePcCtr;
        private String monthlyAveMobileCtr;
        private String plAvgDepth;
        private String compIdx;
    }
}

