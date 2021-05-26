package com.mesalu.viv2.android_ui.data.http.okhttp;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.HttpResponseCache;
import android.util.Log;

import androidx.annotation.RawRes;

import com.google.gson.Gson;
import com.mesalu.viv2.android_ui.Application;
import com.mesalu.viv2.android_ui.BuildConfig;
import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.LoginRepository;
import com.mesalu.viv2.android_ui.data.Serialization;
import com.mesalu.viv2.android_ui.data.http.ApiCompat;
import com.mesalu.viv2.android_ui.data.model.TokenSet;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.tls.HandshakeCertificates;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Builds an OkHttpClient suitable for use amongst frameworks that utilize OkHttp
 * (Picasso, Retrofit, etc.)
 */
public class OkHttpClientFactory {
    private Context ctx;
    private OkHttpClient client;
    private static OkHttpClientFactory instance;

    // For authorizing my self-signed-cert in development:
    // (Kinda lame to have it in source here, but its not re-used & its - hopefully - specific
    // to my cert.)
    private static char[] KEYSTORE_PASSWORD = "demopass".toCharArray();

    public static OkHttpClientFactory getInstance() {
        if (instance == null) instance = new OkHttpClientFactory();
        return instance;
    }

    private OkHttpClientFactory() {
        ctx = Application.getAppContext();

        Interceptor tokenInterceptor = chain -> {
            Request request = chain.request();
            TokenSet authTokens = LoginRepository.getInstance().getTokens();
            if (authTokens != null && request.header("Authorization") == null) {
                // TODO: add extra confirmation that request is to ApiCompat.apiUrl
                request = request.newBuilder()
                        .addHeader("Authorization", "Bearer " + authTokens.getAccessToken())
                        .build();
            }
            return chain.proceed(request);
        };

        Interceptor stripMsHeaders = chain -> {
            Response response = chain.proceed(chain.request());
            // Just testing around: caching seems to be non-existent and devs tend to ask "any headers
            // different" on cache-related issues on github. So let's try stripping out MS
            // headers that change per request.
            return response.newBuilder()
                    .removeHeader("x-ms-request-id")
                    .removeHeader("etag")
                    .removeHeader("date")
                    .build();
        };

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (BuildConfig.DEBUG) {
            setupLogging(builder, HttpLoggingInterceptor.Level.BASIC);
            setupSsl(builder);

            client = builder
                    .addInterceptor(tokenInterceptor)
                    .addNetworkInterceptor(stripMsHeaders)
                    .build();
        }
        else {
            // Use a standard OkHttpClient, just add the interceptor after its created.
            client = builder.addInterceptor(tokenInterceptor).build();
        }

        // set up picasso at this point:
        // TODO: Find documentation on PicassoProvider and determine if its a preferable alternative
        //       to this approach.

        // TODO: setting the downloader (required for interceptor to apply auth header)
        //       removes default caching behavior, so we need to re-supply cache logic.
        Picasso picasso = new Picasso.Builder(ctx)
                .downloader(new OkHttp3Downloader(getClient()))
                .indicatorsEnabled(BuildConfig.DEBUG)
                .loggingEnabled(BuildConfig.DEBUG)
                .build();
        Picasso.setSingletonInstance(picasso);
    }

    public OkHttpClient getClient() {
        return client;
    }

    /**
     * Constructs a new Retrofit instance that utilizes the custom OkHttpClient.
     * @return
     */
    public Retrofit buildRetrofit() {
        Gson gson = Serialization.getGson();

        return new Retrofit.Builder()
                .baseUrl(ApiCompat.apiUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(getClient())
                .build();
    }

    private void setupLogging(OkHttpClient.Builder builder, HttpLoggingInterceptor.Level level) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(level);
        builder.addInterceptor(logging);
    }

    /**
     * Opens a self-signed dev cert from the raw resources folder
     * and sets up the ssl socket factory to accept connections using that cert.
     */
    private void setupSsl(OkHttpClient.Builder builder) {
        if (BuildConfig.DEBUG){
            HandshakeCertificates.Builder certBuilder = new HandshakeCertificates.Builder();
            for (int resId : new int[]{R.raw.ca_certificate, R.raw.certificate}) {
                try {
                    certBuilder.addTrustedCertificate(createCertificate(resId));
                }
                catch (CertificateException | IOException exception) {
                    Log.e("HCF", "Failed to append dev-cert");
                }
            }

            HandshakeCertificates certificates = certBuilder
                    .addPlatformTrustedCertificates()
                    .build();

            // now add to http stuff
            builder.sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager());

            // hostname verifier doesn't seem to really be chill with me - despite having the
            // hostname as both the NC and in SAN. w/e.
            // Unfortunately, OkHttp makes it non-trivial to leverage default behavior, so we'll
            // just have to cope with only running the dev API for now.
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return (hostname.compareTo("192.168.0.10") == 0
                            || hostname.compareTo("forge.local") == 0
                            || hostname.compareTo("viv2api.azurewebsites.net") == 0
                            // and now the blob storage hosts:
                            || hostname.compareTo("viv2blobstorage.blob.core.windows.net") == 0);
                }
            });
        }
        else Log.w("HCF", "Not setting up certificate bypasses - not in debug");
    }

    private X509Certificate createCertificate(@RawRes int resId) throws CertificateException, IOException {
        InputStream trustedCertificateIS = ctx.getResources().openRawResource(resId);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate ca;

        try {
            ca = (X509Certificate) cf.generateCertificate(trustedCertificateIS);
        } finally {
            trustedCertificateIS.close();
        }

        return ca;
    }
}
