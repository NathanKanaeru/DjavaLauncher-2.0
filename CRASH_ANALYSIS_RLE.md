# Analisis Mendalam Crash RLE Decompress & Kompatibilitas Android

## 1. Ringkasan Masalah
Aplikasi mengalami **Force Close (SIGSEGV)** pada layar loading (splash screen) di Android 14 ke atas, sementara berjalan normal di Android 13. Masalah ini berakar pada fungsi dekompresi tekstur bawaan game (`RLEDecompress`) yang tidak memiliki proteksi batas memori sumber.

## 2. Detail Teknis Crash
*   **Fungsi**: `libGTASA.so!_Z13RLEDecompressPhjPKhjj`
*   **Alamat (64-bit)**: Dasar `0x283030`, Crash terjadi di `0x2830c8`.
*   **Penyebab Segfault**: Fungsi membaca melampaui buffer data sumber (compressed data). Saat pointer baca menabrak batas halaman memori (page boundary) yang tidak dipetakan, kernel Android mengirimkan sinyal SIGSEGV.

## 3. Mengapa Android 14+ Lebih Sering Crash?
Perbedaan perilaku antara Android 13 dan versi terbaru (14/15/dst) kemungkinan besar disebabkan oleh:

### A. Penguatan Security (MTE & ASLR)
*   **Memory Tagging Extension (MTE)**: Android 14+ mulai mengadopsi MTE secara lebih luas pada perangkat hardware baru (seperti Pixel 8). MTE akan langsung mematikan aplikasi jika terdeteksi akses memori ilegal (buffer overflow/over-read) sekecil apa pun.
*   **Hardened Allocator**: Alokator memori (Scudo) pada Android terbaru lebih ketat. Jarak antar alokasi memori (guard pages) lebih sering ditempatkan, sehingga kemungkinan fungsi "berlari" ke memori ilegal menjadi jauh lebih tinggi.

### B. Scoped Storage & I/O Latency
*   Data game berada di `/Android/data/com.nathan.djavarp/files`.
*   Di Android 14+, akses ke direktori ini melalui layer **FUSE** atau **MediaProvider** semakin diperketat. Jika sistem I/O mengembalikan data dalam chunk yang tidak sinkron atau sedikit tertunda, fungsi `OS_FileRead` yang asli mungkin tidak mengisi buffer secara instan seperti yang diharapkan oleh logika `RLEDecompress` yang rapuh, memicu pembacaan data sampah di memori.

## 4. Kesalahan Implementasi Fix Sebelumnya
Percobaan perbaikan pada komit `5880de5` dan `8b306ad` gagal karena:
1.  **Global Variable Overwrite**: Variabel `dwRLEDecompressSourceSize` diisi di setiap panggilan `OS_FileRead`. Padahal, fungsi tersebut dipanggil berkali-kali (sekali untuk ukuran, sekali untuk data). Nilai ukuran tertimpa oleh data sampah.
2.  **Logic Race**: Tidak ada verifikasi bahwa data benar-benar dibaca dari `LoadFullTexture`.

## 5. Rekomendasi Solusi Permanen
Untuk memperbaiki crash ini secara total di semua versi Android, hook harus diaktifkan kembali dengan logika yang diperbaiki:

```cpp
// Perbaikan pada OS_FileRead_hook
int OS_FileRead_hook(void* a1, void* a2, int a3) {
    int ret = orig_OS_FileRead(a1, a2, a3);
    uintptr_t caller = (uintptr_t)__builtin_return_address(0) - g_libGTASA;

    // Hanya ambil ukuran jika dipanggil oleh LoadFullTexture DAN ukuran data adalah 4 byte
    if ((caller == 0x2858B8 || caller == 0x2858C0) && a3 == 4) {
        dwRLEDecompressSourceSize = *(uint32_t*)a2;
    }
    return ret;
}
```

## 6. Lokasi Data Game
Lokasi `/Android/data/com.nathan.djavarp/files` adalah lokasi standar yang aman untuk Android 13, namun untuk Android 14+, pastikan aplikasi memiliki izin `MANAGE_EXTERNAL_STORAGE` jika ingin mengakses data di luar folder sandbox miliknya sendiri secara bebas, meskipun untuk folder internal (`Android/data/[package]`) seharusnya tetap bisa diakses oleh aplikasi itu sendiri tanpa masalah permission tambahan.

---
*Laporan ini disimpan untuk referensi tim pengembang DjavaLauncher-2.0.*
