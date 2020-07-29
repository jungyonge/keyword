package com.keyword.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class NaverBlogStatModel {

    int total;
    public List<item> items;

    @Data
    public class item{
        String title;
        String link;
    }
}
