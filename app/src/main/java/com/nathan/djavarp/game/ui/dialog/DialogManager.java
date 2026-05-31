package com.nathan.djavarp.game.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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

    private static final String TAG = "DialogManager";

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

        if (mMainLayout == null) {
            Log.e(TAG, "sd_dialog_main not found in layout, inflating dynamically");
            View dialogView = activity.getLayoutInflater().inflate(R.layout.sd_dialog, null);
            this.mMainLayout = (ConstraintLayout) dialogView;
            activity.addContentView(dialogView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }

        this.mCaption = mMainLayout.findViewById(R.id.sd_dialog_caption);
        this.mContent = mMainLayout.findViewById(R.id.sd_dialog_text);
        this.mMsgBoxLayout = mMainLayout.findViewById(R.id.sd_dialog_text_layout);
        this.mInputLayout = mMainLayout.findViewById(R.id.sd_dialog_input_layout);
        this.mInput = mMainLayout.findViewById(R.id.sd_dialog_input);
        this.mListLayout = mMainLayout.findViewById(R.id.sd_dialog_list_layout);
        this.mRecycler = mMainLayout.findViewById(R.id.sd_dialog_list_recycler);
        this.mLeftBtn = mMainLayout.findViewById(R.id.sd_button_positive);
        this.mRightBtn = mMainLayout.findViewById(R.id.sd_button_negative);

        if (mCaption == null) Log.e(TAG, "sd_dialog_caption not found");
        if (mContent == null) Log.e(TAG, "sd_dialog_text not found");
        if (mMsgBoxLayout == null) Log.e(TAG, "sd_dialog_text_layout not found");
        if (mInput == null) Log.e(TAG, "sd_dialog_input not found");
        if (mRecycler == null) Log.e(TAG, "sd_dialog_list_recycler not found");
        if (mLeftBtn == null) Log.e(TAG, "sd_button_positive not found");
        if (mRightBtn == null) Log.e(TAG, "sd_button_negative not found");

        if (mLeftBtn != null) {
            mLeftBtn.setOnClickListener(v -> sendDialogResponse(1));
        }
        if (mRightBtn != null) {
            mRightBtn.setOnClickListener(v -> sendDialogResponse(0));
        }

        ConstraintLayout headersLayout = mMainLayout.findViewById(R.id.sd_dialog_tablist_row);
        if (headersLayout != null) {
            for (int i = 0; i < headersLayout.getChildCount(); i++) {
                View child = headersLayout.getChildAt(i);
                if (child instanceof TextView) {
                    this.mHeadersList.add((TextView) child);
                }
            }
        }

        this.mRowsList = new ArrayList<>();
        if (mInput != null && mInputLayout != null) {
            mInputLayout.setOnClickListener(v -> {
                mInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mInput, InputMethodManager.SHOW_IMPLICIT);
            });
        }
        Util.HideLayout(this.mMainLayout, false);
        isShow = false;
        Log.d(TAG, "DialogManager initialized successfully");
    }

    public void show(int dialogId, int dialogTypeId, String caption, String content, String leftBtnText, String rightBtnText) {
        activity.runOnUiThread(() -> {
            try {
                if (mMainLayout == null) {
                    Log.e(TAG, "show() called but mMainLayout is null");
                    return;
                }

                clearDialogData();

                this.mCurrentDialogId = dialogId;
                this.mCurrentDialogTypeId = dialogTypeId;

                if (mInput != null) {
                    if (dialogTypeId == DIALOG_STYLE_INPUT) {
                        mInput.setInputType(InputType.TYPE_CLASS_TEXT);
                    } else if (dialogTypeId == DIALOG_STYLE_PASSWORD) {
                        mInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    if (dialogTypeId == DIALOG_STYLE_INPUT || dialogTypeId == DIALOG_STYLE_PASSWORD) {
                        mInput.requestFocus();
                        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(mInput, InputMethodManager.SHOW_IMPLICIT);
                    }
                }

                if (dialogTypeId == DIALOG_STYLE_MSGBOX) {
                    if (mInputLayout != null) mInputLayout.setVisibility(View.GONE);
                    if (mListLayout != null) mListLayout.setVisibility(View.GONE);
                    if (mMsgBoxLayout != null) mMsgBoxLayout.setVisibility(View.VISIBLE);
                } else if (dialogTypeId == DIALOG_STYLE_INPUT || dialogTypeId == DIALOG_STYLE_PASSWORD) {
                    if (mInputLayout != null) mInputLayout.setVisibility(View.VISIBLE);
                    if (mMsgBoxLayout != null) mMsgBoxLayout.setVisibility(View.VISIBLE);
                    if (mListLayout != null) mListLayout.setVisibility(View.GONE);
                } else {
                    if (mInputLayout != null) mInputLayout.setVisibility(View.GONE);
                    if (mMsgBoxLayout != null) mMsgBoxLayout.setVisibility(View.GONE);
                    if (mListLayout != null) mListLayout.setVisibility(View.VISIBLE);

                    loadTabList(content);

                    DialogAdapter adapter = new DialogAdapter(this.mRowsList, this.mHeadersList);
                    adapter.setOnClickListener((i, str) -> {
                        this.mCurrentListItem = i;
                        this.mCurrentInputText = str;
                    });
                    adapter.setOnDoubleClickListener(() -> sendDialogResponse(1));
                    if (mRecycler != null) {
                        mRecycler.setLayoutManager(new LinearLayoutManager(activity));
                        mRecycler.setAdapter(adapter);
                        mMainLayout.post(() -> {
                            if (mCaption != null && mRecycler != null) {
                                int w = mCaption.getWidth();
                                if (mRecycler.getMinimumWidth() < w) {
                                    mRecycler.setMinimumWidth(w);
                                }
                            }
                            if (dialogTypeId != DIALOG_STYLE_LIST) {
                                adapter.updateSizes();
                            }
                            mRecycler.requestLayout();
                        });
                    }
                }

                if (mCaption != null) mCaption.setText(Util.getColoredString(caption));
                if (mContent != null) mContent.setText(Util.getColoredString(content));

                if (mLeftBtn != null && mLeftBtn.getChildCount() > 0) {
                    View child = mLeftBtn.getChildAt(0);
                    if (child instanceof TextView) {
                        ((TextView) child).setText(Util.getColoredString(leftBtnText));
                    }
                }
                if (mRightBtn != null && mRightBtn.getChildCount() > 0) {
                    View child = mRightBtn.getChildAt(0);
                    if (child instanceof TextView) {
                        ((TextView) child).setText(Util.getColoredString(rightBtnText));
                    }
                }

                if (mRightBtn != null) {
                    if (rightBtnText == null || rightBtnText.isEmpty()) {
                        mRightBtn.setVisibility(View.GONE);
                    } else {
                        mRightBtn.setVisibility(View.VISIBLE);
                    }
                }

                Util.ShowLayout(this.mMainLayout, false);
                isShow = true;
                Log.d(TAG, "Dialog shown: id=" + dialogId + " style=" + dialogTypeId);
            } catch (Exception e) {
                Log.e(TAG, "Error showing dialog", e);
            }
        });
    }

    public void hide() {
        activity.runOnUiThread(() -> {
            try {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (mInput != null) {
                    imm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
                }
                if (mMainLayout != null) {
                    Util.HideLayout(this.mMainLayout, false);
                }
                isShow = false;
            } catch (Exception e) {
                Log.e(TAG, "Error hiding dialog", e);
            }
        });
    }

    public void hideWithoutReset() {
        if (mMainLayout != null) {
            Util.HideLayout(this.mMainLayout, false);
        }
        isShow = false;
    }

    public void showWithOldContent() {
        if (mMainLayout != null) {
            Util.ShowLayout(this.mMainLayout, false);
        }
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
            Log.e(TAG, "Encoding error", e);
        } finally {
            hide();
        }
    }

    private void loadTabList(String content) {
        if (content == null) return;
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
        if (mRecycler != null) {
            mRecycler.setMinimumWidth(300);
            mRecycler.setAdapter(null);
        }
        if (mInput != null) {
            mInput.setText("");
        }
        mCurrentDialogId = -1;
        mCurrentDialogTypeId = -1;
        mCurrentListItem = -1;
        if (mRowsList != null) {
            mRowsList.clear();
        }
        for (TextView h : mHeadersList) {
            if (h != null) {
                h.setText("");
                h.setVisibility(View.GONE);
            }
        }
    }

    private void sendDialogResponse(int btnId) {
        try {
            if (mCurrentDialogTypeId == DIALOG_STYLE_INPUT || mCurrentDialogTypeId == DIALOG_STYLE_PASSWORD) {
                this.mCurrentInputText = mInput != null ? mInput.getText().toString() : "";
            } else if (mCurrentDialogTypeId == DIALOG_STYLE_MSGBOX) {
                this.mCurrentInputText = "";
            }

            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (mInput != null) {
                imm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
            }

            sendDialogResponse(btnId, mCurrentDialogId, mCurrentListItem, mCurrentInputText.getBytes("windows-1251"));
        } catch (Exception e) {
            Log.e(TAG, "Error in sendDialogResponse", e);
        }

        if (mMainLayout != null) {
            Util.HideLayout(mMainLayout, false);
        }
        isShow = false;
    }
}
