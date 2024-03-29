package com.hackm.famiryboard.controller.provider;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.HurlStack;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;

/**
 * Created by shunhosaka on 2015/01/12.
 */
public class MultiPartStack extends HurlStack {
    private final static String HEADER_CONTENT_TYPE = "Content-Type";
    private final HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws AuthFailureError, IOException {
        if (!(request instanceof MultipartRequest)) {
            return super.performRequest(request, additionalHeaders);
        }
        HttpPost httpRequest = new HttpPost(request.getUrl());
        httpRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
        setMultiPartBody(httpRequest, request);
        addHeaders(httpRequest, additionalHeaders);
        addHeaders(httpRequest, request.getHeaders());
        HttpParams httpParams = httpRequest.getParams();
        int timeoutMs = request.getTimeoutMs();
        if (request.getPriority() == Request.Priority.HIGH || request.getPriority() == Request.Priority.IMMEDIATE) {
            HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
        } else {
            HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
        }
        HttpConnectionParams.setSoTimeout(httpParams, timeoutMs);

        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", new PlainSocketFactory(), 80));
        registry.register(new Scheme("https", socketFactory, 443));

        ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(httpParams, registry);
        HttpClient httpClient = new DefaultHttpClient(manager, httpParams);
        return httpClient.execute(httpRequest);
    }

    private void addHeaders(HttpUriRequest httpRequest, Map<String, String> headers) {
        for (String key : headers.keySet()) {
            httpRequest.setHeader(key, headers.get(key));
        }
    }

    private static void setMultiPartBody(HttpEntityEnclosingRequestBase httpRequest,
                                         Request<?> request) throws AuthFailureError {
        if (request instanceof MultipartRequest) {
            httpRequest.setEntity(((MultipartRequest) request).getEntity());
        }
    }

}