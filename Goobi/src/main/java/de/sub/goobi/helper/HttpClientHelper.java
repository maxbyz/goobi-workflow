package de.sub.goobi.helper;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *          - https://www.intranda.com
 *          - http://digiverso.com 
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import de.sub.goobi.config.ConfigurationHelper;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HttpClientHelper {

    public static ResponseHandler<byte[]> byteArrayResponseHandler = new ResponseHandler<byte[]>() {
        @Override
        public byte[] handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error("Wrong status code : " + response.getStatusLine().getStatusCode());
                return null;
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return EntityUtils.toByteArray(entity);
            } else {
                return null;
            }
        }
    };

    public static ResponseHandler<String> stringResponseHandler = new ResponseHandler<String>() {
        @Override
        public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error("Wrong status code : " + response.getStatusLine().getStatusCode());
                return null;
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return EntityUtils.toString(entity, "utf-8");
            } else {
                return null;
            }
        }
    };

    public static ResponseHandler<InputStream> streamResponseHandler = new ResponseHandler<InputStream>() {
        @Override
        public InputStream handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error("Wrong status code : " + response.getStatusLine().getStatusCode());
                return null;
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return new ByteArrayInputStream(EntityUtils.toByteArray(entity));
            } else {
                return null;
            }
        }
    };

    //  parameter:
    // * first: url
    // * second: username
    // * third: password
    // * forth: scope (e.g. "localhost")
    // * fifth: port 
    public static String getStringFromUrl(String... parameter) {
        String response = "";
        CloseableHttpClient client = null;
        String url = parameter[0];
        HttpGet method = new HttpGet(url);

        if (parameter != null && parameter.length > 4) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(parameter[3], Integer.valueOf(parameter[4]).intValue()),
                    new UsernamePasswordCredentials(parameter[1], parameter[2]));
            client = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

        } else {
            client = HttpClientBuilder.create().build();
        }

        if (ConfigurationHelper.getInstance().isUseProxy()) {
            HttpHost proxy = new HttpHost(ConfigurationHelper.getInstance().getProxyUrl(), ConfigurationHelper.getInstance().getProxyPort());
            if (log.isDebugEnabled()) {
                log.debug("Using proxy " + proxy.getHostName() + ":" + proxy.getPort());
            }

            Builder builder = RequestConfig.custom();
            builder.setProxy(proxy);

            RequestConfig rc = builder.build();

            method.setConfig(rc);
        }

        try {
            response = client.execute(method, HttpClientHelper.stringResponseHandler);
        } catch (IOException e) {
            log.error("Cannot execute URL " + url, e);
        } finally {
            method.releaseConnection();

            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
        return response;
    }

    @Deprecated
    public static String getStringFromUrl(String url, String[] parameter) {
        String response = "";
        CloseableHttpClient client = null;
        HttpGet method = new HttpGet(url);

        if ((parameter != null) && (parameter.length > 3)) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(parameter[2], Integer.valueOf(parameter[3]).intValue()),
                    new UsernamePasswordCredentials(parameter[0], parameter[1]));
            client = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
        } else {
            client = HttpClientBuilder.create().build();
        }

        if (ConfigurationHelper.getInstance().isUseProxy()) {
            HttpHost proxy = new HttpHost(ConfigurationHelper.getInstance().getProxyUrl(), ConfigurationHelper.getInstance().getProxyPort());
            if (log.isDebugEnabled()) {
                log.debug("Using proxy " + proxy.getHostName() + ":" + proxy.getPort());
            }

            RequestConfig.Builder builder = RequestConfig.custom();
            builder.setProxy(proxy);

            RequestConfig rc = builder.build();

            method.setConfig(rc);
        }

        try {
            response = client.execute(method, stringResponseHandler);
        } catch (IOException e) {
            log.error("Cannot execute URL " + url, e);
        } finally {
            method.releaseConnection();

            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
        return response;
    }

    //  parameter:
    // * first: url
    // * second: username
    // * third: password
    // * forth: scope (e.g. "localhost")
    // * fifth: port 

    public static OutputStream getStreamFromUrl(OutputStream out, String... parameter) {
        CloseableHttpClient httpclient = null;
        HttpGet method = null;
        InputStream istr = null;
        String url = parameter[0];
        try {

            method = new HttpGet(url);
            if (parameter != null && parameter.length > 4) {
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(new AuthScope(parameter[3], Integer.valueOf(parameter[4]).intValue()),
                        new UsernamePasswordCredentials(parameter[1], parameter[2]));
                httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

            } else {
                httpclient = HttpClientBuilder.create().build();

            }

            if (ConfigurationHelper.getInstance().isUseProxy()) {
                HttpHost proxy = new HttpHost(ConfigurationHelper.getInstance().getProxyUrl(), ConfigurationHelper.getInstance().getProxyPort());
                if (log.isDebugEnabled()) {
                    log.debug("Using proxy " + proxy.getHostName() + ":" + proxy.getPort());
                }

                Builder builder = RequestConfig.custom();
                builder.setProxy(proxy);

                RequestConfig rc = builder.build();

                method.setConfig(rc);
            }

            Integer contentServerTimeOut = ConfigurationHelper.getInstance().getGoobiContentServerTimeOut();
            Builder builder = RequestConfig.custom();
            builder.setSocketTimeout(contentServerTimeOut);
            RequestConfig rc = builder.build();
            method.setConfig(rc);

            byte[] response = httpclient.execute(method, HttpClientHelper.byteArrayResponseHandler);
            if (response == null) {
                log.error("Response stream is null");
            }
            istr = new ByteArrayInputStream(response);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = istr.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            log.error("Unable to connect to url " + url, e);

        } finally {
            method.releaseConnection();
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
            if (istr != null) {
                try {
                    istr.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }

        return out;
    }

}