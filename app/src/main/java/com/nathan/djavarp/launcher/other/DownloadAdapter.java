package com.nathan.djavarp.launcher.other;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.nathan.djavarp.R;
import com.nathan.djavarp.launcher.util.DownloadStore;

import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {

    public interface OnDownloadClickListener {
        void onDownloadClick(DownloadModel item);
    }

    private final List<DownloadModel> items;
    private final OnDownloadClickListener listener;
    private final DownloadStore downloadStore;

    public DownloadAdapter(List<DownloadModel> items, DownloadStore downloadStore, OnDownloadClickListener listener) {
        this.items = items;
        this.downloadStore = downloadStore;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        onBindViewHolder(holder, position, java.util.Collections.emptyList());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        DownloadModel item = items.get(position);
        
        if (!payloads.isEmpty()) {
            for (Object payload : payloads) {
                if ("PROGRESS".equals(payload)) {
                    updateProgressViews(holder, item);
                }
            }
            return;
        }

        holder.tvTitle.setText(item.title);
        holder.tvStatus.setText(item.status);
        
        DownloadStore.Status status = downloadStore.getStatus(item.fileName);
        
        if (status == DownloadStore.Status.EXTRACTED) {
            holder.ivDone.setVisibility(View.VISIBLE);
            holder.btnDownload.setVisibility(View.GONE);
            holder.progressContainer.setVisibility(View.GONE);
            holder.tvStatus.setText("Finished");
        } else if (status == DownloadStore.Status.DOWNLOADED && !item.isDownloading && !item.isExtracting) {
            holder.ivDone.setVisibility(View.GONE);
            holder.btnDownload.setVisibility(View.VISIBLE);
            holder.btnDownload.setEnabled(true);
            holder.progressContainer.setVisibility(View.GONE);
            holder.tvStatus.setText("Ready to extract");
        } else {
            holder.ivDone.setVisibility(View.GONE);
            holder.btnDownload.setVisibility(View.VISIBLE);
            
            if (item.isDownloading || item.isExtracting) {
                holder.btnDownload.setEnabled(false);
                holder.progressContainer.setVisibility(View.VISIBLE);
                updateProgressViews(holder, item);
            } else {
                holder.btnDownload.setEnabled(true);
                holder.progressContainer.setVisibility(View.GONE);
            }
        }

        holder.btnDownload.setOnClickListener(v -> listener.onDownloadClick(item));
    }

    private void updateProgressViews(ViewHolder holder, DownloadModel item) {
        holder.progressBar.setProgress(item.percent);
        holder.tvPercent.setText(item.percent + "%");
        holder.tvSize.setText(item.size);
        holder.tvSpeed.setText(item.speed);
        holder.tvEta.setText(item.eta);
        holder.tvStatus.setText(item.status);
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus, tvPercent, tvSize, tvSpeed, tvEta;
        LinearProgressIndicator progressBar;
        MaterialButton btnDownload;
        ImageView ivDone;
        LinearLayout progressContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvPercent = itemView.findViewById(R.id.tv_percent);
            tvSize = itemView.findViewById(R.id.tv_size);
            tvSpeed = itemView.findViewById(R.id.tv_speed);
            tvEta = itemView.findViewById(R.id.tv_eta);
            progressBar = itemView.findViewById(R.id.progress_bar);
            btnDownload = itemView.findViewById(R.id.btn_download);
            ivDone = itemView.findViewById(R.id.iv_done);
            progressContainer = itemView.findViewById(R.id.progress_container);
        }
    }
}
