package com.keyword.service;

import com.google.gson.Gson;
import com.keyword.model.RelateKeywordStatModel;
import com.keyword.model.request.NaverAdRequest;
import com.keyword.repository.KeywordStatRepository;
import com.keyword.util.Signatures;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class NaverAdService {

    private final KeywordStatRepository keywordStatRepository;

    @Value("${naver.apiKey}")
    private String apiKey;

    @Value("${naver.secretKey}")
    private String secretKey;

    @Value("${naver.customerId}")
    private String customerId;

    private final String baseUrl = "https://api.naver.com";
    private final String charset = "UTF-8";
    private final String showDetail = "1";

    public NaverAdService(KeywordStatRepository keywordStatRepository) {
        this.keywordStatRepository = keywordStatRepository;
    }

    @Transactional
    public String naverAdKeywordStat(NaverAdRequest request) throws SignatureException, UnsupportedEncodingException {
        String keyword = request.getKeyword();
        Gson gson = new Gson();

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String hmacSHA256 = Signatures.of(timestamp,"GET","/keywordstool",secretKey);
        String query = String.format("hintKeywords=%s&showDetail=%s", URLEncoder.encode(keyword,charset),URLEncoder.encode(showDetail,charset));
        Map<String, Object> map = new HashMap<String, Object>();

        BufferedReader input = getNaverAdKeywordStat(baseUrl+"/keywordstool",query,hmacSHA256);

        RelateKeywordStatModel relateKeywordStatModel = gson.fromJson(input, RelateKeywordStatModel.class);
        int size = relateKeywordStatModel.getKeywordList().size();
        for (int i = 0; i < size; i++) {
            String key = relateKeywordStatModel.getKeywordList().get(i).getRelKeyword();
            map.put("keyword", key);
        }

        return "";
    }


    private BufferedReader getNaverAdKeywordStat(String requestURL, String query, String hmacSHA256) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        HttpURLConnection connection = null;
        Gson gson = new Gson();
        BufferedReader input = null;
        RelateKeywordStatModel relateKeywordStatModel = null;
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            //Private API Header 세팅
            URL url = new URL(requestURL + "?" + query);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("X-Timestamp", timestamp);
            connection.setRequestProperty("X-API-KEY", apiKey);
            connection.setRequestProperty("X-Customer", String.valueOf(customerId));
            connection.setRequestProperty("X-Signature", hmacSHA256);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-type", "application/json");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            input = new BufferedReader(new InputStreamReader(connection.getInputStream()));


        } catch (IOException e) {
            e.printStackTrace();
        }
        return input;
    }


}
