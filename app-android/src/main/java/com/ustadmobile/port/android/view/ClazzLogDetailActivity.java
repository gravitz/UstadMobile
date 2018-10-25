package com.ustadmobile.port.android.view;

import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.arch.paging.PagedListAdapter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.ClazzLogDetailPresenter;
import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.core.view.ClassLogDetailView;
import com.ustadmobile.lib.db.entities.ClazzLogAttendanceRecord;
import com.ustadmobile.lib.db.entities.ClazzLogAttendanceRecordWithPerson;
import com.ustadmobile.port.android.util.UMAndroidUtil;

import java.util.HashMap;
import java.util.Map;

import ru.dimorinny.floatingtextbutton.FloatingTextButton;

import static com.ustadmobile.core.controller.ClazzListPresenter.ARG_CLAZZ_UID;
import static com.ustadmobile.core.controller.ClazzLogDetailPresenter.ARG_LOGDATE;
import static com.ustadmobile.lib.db.entities.ClazzLogAttendanceRecord.STATUS_ABSENT;
import static com.ustadmobile.lib.db.entities.ClazzLogAttendanceRecord.STATUS_ATTENDED;
import static com.ustadmobile.lib.db.entities.ClazzLogAttendanceRecord.STATUS_PARTIAL;

/**
 * The ClassLogDetail activity.
 *
 * This Activity extends UstadBaseActivity and implements ClassLogDetailView
 */
public class ClazzLogDetailActivity extends UstadBaseActivity
        implements ClassLogDetailView, View.OnClickListener, View.OnLongClickListener{

    private Toolbar toolbar;

    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mRecyclerLayoutManager;

    private ClazzLogDetailPresenter mPresenter;
    public static final String ARGS_CLAZZLOG_UID = "clazzloguid";
    public static final String TAG_STATUS = "status";

    public long clazzLogUid;
    public long clazzUid;
    public long logDate;

    //static map matching attendance status code value with color tint
    private static Map<Integer, Integer> STATUS_TO_COLOR_MAP = new HashMap<>();

    //static map matching attendance status code value with
    private static Map<Integer, Integer> STATUS_TO_STRING_ID_MAP = new HashMap<>();

    private static Map<Integer, Integer> SELECTED_STATUS_TO_STATUS_TAG = new HashMap<>();

    private static Map<Integer, Integer> UNSELECTED_STATUS_TO_STATUS_TAG = new HashMap<>();

    static {
        STATUS_TO_COLOR_MAP.put(STATUS_ATTENDED, R.color.traffic_green);
        STATUS_TO_COLOR_MAP.put(STATUS_ABSENT, R.color.traffic_red);
        STATUS_TO_COLOR_MAP.put(STATUS_PARTIAL, R.color.traffic_orange);

        STATUS_TO_STRING_ID_MAP.put(STATUS_ATTENDED, R.string.attendance);
        STATUS_TO_STRING_ID_MAP.put(STATUS_ABSENT, R.string.attendance);
        STATUS_TO_STRING_ID_MAP.put(STATUS_PARTIAL, R.string.attendance);

        SELECTED_STATUS_TO_STATUS_TAG.put(STATUS_ATTENDED, R.string.present_selected);
        SELECTED_STATUS_TO_STATUS_TAG.put(STATUS_ABSENT, R.string.absent_selected);
        SELECTED_STATUS_TO_STATUS_TAG.put(STATUS_PARTIAL, R.string.partial_selected);

        UNSELECTED_STATUS_TO_STATUS_TAG.put(STATUS_ATTENDED, R.string.present_unselected);
        UNSELECTED_STATUS_TO_STATUS_TAG.put(STATUS_ABSENT, R.string.absent_unselected);
        UNSELECTED_STATUS_TO_STATUS_TAG.put(STATUS_PARTIAL, R.string.partial_unselected);

    }

    /**
     * The Log Detail (Attendance) Recycler Adapter
     */
    protected class ClazzLogDetailRecyclerAdapter
            extends PagedListAdapter<ClazzLogAttendanceRecordWithPerson,
                ClazzLogDetailRecyclerAdapter.ClazzLogDetailViewHolder>{

        protected class ClazzLogDetailViewHolder extends RecyclerView.ViewHolder{
            protected ClazzLogDetailViewHolder(View itemView){
                super(itemView);
            }
        }

        protected ClazzLogDetailRecyclerAdapter(
                @NonNull DiffUtil.ItemCallback<ClazzLogAttendanceRecordWithPerson> diffCallback){
            super(diffCallback);
        }

        @NonNull
        @Override
        public ClazzLogDetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){

            View clazzLogDetailListItem =
                    LayoutInflater.from(getApplicationContext()).inflate(
                            R.layout.item_clazzlog_detail_student, parent, false);
            return new ClazzLogDetailViewHolder(clazzLogDetailListItem);
        }

        /**
         * This method sets the elements after it has been obtained for that item'th position.
         *
         * Every item in the recycler view will have set its colors if no attendance status is set.
         * every attendance button will have it-self mapped to tints on activation.
         *
         * @param holder
         * @param position
         */
        @Override
        public void onBindViewHolder(@NonNull ClazzLogDetailViewHolder holder, int position){
            ClazzLogAttendanceRecordWithPerson attendanceRecord = getItem(position);

            String studentName = attendanceRecord.getPerson().getFirstNames() + " " +
                    attendanceRecord.getPerson().getLastName();

            holder.itemView.setTag(attendanceRecord.getClazzLogAttendanceRecordUid());

            ((TextView)holder.itemView
                    .findViewById(R.id.item_clazzlog_detail_student_name)).setText(studentName);
            ((ImageView)holder.itemView
                    .findViewById(R.id.item_clazzlog_detail_student_present_icon))
                    .setColorFilter(Color.BLACK);

            final long clazzLogAttendanceRecordUid =
                    attendanceRecord.getClazzLogAttendanceRecordUid();

            Map<Integer, ImageView> attendanceButtons = new HashMap<>();
            attendanceButtons.put(STATUS_ATTENDED, holder.itemView.findViewById(
                    R.id.item_clazzlog_detail_student_present_icon));
            attendanceButtons.put(STATUS_ABSENT, holder.itemView.findViewById(
                    R.id.item_clazzlog_detail_student_absent_icon));
            attendanceButtons.put(STATUS_PARTIAL, holder.itemView.findViewById(
                    R.id.item_clazzlog_detail_student_delay_icon));

            for(Map.Entry<Integer, ImageView> entry : attendanceButtons.entrySet()) {
                boolean selectedOption = attendanceRecord.getAttendanceStatus() == entry.getKey();
                entry.getValue().setOnClickListener((view) -> mPresenter.handleMarkStudent(
                        clazzLogAttendanceRecordUid, entry.getKey()));
                entry.getValue().setColorFilter(
                        selectedOption ?
                        ContextCompat.getColor(ClazzLogDetailActivity.this,
                                STATUS_TO_COLOR_MAP.get(entry.getKey())) :
                                ContextCompat.getColor(
                                        ClazzLogDetailActivity.this, R.color.color_gray));

                String status_tag = getResources().getString(
                        STATUS_TO_STRING_ID_MAP.get(entry.getKey())) + " " +
                        (selectedOption ? SELECTED_STATUS_TO_STATUS_TAG.get(entry.getKey()) :
                                UNSELECTED_STATUS_TO_STATUS_TAG.get(entry.getKey()));
                entry.getValue().setTag(status_tag);
                //Set any content description here.
            }

        }
    }

    // Diff callback.
    public static final DiffUtil.ItemCallback<ClazzLogAttendanceRecordWithPerson> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ClazzLogAttendanceRecordWithPerson>() {
                @Override
                public boolean areItemsTheSame(ClazzLogAttendanceRecordWithPerson oldItem,
                                               ClazzLogAttendanceRecordWithPerson newItem) {
                    return oldItem.getClazzLogAttendanceRecordUid() ==
                            newItem.getClazzLogAttendanceRecordUid();
//                    return oldItem.getPerson().getPersonUid() ==
//                            newItem.getPerson().getPersonUid();
                }

                @Override
                public boolean areContentsTheSame(ClazzLogAttendanceRecordWithPerson oldItem,
                                                  ClazzLogAttendanceRecordWithPerson newItem) {
                    return oldItem.equals(newItem);
                }
            };

    /**
     * onCreate of the Activity does the following:
     *
     * 1. Gets arguments for clazz log uid, clazz uid, logdate
     * 2. sets the recycler view
     * 3. adds handlers to all buttons on the view
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Setting layout:
        setContentView(R.layout.activity_clazz_log_detail);

        //Toolbar
        toolbar = findViewById(R.id.class_log_detail_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Get arguments
        if (getIntent().hasExtra(ARGS_CLAZZLOG_UID)){
            clazzLogUid = getIntent().getLongExtra(ARGS_CLAZZLOG_UID, -1L);
        }

        if(getIntent().hasExtra(ARG_CLAZZ_UID)){
            clazzUid = getIntent().getLongExtra(ARG_CLAZZ_UID, -1L);
        }

        if(getIntent().hasExtra(ARG_LOGDATE)){
            logDate = getIntent().getLongExtra(ARG_LOGDATE, -1L);
        }


        mRecyclerView = (RecyclerView) findViewById(R.id.class_log_detail_container_recyclerview);
        mRecyclerLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mRecyclerLayoutManager);


        mPresenter = new ClazzLogDetailPresenter(this,
                UMAndroidUtil.bundleToHashtable(getIntent().getExtras()), this);
        mPresenter.onCreate(UMAndroidUtil.bundleToHashtable(savedInstanceState));

        if(mPresenter.currentClazz != null ){
            String clazzName = mPresenter.currentClazz.getClazzName();
            if (clazzName != null && clazzName.length() > 0) {
                toolbar.setTitle(clazzName);
            }
        }

        //FAB
        FloatingTextButton fab = findViewById(R.id.class_log_detail__done_fab);
        fab.setOnClickListener(v -> mPresenter.handleClickDone());

        //Mark all present
        findViewById(R.id.activity_class_log_detail_mark_all_present_text)
            .setOnClickListener((view) ->
                    mPresenter.handleMarkAll(ClazzLogAttendanceRecord.STATUS_ATTENDED));

        //Mark all absent
        findViewById(R.id.activity_class_log_detail_mark_all_absent_text)
            .setOnClickListener((view) ->
                    mPresenter.handleMarkAll(ClazzLogAttendanceRecord.STATUS_ABSENT));
    }

    @Override
    public void setClazzLogAttendanceRecordProvider(
            UmProvider<ClazzLogAttendanceRecordWithPerson> clazzLogAttendanceRecordProvider) {

        ClazzLogDetailRecyclerAdapter recyclerAdapter =
                new ClazzLogDetailRecyclerAdapter(DIFF_CALLBACK);

        DataSource.Factory<Integer, ClazzLogAttendanceRecordWithPerson> factory =
                (DataSource.Factory<Integer, ClazzLogAttendanceRecordWithPerson>)
                        clazzLogAttendanceRecordProvider.getProvider();
        LiveData<PagedList<ClazzLogAttendanceRecordWithPerson>> data =
                new LivePagedListBuilder<>(factory, 20).build();
        data.observe(this, recyclerAdapter::submitList);
        mRecyclerView.setAdapter(recyclerAdapter);
    }

    @Override
    public void updateToolbarTitle(String title) {

        runOnUiThread(() -> {
            toolbar.setTitle(title);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        });

    }


    @Override
    public void onClick(View v) {
        int x;
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }


}
