package com.keyword.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Builder
@Data
public class RelateKeywordStatModel {

    public List<Keyword> keywordList;

    @Data
    @EqualsAndHashCode
    public class Keyword implements Comparable<Keyword> {
        private String relKeyword;
        private String monthlyPcQcCnt;
        private String monthlyMobileQcCnt;
        private String monthlyAvePcClkCnt;
        private String monthlyAveMobileClkCnt;
        private String monthlyAvePcCtr;
        private String monthlyAveMobileCtr;
        private String plAvgDepth;
        private String compIdx;
        private int whereBlog;
        private int whereWeb;
        private int whereMobileBlog;
        private int whereMobileWeb;
        private int blogTotalPost;
        private int naverCnt;
        private int tistoryCnt;
        private int elseCnt;

        @Override
        public int compareTo(Keyword o) {
            if (Integer.parseInt(this.monthlyMobileQcCnt) > Integer.parseInt(o.getMonthlyMobileQcCnt())) {
                return -1;
            } else if (Integer.parseInt(this.monthlyMobileQcCnt) < Integer.parseInt(o.getMonthlyMobileQcCnt())) {
                return 1;
            }
            return 0;
        }
    }
}

