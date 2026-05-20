package com.nathan.djavarp.launcher.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
        downloadItems.add(new DownloadModel("Base Game Data", 
                "https://github.com/Nathan-Studios/DjavaLauncher/releases/download/gamedata/base.zip", "base.zip"));
        
        String gpuUrl = "";
        String gpuFile = "";
        switch (gpuType) {
            case ADRENO: gpuUrl = "https://github.com/Nathan-Studios/DjavaLauncher/releases/download/gamedata/adreno.zip"; gpuFile = "adreno.zip"; break;
            case MALI: gpuUrl = "https://github.com/Nathan-Studios/DjavaLauncher/releases/download/gamedata/mali.zip"; gpuFile = "mali.zip"; break;
            default: gpuUrl = "https://github.com/Nathan-Studios/DjavaLauncher/releases/download/gamedata/powervr.zip"; gpuFile = "powervr.zip"; break;
        }
        downloadItems.add(new DownloadModel("GPU Textures (" + gpuType.name() + ")", gpuUrl, gpuFile));

        adapter = new DownloadAdapter(downloadItems, downloadStore, item -> {
            Intent intent = new Intent(requireContext(), DownloadService.class);
            intent.setAction(DownloadService.ACTION_START_DOWNLOAD);
            intent.putExtra("url", item.url);
            intent.putExtra("file_name", item.fileName);
            intent.putExtra("label", item.title);
            requireContext().startService(intent);
            
            item.isDownloading = true;
            item.status = "Starting...";
            adapter.notifyDataSetChanged();
        });

        rvDownloads.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDownloads.setAdapter(adapter);
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
