package com.ustadmobile.port.android.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.ByteArrayDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.VideoPlayerPresenter;
import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.impl.UmAccountManager;
import com.ustadmobile.core.util.UMIOUtils;
import com.ustadmobile.core.view.VideoPlayerView;
import com.ustadmobile.lib.db.entities.ContentEntry;
import com.ustadmobile.port.android.impl.audio.Codec2Player;
import com.ustadmobile.port.sharedse.container.ContainerManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static com.ustadmobile.port.android.util.UMAndroidUtil.bundleToMap;

public class VideoPlayerActivity extends UstadBaseActivity implements VideoPlayerView {

    private PlayerView playerView;

    private SimpleExoPlayer player;

    private boolean playWhenReady;

    private int currentWindow = 0;

    private long playbackPosition = 0;

    private VideoPlayerPresenter mPresenter;

    boolean isPortrait = true;

    private Codec2Player audioPlayer;

    private int subtitleSelection = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT) {
            isPortrait = true;
            setContentView(R.layout.activity_portrait_video_player_view);
        } else {
            isPortrait = false;
            setContentView(R.layout.activity_landscape_video_player_view);
        }

        if (savedInstanceState != null) {
            playbackPosition = (long) savedInstanceState.get("playback");
            playWhenReady = (boolean) savedInstanceState.get("playWhenReady");
            currentWindow = (int) savedInstanceState.get("currentWindow");
        }

        playerView = findViewById(R.id.activity_video_player_view);

        if (isPortrait) {
            setUMToolbar(R.id.activity_video_player_toolbar);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mPresenter = new VideoPlayerPresenter(getContext(),
                Objects.requireNonNull(bundleToMap(getIntent().getExtras())), this);
        mPresenter.onCreate(bundleToMap(savedInstanceState));
    }

    private void clickUpNavigation() {
        runOnUiThread(() -> {
            if (mPresenter != null) {
                mPresenter.handleUpNavigation();
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                clickUpNavigation();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    private void initializePlayer() {
        player = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(this),
                new DefaultTrackSelector(), new DefaultLoadControl());

        playerView.setPlayer(player);
        if (mPresenter.getAudioPath() != null && !mPresenter.getAudioPath().isEmpty()) {

            player.addListener(new Player.DefaultEventListener() {
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    if (playbackState == (Player.STATE_READY) && playWhenReady) {
                        playbackPosition = player.getContentPosition();
                        releaseAudio();
                        playAudio(playbackPosition);
                    } else {
                        releaseAudio();
                    }
                    super.onPlayerStateChanged(playWhenReady, playbackState);
                }
            });
        }

        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        setVideoParams(mPresenter.getVideoPath(), mPresenter.getAudioPath(), mPresenter.getSrtLangList(), mPresenter.getSrtMap());
    }

    @Override
    public void setVideoParams(String videoPath, String audioPath, ArrayList<String> langList, HashMap<String, String> srtMap) {
        if (audioPath != null && !audioPath.isEmpty()) {

            player.addListener(new Player.DefaultEventListener() {
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    if (playbackState == (Player.STATE_READY) && playWhenReady) {
                        playbackPosition = player.getContentPosition();
                        releaseAudio();
                        playAudio(playbackPosition);
                    } else {
                        releaseAudio();
                    }
                    super.onPlayerStateChanged(playWhenReady, playbackState);
                }
            });
        }

        if (videoPath != null && !videoPath.isEmpty()) {
            Uri uri = Uri.parse(videoPath);
            MediaSource mediaSource = buildMediaSource(uri);

            ImageButton subtitles = findViewById(R.id.exo_subtitle_button);
            if (langList != null && !langList.isEmpty()) {

                subtitles.setVisibility(View.VISIBLE);
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>((Context) getContext(),
                        android.R.layout.select_dialog_singlechoice, langList);

                subtitles.setOnClickListener(view -> {

                    AlertDialog.Builder builderSingle = new AlertDialog.Builder((Context) getContext());
                    builderSingle.setTitle("Select Subtitle Language");
                    builderSingle.setSingleChoiceItems(arrayAdapter, subtitleSelection, (dialogInterface, position) -> {
                        subtitleSelection = position;
                        String srtName = arrayAdapter.getItem(position);
                        setSubtitle(srtMap.get(srtName), mediaSource);
                        dialogInterface.cancel();
                    });
                    builderSingle.setNegativeButton("cancel", (dialog, which) -> dialog.dismiss());
                    builderSingle.show();

                });
                setSubtitle(srtMap.get(langList.get(1)), mediaSource);

            } else {
                subtitles.setVisibility(View.GONE);

                player.prepare(mediaSource, false, false);
            }


        }
    }

    public void setSubtitle(String subtitleData, MediaSource mediaSource) {

        if (subtitleData == null) {
            playerView.getSubtitleView().setVisibility(View.GONE);
            return;
        }

        playerView.getSubtitleView().setVisibility(View.VISIBLE);

        Format subtitleFormat = Format.createTextSampleFormat(
                null, MimeTypes.APPLICATION_SUBRIP, // The mime type. Must be set correctly.
                C.SELECTION_FLAG_DEFAULT, null);

        UmAppDatabase repoAppDatabase = UmAccountManager.INSTANCE.getRepositoryForActiveAccount(getContext());
        UmAppDatabase appDatabase = UmAppDatabase.getInstance(getContext());

        HandlerThread ht = new HandlerThread("SubtitleThread");
        ht.start();
        new Handler(ht.getLooper()).post(() -> {

            try {
                ContainerManager containerManager = new ContainerManager(mPresenter.getContainer(), appDatabase, repoAppDatabase);

                ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(
                        UMIOUtils.readStreamToString(containerManager.getInputStream(containerManager.getEntry(subtitleData))).getBytes());

                DataSource.Factory factory = () -> byteArrayDataSource;

                MediaSource subTitleSource = new SingleSampleMediaSource.Factory(factory).
                        createMediaSource(Uri.EMPTY, subtitleFormat, C.TIME_UNSET);

                MergingMediaSource mergedSource = new MergingMediaSource(mediaSource, subTitleSource);
                player.prepare(mergedSource, false, false);
            } catch (IOException ignored) {

            }

        });


    }


    public void playAudio(long fromMs) {
        audioPlayer = new Codec2Player(mPresenter.getAudioPath(), fromMs);
        audioPlayer.play();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong("playback", playbackPosition);
        outState.putBoolean("playWhenReady", playWhenReady);
        outState.putInt("currentWindow", currentWindow);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        hideSystemUi();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
            releaseAudio();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
            releaseAudio();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();
            player.release();
            player = null;
        }
    }

    private void releaseAudio() {
        if (audioPlayer != null) {
            audioPlayer.stop();
        }
    }

    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        if (!isPortrait) {
            playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }

    }

    @Override
    public void loadUrl(String firstUrl) {


    }

    @Override
    public void setVideoInfo(ContentEntry result) {
        runOnUiThread(() -> {
            if (isPortrait) {
                ((Toolbar) findViewById(R.id.activity_video_player_toolbar)).setTitle(result.getTitle());
                ((TextView) findViewById(R.id.activity_video_player_description)).setText(result.getDescription());
            }
        });
    }

    private MediaSource buildMediaSource(Uri uri) {
        return new ExtractorMediaSource.Factory(
                new FileDataSourceFactory())
                .createMediaSource(uri);
    }
}
