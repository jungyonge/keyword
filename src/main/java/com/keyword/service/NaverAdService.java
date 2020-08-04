package com.keyword.service;

import com.google.gson.Gson;
import com.keyword.model.NaverBlogStatModel;
import com.keyword.model.RelateKeywordStatModel;
import com.keyword.model.request.NaverAdRequest;
import com.keyword.repository.KeywordStatRepository;
import com.keyword.util.Signatures;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    @Value("${naver.naverApiId}")
    private String naverApiId;

    @Value("${naver.naverApiSecret}")
    private String naverApiSecret;

    private final String baseUrl = "https://api.naver.com";
    private final String charset = "UTF-8";
    private final String showDetail = "1";

    public NaverAdService(KeywordStatRepository keywordStatRepository) {
        this.keywordStatRepository = keywordStatRepository;
    }

    @Transactional
    public List<RelateKeywordStatModel.Keyword> naverAdKeywordStat(NaverAdRequest request) throws SignatureException, UnsupportedEncodingException, InterruptedException {

        List<RelateKeywordStatModel.Keyword> lastKeywordList = new ArrayList<>();
        Gson gson = new Gson();

        String keyword = request.getKeyword();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String hmacSHA256 = Signatures.of(timestamp, "GET", "/keywordstool", secretKey);
        String query = String.format("hintKeywords=%s&showDetail=%s", URLEncoder.encode(keyword, charset), URLEncoder.encode(showDetail, charset));
        BufferedReader input = getNaverAdKeywordStat(baseUrl + "/keywordstool", query, hmacSHA256);

        checkUnder10(lastKeywordList, input);

        Collections.sort(lastKeywordList);
//        List<RelateKeywordStatModel.Keyword> tempKeywordList = getRelKeywordStat(lastKeywordList);
//
//        lastKeywordList.addAll(tempKeywordList);
//
//        List<RelateKeywordStatModel.Keyword> removeDupKeyword = new ArrayList<>(new HashSet<>(lastKeywordList));
//
//        Collections.sort(removeDupKeyword);


//        Flux.range(0, lastKeywordList.size())
//                .retry(5)
//                .parallel(5)
//                .runOn(Schedulers.newParallel("PAR", 5))
//                .map(i -> {
//                    naverBlogStatFlux(lastKeywordList, i);
//                    return i;
//                })
//                .subscribe(i -> log.info("완료 : " + i));

        List<RelateKeywordStatModel.Keyword> ttt = new ArrayList<>();

        List<RelateKeywordStatModel.Keyword> test =  Flux.range(0, lastKeywordList.size())
                .retry(5)
                .parallel(1000)
                .runOn(Schedulers.newParallel("PAR", 1000))
                .map(i -> {
                    naverBlogStatFlux(lastKeywordList, i);
                    return lastKeywordList.get(i);
                })
                .sequential()
                .collectList()
                .block();

//        naverBlogStatFlux();
//        naverBlogStat(lastKeywordList);
        return test;
    }

    private List<RelateKeywordStatModel.Keyword> getRelKeywordStat(List<RelateKeywordStatModel.Keyword> keywordList) throws SignatureException, UnsupportedEncodingException, InterruptedException {
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
            Thread.sleep(1000);

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
        HttpURLConnection connection;
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

    public void naverBlogStat(List<RelateKeywordStatModel.Keyword> removeDupKeyword) {
        Gson gson = new Gson();
        for (RelateKeywordStatModel.Keyword tempKeyword : removeDupKeyword) {

            String keyword = tempKeyword.getRelKeyword();
            Document naverBlogDocument = null;
            Document naverPCDocument = null;
            Document naverMobileDocument = null;
            boolean ban = false;
            Map<String, Integer> result = new HashMap<String, Integer>();

            int whereBlog = 0;
            int whereWeb = 0;
            int whereMobileBlog = 0;
            int whereMobileWeb = 0;
            int blogTotalPost = 0;
            int naverCnt = 0;
            int tistoryCnt = 0;
            int elseCnt = 0;

//            String naverBlogURL = "https://search.naver.com/search.naver?where=post&sm=tab_jum&query=" + keyword;
            String naverPCURL = "https://search.naver.com/search.naver?sm=top_hty&fbm=1&ie=utf8&query=" + keyword;
            String naverMobileURL = "https://m.search.naver.com/search.naver?query=" + keyword;

            try {

//                naverBlogDocument = Jsoup.connect(naverBlogURL).get();
//                naverPCDocument = Jsoup.connect(naverPCURL).get();
                naverMobileDocument = Jsoup.connect(naverMobileURL).get();

//                Elements elements1 = naverBlogDocument.select("ul#elThumbnailResultArea a.url");// 블로그 url
//                Elements elements2 = naverBlogDocument.select("div#main_pack.main_pack span.title_num");// 블로그 포스팅 개수
//                Elements elements3 = naverPCDocument.select("div#main_pack.main_pack div.section_head h2"); // pc 검색시 블로그 몇번째 있는지 search
                Elements elements4 = naverMobileDocument.select("div#ct a.api_more"); // 모바일 검색시 블로그 몇번째 있는지 search
//                int element1Size = elements1.size();
//                int element2Size = elements2.size();
//                int element3Size = elements3.size();
                int element4Size = elements4.size();

//                if (element3Size != 0) {
//                    for (int b = 0; b < element3Size; b++) {
//                        String find = String.valueOf(elements3.get(b).childNode(0));
//                        if (find.equals("블로그")) {
//                            whereBlog = b + 1;
//                        }
//                        if (find.equals("웹사이트")) {
//                            whereWeb = b + 1;
//                        }
//                    }
//                }

                if (element4Size != 0) {
                    for (int b = 0; b < element4Size; b++) {
                        String find = String.valueOf(elements4.get(b).childNode(0));
                        if (find.equals("VIEW 더보기")) {
                            whereMobileBlog = b + 1;
                        }
                        if (find.equals(" 더보기")) {
                            whereMobileWeb = b + 1;
                        }
                    }
                }

//                if (element1Size != 0 && element2Size != 0) {
//                    String str = String.valueOf(elements2.get(0).childNode(0));
//                    blogTotalPost = Integer.parseInt(str.substring(6).replaceAll("[^0-9]", ""));
//
//                    for (int i = 0; i < element1Size; i++) {
//                        Element element = elements1.get(i);
//                        String blogURL = String.valueOf(element.childNode(0));
//                        if (blogURL.indexOf("naver") > -1 || blogURL.indexOf("blog.me") > -1) {
//                            naverCnt++;
//                        } else if (blogURL.indexOf("tistory") > -1) {
//                            tistoryCnt++;
//                        } else {
//                            elseCnt++;
//                        }
//                    }
//                }

                String apiString = getNaverAPIString("https://openapi.naver.com/v1/search/blog.json?query=", tempKeyword.getRelKeyword());
                NaverBlogStatModel naverBlogStatModel = gson.fromJson(apiString, NaverBlogStatModel.class);

                for (int i = 0; i < naverBlogStatModel.getItems().size(); i++) {
                    NaverBlogStatModel.item tempItem = naverBlogStatModel.getItems().get(i);
                    if (tempItem.getLink().contains("naver") || tempItem.getLink().contains("blog.me")) {
                        naverCnt++;
                    } else if (tempItem.getLink().contains("tistory")) {
                        tistoryCnt++;
                    } else {
                        elseCnt++;
                    }
                }
                tempKeyword.setWhereBlog(whereBlog);
                tempKeyword.setWhereWeb(whereWeb);
                tempKeyword.setWhereMobileBlog(whereMobileBlog);
                tempKeyword.setWhereMobileWeb(whereMobileWeb);
                tempKeyword.setBlogTotalPost(naverBlogStatModel.getTotal());
                tempKeyword.setNaverCnt(naverCnt);
                tempKeyword.setTistoryCnt(tistoryCnt);
                tempKeyword.setElseCnt(elseCnt);
                log.info(tempKeyword.getRelKeyword());

                Thread.sleep(10);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void naverBlogStatFlux(List<RelateKeywordStatModel.Keyword> removeDupKeyword, int x) {
        Gson gson = new Gson();

        RelateKeywordStatModel.Keyword tempKeyword = removeDupKeyword.get(x);
        String keyword = tempKeyword.getRelKeyword();
        Document naverBlogDocument = null;
        Document naverPCDocument = null;
        Document naverMobileDocument = null;
        boolean ban = false;
        Map<String, Integer> result = new HashMap<String, Integer>();

        int whereBlog = 0;
        int whereWeb = 0;
        int whereMobileBlog = 0;
        int whereMobileWeb = 0;
        int blogTotalPost = 0;
        int naverCnt = 0;
        int tistoryCnt = 0;
        int elseCnt = 0;

        String naverBlogURL = "https://search.naver.com/search.naver?where=post&sm=tab_jum&query=" + keyword;
        String naverPCURL = "https://search.naver.com/search.naver?sm=top_hty&fbm=1&ie=utf8&query=" + keyword;
        String naverMobileURL = "https://m.search.naver.com/search.naver?query=" + keyword;

        try {

            naverBlogDocument = Jsoup.connect(naverBlogURL).get();
            naverPCDocument = Jsoup.connect(naverPCURL).get();
            naverMobileDocument = Jsoup.connect(naverMobileURL).get();

            Elements elements1 = naverBlogDocument.select("ul#elThumbnailResultArea a.url");// 블로그 url
            Elements elements2 = naverBlogDocument.select("div#main_pack.main_pack span.title_num");// 블로그 포스팅 개수
            Elements elements3 = naverPCDocument.select("div#main_pack.main_pack div.section_head h2"); // pc 검색시 블로그 몇번째 있는지 search
            Elements elements4 = naverMobileDocument.select("div#ct a.api_more"); // 모바일 검색시 블로그 몇번째 있는지 search
            int element1Size = elements1.size();
            int element2Size = elements2.size();
            int element3Size = elements3.size();
            int element4Size = elements4.size();

            if (element3Size != 0) {
                for (int b = 0; b < element3Size; b++) {
                    String find = String.valueOf(elements3.get(b).childNode(0));
                    if (find.equals("블로그")) {
                        whereBlog = b + 1;
                    }
                    if (find.equals("웹사이트")) {
                        whereWeb = b + 1;
                    }
                }
            }

            if (element4Size != 0) {
                for (int b = 0; b < element4Size; b++) {
                    String find = String.valueOf(elements4.get(b).childNode(0));
                    if (find.equals("VIEW 더보기")) {
                        whereMobileBlog = b + 1;
                    }
                    if (find.equals(" 더보기")) {
                        whereMobileWeb = b + 1;
                    }
                }
            }

            if (element1Size != 0 && element2Size != 0) {
                String str = String.valueOf(elements2.get(0).childNode(0));
                blogTotalPost = Integer.parseInt(str.substring(6).replaceAll("[^0-9]", ""));

                for (int i = 0; i < element1Size; i++) {
                    Element element = elements1.get(i);
                    String blogURL = String.valueOf(element.childNode(0));
                    if (blogURL.indexOf("naver") > -1 || blogURL.indexOf("blog.me") > -1) {
                        naverCnt++;
                    } else if (blogURL.indexOf("tistory") > -1) {
                        tistoryCnt++;
                    } else {
                        elseCnt++;
                    }
                }
            }

//            String apiString = getNaverAPIString("https://openapi.naver.com/v1/search/blog.json?query=", tempKeyword.getRelKeyword());
//            NaverBlogStatModel naverBlogStatModel = gson.fromJson(apiString, NaverBlogStatModel.class);
//
//            for (int i = 0; i < naverBlogStatModel.getItems().size(); i++) {
//                NaverBlogStatModel.item tempItem = naverBlogStatModel.getItems().get(i);
//                if (tempItem.getLink().contains("naver") || tempItem.getLink().contains("blog.me")) {
//                    naverCnt++;
//                } else if (tempItem.getLink().contains("tistory")) {
//                    tistoryCnt++;
//                } else {
//                    elseCnt++;
//                }
//            }
            tempKeyword.setWhereBlog(whereBlog);
            tempKeyword.setWhereWeb(whereWeb);
            tempKeyword.setWhereMobileBlog(whereMobileBlog);
            tempKeyword.setWhereMobileWeb(whereMobileWeb);
//            tempKeyword.setBlogTotalPost(naverBlogStatModel.getTotal());
            tempKeyword.setBlogTotalPost(blogTotalPost);
            tempKeyword.setNaverCnt(naverCnt);
            tempKeyword.setTistoryCnt(tistoryCnt);
            tempKeyword.setElseCnt(elseCnt);
            log.info(tempKeyword.getRelKeyword());

            Thread.sleep(10);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private String getNaverAPIString(String requestURL, String keyword) {
        HttpURLConnection connection = null;
        BufferedReader input = null;
        StringBuilder responseBody = new StringBuilder();

        try {
            URL url = new URL(requestURL + "?" + URLEncoder.encode(keyword, "UTF-8"));
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Naver-Client-Id", naverApiId);
            connection.setRequestProperty("X-Naver-Client-Secret", naverApiSecret);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
                BufferedReader lineReader = new BufferedReader(streamReader);
                String line;
                while ((line = lineReader.readLine()) != null) {
                    responseBody.append(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            connection.disconnect();
        }

        return responseBody.toString();
    }


    public void naverBlogStatFlux(RelateKeywordStatModel.Keyword tempKeyword) {
        Gson gson = new Gson();
//           for (RelateKeywordStatModel.Keyword tempKeyword : removeDupKeyword) {

        String keyword = tempKeyword.getRelKeyword();
        Document naverBlogDocument = null;
        Document naverPCDocument = null;
        Document naverMobileDocument = null;
        boolean ban = false;
        Map<String, Integer> result = new HashMap<String, Integer>();

        int whereBlog = 0;
        int whereWeb = 0;
        int whereMobileBlog = 0;
        int whereMobileWeb = 0;
        int blogTotalPost = 0;
        int naverCnt = 0;
        int tistoryCnt = 0;
        int elseCnt = 0;

        //            String naverBlogURL = "https://search.naver.com/search.naver?where=post&sm=tab_jum&query=" + keyword;
        String naverPCURL = "https://search.naver.com/search.naver?sm=top_hty&fbm=1&ie=utf8&query=" + keyword;
        String naverMobileURL = "https://m.search.naver.com/search.naver?query=" + keyword;

        try {

            //                naverBlogDocument = Jsoup.connect(naverBlogURL).get();
            //                naverPCDocument = Jsoup.connect(naverPCURL).get();
            naverMobileDocument = Jsoup.connect(naverMobileURL).get();

            //                Elements elements1 = naverBlogDocument.select("ul#elThumbnailResultArea a.url");// 블로그 url
            //                Elements elements2 = naverBlogDocument.select("div#main_pack.main_pack span.title_num");// 블로그 포스팅 개수
            //                Elements elements3 = naverPCDocument.select("div#main_pack.main_pack div.section_head h2"); // pc 검색시 블로그 몇번째 있는지 search
            Elements elements4 = naverMobileDocument.select("div#ct a.api_more"); // 모바일 검색시 블로그 몇번째 있는지 search
            //                int element1Size = elements1.size();
            //                int element2Size = elements2.size();
            //                int element3Size = elements3.size();
            int element4Size = elements4.size();

            //                if (element3Size != 0) {
            //                    for (int b = 0; b < element3Size; b++) {
            //                        String find = String.valueOf(elements3.get(b).childNode(0));
            //                        if (find.equals("블로그")) {
            //                            whereBlog = b + 1;
            //                        }
            //                        if (find.equals("웹사이트")) {
            //                            whereWeb = b + 1;
            //                        }
            //                    }
            //                }

            if (element4Size != 0) {
                for (int b = 0; b < element4Size; b++) {
                    String find = String.valueOf(elements4.get(b).childNode(0));
                    if (find.equals("VIEW 더보기")) {
                        whereMobileBlog = b + 1;
                    }
                    if (find.equals(" 더보기")) {
                        whereMobileWeb = b + 1;
                    }
                }
            }

            //                if (element1Size != 0 && element2Size != 0) {
            //                    String str = String.valueOf(elements2.get(0).childNode(0));
            //                    blogTotalPost = Integer.parseInt(str.substring(6).replaceAll("[^0-9]", ""));
            //
            //                    for (int i = 0; i < element1Size; i++) {
            //                        Element element = elements1.get(i);
            //                        String blogURL = String.valueOf(element.childNode(0));
            //                        if (blogURL.indexOf("naver") > -1 || blogURL.indexOf("blog.me") > -1) {
            //                            naverCnt++;
            //                        } else if (blogURL.indexOf("tistory") > -1) {
            //                            tistoryCnt++;
            //                        } else {
            //                            elseCnt++;
            //                        }
            //                    }
            //                }

            String apiString = getNaverAPIString("https://openapi.naver.com/v1/search/blog.json?query=", tempKeyword.getRelKeyword());
            NaverBlogStatModel naverBlogStatModel = gson.fromJson(apiString, NaverBlogStatModel.class);

            for (int i = 0; i < naverBlogStatModel.getItems().size(); i++) {
                NaverBlogStatModel.item tempItem = naverBlogStatModel.getItems().get(i);
                if (tempItem.getLink().contains("naver") || tempItem.getLink().contains("blog.me")) {
                    naverCnt++;
                } else if (tempItem.getLink().contains("tistory")) {
                    tistoryCnt++;
                } else {
                    elseCnt++;
                }
            }
            tempKeyword.setWhereBlog(whereBlog);
            tempKeyword.setWhereWeb(whereWeb);
            tempKeyword.setWhereMobileBlog(whereMobileBlog);
            tempKeyword.setWhereMobileWeb(whereMobileWeb);
            tempKeyword.setBlogTotalPost(naverBlogStatModel.getTotal());
            tempKeyword.setNaverCnt(naverCnt);
            tempKeyword.setTistoryCnt(tistoryCnt);
            tempKeyword.setElseCnt(elseCnt);
            log.info(tempKeyword.getRelKeyword());

            Thread.sleep(10);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//           }
    }

}
