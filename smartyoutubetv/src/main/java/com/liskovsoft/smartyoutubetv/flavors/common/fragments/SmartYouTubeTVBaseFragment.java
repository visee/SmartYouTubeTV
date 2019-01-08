package com.liskovsoft.smartyoutubetv.flavors.common.fragments;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import com.liskovsoft.browser.Browser;
import com.liskovsoft.smartyoutubetv.fragments.FragmentManager;
import com.liskovsoft.browser.Controller;
import com.liskovsoft.browser.addons.MainBrowserFragment;
import com.liskovsoft.browser.addons.SimpleUIController;
import com.liskovsoft.smartyoutubetv.R;
import com.liskovsoft.smartyoutubetv.bootstrap.BootstrapActivity;
import com.liskovsoft.smartyoutubetv.events.ControllerEventListener;
import com.liskovsoft.smartyoutubetv.common.helpers.Helpers;
import com.liskovsoft.smartyoutubetv.misc.KeysTranslator;
import com.liskovsoft.smartyoutubetv.common.helpers.LangUpdater;
import com.liskovsoft.smartyoutubetv.misc.UserAgentManager;
import com.liskovsoft.smartyoutubetv.common.helpers.PermissionManager;
import android.annotation.SuppressLint;

public abstract class SmartYouTubeTVBaseFragment extends MainBrowserFragment {
    private static final String TAG = SmartYouTubeTVBaseFragment.class.getSimpleName();
    private Controller mController;
    private String mServiceUrl; // youtube url here
    private KeysTranslator mTranslator;
    private final static String DIAL_EXTRA = "com.amazon.extra.DIAL_PARAM";
    private final static String TEMPLATE_URL = "https://www.youtube.com/tv#?%s";
    private UserAgentManager mUAManager;

    @Override
    public void onActivityCreated(Bundle icicle) {
        Log.i(TAG, "SmartYouTubeTVActivityBase::init");

        // fix lang in case activity has been destroyed and then restored
        setupLang();
        setupUA();
        super.onActivityCreated(icicle);

        initRemoteUrl();
        initKeys();
        initPermissions();

        createController(icicle);

        makeActivityFullscreen();
        makeActivityHorizontal();
    }

    private String getLocalizedTitle() {
        String label = null;
        try {
            label = getResources().getString(
                    getActivity().getPackageManager().getActivityInfo(getActivity().getComponentName(), 0).labelRes);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return label;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && !savedInstanceState.isEmpty()) {
            Browser.activityRestored = true;
        }
    }

    private void setupUA() {
        mUAManager = new UserAgentManager(getActivity());
    }

    private void initPermissions() {
        PermissionManager.verifyStoragePermissions(getActivity());
    }

    private void initKeys() {
        mTranslator = new KeysTranslator();
    }

    private void initRemoteUrl() {
        mServiceUrl = getString(R.string.service_url);
    }

    private void setupLang() {
        new LangUpdater(getActivity()).update();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void createController(Bundle icicle) {
        Log.d(TAG, "creating controller: " + "icicle=" + icicle + ", intent=" + getActivity().getIntent());

        mController = new SimpleUIController(this);
        mController.setListener(new ControllerEventListener(getActivity(), mController, mTranslator));
        mController.setDefaultUrl(Uri.parse(mServiceUrl));
        mController.setDefaultHeaders(mUAManager.getUAHeaders());
        Intent intent = (icicle == null) ? transformIntentData(getActivity().getIntent()) : null;
        mController.start(intent);
        setController(mController);
    }

    private void makeActivityFullscreen() {
        getActivity().getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);

        if (VERSION.SDK_INT >= 19) {
            View decorView = getActivity().getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void makeActivityHorizontal() {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        event = mTranslator.doTranslateKeys(event);
        setDispatchEvent(event);

        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) { // remember real back remapped in translator
            onBackPressed();
            return true;
        }


        return false;
    }

    private void setDispatchEvent(KeyEvent event) {
        if (getActivity() instanceof FragmentManager) {
            ((FragmentManager) getActivity()).setDispatchEvent(event);
        }
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return translateMouseWheelToArrowKeys(event);
    }

    private boolean translateMouseWheelToArrowKeys(MotionEvent event) {
        if (0 != (event.getSource() & InputDevice.SOURCE_CLASS_POINTER)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_SCROLL:
                    fakeHorizontalScroll(event);
                    fakeVerticalScroll(event);
                    return false;
                // Disable events below completely.
                // This should fix hide off keyboard using air-mouse.
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_HOVER_ENTER:
                case MotionEvent.ACTION_HOVER_EXIT:
                case MotionEvent.ACTION_HOVER_MOVE:
                    return true;
            }
        }
        return false;
    }

    private void fakeVerticalScroll(MotionEvent event) {
        if (Helpers.floatEquals(event.getAxisValue(MotionEvent.AXIS_VSCROLL), 0.0f)) {
            return;
        }
        KeyEvent keyEvent = null;
        if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f)
            keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
        else
            keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP);
        dispatchKeyEvent(keyEvent);
    }

    private void fakeHorizontalScroll(MotionEvent event) {
        if (Helpers.floatEquals(event.getAxisValue(MotionEvent.AXIS_HSCROLL), 0.0f)) {
            return;
        }
        KeyEvent keyEvent = null;
        if (event.getAxisValue(MotionEvent.AXIS_HSCROLL) < 0.0f)
            keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);
        else
            keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
        dispatchKeyEvent(keyEvent);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(transformIntentData(intent));
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onBackPressed() {
        returnToLaunchersDialog();
        super.onBackPressed();
    }

    @SuppressLint("WrongConstant")
    private void returnToLaunchersDialog() {
        Intent intent = new Intent();
        intent.setClass(getActivity(), BootstrapActivity.class);
        intent.putExtra(BootstrapActivity.SKIP_RESTORE, true);

        boolean activityExists = intent.resolveActivityInfo(getActivity().getPackageManager(), PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) != null;

        if (activityExists) {
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionManager.REQUEST_EXTERNAL_STORAGE) {
            // Check if the only required permission has been granted
            if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
                Toast.makeText(getActivity(), "REQUEST_EXTERNAL_STORAGE permission has been granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "Unable to grant REQUEST_EXTERNAL_STORAGE permission", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    ///////////////////////// Begin Youtube filter /////////////////////


    private Intent transformIntentData(Intent intent) {
        if (intent == null)
            return null;

        transformRegularIntentData(intent);
        transformAmazonIntentData(intent);

        return intent;
    }

    private void transformRegularIntentData(Intent intent) {
        Uri data = intent.getData();
        if (data == null) {
            return;
        }

        intent.setData(transformUri(data));
    }

    // see Amazon's youtube apk: "org.chromium.youtube_apk.YouTubeActivity.loadStartPage(dialParam)"
    private void transformAmazonIntentData(Intent intent) {
        String dialParam = intent.getStringExtra(DIAL_EXTRA);
        if (dialParam == null) {
            return;
        }

        String uriString = String.format(TEMPLATE_URL, dialParam);
        intent.setData(Uri.parse(uriString));
    }

    /**
     * Extracts video params e.g. <code>v=xtx33RuFCik</code> from url
     * <br/>
     * Examples of the input/output url:
     * <pre>
     * origin video: https://www.youtube.com/watch?v=xtx33RuFCik
     * needed video: https://www.youtube.com/tv#/watch/video/control?v=xtx33RuFCik
     * needed video: https://www.youtube.com/tv?gl=us&hl=en-us&v=xtx33RuFCik
     * needed video: https://www.youtube.com/tv?v=xtx33RuFCik
     *
     * origin playlist: https://www.youtube.com/playlist?list=PLbl01QFpbBY1XGwNb8SBmoA3hshpK1pZj
     * needed playlist: https://www.youtube.com/tv#/watch/video/control?list=PLbl01QFpbBY1XGwNb8SBmoA3hshpK1pZj&resume
     * </pre>
     * @param url desktop url (see manifest file for the patterns)
     * @return video params
     */
    private String extractVideoIdParamFromUrl(String url) {
        String[] patterns = {"list=[^&\\s]*", "v=[^&\\s]*", "youtu.be/[^&\\s]*"};
        String res = Helpers.runMultiMatcher(url, patterns);
        if (res == null) {
            Log.w(TAG, "Url not supported: " + url);
            // Uncomment next section to debug
            // Toast.makeText(this, "Url not supported: " + url, Toast.LENGTH_LONG).show();
            return null;
        }
        return res.replace("youtu.be/", "v=");
    }

    private Uri transformUri(final Uri uri) {
        if (uri == null)
            return null;
        String url = uri.toString();
        String videoParam = extractVideoIdParamFromUrl(url);
        if (videoParam == null) {
            return Uri.parse(mServiceUrl);
        }
        String fullUrl = String.format(TEMPLATE_URL, videoParam);
        return Uri.parse(fullUrl);
    }

    ///////////////////////// End Youtube filter /////////////////////
}
