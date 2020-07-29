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
import java.util.*;

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
    public List naverAdKeywordStat(NaverAdRequest request) throws SignatureException, UnsupportedEncodingException, InterruptedException {

        List<RelateKeywordStatModel.Keyword> lastKeywordList = new ArrayList<>();

        String keyword = request.getKeyword();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String hmacSHA256 = Signatures.of(timestamp, "GET", "/keywordstool", secretKey);
        String query = String.format("hintKeywords=%s&showDetail=%s", URLEncoder.encode(keyword, charset), URLEncoder.encode(showDetail, charset));
        BufferedReader input = getNaverAdKeywordStat(baseUrl + "/keywordstool", query, hmacSHA256);

        checkUnder10(lastKeywordList, input);

        Collections.sort(lastKeywordList);
        List tempKeywordList = getRelKeywordStat(lastKeywordList);;
        lastKeywordList.addAll(tempKeywordList);

        List<RelateKeywordStatModel.Keyword> removeDupKeyword = new ArrayList<>(new HashSet<>(lastKeywordList));

        Collections.sort(removeDupKeyword);

        return removeDupKeyword;
    }

    private List getRelKeywordStat(List<RelateKeywordStatModel.Keyword> keywordList) throws SignatureException, UnsupportedEncodingException, InterruptedException {
        List<RelateKeywordStatModel.Keyword> result = new ArrayList<>();

        int index = 30;
        if (keywordList.size() < 30) {
            index = keywordList.size();
        }
        for (int i = 1; i < index; i++) {

            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String hmacSHA256 = Signatures.of(timestamp, "GET", "/keywordstool", secretKey);
            String query = String.format("hintKeywords=%s&showDetail=%s", URLEncoder.encode(keywordList.get(i).getRelKeyword(), charset), URLEncoder.encode(showDetail, charset));

            BufferedReader input = getNaverAdKeywordStat(baseUrl + "/keywordstool", query, hmacSHA256);
            Thread.sleep(3000);

            if (input == null) {
                continue;
            }

            checkUnder10(result, input);
        }
        return result;

    }

    private void checkUnder10(List<RelateKeywordStatModel.Keyword> result, BufferedReader input) {
        Gson gson = new Gson();

        RelateKeywordStatModel relateKeywordStatModel = gson.fromJson(input, RelateKeywordStatModel.class);
        int size = relateKeywordStatModel.getKeywordList().size();
        for (int j = 0; j < size; j++) {
            String monthlyPcQcCnt = relateKeywordStatModel.getKeywordList().get(j).getMonthlyPcQcCnt();
            String monthlyMobileQcCnt = relateKeywordStatModel.getKeywordList().get(j).getMonthlyMobileQcCnt();
            if (!monthlyPcQcCnt.equals("< 10") && !monthlyMobileQcCnt.equals("< 10")) {
                result.add(relateKeywordStatModel.getKeywordList().get(j));
            }
        }
    }

    private BufferedReader getNaverAdKeywordStat(String requestURL, String query, String hmacSHA256) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        HttpURLConnection connection = null;
        Gson gson = new Gson();
        BufferedReader input = null;

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
            if (connection.getResponseCode() == 200) {
                input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                log.error(connection.getResponseMessage());
                log.error(String.valueOf(connection.getResponseCode()));

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return input;
    }


}
