package com.nathan.djavarp.game.ui.dialog;

import android.text.TextPaint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.nathan.djavarp.R;
import com.nathan.djavarp.launcher.other.Util;

import java.util.ArrayList;

public class DialogAdapter extends RecyclerView.Adapter {

    public interface OnClickListener {
        void onClick(int i, String str);
    }

    public interface OnDoubleClickListener {
        void onDoubleClick();
    }

    private int mCurrentSelectedPosition = 0;
    private ImageView mCurrentSelectedView;
    private final ArrayList<String> mFieldTexts;
    private final ArrayList<ArrayList<TextView>> mFields;
    private final ArrayList<TextView> mFieldHeaders;
    private OnClickListener mOnClickListener;
    private OnDoubleClickListener mOnDoubleClickListener;

    public DialogAdapter(ArrayList<String> fields, ArrayList<TextView> fieldHeaders) {
        this.mFieldTexts = fields;
        this.mFieldHeaders = fieldHeaders;
        this.mFields = new ArrayList<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.sd_dialog_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        onBindViewHolder((ViewHolder) holder, position);
    }

    public void onBindViewHolder(ViewHolder holder, int position) {
        String[] headers = this.mFieldTexts.get(position).split("\t");
        for (int i = 0; i < headers.length; i++) {
            if (i >= holder.mFields.size()) continue;
            TextView field = holder.mFields.get(i);
            field.setText(Util.getColoredString(headers[i].replace("\\t", "")));
            field.setVisibility(View.VISIBLE);
        }

        if (this.mCurrentSelectedPosition == position) {
            ImageView imageView = holder.mFieldBg;
            this.mCurrentSelectedView = imageView;
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(R.drawable.dialog_item_btn);
            this.mOnClickListener.onClick(position, headers[0]);
        } else {
            holder.mFieldBg.setVisibility(View.VISIBLE);
            holder.mFieldBg.setImageResource(R.drawable.dialog_item_btn_none);
        }

        holder.getView().setOnClickListener(view -> {
            if (this.mCurrentSelectedPosition != holder.getAdapterPosition()) {
                this.mCurrentSelectedView.setImageResource(R.drawable.dialog_item_btn_none);
                this.mCurrentSelectedPosition = holder.getAdapterPosition();
                this.mCurrentSelectedView = holder.mFieldBg;
                holder.mFieldBg.setVisibility(View.VISIBLE);
                holder.mFieldBg.setImageResource(R.drawable.dialog_item_btn);
                this.mOnClickListener.onClick(holder.getAdapterPosition(), this.mFieldTexts.get(holder.getAdapterPosition()).split("\t")[0]);
                return;
            }
            OnDoubleClickListener d = this.mOnDoubleClickListener;
            if (d != null) {
                d.onDoubleClick();
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.mFieldTexts.size();
    }

    public void updateSizes() {
        int[] max = new int[4];
        for (ArrayList<TextView> row : this.mFields) {
            for (int j = 0; j < row.size(); j++) {
                int w = row.get(j).getWidth();
                if (max[j] < w) max[j] = w;
            }
        }
        for (int i = 0; i < this.mFieldHeaders.size(); i++) {
            int hw = measureTextWidth(this.mFieldHeaders.get(i));
            if (max[i] < hw) max[i] = hw;
        }
        for (ArrayList<TextView> row : this.mFields) {
            for (int j = 0; j < row.size(); j++) {
                row.get(j).setWidth(max[j]);
            }
        }
        for (int i = 0; i < this.mFieldHeaders.size(); i++) {
            this.mFieldHeaders.get(i).setWidth(max[i]);
        }
    }

    private int measureTextWidth(TextView view) {
        String text = String.valueOf(view.getText());
        if (TextUtils.isEmpty(text)) return 0;
        return (int) view.getPaint().measureText(text);
    }

    public void setOnClickListener(OnClickListener l) {
        this.mOnClickListener = l;
    }

    public void setOnDoubleClickListener(OnDoubleClickListener l) {
        this.mOnDoubleClickListener = l;
    }

    public int[] mergeTabSizes(int[] external) {
        int[] max = new int[4];
        for (ArrayList<TextView> row : this.mFields) {
            for (int j = 0; j < row.size(); j++) {
                int w = row.get(j).getWidth();
                if (max[j] < w) max[j] = w;
            }
        }
        for (int i = 0; i < 4; i++) {
            max[i] = Math.max(max[i], external[i]);
        }
        return max;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mFieldBg;
        ArrayList<TextView> mFields = new ArrayList<>();
        private final View mView;

        ViewHolder(View itemView) {
            super(itemView);
            this.mView = itemView;
            this.mFieldBg = itemView.findViewById(R.id.sd_dialog_item_bg);
            ConstraintLayout field = itemView.findViewById(R.id.sd_dialog_item_main);
            for (int i = 1; i < field.getChildCount(); i++) {
                this.mFields.add((TextView) field.getChildAt(i));
            }
        }

        View getView() {
            return this.mView;
        }
    }
}
