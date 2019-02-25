package com.ustadmobile.core.controller;

import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.dao.ContentEntryDao;
import com.ustadmobile.core.impl.UmAccountManager;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.core.view.VideoPlayerView;
import com.ustadmobile.lib.db.entities.ContentEntry;

import java.util.Hashtable;

import static com.ustadmobile.core.view.VideoPlayerView.ARG_AUDIO_PATH;
import static com.ustadmobile.core.view.VideoPlayerView.ARG_CONTENT_ENTRY_ID;
import static com.ustadmobile.core.view.VideoPlayerView.ARG_VIDEO_PATH;

public class VideoPlayerPresenter extends UstadBaseController<VideoPlayerView> {

    private String videoPath;
    private ContentEntryDao contentEntryDao;
    private String audioPath;

    public VideoPlayerPresenter(Object context, Hashtable arguments, VideoPlayerView view) {
        super(context, arguments, view);
    }

    @Override
    public void onCreate(Hashtable savedState) {
        super.onCreate(savedState);
        UmAppDatabase appDatabase = UmAccountManager.getRepositoryForActiveAccount(getContext());
        contentEntryDao = appDatabase.getContentEntryDao();


        videoPath = (String) getArguments().get(ARG_VIDEO_PATH);
        audioPath = (String) getArguments().get(ARG_AUDIO_PATH);
        long entryUuid = Long.parseLong(getArguments().get(ARG_CONTENT_ENTRY_ID).toString());
        contentEntryDao.getContentByUuid(entryUuid, new UmCallback<ContentEntry>() {
            @Override
            public void onSuccess(ContentEntry result) {
                view.setVideoInfo(result);
            }

            @Override
            public void onFailure(Throwable exception) {

            }
        });

    }

    public String getVideoPath() {
        return videoPath;
    }

    public String getAudioPath() {
        return audioPath;
    }


}
