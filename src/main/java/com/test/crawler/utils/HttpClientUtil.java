package com.test.crawler.utils;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
public class HttpClientUtil {

    private static HttpClientConnectionManager connMgr;
    private static final int DEFAULT_CONNECT_TIMEOUT = 2*1000;
    private static final int DEFAULT_SOCKECT_TIMEOUT = 2*1000;

    public static String get(String url, boolean isRetry) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        HttpEntity entity = null;

        Exception exception = null;
        for (int i = 0; i < 3; i++) {
            try {
                httpClient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(url);
                response = httpClient.execute(httpGet);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    log.error("[HttpClientUtil#get] 请求失败, url={}", url);
                    throw new RuntimeException("请求失败. httpStatusCode = " + statusCode);
                }
                entity = response.getEntity();
                if (entity.getContentType() == null) {
                    return EntityUtils.toString(entity, "UTF-8");
                }
                return EntityUtils.toString(entity);
            } catch (Exception e) {
                log.error("[HttpClientUtil#get] 发送post请求失败, url={}", url, e);
                exception = e;
                if (!isRetry) {
                    throw new RuntimeException(e);
                }
            } finally {
                EntityUtils.consumeQuietly(entity);
                HttpClientUtils.closeQuietly(httpClient);
                HttpClientUtils.closeQuietly(response);
            }
        }
        throw new RuntimeException(exception);
    }

    public static String post(Map<String, Object> paramMap, String url) {
        return doPost(url, buildPostParamMap(paramMap), 2000, 2000);
    }

    public static String postByConfig(Map<String, Object> paramMap, String url, int socketTimeout, int connectTimeout) {
        return doPost(url, buildPostParamMap(paramMap), socketTimeout, connectTimeout);
    }

    /**
     * 发送一次post请求
     */
    static String doPost(String url, Map<String, String> paramMap, int socketTimeout, int connectTimeout) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        HttpEntity entity = null;
        try {
            HttpPost httpPost = new HttpPost(url);
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            if (paramMap != null) {
                nameValuePairs.addAll(paramMap.keySet().stream().map(paramName -> new BasicNameValuePair(paramName, paramMap.get(paramName))).collect(Collectors.toList()));
            }

            RequestConfig.Builder builder = RequestConfig.custom();
            RequestConfig requestConfig = builder.setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).build();
            httpPost.setConfig(requestConfig);
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
            httpClient = HttpClients.createDefault();
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                log.error("[HttpClientUtil#doPost] 请求失败, url={}, params={}", url, paramMap);
                throw new RuntimeException("请求失败. httpStatusCode = " + statusCode);
            }
            entity = response.getEntity();
            if (entity.getContentType() == null) {
                return EntityUtils.toString(entity, "UTF-8");
            }
            return EntityUtils.toString(entity);
        } catch (Exception e) {
            log.error("[HttpClientUtil#doPost] 发送post请求失败, url={}, params={}", url, paramMap, e);
            throw new RuntimeException(e);
        } finally {
            closeGracefully(httpClient, response, entity);
        }
    }

    public static String postJson(String url, String json) {
        return postJson(url, json, DEFAULT_CONNECT_TIMEOUT, DEFAULT_SOCKECT_TIMEOUT);
    }

    public static String postJson(String url, String json, int connectTimeout, int socketTimeout) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        HttpEntity entity = null;
        try {
            HttpPost httpPost = new HttpPost(url);
            StringEntity postingString = new StringEntity(json);
            httpPost.setEntity(postingString);
            httpPost.setHeader("Content-type", "application/json");
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).build();
            httpPost.setConfig(requestConfig);
            httpClient = HttpClients.createDefault();
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                log.error("[HttpClientUtil#doPost] 请求失败, url={}, json={}", url, json);
                throw new RuntimeException("请求失败. httpStatusCode = " + statusCode);
            }
            entity = response.getEntity();
            if (entity.getContentType() == null) {
                return EntityUtils.toString(entity, "UTF-8");
            }
            return EntityUtils.toString(entity);
        } catch (Exception e) {
            log.error("[HttpClientUtil#doPost] 发送post请求失败, url={}, json={}", url, json, e);
            throw new RuntimeException(e);
        } finally {
            closeGracefully(httpClient, response, entity);
        }
    }


    public static String postXml(String url, String body) throws IOException {
        return postXmlByConfig(url, body, 2 * 1000, 2 * 1000);
    }

    public static String postXmlByConfig(String url, String body, int socketTimeout, int connectTimeout) throws IOException {
        log.trace("Sending POST to URL {}", url);
        CloseableHttpClient httpClient = null;
        String responseText = null;
        CloseableHttpResponse response = null;
        try {
            HttpPost postRequest = new HttpPost(url);
            postRequest.setHeader("Content-Type", "text/xml;charset=utf-8");
            StringEntity postEntity = new StringEntity(body, Charset.forName("UTF-8"));
            postRequest.setEntity(postEntity);
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).build();
            postRequest.setConfig(requestConfig);
            httpClient = HttpClients.createDefault();
            response = httpClient.execute(postRequest);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException("Received not 200 OK status code:" + response.getStatusLine().toString());
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                responseText = EntityUtils.toString(entity, "UTF-8");
                log.debug("Received Unified Order resposne from wechat: " + responseText);
                return responseText;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("[HttpClientUtil#doPostXml] 发送post请求失败, url={}, json={}", url, body, e);
            throw new RuntimeException(e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private static void closeGracefully(CloseableHttpClient httpClient, CloseableHttpResponse response, HttpEntity entity) {
        try {
            EntityUtils.consume(entity);
            if (httpClient != null) {
                httpClient.close();
            }
            if (response != null) {
                response.close();
            }
        } catch (IOException e) {
            log.error("[HttpClientUtil#doPost] 关闭http post请求资源失败!", e);
        }
    }

    /**
     * 发送 SSL POST 请求（HTTPS），K-V形式
     *
     * @param apiUrl API接口URL
     * @param params 参数map
     * @return
     */
    public static String doPostSSL(String apiUrl, Map<String, Object> params) {

        return postSSLByConfig(apiUrl, params, 2 * 1000, 2 * 1000);
    }

    public static String postSSLByConfig(String apiUrl, Map<String, Object> params, int socketTimeout, int connectTimeout) {
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).build();
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory()).setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).build();
        HttpPost httpPost = new HttpPost(apiUrl);
        CloseableHttpResponse response = null;
        String httpStr = null;

        try {
            httpPost.setConfig(requestConfig);
            List<NameValuePair> pairList = new ArrayList<NameValuePair>(params.size());
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                NameValuePair pair = new BasicNameValuePair(entry.getKey(), entry.getValue().toString());
                pairList.add(pair);
            }
            httpPost.setEntity(new UrlEncodedFormEntity(pairList, Charset.forName("utf-8")));
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return null;
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }
            httpStr = EntityUtils.toString(entity, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return httpStr;


    }

    /**
     * 创建SSL安全连接
     *
     * @return
     */
    private static SSLConnectionSocketFactory createSSLConnSocketFactory() {
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true).build();
            sslsf = new SSLConnectionSocketFactory(sslContext, (arg0, arg1) -> true);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return sslsf;
    }

    static String buildAccessUrl(Map<String, Object> paramMap, String url) {
        if (!paramMap.isEmpty()) {
            StringBuilder sb = new StringBuilder(url);
            sb.append("?");
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            return sb.substring(0, sb.length() - 1);
        }
        return url;
    }

    static Map<String, String> buildPostParamMap(Map<String, Object> paramMap) {
        Map<String, String> map = Maps.newHashMap();
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            if (entry.getValue() != null) {
                map.put(entry.getKey(), entry.getValue().toString());
            } else {
                map.put(entry.getKey(), null);
            }
        }
        return map;
    }

}
