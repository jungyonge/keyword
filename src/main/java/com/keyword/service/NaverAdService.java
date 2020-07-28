package com.keyword.service;

import com.google.gson.Gson;
import com.keyword.model.RelateKeywordStatModel;
import com.keyword.model.request.NaverAdRequest;
import com.keyword.repository.KeywordStatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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


    public NaverAdService(KeywordStatRepository keywordStatRepository) {
        this.keywordStatRepository = keywordStatRepository;
    }

    @Transactional
    public String naverAdKeywordStat(NaverAdRequest request) {



        return "";
    }


    private BufferedReader getKeywordStat(String requestURL, String query, String hmacSHA256) {
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
//             relateKeywordStatModel = gson.fromJson(input, RelateKeywordStatModel.class);
//             int size = relateKeywordStatModel.getKeywordList().size();
//                 for(int i = 0; i < size; i++){
//                     String key = relateKeywordStatModel.getKeywordList().get(i).getRelKeyword();
//                     map.put("keyword",key);
//                 }

         } catch (IOException e) {
             e.printStackTrace();
         }
        return input;
    }


}
