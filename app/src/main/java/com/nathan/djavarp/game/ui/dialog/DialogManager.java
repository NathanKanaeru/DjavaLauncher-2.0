package com.nathan.djavarp.game.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nathan.djavarp.R;
import com.nathan.djavarp.launcher.other.Util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class DialogManager {

    static final int DIALOG_STYLE_MSGBOX         = 0;
    static final int DIALOG_STYLE_INPUT          = 1;
    static final int DIALOG_STYLE_LIST           = 2;
    static final int DIALOG_STYLE_PASSWORD       = 3;
    static final int DIALOG_STYLE_TABLIST        = 4;
    static final int DIALOG_STYLE_TABLIST_HEADERS= 5;

    native void sendDialogResponse(int i, int i2, int i3, byte[] bArr);

    private Activity activity;
    private ConstraintLayout mMainLayout;
    private TextView mCaption;
    private TextView mContent;
    private ScrollView mMsgBoxLayout;
    private ConstraintLayout mInputLayout;
    private EditText mInput;
    private ConstraintLayout mListLayout;
    private RecyclerView mRecycler;
    private ConstraintLayout mLeftBtn;
    private ConstraintLayout mRightBtn;

    private ArrayList<String> mRowsList;
    private final ArrayList<TextView> mHeadersList = new ArrayList<>();

    private int mCurrentDialogId = -1;
    private int mCurrentDialogTypeId = -1;
    private String mCurrentInputText = "";
    private int mCurrentListItem = -1;

    public boolean isShow;

    public DialogManager(Activity activity) {
        this.activity = activity;
        this.mMainLayout = activity.findViewById(R.id.sd_dialog_main);

        if (mMainLayout == null) return;

        this.mCaption = activity.findViewById(R.id.sd_dialog_caption);
        this.mContent = activity.findViewById(R.id.sd_dialog_text);
        this.mMsgBoxLayout = activity.findViewById(R.id.sd_dialog_text_layout);
        this.mInputLayout = activity.findViewById(R.id.sd_dialog_input_layout);
        this.mInput = activity.findViewById(R.id.sd_dialog_input);
        this.mListLayout = activity.findViewById(R.id.sd_dialog_list_layout);
        this.mRecycler = activity.findViewById(R.id.sd_dialog_list_recycler);
        this.mLeftBtn = activity.findViewById(R.id.sd_button_positive);
        this.mRightBtn = activity.findViewById(R.id.sd_button_negative);

        ConstraintLayout headersLayout = activity.findViewById(R.id.sd_dialog_tablist_row);
        for (int i = 0; i < headersLayout.getChildCount(); i++) {
            this.mHeadersList.add((TextView) headersLayout.getChildAt(i));
        }

        mLeftBtn.setOnClickListener(v -> sendDialogResponse(1));
        mRightBtn.setOnClickListener(v -> sendDialogResponse(0));

        this.mRowsList = new ArrayList<>();

        Util.HideLayout(this.mMainLayout, false);
        isShow = false;
    }

    public void show(int dialogId, int dialogTypeId, String caption, String content, String leftBtnText, String rightBtnText) {
        switch (dialogTypeId) {
            case DIALOG_STYLE_INPUT:
                mInput.setInputType(InputType.TYPE_CLASS_TEXT);
                break;
            case DIALOG_STYLE_PASSWORD:
                mInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                break;
            case DIALOG_STYLE_TABLIST_HEADERS:
            case DIALOG_STYLE_TABLIST:
                break;
        }

        activity.runOnUiThread(() -> {
            clearDialogData();

            this.mCurrentDialogId = dialogId;
            this.mCurrentDialogTypeId = dialogTypeId;

            if (dialogTypeId == DIALOG_STYLE_MSGBOX) {
                mInputLayout.setVisibility(View.GONE);
                mListLayout.setVisibility(View.GONE);
                mMsgBoxLayout.setVisibility(View.VISIBLE);
            } else if (dialogTypeId == DIALOG_STYLE_INPUT || dialogTypeId == DIALOG_STYLE_PASSWORD) {
                mInputLayout.setVisibility(View.VISIBLE);
                mMsgBoxLayout.setVisibility(View.VISIBLE);
                mListLayout.setVisibility(View.GONE);
            } else {
                mInputLayout.setVisibility(View.GONE);
                mMsgBoxLayout.setVisibility(View.GONE);
                mListLayout.setVisibility(View.VISIBLE);
                loadTabList(content);

                DialogAdapter adapter = new DialogAdapter(this.mRowsList, this.mHeadersList);
                adapter.setOnClickListener((i, str) -> {
                    this.mCurrentListItem = i;
                    this.mCurrentInputText = str;
                });
                adapter.setOnDoubleClickListener(() -> sendDialogResponse(1));
                mRecycler.setLayoutManager(new LinearLayoutManager(activity));
                mRecycler.setAdapter(adapter);

                mMainLayout.post(() -> {
                    int width = mCaption.getWidth();
                    if (mRecycler.getMinimumWidth() < width) {
                        mRecycler.setMinimumWidth(width);
                    }
                    if (dialogTypeId != DIALOG_STYLE_LIST) {
                        adapter.updateSizes();
                    }
                    mRecycler.requestLayout();
                });
            }

            mCaption.setText(Util.getColoredString(caption));
            mContent.setText(Util.getColoredString(content));
            ((TextView) mLeftBtn.getChildAt(0)).setText(Util.getColoredString(leftBtnText));
            ((TextView) mRightBtn.getChildAt(0)).setText(Util.getColoredString(rightBtnText));

            if (rightBtnText == null || rightBtnText.isEmpty()) {
                mRightBtn.setVisibility(View.GONE);
            } else {
                mRightBtn.setVisibility(View.VISIBLE);
            }

            Util.ShowLayout(this.mMainLayout, false);
            isShow = true;
        });
    }

    public void hide() {
        activity.runOnUiThread(() -> {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
            Util.HideLayout(this.mMainLayout, false);
            isShow = false;
        });
    }

    public void hideWithoutReset() {
        Util.HideLayout(this.mMainLayout, false);
        isShow = false;
    }

    public void showWithOldContent() {
        Util.ShowLayout(this.mMainLayout, false);
        isShow = true;
    }

    public void SendDialogResponse(int btnId, int listItem, String inputText) {
        if (listItem == -1) {
            int style = this.mCurrentDialogTypeId;
            if (style == DIALOG_STYLE_LIST || style == DIALOG_STYLE_TABLIST || style == DIALOG_STYLE_TABLIST_HEADERS) {
                listItem = 0;
            }
        }

        try {
            byte[] bytes = inputText.getBytes("windows-1251");
            sendDialogResponse(btnId, this.mCurrentDialogId, listItem, bytes);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            hide();
        }
    }

    private void loadTabList(String content) {
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (this.mCurrentDialogTypeId == DIALOG_STYLE_TABLIST_HEADERS && i == 0) {
                String[] headers = lines[i].split("\t");
                for (int j = 0; j < headers.length && j < mHeadersList.size(); j++) {
                    mHeadersList.get(j).setText(Util.getColoredString(headers[j]));
                    mHeadersList.get(j).setVisibility(View.VISIBLE);
                }
            } else {
                this.mRowsList.add(lines[i]);
            }
        }
    }

    private void clearDialogData() {
        mRecycler.setMinimumWidth(300);
        mInput.setText("");
        mCurrentDialogId = -1;
        mCurrentDialogTypeId = -1;
        mCurrentListItem = -1;
        mRowsList.clear();
        mRecycler.setAdapter(null);
        for (TextView h : mHeadersList) {
            h.setText("");
            h.setVisibility(View.GONE);
        }
    }

    private void sendDialogResponse(int btnId) {
        if (mCurrentDialogTypeId == DIALOG_STYLE_INPUT || mCurrentDialogTypeId == DIALOG_STYLE_PASSWORD) {
            this.mCurrentInputText = this.mInput.getText().toString();
        } else if (mCurrentDialogTypeId == DIALOG_STYLE_MSGBOX) {
            this.mCurrentInputText = "";
        }
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);

        try {
            sendDialogResponse(btnId, mCurrentDialogId, mCurrentListItem, mCurrentInputText.getBytes("windows-1251"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Util.HideLayout(mMainLayout, false);
        isShow = false;
    }
}
