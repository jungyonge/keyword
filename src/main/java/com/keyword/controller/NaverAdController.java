package com.keyword.controller;


import com.keyword.model.request.NaverAdRequest;
import com.keyword.model.response.NaverAdResponse;
import com.keyword.service.NaverAdService;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.security.SignatureException;


@RestController
@RequestMapping("/share-event")
public class NaverAdController {

    private final NaverAdService naverAdService;

    public NaverAdController(NaverAdService naverAdService) {
        this.naverAdService = naverAdService;
    }


    @GetMapping
    public NaverAdResponse naverAdKeywordStat(
                                           @RequestBody NaverAdRequest request) throws UnsupportedEncodingException, SignatureException {
        return NaverAdResponse.of(naverAdService.naverAdKeywordStat(request));

    }

}
