package com.ustadmobile.port.sharedse.controller;

import com.ustadmobile.core.controller.UstadBaseController;
import com.ustadmobile.core.db.JobStatus;
import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.core.db.dao.ContentEntryParentChildJoinDao;
import com.ustadmobile.core.db.dao.DownloadJobItemDao;
import com.ustadmobile.core.generated.locale.MessageID;
import com.ustadmobile.core.impl.UMStorageDir;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.util.UMFileUtil;
import com.ustadmobile.lib.db.entities.ContentEntryStatus;
import com.ustadmobile.lib.db.entities.DownloadJob;
import com.ustadmobile.lib.db.entities.DownloadJobItem;
import com.ustadmobile.lib.db.entities.DownloadSet;
import com.ustadmobile.lib.db.entities.DownloadSetItem;
import com.ustadmobile.port.sharedse.view.DownloadDialogView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

public class DownloadDialogPresenter extends UstadBaseController<DownloadDialogView> {

    public static final String ARG_CONTENT_ENTRY_UID = "contentEntryUid";

    private boolean deleteFileOptions = false;

    private UmAppDatabase umAppDatabase;

    private long contentEntryUid = 0L;

    private UmLiveData<DownloadJob> downloadDownloadJobLive;

    private UmLiveData<Boolean> allowedMeteredLive;

    private long downloadSetUid;

    private long downloadJobUid = 0L;

    private UstadMobileSystemImpl impl;

    private String statusMessage = null;

    private String destinationDir = null;

    private static final int stackedButtonPauseIndex = 0;

    private static final int stackedButtonCancelIndex = 1;

    private static final int stackedButtonContinueIndex = 2;

    public DownloadDialogPresenter(Object context, Hashtable arguments, DownloadDialogView view) {
        super(context, arguments, view);

    }

    @Override
    public void onCreate(Hashtable savedState) {
        super.onCreate(savedState);
        umAppDatabase = UmAppDatabase.getInstance(context);
        impl = UstadMobileSystemImpl.getInstance();
        contentEntryUid = Long.parseLong(String.valueOf(getArguments()
                .get(ARG_CONTENT_ENTRY_UID)));
        view.runOnUiThread(() ->{
            view.setWifiOnlyOptionVisible(false);
        });
        new Thread(this::setup).start();
    }


    private void startObservingJob(){
        view.runOnUiThread(() -> {
            downloadDownloadJobLive = umAppDatabase.getDownloadJobDao().getJobLive(downloadJobUid);
            downloadDownloadJobLive.observe(DownloadDialogPresenter.this,
                    this::handleDownloadJobStatusChange);
        });
    }

    private void startObservingDownloadSetMeteredState(){
        view.runOnUiThread(() -> {
            allowedMeteredLive = umAppDatabase.getDownloadSetDao()
                    .getLiveMeteredNetworkAllowed(downloadSetUid);
            allowedMeteredLive.observe(DownloadDialogPresenter.this,
                    this::handleDownloadSetMeteredStateChange);
        });
    }

    private void handleDownloadSetMeteredStateChange(Boolean meteredConnection){
        view.setDownloadOverWifiOnly(meteredConnection != null && !meteredConnection);
    }

    private void handleDownloadJobStatusChange(DownloadJob downloadJob){
        if(downloadJob != null){
            int downloadStatus = downloadJob.getDjStatus();
            view.setCalculatingViewVisible(false);
            view.setWifiOnlyOptionVisible(true);
            if(downloadStatus >= JobStatus.COMPLETE_MIN
                    && downloadStatus <= JobStatus.COMPLETE_MAX){
                deleteFileOptions = true;
                view.setStackOptionsVisible(false);
                view.setBottomButtonsVisible(true);
                statusMessage = impl.getString(MessageID.download_state_downloaded,
                        getContext());
                view.setBottomButtonPositiveText(impl.getString(
                        MessageID.download_delete_btn_label,getContext()));
                view.setBottomButtonNegativeText(impl.getString(
                        MessageID.download_cancel_label,getContext()));
            }else if(downloadStatus >= JobStatus.RUNNING_MIN
                    && downloadStatus <= JobStatus.RUNNING_MAX){
                view.setStackOptionsVisible(true);
                view.setBottomButtonsVisible(false);
                String [] optionTexts = new String[]{
                        impl.getString(MessageID.download_pause_stacked_label,getContext()),
                        impl.getString(MessageID.download_cancel_stacked_label,getContext()),
                        impl.getString(MessageID.download_continue_stacked_label,getContext())
                };
                statusMessage = impl.getString(MessageID.download_state_downloading,
                        getContext());
                view.setStackedOptions(view.getOptionIds(), optionTexts);
            }else{
                statusMessage = impl.getString(MessageID.download_state_download,
                        getContext());
                view.setStackOptionsVisible(false);
                view.setBottomButtonsVisible(true);
                view.setBottomButtonPositiveText(impl.getString(
                        MessageID.download_continue_btn_label,getContext()));
                view.setBottomButtonNegativeText(impl.getString(
                        MessageID.download_cancel_label,getContext()));
            }


            new Thread(() -> {
                DownloadJobItemDao.DownloadJobInfo jobInfo = umAppDatabase.getDownloadJobItemDao()
                        .getDownloadJobInfoByJobUid(downloadJobUid);
                view.runOnUiThread(() -> view.setStatusText(statusMessage,
                        jobInfo.getTotalDownloadItems(),
                        UMFileUtil.formatFileSize(jobInfo.getTotalSize())));
            }).start();

        }
    }

    private void setup() {
        downloadSetUid = umAppDatabase.getDownloadSetDao()
                .findDownloadSetUidByRootContentEntryUid(contentEntryUid);
        if(downloadSetUid == 0)
            downloadSetUid = umAppDatabase.getDownloadSetItemDao()
                    .findDownloadSetUidByContentEntryUid(contentEntryUid);

        if(downloadSetUid == 0)
            createDownloadSet();

        downloadJobUid = umAppDatabase.getDownloadJobDao().getLatestDownloadJobUidForDownloadSet(
                downloadSetUid);

        startObservingJob();

        startObservingDownloadSetMeteredState();

    }

    private void createDownloadSet() {
        DownloadSet downloadSet = new DownloadSet();
        UMStorageDir[] storageDir = UstadMobileSystemImpl.getInstance()
                .getStorageDirs(UstadMobileSystemImpl.SHARED_RESOURCE, getContext());
        destinationDir = storageDir[0].getDirURI();
        downloadSet.setDestinationDir(destinationDir);
        downloadSet.setDsRootContentEntryUid(contentEntryUid);

        downloadSetUid = umAppDatabase.getDownloadSetDao().insert(downloadSet);
        List<DownloadSetItem> downloadSetItems = new ArrayList<>();

        List<ContentEntryParentChildJoinDao.ContentEntryParentChildJoinSummary> currentChildUids;
        Set<Long> allChildUids = new HashSet<>();
        List<Long> parentUids = new ArrayList<>();
        parentUids.add(contentEntryUid);
        allChildUids.add(contentEntryUid);

        do {
            currentChildUids = umAppDatabase.getContentEntryParentChildJoinDao()
                    .findChildEntriesByParents(parentUids);
            parentUids.clear();
            for(ContentEntryParentChildJoinDao.ContentEntryParentChildJoinSummary child : currentChildUids){
                if(!allChildUids.contains(child.getChildContentEntryUid())) {
                    allChildUids.add(child.getChildContentEntryUid());
                    if(!child.isLeaf())
                        parentUids.add(child.getChildContentEntryUid());
                }
            }
        }while(!parentUids.isEmpty());

        for(long childUid : allChildUids) {
            downloadSetItems.add(new DownloadSetItem(downloadSetUid, childUid));
        }

        umAppDatabase.getDownloadSetItemDao().insert(downloadSetItems);

        createDownloadJob();
    }

    private void createDownloadJob() {
        DownloadJob downloadJob = new DownloadJob();
        downloadJob.setTimeRequested(System.currentTimeMillis());
        downloadJob.setTimeCreated(System.currentTimeMillis());
        downloadJob.setDjDsUid(downloadSetUid);

        long downloadJobId = umAppDatabase.getDownloadJobDao().insert(downloadJob);

        List<DownloadJobItemDao.DownloadJobItemToBeCreated> itemToBeCreated =
                umAppDatabase.getDownloadJobItemDao()
                        .findJobItemsToBeCreatedForDownloadSet(downloadSetUid);

        List<DownloadJobItem> jobItems = new ArrayList<>();
        List<ContentEntryStatus> statusList = new ArrayList<>();

        for(DownloadJobItemDao.DownloadJobItemToBeCreated item: itemToBeCreated){
            DownloadJobItem jobItem = new DownloadJobItem();
            jobItem.setDjiContentEntryFileUid(item.getContentEntryFileUid());
            jobItem.setDjiDjUid(downloadJobId);
            jobItem.setDownloadLength(item.getFileSize());
            jobItem.setDjiDsiUid(item.getDownloadSetItemUid());
            jobItem.setDestinationFile(UMFileUtil.joinPaths(destinationDir,
                    String.valueOf(item.getContentEntryFileUid())));
            jobItems.add(jobItem);

            statusList.add(new ContentEntryStatus(item.getContentEntryUid(),
                    item.getFileSize() > 0, item.getFileSize()));
        }

        umAppDatabase.getContentEntryStatusDao().insertOrAbort(statusList);
        umAppDatabase.getDownloadJobItemDao().insert(jobItems);
        umAppDatabase.getDownloadJobDao().updateTotalBytesToDownload(downloadJobId,null);
    }


    public void handleClickPositive() {
        if(deleteFileOptions){
            deleteDownloadFile();
        }else{
            continueDownloading();
        }
    }

    /**
     * Handle negative click. If the underlying system is already dismissing the dialog
     * set dismissAfter to false to avoid a call to dismissDialog
     * @param dismissAfter
     */
    public void handleClickNegative(boolean dismissAfter) {
        //if the download has not been started
        umAppDatabase.getDownloadSetDao().cleanupUnused(downloadSetUid);
        if(dismissAfter)
            dismissDialog();
    }

    public void handleClickNegative() {
        handleClickNegative(true);
    }

    public void handleClickStackedButton(int idClicked) {
        if (idClicked == view.getOptionIds()[stackedButtonPauseIndex]) {
            new Thread(() -> umAppDatabase.getDownloadJobDao().updateJobAndItems(downloadJobUid,
                    JobStatus.PAUSED, JobStatus.PAUSING)).start();
            dismissDialog();
        }else if(idClicked == view.getOptionIds()[stackedButtonCancelIndex]){
            cancelDownload();
            dismissDialog();
        }else if(idClicked == view.getOptionIds()[stackedButtonContinueIndex]){
            continueDownloading();
            dismissDialog();
        }
    }



    private void continueDownloading(){
        new Thread(() -> umAppDatabase.getDownloadJobDao().updateJobAndItems(downloadJobUid,
                JobStatus.QUEUED, -1)).start();
    }

    private void dismissDialog(){
        view.runOnUiThread(() -> view.dismissDialog());
    }

    private void cancelDownload(){
        new Thread(() -> umAppDatabase.getDownloadJobDao()
                .update(downloadSetUid,JobStatus.CANCELED)).start();
    }


    private void deleteDownloadFile(){
        new Thread(() -> {
            List<DownloadJobItem> downloadSetItemList = umAppDatabase.getDownloadJobItemDao()
                            .findByJobUid(downloadJobUid);

            if(umAppDatabase.getDownloadSetDao().deleteByUid(downloadSetUid) != 0
                && umAppDatabase.getDownloadSetItemDao().deleteByDownloadSetUid(downloadSetUid) != 0
                && umAppDatabase.getDownloadJobDao().deleteByDownloadSetUid(downloadSetUid) != 0
                && umAppDatabase.getDownloadJobItemDao().deleteByDownloadSetUid(downloadSetUid) != 0){
                for(DownloadJobItem item : downloadSetItemList){
                    File file = new File(item.getDestinationFile());
                    if(file.exists()){
                        file.delete();
                    }
                }
            }
        }).start();
    }

    public void handleWiFiOnlyOption(boolean wifiOnly){
        new Thread(() -> umAppDatabase.getDownloadSetDao()
                .setMeteredConnectionBySetUid(downloadSetUid,!wifiOnly)).start();
    }

    public void handleStorageOptionSelection(String destinationDir){
        this.destinationDir = destinationDir;
    }

    /**
     * Testing purpose
     */
    protected long getCurrentJobId(){
        return downloadJobUid;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
