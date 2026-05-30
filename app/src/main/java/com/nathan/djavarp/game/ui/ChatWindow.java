package com.nathan.djavarp.game.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nathan.djavarp.R;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatWindow {
    static native void SendChatMessage(byte[] str);

    static EditText chat_input;
    ConstraintLayout chat_input_layout;

    TextView me_button;
    TextView try_button;
    TextView do_button;
    ImageView hide_chat;
    ConstraintLayout chat_box;

    private final int INVALID = -1;
    private final int ME_BUTTON = 0;
    private final int DO_BUTTON = 1;
    private final int TRY_BUTTON = 2;
    private int chat_button = INVALID;

    private int chatFontSize;

    private RecyclerView chat;
    int defaultChatFontSize;

    ChatAdapter adapter;
    ArrayList<Spanned> chat_lines = new ArrayList<>();

    public ChatWindow(android.app.Activity activity) {

        chat_box = activity.findViewById(R.id.chat_box);
        if (chat_box == null) return;

        hide_chat = activity.findViewById(R.id.hide_chat);
        hide_chat.setOnClickListener(view -> {
            if (chat_box.getVisibility() == View.GONE) {
                showChat(activity);
            } else {
                hideChat(activity);
            }
        });

        me_button = activity.findViewById(R.id.me_button);
        if (me_button != null) {
            me_button.setOnClickListener(view -> {
                if (chat_button == ME_BUTTON) {
                    me_button.setBackgroundTintList(null);
                    chat_button = INVALID;
                } else {
                    chat_button = ME_BUTTON;
                    me_button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9c27b0")));
                    try_button.setBackgroundTintList(null);
                    do_button.setBackgroundTintList(null);
                }
            });
        }

        try_button = activity.findViewById(R.id.try_button);
        if (try_button != null) {
            try_button.setOnClickListener(view -> {
                if (chat_button == TRY_BUTTON) {
                    try_button.setBackgroundTintList(null);
                    chat_button = INVALID;
                } else {
                    chat_button = TRY_BUTTON;
                    try_button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#087f23")));
                    me_button.setBackgroundTintList(null);
                    do_button.setBackgroundTintList(null);
                }
            });
        }

        do_button = activity.findViewById(R.id.do_button);
        if (do_button != null) {
            do_button.setOnClickListener(view -> {
                if (chat_button == DO_BUTTON) {
                    do_button.setBackgroundTintList(null);
                    chat_button = INVALID;
                } else {
                    chat_button = DO_BUTTON;
                    do_button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#c67100")));
                    try_button.setBackgroundTintList(null);
                    me_button.setBackgroundTintList(null);
                }
            });
        }

        chat_input_layout = activity.findViewById(R.id.chat_input_layout);
        chat_input_layout.setVisibility(View.GONE);
        chat_input = activity.findViewById(R.id.chat_input);
        chat_input.setShowSoftInputOnFocus(false);

        chat_input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                try {
                    SendChatMessage(chat_input.getText().toString().getBytes("windows-1251"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                toggleKeyboard(activity, false);
                return true;
            }
            return false;
        });

        defaultChatFontSize = 27;
        chat = activity.findViewById(R.id.chat);

        FadingEdgeLayout chatBox = activity.findViewById(R.id.chat_fade_box);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(activity);
        mLayoutManager.setStackFromEnd(true);
        chat.setLayoutManager(mLayoutManager);

        adapter = new ChatAdapter(activity, chat_lines);
        chat.setAdapter(adapter);
    }

    void hideChat(android.app.Activity activity) {
        activity.runOnUiThread(() -> {
            chat_box.setVisibility(View.GONE);
            hide_chat.setRotation(180);
        });
    }

    void showChat(android.app.Activity activity) {
        activity.runOnUiThread(() -> {
            chat_box.setVisibility(View.VISIBLE);
            hide_chat.setRotation(0);
        });
    }

    public void ToggleChat(boolean toggle, android.app.Activity activity) {
        activity.runOnUiThread(() -> {
            if (toggle) {
                chat.setVisibility(View.VISIBLE);
            } else {
                chat.setVisibility(View.GONE);
            }
        });
    }

    public void AddChatMessage(String msg) {
        adapter.addItem(msg);
    }

    public void ToggleChatInput(boolean toggle, android.app.Activity activity) {
        activity.runOnUiThread(() -> {
            if (toggle) {
                chat_input_layout.setVisibility(View.VISIBLE);
            } else {
                chat_input_layout.setVisibility(View.GONE);
                chat_input.getText().clear();
            }
        });
    }

    void toggleKeyboard(android.app.Activity activity, boolean toggle) {
        ToggleChatInput(toggle, activity);
        chat_input.requestFocus();
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (toggle)
            imm.showSoftInput(chat_input, InputMethodManager.SHOW_IMPLICIT);
        else
            imm.hideSoftInputFromWindow(chat_input.getWindowToken(), 0);
    }

    public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

        private final LayoutInflater inflater;
        private final List<Spanned> chat_lines;

        ChatAdapter(Context context, List<Spanned> chat_lines) {
            this.chat_lines = chat_lines;
            this.inflater = LayoutInflater.from(context);
        }

        @NotNull
        @Override
        public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.chat_message_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.chat_line_text.setTextSize(TypedValue.COMPLEX_UNIT_PX, chatFontSize > 0 ? chatFontSize : defaultChatFontSize);
            holder.chat_line_text.setText(chat_lines.get(position));
        }

        @Override
        public int getItemCount() {
            return chat_lines.size();
        }

        public List getItems() {
            return chat_lines;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            final StrokedTextView chat_line_text;

            ViewHolder(View view) {
                super(view);
                chat_line_text = view.findViewById(R.id.chat_line_text);
            }
        }

        public void addItem(String item) {
            if (chat_lines.size() > 40) {
                chat_lines.remove(0);
                notifyItemRemoved(0);
            }
            chat_lines.add(transfromColors(item));
            notifyItemInserted(chat_lines.size() - 1);
            if (chat.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                chat.scrollToPosition(chat_lines.size() - 1);
            }
        }
    }

    public static Spanned transfromColors(String inputText) {
        Pattern pattern = Pattern.compile("\\{(.{6})\\}([^\\{]*)");
        java.util.regex.Matcher matcher = pattern.matcher(inputText);
        StringBuilder sb = new StringBuilder();
        int currentIndex = 0;
        while (matcher.find()) {
            String colorHex = matcher.group(1);
            String textToColor = matcher.group(2);
            sb.append(inputText, currentIndex, matcher.start());
            sb.append("<font color='#").append(colorHex).append("'>").append(textToColor).append("</font>");
            currentIndex = matcher.end();
        }
        sb.append(inputText.substring(currentIndex));
        return Html.fromHtml(sb.toString().replace("\n", "<br>"));
    }
}
