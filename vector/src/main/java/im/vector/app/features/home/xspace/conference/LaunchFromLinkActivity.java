package im.vector.app.features.home.xspace.conference;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;

import java.net.MalformedURLException;
import java.net.URL;

import im.vector.app.R;

public class LaunchFromLinkActivity extends AppCompatActivity {

    private URL                     serverURL;
    private String                  configRoomName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_jitsi);
        getSupportActionBar().hide();

        if (getIntent().getData() != null) {
            try {
                serverURL = new URL("https://x-linx.space");

                String[] intentRoomName = getIntent().getData().toString().split("https://x-linx.space/");
                configRoomName = intentRoomName[1];

                JitsiMeetConferenceOptions options
                        = new JitsiMeetConferenceOptions.Builder()
                        .setServerURL(serverURL)
                        .setWelcomePageEnabled(false)
                        .setFeatureFlag("recording.enabled", false)
                        .setFeatureFlag("ios.recording.enabled", false)
                        .setFeatureFlag("live-streaming.enabled", false)
                        .setFeatureFlag("close-captions.enabled", false)
                        .setFeatureFlag("video-share.enabled", false)
                        .setFeatureFlag("meeting-name.enabled", true)
                        .setFeatureFlag("toolbox.alwaysVisible", true)
                        .setFeatureFlag("call-integration.enabled", false)
                        .setRoom(configRoomName)
                        .build();

                JitsiMeetActivity.launch(this, options);
                finish();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }
}
