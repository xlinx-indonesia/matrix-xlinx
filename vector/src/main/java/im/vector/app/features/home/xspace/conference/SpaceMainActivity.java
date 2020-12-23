package im.vector.app.features.home.xspace.conference;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;

import java.net.MalformedURLException;
import java.net.URL;

import im.vector.app.R;

public class SpaceMainActivity extends AppCompatActivity {

    public static final String CHANNEL_EXTRA                     = "channel_id";
    public static final String PASSPHRASE_EXTRA                  = "passphrase_key";
    public static final String SERVER_EXTRA                      = "server_url";
    public static final String USERNAME_EXTRA                    = "username_extra";
    public static final String AUDIO_ONLY_MODE                   = "xlinx_audio_only_mode";
    public static final String LOCALE_CODE                       = "xlinx_locale_code";

    private URL                     serverURL;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_jitsi);

        try {
            serverURL = new URL("https://x-linx.space");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid server URL!");
        }

        JitsiMeetUserInfo jitsiMeetUserInfo = new JitsiMeetUserInfo();
        jitsiMeetUserInfo.setDisplayName(getIntent().getStringExtra(USERNAME_EXTRA));

        JitsiMeetConferenceOptions options
                = new JitsiMeetConferenceOptions.Builder()
                .setServerURL(serverURL)
                .setWelcomePageEnabled(false)
                .setUserInfo(jitsiMeetUserInfo)
                .setFeatureFlag("recording.enabled", false)
                .setFeatureFlag("ios.recording.enabled", false)
                .setFeatureFlag("live-streaming.enabled", false)
                .setFeatureFlag("close-captions.enabled", false)
                .setFeatureFlag("video-share.enabled", false)
                .setFeatureFlag("meeting-name.enabled", true)
                .setFeatureFlag("toolbox.alwaysVisible", true)
                .setFeatureFlag("call-integration.enabled", false)
                .setRoom(getIntent().getStringExtra(CHANNEL_EXTRA))
                .build();

        JitsiMeetActivity.launch(this, options);
    }

}
