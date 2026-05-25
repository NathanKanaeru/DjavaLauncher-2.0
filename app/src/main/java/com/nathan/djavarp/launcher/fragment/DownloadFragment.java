package com.nathan.djavarp.launcher.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nathan.djavarp.R;
import com.nathan.djavarp.launcher.other.DownloadAdapter;
import com.nathan.djavarp.launcher.other.DownloadModel;
import com.nathan.djavarp.launcher.service.DownloadService;
import com.nathan.djavarp.launcher.util.DownloadStore;
import com.nathan.djavarp.launcher.util.GPUUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadFragment extends Fragment {

    private TextView tvGpuModel, tvGpuInfo;
    private ImageView gpuIcon;
    private RecyclerView rvDownloads;
    private DownloadAdapter adapter;
    private List<DownloadModel> downloadItems;
    private DownloadStore downloadStore;

    private GPUUtil.GPU_TYPE gpuType;
    private String renderer;

    private static class DownloadSource {
        final String name;
        final String url;
        final int iconRes;
        final String tag;

        DownloadSource(String name, String url, int iconRes, String tag) {
            this.name = name;
            this.url = url;
            this.iconRes = iconRes;
            this.tag = tag;
        }
    }

    private DownloadModel pendingDownloadItem;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (pendingDownloadItem != null) {
                    showSourceDialog(pendingDownloadItem);
                    pendingDownloadItem = null;
                }
            });

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String fileName = intent.getStringExtra("fileName");
            
            DownloadModel item = findItem(fileName);
            if (item == null) return;
            int index = downloadItems.indexOf(item);

            if (DownloadService.ACTION_PROGRESS_UPDATE.equals(action)) {
                item.isDownloading = true;
                item.status = "Downloading...";
                item.percent = intent.getIntExtra("percent", 0);
                long current = intent.getLongExtra("current", 0);
                long total = intent.getLongExtra("total", 0);
                item.size = String.format(Locale.getDefault(), "%.1f/%.1f MB", 
                        current / 1024f / 1024f, total / 1024f / 1024f);
                item.speed = intent.getStringExtra("speed");
                item.eta = intent.getStringExtra("eta");
                adapter.notifyItemChanged(index, "PROGRESS");
            } else if (DownloadService.ACTION_EXTRACTING.equals(action)) {
                item.isDownloading = false;
                item.isExtracting = true;
                item.status = "Extracting...";
                item.percent = 0;
                adapter.notifyItemChanged(index);
            } else if (DownloadService.ACTION_EXTRACT_PROGRESS.equals(action)) {
                item.percent = intent.getIntExtra("percent", 0);
                item.status = "Extracting: " + intent.getStringExtra("currentFile");
                adapter.notifyItemChanged(index, "PROGRESS");
            } else if (DownloadService.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                item.isDownloading = false;
                item.isExtracting = false;
                item.status = "Finished";
                adapter.notifyItemChanged(index);
            } else if (DownloadService.ACTION_DOWNLOAD_ERROR.equals(action)) {
                item.isDownloading = false;
                item.isExtracting = false;
                item.status = "Error: " + intent.getStringExtra("message");
                adapter.notifyItemChanged(index);
            }
        }
    };

    private DownloadModel findItem(String fileName) {
        if (fileName == null) return null;
        for (DownloadModel item : downloadItems) {
            if (fileName.equals(item.fileName)) return item;
        }
        return null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_download, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvGpuModel = view.findViewById(R.id.tv_gpu_model);
        tvGpuInfo = view.findViewById(R.id.tv_gpu_info);
        gpuIcon = view.findViewById(R.id.gpu_icon);
        rvDownloads = view.findViewById(R.id.rv_downloads);

        downloadStore = new DownloadStore(requireContext());
        detectGPU();
        setupList();
    }

    private void setupList() {
        downloadItems = new ArrayList<>();
        downloadItems.add(new DownloadModel("Game Data",
                "https://github.com/Nathan-Studios/DjavaLauncher/releases/download/datagame/SAMP.zip", "SAMP.zip"));

        adapter = new DownloadAdapter(downloadItems, downloadStore, item -> {
            DownloadStore.Status status = downloadStore.getStatus(item.fileName);
            if (status == DownloadStore.Status.EXTRACTED) return;

            Context ctx = getContext();
            if (ctx == null) return;
            File zipFile = new File(ctx.getExternalFilesDir(null), item.fileName);
            if (status == DownloadStore.Status.DOWNLOADED && zipFile.exists()) {
                startService(item.url, item.fileName, item.title, true);
                item.isExtracting = true;
                item.status = "Extracting...";
                adapter.notifyDataSetChanged();
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    pendingDownloadItem = item;
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return;
                }
            }

            showSourceDialog(item);
        });

        rvDownloads.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDownloads.setAdapter(adapter);
    }

    private void startService(String url, String fileName, String label, boolean skipDownload) {
        Context ctx = getContext();
        if (ctx == null) return;
        Intent intent = new Intent(ctx, DownloadService.class);
        intent.setAction(DownloadService.ACTION_START_DOWNLOAD);
        intent.putExtra("url", url);
        intent.putExtra("file_name", fileName);
        intent.putExtra("label", label);
        intent.putExtra("skip_download", skipDownload);
        ctx.startService(intent);
    }

    private void showSourceDialog(DownloadModel item) {
        Context ctx = getContext();
        if (ctx == null || !isAdded()) return;

        List<DownloadSource> sources = new ArrayList<>();
        sources.add(new DownloadSource("GitHub Release",
                "https://github.com/Nathan-Studios/DjavaLauncher/releases/download/datagame/SAMP.zip",
                R.drawable.ic_source_github, "Recommended"));
        sources.add(new DownloadSource("Pixeldrain",
                "https://pixeldrain.com/api/file/AokkNatH",
                R.drawable.ic_source_pixeldrain, "Cepat"));
        sources.add(new DownloadSource("Dropbox",
                "https://www.dropbox.com/scl/fi/mone7qpnna27fey475ugo/SAMP.zip?rlkey=ewhtt87jl7vl9dal2an1eh1se&st=n8twh3rc&dl=1",
                R.drawable.ic_source_dropbox, "Cepat"));

        View dialogView = LayoutInflater.from(ctx)
                .inflate(R.layout.dialog_download_source, null);

        com.google.android.material.card.MaterialCardView cardGithub =
                dialogView.findViewById(R.id.card_github);
        com.google.android.material.card.MaterialCardView cardPixeldrain =
                dialogView.findViewById(R.id.card_pixeldrain);
        com.google.android.material.card.MaterialCardView cardDropbox =
                dialogView.findViewById(R.id.card_dropbox);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        cardGithub.setOnClickListener(v -> {
            dialog.dismiss();
            if (!isAdded()) return;
            startService(sources.get(0).url, item.fileName, item.title, false);
            item.isDownloading = true;
            item.status = "Starting...";
            if (adapter != null) adapter.notifyDataSetChanged();
        });
        cardPixeldrain.setOnClickListener(v -> {
            dialog.dismiss();
            if (!isAdded()) return;
            startService(sources.get(1).url, item.fileName, item.title, false);
            item.isDownloading = true;
            item.status = "Starting...";
            if (adapter != null) adapter.notifyDataSetChanged();
        });
        cardDropbox.setOnClickListener(v -> {
            dialog.dismiss();
            if (!isAdded()) return;
            startService(sources.get(2).url, item.fileName, item.title, false);
            item.isDownloading = true;
            item.status = "Starting...";
            if (adapter != null) adapter.notifyDataSetChanged();
        });

        dialog.show();
    }

    private void detectGPU() {
        renderer = GPUUtil.getRenderer(requireContext());
        gpuType = GPUUtil.getGpuType(renderer);
        tvGpuModel.setText(String.format("GPU: %s", gpuType.name()));
        tvGpuInfo.setText(renderer);
        
        if (gpuType == GPUUtil.GPU_TYPE.ADRENO) gpuIcon.setImageResource(R.drawable.ic_signal_24);
        else if (gpuType == GPUUtil.GPU_TYPE.MALI) gpuIcon.setImageResource(R.drawable.ic_globe_24);
        else gpuIcon.setImageResource(R.drawable.ic_settings_m3);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_PROGRESS_UPDATE);
        filter.addAction(DownloadService.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(DownloadService.ACTION_DOWNLOAD_ERROR);
        filter.addAction(DownloadService.ACTION_EXTRACTING);
        filter.addAction(DownloadService.ACTION_EXTRACT_PROGRESS);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(downloadReceiver, filter);
        
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(downloadReceiver);
    }
}
