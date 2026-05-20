# Analisis Sistem Manajemen Server & Proses Koneksi - DjavaLauncher-2.0

Dokumen ini menjelaskan bagaimana launcher ini menangani daftar server, penambahan server kustom, dan mekanisme koneksi menyeluruh ke dalam game.

## 1. Manajemen Server

Launcher menggunakan dua sumber utama untuk daftar server:
- **Hosted Servers:** Diambil dari API eksternal (`https://samp-mobile.shop/hosted.json`).
- **Favorite Servers:** Disimpan secara lokal dalam format JSON.

### Sistem Penyimpanan (Favorites)
Data server favorit dikelola oleh kelas `com.nathan.djavarp.launcher.data.FavoritesInfo`.
- **Format File:** `SAMP/favorites.json` di penyimpanan eksternal aplikasi.
- **Struktur Data:** Menyimpan `id`, `serverid`, `ip`, dan `port`.
- **Proses Load/Save:** Menggunakan `org.json` untuk parsing dan `BufferedWriter` untuk menyimpan data ke file.

## 2. Menambahkan Server Kustom

Proses penambahan server kustom dilakukan melalui dialog `ServerAddFragment`.

### Alur Penambahan:
1. **Input IP/Host:** Pengguna memasukkan alamat server (misal: `127.0.0.1:7777` atau `play.server.com`).
2. **Validasi Regex:** Menggunakan dua pola regex untuk memastikan format IP atau Hostname benar:
    - Regex 1: Mendukung format `IP:PORT`.
    - Regex 2: Mendukung format `IP` saja (port default 7777 akan ditambahkan otomatis).
3. **Pengecekan Duplikasi:** Memanggil `FavoritesInfo.IsServerExists` untuk memastikan server belum ada di daftar.
4. **Penyimpanan:** Data ditambahkan ke `serverList` dan disimpan secara permanen ke `favorites.json`.
5. **UI Refresh:** Memanggil `refreshFavoriteServers()` pada `MainActivity` untuk memperbarui tampilan secara real-time.

## 3. Proses Koneksi Menyeluruh (Connect)

Proses koneksi terjadi saat pengguna menekan tombol **Connect** di dialog informasi server (`ServerInformationFragment`).

### Langkah-langkah Detail:
1. **Validasi File Konfigurasi:** Memanggil `ConfigValidator.validateConfigFiles(activity)` untuk memastikan file dasar game ada.
2. **Update settings.ini:** 
    - Launcher menggunakan library `ini4j` (kelas `Wini`).
    - Alamat IP (`host`) dan `port` server yang dipilih ditulis ke dalam file `SAMP/settings.ini`.
    - Ini adalah langkah krusial karena client game nantinya akan membaca file ini untuk mengetahui ke mana harus terkoneksi.
3. **Pengecekan Aset Penting:**
    - Mengecek keberadaan file `Text/american.gxt` dan `Textures/fonts/RussianFont.png` di dalam assets.
    - Jika file hilang (terutama pada versi modifikasi), aplikasi akan memberikan peringatan.
4. **Inisialisasi Game:**
    - Jika validasi sukses, launcher menjalankan `Intent` untuk memulai `com.nathan.djavarp.game.SAMP`.
    - `activity.finish()` dipanggil untuk menutup launcher saat game dimulai.
5. **Native Handover (Bridge):**
    - Di dalam kelas `SAMP.java`, game memanggil method native `initializeSAMP(path)`.
    - Method native ini (di `main.cpp`) akan menginisialisasi engine GTA SA, melakukan *hooking* menggunakan **Shadowhook**, dan memulai koneksi jaringan menggunakan IP/Port yang tadi sudah ditulis di `settings.ini`.

## 4. Komponen Utama yang Terlibat

| Komponen | Peran |
| :--- | :--- |
| `FavoritesInfo` | Manajer data server lokal (JSON). |
| `ServerAddFragment` | Antarmuka untuk input IP kustom dengan validasi regex. |
| `ServerInformationFragment` | Jembatan antara UI launcher dan inisialisasi game. |
| `Wini (ini4j)` | Menulis parameter koneksi ke file `.ini`. |
| `SAMP.java` | Activity utama yang memuat library native dan menjalankan game. |
| `main.cpp` | Implementasi native (C++) yang menangani koneksi jaringan SAMP yang sebenarnya. |
