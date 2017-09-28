package se.wibrick.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import se.wibrick.sdk.*;

public class MainActivity extends AppCompatActivity implements ServiceCallback {

    private static final String TAG = "DEMO " + MainActivity.class.getSimpleName();
    private final static int REQUEST_ENABLE_BT = 1;
    RelativeLayout relativeLayout;
    private Context context;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onStart() {
        super.onStart();

        // enabling actionbar app icon and behaving it as toggle button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            getSupportActionBar().setCustomView(R.layout.actionbar_layout);
            getSupportActionBar().setDisplayShowCustomEnabled(true);

            View mCustomView = getSupportActionBar().getCustomView();
            TextView TitleToolBar = (TextView) mCustomView.findViewById(R.id.toolbar_title);
            TitleToolBar.setText(R.string.app_name);

            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        setToolbarTheme();

    }

    @Override
    protected void onDestroy() {

        // Clean up beacon-manager
        final ServiceHandler serviceHandler = ServiceHandler.getInstance(MainActivity.this);
        serviceHandler.onDestroy(context);

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK)
            checkLocationPermissions(mBluetoothAdapter);

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(context, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        }

        assert mBluetoothAdapter != null;
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else
            checkLocationPermissions(mBluetoothAdapter);

    }

    private void checkLocationPermissions(final BluetoothAdapter bluetoothAdapter) {

        final StorageManager storageManager = StorageManager.getInstance(context);
        final APIHandler apiHandler = new APIHandler(storageManager, context);

        PermissionHelper.Builder.goWithPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                new PermissionHelper.PermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        if (bluetoothAdapter.isEnabled())
                            makeHandshake(apiHandler);
                    }

                    @Override
                    public void onPermissionDenied() {

                    }
                },
                R.string.location_alert_title,
                R.string.location_alert_message
        )
                .start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionHelper.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void getResource(final APIHandler apiHandler, final String resource) {

        final LinearLayout baseView = (LinearLayout) this.findViewById(R.id.baseView);

        apiHandler.getResource(resource, new PostTaskListener<APIResponse>() {

            @Override
            public void onPostTaskFailure(APIResponse result) {
                if (result.getExeption() != null)
                    Log.d(TAG, result.getExeption().getClass().getSimpleName() + ", " + result.getExeption().getMessage());
            }

            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void onPostTaskSuccess(APIResponse result) {

                if (result.getExeption() != null)
                    Log.d(TAG, result.getExeption().getClass().getSimpleName() + ", " + result.getExeption().getMessage());

                Log.d(TAG, result.getAPIResource().getContentType());

                if (result.getHttpStatus() == 200) {

                    if (result.getAPIResource().getContentType().contains("text")) {
                        Log.d(TAG, result.getAPIResource().getText());

                        WebView webView = new WebView(context);
                        WebSettings webSettings = webView.getSettings();
                        webSettings.setJavaScriptEnabled(true);
                        webView.setWebViewClient(new WebViewClient());
                        webView.loadUrl(result.getAPIResource().getText());
                        baseView.addView(webView);
                    } else {
                        renderContent(result);
                    }
                }
            }
        });
    }

    private void setActionBarSoundButton(int icon, final MediaPlayer mp) {

        ActionBar actionBar = getSupportActionBar();
        View view = actionBar != null ? actionBar.getCustomView() : null;

        if (view != null) {
            ImageView imageview = (ImageView) view.findViewById(R.id.bar_sound_button);
            imageview.setImageDrawable(ContextCompat.getDrawable(this, icon));
            imageview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mp.isPlaying()) {
                        mp.pause();
                        setActionBarSoundButton(R.drawable.sound_on_white, mp);
                    } else {
                        mp.start();
                        setActionBarSoundButton(R.drawable.sound_off_white, mp);
                    }
                }
            });
        }
    }

    @Override
    public void onSuccess(APIResponse apiResponse) {

        String proximityUUID = "";

        if (apiResponse.getContent().getWibrickBeacon() != null)
            proximityUUID = apiResponse.getContent().getWibrickBeacon().getProximityUUID().toString();

        switch (apiResponse.getAPIResponseType()) {
            case APIResponseType.RESPONSE_TYPE_SERVICE_CONNECTED:
                Toast.makeText(context, "Service connected...", Toast.LENGTH_SHORT).show();
                break;
            case APIResponseType.RESPONSE_TYPE_SERVICE_DISCONNECTED:
                Toast.makeText(context, "Service disconnected...", Toast.LENGTH_SHORT).show();
                break;
            case APIResponseType.RESPONSE_TYPE_ENTER_REGION:
                notifyBeacon(proximityUUID, true);
                break;
            case APIResponseType.RESPONSE_TYPE_EXIT_REGION:
                notifyBeacon(proximityUUID, false);
                break;
            case APIResponseType.RESPONSE_TYPE_ENTER_TRIGGERZONE:

                // If you would like to render content from response
                renderContent(apiResponse);

                // If you would like to shape your own content through properties and key/pair values
                //renderContentFromProperties(apiResponse);

                break;
            case APIResponseType.RESPONSE_TYPE_EXIT_TRIGGERZONE:
                Toast.makeText(context, "Exit trigger-zone", Toast.LENGTH_SHORT).show();
                break;
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void renderContent(APIResponse result) {

        final LinearLayout linearLayout = (LinearLayout) this.findViewById(R.id.baseView);
        final ContentRenderer contentRenderer = new ContentRenderer(context);

        final APIContent apiContent = result.getContent();

        if (apiContent != null) {

            linearLayout.removeAllViews();

            relativeLayout = null;
            relativeLayout = contentRenderer.renderUI(apiContent, new ContentCallback() {
                @Override
                public void onEvent(View view, ContentEvent contentEvent) {

                    String eventMessage = contentEvent.getMessage();
                    String eventPayload = contentEvent.getPayload();

                    switch (contentEvent.getEventType()) {
                        case ContentEventType.EVENT_TYPE_TAP:
                            if (eventMessage.equals("link") && Patterns.WEB_URL.matcher(eventPayload).matches()) {
                                Log.d(TAG, "EVENT_TYPE_TAP, payload: " + eventPayload);
                                Uri uri = Uri.parse(eventPayload);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                            }
                            break;
                        case ContentEventType.EVENT_TYPE_DOUBLETAP:
                            Log.d(TAG, "EVENT_TYPE_DOUBLETAP, payload: " + eventPayload);
                    }
                }
            });

            String jsonProperties = apiContent.getProperties();

            String soundURL = contentRenderer.getKeyPairValue(jsonProperties, "", "audio");
            String logoURL = contentRenderer.getKeyPairValue(jsonProperties, "theme", "logo");
            String actionBarBgColor = contentRenderer.getKeyPairValue(jsonProperties, "theme", "color");

            ActionBar actionBar = getSupportActionBar();

            if (!logoURL.equals("")) {
                try {
                    if (actionBar != null) {
                        View view = actionBar.getCustomView();
                        new DownloadImageTask(context, (ImageView) view.findViewById(R.id.action_bar_image), ImageView.ScaleType.FIT_CENTER)
                                .execute(logoURL);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Exception, setting toolbar-theme");
                }
            }

            if (!actionBarBgColor.equals("")) {
                if (actionBar != null)
                    actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(actionBarBgColor)));
            }

            if (!soundURL.equals("")) {
                initMediaPlayer(soundURL);
            }

            linearLayout.addView(relativeLayout);
        }

    }

    private void initMediaPlayer(String soundURL) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            mediaPlayer.setAudioAttributes(attributes);
        } else {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        try {
            mediaPlayer.setDataSource(context, Uri.parse(soundURL));
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(final MediaPlayer mp) {
                    mp.start();

                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams
                            (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(10, 10, 0, 0);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifyBeacon(String uuid, boolean isEnter) {
        String message;
        if (isEnter) {
            message = "You have enter at " + uuid;
        } else {
            message = "You have exit from " + uuid;
        }

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notifyID = (int) System.currentTimeMillis();
        NotificationCompat.Builder mNotifyBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setContentTitle("Beacon Notification")
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true);

        mNotifyBuilder.setContentText(message);

        mNotificationManager.notify(
                notifyID,
                mNotifyBuilder.build());
    }

    private void setToolbarTheme() {

        try {

            if (getSupportActionBar() != null) {

                Drawable d = ContextCompat.getDrawable(getApplicationContext(), R.drawable.top_boarder);
                getSupportActionBar().setBackgroundDrawable(d);

                View view = getSupportActionBar().getCustomView();

                if (view != null) {
                    ImageView imageview = (ImageView) view.findViewById(R.id.action_bar_image);
                    imageview.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.symbol_wi));
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception, setting toolbar-theme");
        }
    }

    @Override
    public void onFailure(WibrickException e) {
        Log.d(TAG, "onFailure: " + e.getMessage());
    }

    private void makeHandshake(final APIHandler apiHandler) {
        apiHandler.makeHandshake(new PostTaskListener<APIResponse>() {

            @Override
            public void onPostTaskFailure(APIResponse result) {
                if (result.getExeption() != null)
                    Log.d(TAG, result.getExeption().getClass().getSimpleName() + ", " + result.getExeption().getMessage());
            }

            @Override
            public void onPostTaskSuccess(APIResponse result) {

                Log.d(TAG, "HTTP-status: " + result.getHttpStatus());

                if (result.getExeption() != null)
                    Log.d(TAG, result.getExeption().getClass().getSimpleName() + ", " + result.getExeption().getMessage());

                if (result.getHttpStatus() == 200) {
                    final ServiceHandler serviceHandler = ServiceHandler.getInstance(MainActivity.this);
                    serviceHandler.init(MainActivity.this);
                }
            }

        });
    }

}
