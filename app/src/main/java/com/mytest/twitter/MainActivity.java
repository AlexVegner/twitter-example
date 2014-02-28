package com.mytest.twitter;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class MainActivity extends Activity {

    private static String TAG = MainActivity.class.getName();
    // TODO Update TWITTER_CONSUMER_KEY with your consumer key
    private static final String TWITTER_CONSUMER_KEY = "<Put your consumer key here>";
    // TODO Update TWITTER_CONSUMER_SECRET with your consumer secret
    private static final String TWITTER_CONSUMER_SECRET = "<Put your consumer secret here>";
    private static final String TWITTER_CALLBACK_URL = "twitterapp://twitterapp";

    private static final String PREF_ACCESS_TOKEN = "accessToken";
    private static final String PREF_ACCESS_TOKEN_SECRET = "accessTokenSecret";

    private Twitter mTwitter;
    private RequestToken mRequestToken;
    private SharedPreferences mPrefs;
    private Button mLoginButton;
    private Button mLogoutButton;
    private Button mPostStatusButton;
    private EditText mStatusEdit;
    private TextView mStatusLabel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // map all view
        mLoginButton = (Button) findViewById(R.id.btnLoginTwitter);
        mLogoutButton = (Button) findViewById(R.id.btnLogoutTwitter);
        mPostStatusButton = (Button) findViewById(R.id.btnUpdateStatus);
        mStatusEdit = (EditText) findViewById(R.id.txtUpdateStatus);
        mStatusLabel = (TextView) findViewById(R.id.lblUpdate);

        mPrefs = getSharedPreferences("twitterPrefs", MODE_PRIVATE);
        mTwitter = new TwitterFactory().getInstance();
        mTwitter.setOAuthConsumer(TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_SECRET);
    }

    public void onClick_Login(View button) {
        Log.i(TAG, "Login Pressed");
        if (mPrefs.contains(PREF_ACCESS_TOKEN) && !TextUtils.isEmpty(mPrefs.getString(PREF_ACCESS_TOKEN, null))) {
            Log.i(TAG, "Repeat User");
            loginAuthorisedUser();
        } else {
            Log.i(TAG, "New User");
            loginNewUser();
        }
    }

    public void onClick_Logout(View button) {
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeSessionCookie();
        mTwitter.setOAuthAccessToken(null);
        mTwitter.shutdown();
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ACCESS_TOKEN, "");
        editor.putString(PREF_ACCESS_TOKEN_SECRET, "");
        editor.commit();
        setLginedState(false);
    }

    public void onClick_PostStatus(View button) {
        postTweet();
    }

    private void loginNewUser() {
        final boolean[] prevendDoubleCallbackEvent = {false};
        new AsyncTask<Void, Void, Boolean>(){

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    mRequestToken = mTwitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
                    return true;
                } catch (TwitterException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result){
                    String url = mRequestToken.getAuthenticationURL();
                    WebView wv = new WebView(getActivity());
                    wv.loadUrl(url);


                    final Dialog dialog = new Dialog(getActivity());
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                    dialog.setContentView(wv);


                    wv.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            view.loadUrl(url);


                            return true;
                        }

                        @Override
                        public void onPageStarted(WebView view, String url, Bitmap favicon) {
                            if (!prevendDoubleCallbackEvent[0] && url.contains(TWITTER_CALLBACK_URL)){
                                prevendDoubleCallbackEvent[0] = true;
                                Log.i("credentials", url);
                                String verifier = Uri.parse(url).getQueryParameter("oauth_verifier");

                                new AsyncTask<String, Void, AccessToken>(){

                                    @Override
                                    protected AccessToken doInBackground(String... params) {
                                        try {
                                            return mTwitter.getOAuthAccessToken(mRequestToken, params[0]);

                                        } catch (TwitterException e) {
                                            e.printStackTrace();
                                            return null;
                                        }
                                    }

                                    @Override
                                    protected void onPostExecute(AccessToken result) {
                                        super.onPostExecute(result);
                                        if (result != null){
                                            mTwitter.setOAuthAccessToken(result);
                                            saveAccessToken(result);
                                            setLginedState(true);
                                        }
                                    }
                                }.execute(verifier);
                                dialog.dismiss();

                            } else
                                super.onPageStarted(view, url, favicon);

                        }


                    });

                    dialog.show();
                }

            }
        }.execute();
    }

    private void loginAuthorisedUser() {
        String token = mPrefs.getString(PREF_ACCESS_TOKEN, null);
        String secret = mPrefs.getString(PREF_ACCESS_TOKEN_SECRET, null);

        // Create the twitter access token from the credentials we got previously
        AccessToken at = new AccessToken(token, secret);
        mTwitter.setOAuthAccessToken(at);
        setLginedState(true);
    }

    private void setLginedState(boolean isLogined){
        mLoginButton.setVisibility(isLogined ? View.GONE : View.VISIBLE);
        mLogoutButton.setVisibility(isLogined ? View.VISIBLE : View.GONE);
        mPostStatusButton.setVisibility(isLogined ? View.VISIBLE : View.GONE);
        mStatusEdit.setVisibility(isLogined ? View.VISIBLE : View.GONE);
        mStatusLabel.setVisibility(isLogined ? View.VISIBLE : View.GONE);
    }

    private void postTweet() {
        new AsyncTask<Void, Void, Boolean>(){

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    mTwitter.updateStatus(mStatusEdit.getText().toString() + " - Tweeting with @Blundell_apps #AndroidDev Tutorial using #Twitter4j http://blog.blundell-apps.com/sending-a-tweet");
                    return true;
                } catch (TwitterException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                showTweetPosted(result);
            }
        }.execute();
    }

    private void showTweetPosted(boolean value){
        if (value){
            Toast.makeText(getActivity(), getActivity().getString(R.string.twitter_posted), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), getActivity().getString(R.string.twitter_failed_to_post), Toast.LENGTH_LONG).show();
        }
    }

    private void saveAccessToken(AccessToken at) {
        String token = at.getToken();
        String secret = at.getTokenSecret();
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ACCESS_TOKEN, token);
        editor.putString(PREF_ACCESS_TOKEN_SECRET, secret);
        editor.commit();
    }


    public Activity getActivity() {
        return this;
    }
}