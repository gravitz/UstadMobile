package com.ustadmobile.port.android.view;

import android.app.Activity;
import android.arch.paging.PagedListAdapter;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.CommonHandlerPresenter;
import com.ustadmobile.lib.db.entities.PersonWithEnrollment;

import java.util.HashMap;

public class PersonWithEnrollmentRecyclerAdapter
        extends PagedListAdapter<PersonWithEnrollment,
        PersonWithEnrollmentRecyclerAdapter.ClazzLogDetailViewHolder> {

    Context theContext;
    Activity theActivity;
    Fragment theFragment;
    private CommonHandlerPresenter mPresenter;
    boolean showAttendance = false;
    boolean showEnrollment = false;

    HashMap checkBoxHM = new HashMap();

    protected class ClazzLogDetailViewHolder extends RecyclerView.ViewHolder{
        protected ClazzLogDetailViewHolder(View itemView){
            super(itemView);
        }
    }

    protected PersonWithEnrollmentRecyclerAdapter(
            @NonNull DiffUtil.ItemCallback<PersonWithEnrollment> diffCallback){
        super(diffCallback);
    }

    protected PersonWithEnrollmentRecyclerAdapter(
            @NonNull DiffUtil.ItemCallback<PersonWithEnrollment> diffCallback, Context context,
            Activity activity, CommonHandlerPresenter  presenter, boolean attendance,
            boolean enrollment){
        super(diffCallback);
        theContext = context;
        theActivity = activity;
        mPresenter = presenter;
        showAttendance = attendance;
        showEnrollment = enrollment;
    }

    protected PersonWithEnrollmentRecyclerAdapter(
            @NonNull DiffUtil.ItemCallback<PersonWithEnrollment> diffCallback, Context context,
            Fragment fragment, CommonHandlerPresenter  presenter, boolean attendance,
            boolean enrollment){
        super(diffCallback);
        theContext = context;
        theFragment = fragment;
        mPresenter = presenter;
        showAttendance = attendance;
        showEnrollment = enrollment;
    }

    @NonNull
    @Override
    public PersonWithEnrollmentRecyclerAdapter.ClazzLogDetailViewHolder
            onCreateViewHolder(@NonNull ViewGroup parent, int viewType){

        View clazzLogDetailListItem =
                LayoutInflater.from(theContext).inflate(
                        R.layout.item_studentlistenroll_student, parent, false);
        return new PersonWithEnrollmentRecyclerAdapter.ClazzLogDetailViewHolder(
                clazzLogDetailListItem);
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
    public void onBindViewHolder(
            @NonNull PersonWithEnrollmentRecyclerAdapter.ClazzLogDetailViewHolder holder,
            int position){

        PersonWithEnrollment personWithEnrollment = getItem(position);

        String studentName = personWithEnrollment.getFirstNames() + " " +
                personWithEnrollment.getLastName();

        TextView studentNameTextView =
                holder.itemView.findViewById(R.id.item_studentlist_student_simple_student_title);
        studentNameTextView.setText(studentName);

        ImageView trafficLight = ((ImageView) holder.itemView
                .findViewById(R.id.item_studentlist_student_simple_attendance_trafficlight));
        TextView attendanceTextView =
                holder.itemView.findViewById(R.id.item_studentlist_student_simple_attendance_percentage);

        View cl = holder.itemView.findViewById(R.id.item_studentlist_student_cl);
        cl.setOnClickListener(v ->
                mPresenter.handleCommonPressed(personWithEnrollment.getPersonUid()));

        if(showAttendance){
            long attendancePercentage =
                    (long) (personWithEnrollment.getAttendancePercentage() * 100);

            String attendanceStringLiteral;
            if(theActivity != null){
                attendanceStringLiteral = theActivity.getText(R.string.attendance).toString();
            }else{
                attendanceStringLiteral = theFragment.getText(R.string.attendance).toString();
            }

            String studentAttendancePercentage = attendancePercentage +
                    "% " + attendanceStringLiteral;

            trafficLight.setVisibility(View.VISIBLE);
            if(attendancePercentage > 75L){
                trafficLight.setColorFilter(ContextCompat.getColor(theContext,
                        R.color.traffic_green));
            }else if(attendancePercentage > 50L){
                trafficLight.setColorFilter(ContextCompat.getColor(theContext,
                        R.color.traffic_orange));
            }else{
                trafficLight.setColorFilter(ContextCompat.getColor(theContext,
                        R.color.traffic_red));
            }


            attendanceTextView.setVisibility(View.VISIBLE);
            attendanceTextView.setText(studentAttendancePercentage);
        }else{

            //Change the constraint layout so that the hidden bits are not empty spaces.

            ConstraintLayout mainCL = holder.itemView.findViewById(R.id.item_studentlist_student_cl);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(mainCL);

            constraintSet.connect(R.id.item_studentlist_student_simple_horizontal_divider,
                    ConstraintSet.TOP, R.id.item_studentlist_student_simple_student_image,
                    ConstraintSet.BOTTOM, 16);
            constraintSet.connect(R.id.item_studentlist_student_simple_attendance_percentage,
                    ConstraintSet.TOP, R.id.item_studentlist_student_simple_student_title,
                    ConstraintSet.BOTTOM, 0);
            constraintSet.connect(R.id.item_studentlist_student_simple_attendance_trafficlight,
                    ConstraintSet.TOP, R.id.item_studentlist_student_simple_student_title,
                    ConstraintSet.BOTTOM, 0);
            constraintSet.applyTo(mainCL);

        }


        if(showEnrollment){

            //Get checkbox and set it to visible.
            CheckBox checkBox =
                    holder.itemView.findViewById(R.id.item_studentlist_student_simple_student_checkbox);
            checkBox.setVisibility(View.VISIBLE);

            //Get current person's enrollment w.r.t. this class. (Its either set or null (not enrolled)
            boolean personWithEnrollmentBoolean = false;
            if (personWithEnrollment.getEnrolled() != null){
                personWithEnrollmentBoolean = personWithEnrollment.getEnrolled();
            }

            //To preserve checkboxes, add this enrollment to the Map.
            checkBoxHM.put(personWithEnrollment.getPersonUid(), personWithEnrollmentBoolean);
            //set the value of the check according to the value..
            checkBox.setChecked((Boolean) checkBoxHM.get(personWithEnrollment.getPersonUid()));

            //Add a change listener to the checkbox
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                    checkBox.setChecked(isChecked));

            checkBox.setOnClickListener(v -> {
                final boolean isChecked = checkBox.isChecked();
                //mPresenter.handleEnrollChanged(personWithEnrollment, isChecked);
                HashMap<PersonWithEnrollment, Boolean> arguments = new HashMap<>();
                arguments.put(personWithEnrollment, isChecked);
                mPresenter.handleSecondaryPressed(arguments.entrySet().iterator().next());
            });

        }



    }
}