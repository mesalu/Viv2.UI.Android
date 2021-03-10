package com.mesalu.viv2.android_ui.data;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.mesalu.viv2.android_ui.BuildConfig;
import com.mesalu.viv2.android_ui.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class HttpClient {
    private static class HostNamePermissiveHurlStack extends HurlStack {
        public HostNamePermissiveHurlStack(UrlRewriter rewriter, SSLSocketFactory sslSocketFactory) {
            super(rewriter, sslSocketFactory);
        }

        @Override
        protected HttpURLConnection createConnection(URL url) throws IOException {
            HttpsURLConnection conn = (HttpsURLConnection) super.createConnection(url);
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    if (hostname.equals("192.168.0.10")) return true;
                    return HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session);
                }
            });
            return conn;
        }
    }

    private static HttpClient instance;
    private RequestQueue requestQueue;
    private static Context ctx;

    // For authorizing my self-signed-cert in development:
    private static char[] KEYSTORE_PASSWORD = "demopass".toCharArray();

    private HttpClient(Context context) {
        ctx = context.getApplicationContext();

        // WARN: this is a stop gap while development occurs. Before long this should
        //       be removed in favor of adding a ssl cert to dev devices.


        // NOTE: example has a semi-singleton setup for this member, where
        //      a getter method is used to check for existence and instantiate if
        //      missing, much like a singleton instance fetcher.
        //      It would make sense if contexts can persist through changes that would
        //      otherwise reset state (thus making the requestQueue member null).
        //      I am... skeptical.. this is true though, so we'll just do a one-and-done
        //      instantiation. If there are errors where this member is null where it should
        //      not be, then taking an approach in-line with the example would be the thing
        //      to do.
        HurlStack hurlStack;
        if (BuildConfig.DEBUG) {
            hurlStack = new HostNamePermissiveHurlStack(null, newSslSocketFactory());
            requestQueue = Volley.newRequestQueue(ctx, hurlStack);
        }
        else {
            requestQueue = Volley.newRequestQueue(ctx);
        }
    }

    public static synchronized HttpClient getInstance(Context context) {
        if (instance == null)
            instance = new HttpClient(context);
        return instance;
    }

    /**
     * Creates a socket factory configured to trust self-signed keys stored in res.raw.dev_keystore.
     * Only for non-release builds
     */
    private SSLSocketFactory newSslSocketFactory() {
        if (!BuildConfig.DEBUG) return null;
        try {
            // Get an instance of the Bouncy Castle KeyStore format
            KeyStore trusted = KeyStore.getInstance("PKCS12");

            // Get the raw resource, which contains the keystore with
            // your trusted certificates (root and any intermediate certs)
            InputStream in = ctx.getApplicationContext().getResources().openRawResource(R.raw.dev_keystore);
            try {
                // Initialize the keystore with the provided trusted certificates
                // Provide the password of the keystore
                trusted.load(in, KEYSTORE_PASSWORD);
            } finally {
                in.close();
            }

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(trusted);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            SSLSocketFactory sf = context.getSocketFactory();
            return sf;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public <T> Request<T> add(Request<T> r) {
        return requestQueue.add(r);
    }
}
