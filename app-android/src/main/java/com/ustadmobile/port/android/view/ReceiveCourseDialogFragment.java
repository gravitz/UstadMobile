package com.ustadmobile.port.android.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.toughra.ustadmobile.R;
import com.ustadmobile.port.sharedse.controller.ReceiveCoursePresenter;
import com.ustadmobile.port.sharedse.view.ReceiveCourseView;

/**
 * Created by mike on 8/22/17.
 */

public class ReceiveCourseDialogFragment extends UstadDialogFragment implements ReceiveCourseView, DialogInterface.OnClickListener {

    private View rootView;

    private ReceiveCoursePresenter mPresenter;

    private AlertDialog alertDialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = inflater.inflate(R.layout.fragment_receive_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.receive);
        builder.setView(rootView);
        builder.setPositiveButton("Accept", this);
        builder.setNegativeButton("Decline", this);
        alertDialog = builder.create();
        mPresenter = new ReceiveCoursePresenter(getContext(), this);
        return alertDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        mPresenter.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mPresenter.onStop();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        switch(which) {
            case AlertDialog.BUTTON_POSITIVE:
                mPresenter.handleClickAccept();
                break;

            case AlertDialog.BUTTON_NEGATIVE:
                mPresenter.handleClickDecline();
                break;
        }
    }

    @Override
    public void setMode(int mode) {
        setButtonsEnabled(mode == MODE_ACCEPT_DECLINE);
        rootView.findViewById(R.id.fragment_receive_dialog_waiting_layout).setVisibility(
                mode == MODE_WAITING ? View.VISIBLE : View.GONE);
        rootView.findViewById(R.id.fragment_receive_dialog_info_layout).setVisibility(
                mode == MODE_ACCEPT_DECLINE ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setSharedCourseName(String courseName) {
        TextView receiveText = rootView.findViewById(R.id.fragment_receive_dialog_info_text);
        receiveText.setText(courseName);
    }

    @Override
    public void setWaitingStatusText(int messageCode) {

    }

    @Override
    public void setSenderName(String senderName) {

    }

    @Override
    public void setButtonsEnabled(boolean enabled) {
        alertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(enabled);
        alertDialog.getButton(Dialog.BUTTON_NEGATIVE).setEnabled(enabled);
    }

    @Override
    public void setDeviceName(String deviceName) {
        TextView deviceNameView = rootView.findViewById(R.id.fragment_receive_dialog_device_name);
        String deviceNameStr = getResources().getString(R.string.device_name) + ": " + deviceName;
        deviceNameView.setText(deviceNameStr);
    }
}
