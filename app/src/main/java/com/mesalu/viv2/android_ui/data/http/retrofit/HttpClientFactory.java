package com.mesalu.viv2.android_ui.data.http.retrofit;

import android.content.Context;
import android.util.Log;

import androidx.annotation.RawRes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mesalu.viv2.android_ui.Application;
import com.mesalu.viv2.android_ui.BuildConfig;
import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.Serialization;

import java.io.IOException;
import java.io.InputStream;

import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.tls.HandshakeCertificates;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Helper class to construct an OkHttpClient class suitable for use in a debug environment
 * - Allows (my) self signed certificate
 * - Sets up logging for the http module.
 */
class HttpClientFactory {
    private Context ctx;
    private OkHttpClient.Builder builder;

    // For authorizing my self-signed-cert in development:
    // (Kinda lame to have it in source here, but its not re-used & its - hopefully - specific
    // to my cert.)
    private static char[] KEYSTORE_PASSWORD = "demopass".toCharArray();

    public HttpClientFactory() {
        this.ctx = Application.getAppContext();
        builder = new OkHttpClient.Builder();

        setupLogging(HttpLoggingInterceptor.Level.BASIC);
        setupSsl();
    }

    public Retrofit composeClient() {
        Gson gson = Serialization.getGson();

        String url = (BuildConfig.DEBUG) ? "https://192.168.0.10:5001/api/" : "https://viv2api.azurewebsites.net/api/";
        //String url = "https://viv2api.azurewebsites.net/api/";
        return new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(builder.build())
                .build();
    }

    private void setupLogging(HttpLoggingInterceptor.Level level) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(level);
        builder.addInterceptor(logging);
    }

    /**
     * Opens a self-signed dev cert from the raw resources folder
     * and sets up the ssl socket factory to accept connections using that cert.
     */
    private void setupSsl() {
        if (BuildConfig.DEBUG){
            HandshakeCertificates.Builder certBuilder = new HandshakeCertificates.Builder();
            for (int resId : new int[]{R.raw.ca_certificate, R.raw.certificate}) {
                try {
                    certBuilder.addTrustedCertificate(createCertificate(resId));
                }
                catch (CertificateException|IOException exception) {
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
                        || hostname.compareTo("viv2api.azurewebsites.net") == 0);
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
