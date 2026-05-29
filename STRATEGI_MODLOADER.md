# Strategi Implementasi Modloader Aset Eksternal

Dokumen ini menjelaskan langkah-langkah teknis untuk membuat fitur pemuatan aset eksternal (.dff, .txd, .img) dari folder Android secara otomatis di SMART RP.

---

## 1. Arsitektur Modloader
Modloader bekerja dengan cara mencegat (intercepting) permintaan pembacaan file dari mesin game dan mengalihkannya ke lokasi penyimpanan eksternal.

### Komponen Utama yang Dibutuhkan:
- **File Scanner:** Logika Java atau C++ untuk memindai folder `/sdcard/Android/data/launcher.samp.game/files/modloader/`.
- **Redirector Hook:** Hook pada fungsi I/O tingkat rendah (seperti `fopen` atau `NvFOpen`).
- **Streaming Injector:** Hook pada sistem streaming GTA untuk mendaftarkan file `.img` tambahan.

---

## 2. Teknik Hooking yang Diperlukan

### A. Pengalihan Path File
Menggunakan fungsi `NvFOpen` yang sudah ada di `hooks.cpp`. 
**Logika:**
```cpp
stFile* NvFOpen_hook(const char* name, const char* mode, int r2, int r3) {
    char modPath[512];
    sprintf(modPath, "%s/modloader/%s", g_pszStorage, name);
    
    if(FileExists(modPath)) {
        return NvFOpen(modPath, mode, r2, r3); // Muat versi mod
    }
    return NvFOpen(name, mode, r2, r3); // Muat versi original
}
```

### B. Menambahkan File IMG Kustom
Menambahkan baris berikut pada `CStreaming_InitImageList_hook` untuk memuat semua IMG di folder mod:
```cpp
// Pseudocode
for (auto& file : modFolder.getFiles(".img")) {
    CStreaming::AddImageToList(file.path, true);
}
```

### C. Penggantian Tekstur Langsung (TXD)
Melakukan hook pada `CTextureDatabaseRuntime::Load` (Offset `0x1BF244` di libGTASA v2.00). Ini memungkinkan Anda memuat database tekstur tambahan secara dinamis tanpa harus mengganti file `gta3.img`.

---

## 3. Tantangan Teknis & Solusi
1.  **Izin Akses File:** Pada Android 11+, Anda harus menangani *Scoped Storage* atau meminta izin `MANAGE_EXTERNAL_STORAGE` agar JNI bisa membaca file di luar folder internal.
2.  **Memory Management:** Memuat terlalu banyak model HD dari folder eksternal dapat menyebabkan `Out of Memory` (OOM). Diperlukan optimasi pada `CPools` (sudah ada dasarnya di `CPools_Initialise_hook`).
3.  **Versi Library:** Offset (alamat memori) fungsi di `libGTASA.so` berbeda-beda setiap versi. Pastikan menggunakan offset yang sesuai dengan versi 2.00 (versi paling stabil untuk modding).

---

## 4. Rencana Kerja (Roadmap)
1.  **Tahap 1:** Implementasi sistem scan folder di Java dan kirim daftar file ke C++ melalui JNI.
2.  **Tahap 2:** Perbaikan `NvFOpen_hook` untuk mendukung sub-folder mod.
3.  **Tahap 3:** Implementasi auto-inject untuk file `.img` di folder modloader.
4.  **Tahap 4:** Testing stabilitas pemuatan model `.dff` HD untuk mencegah crash saat rendering.
