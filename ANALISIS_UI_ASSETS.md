# Analisis Implementasi UI & Aset di SMART RP

Dokumen ini menjelaskan bagaimana proyek SMART RP mengganti aset bawaan game GTA: San Andreas dengan aset Android (drawable) dan bagaimana sistem "Welcome" diimplementasikan menggunakan Java dan XML.

---

## 1. Penggantian Aset Game dengan Android Drawable

Proyek ini tidak mengganti file aset internal GTA (`.txd` atau `.dff`) secara fisik. Sebagai gantinya, pengembang menggunakan teknik **Android UI Overlay**.

### Mekanisme Kerja:
1.  **Overlay UI:** Aplikasi membuat lapisan (layer) Android di atas permukaan rendering game (SurfaceView). Lapisan ini bersifat transparan sehingga game tetap terlihat di latar belakang.
2.  **Definisi di XML:** Komponen seperti HUD (Health, Armor, Money, Weapon) didefinisikan dalam file layout XML Android standar.
3.  **Pemuatan Aset Dinamis:** 
    Di dalam `HudManager.java`, aset gambar (seperti ikon senjata) dimuat secara dinamis dari folder `res/drawable` berdasarkan ID senjata dari game.
    ```java
    // Contoh pengambilan ID drawable berdasarkan nama file (misal: weapon_31)
    int id = activity.getResources().getIdentifier(
        new Formatter().format("weapon_%d", Integer.valueOf(weaponid)).toString(), 
        "drawable", 
        activity.getPackageName()
    );
    hud_weapon.setImageResource(id);
    ```
4.  **JNI Bridge (C++ ke Java):**
    *   Inti game (C++) memantau statistik pemain (darah, uang, senjata).
    *   C++ memanggil fungsi Java `updateHudInfo` di `NvEventQueueActivity.java` melalui JNI (Java Native Interface).
    *   Fungsi Java tersebut kemudian memperbarui tampilan Android (Progress Bar untuk darah, ImageView untuk senjata).

### Keuntungan:
- Kualitas aset jauh lebih tinggi (High Definition).
- Mendukung transparansi dan animasi Android yang mulus.
- Lebih mudah dimodifikasi tanpa perlu alat pengeditan file `.txd`.

---

## 2. Sistem Welcome (Java + XML)

Sistem Welcome adalah layar sambutan yang muncul saat pemain pertama kali masuk ke server.

### Komponen Utama:
1.  **Layout XML (`brp_welcome.xml`):**
    - Menggunakan `ConstraintLayout` untuk desain yang responsif.
    - Berisi `ImageView` untuk latar belakang, `TextView` untuk judul dan deskripsi, serta `Button` untuk mulai bermain.
    - Menggunakan font khusus (seperti Montserrat Bold) yang didefinisikan di folder `res/font`.

2.  **Logika Java (`Welcome.java`):**
    - **Inisialisasi:** Menghubungkan variabel Java dengan elemen UI di XML menggunakan `findViewById`.
    - **Animasi:** Menggunakan API animasi Android untuk memberikan efek transisi yang menarik.
        ```java
        mTitle.animate().setDuration(1500).translationXBy(2000.0f).start();
        ```
    - **Kondisi:** Teks berubah secara dinamis tergantung apakah pemain adalah pengguna baru (Register) atau pemain lama (Login).

3.  **Pemicu (Trigger):**
    - Ketika status koneksi di C++ (RakNet) berubah menjadi `CONNECTED` atau saat pemain masuk ke tahap pemilihan karakter, kode C++ memanggil `showWelcome(true/false)` melalui JNI ke `NvEventQueueActivity`.

---

## 3. Struktur File Terkait

| Komponen | Lokasi File |
| :--- | :--- |
| **HUD Logic** | `app/src/main/java/launcher/samp/game/gui/HudManager.java` |
| **Welcome Logic** | `app/src/main/java/launcher/samp/game/gui/Welcome.java` |
| **Welcome UI** | `app/src/main/res/layout/brp_welcome.xml` |
| **Bridge C++/Java** | `app/src/main/java/com/nvidia/devtech/NvEventQueueActivity.java` |
| **JNI Entry** | `app/src/main/jni/main.cpp` |

---

## Kesimpulan
Teknik ini menggabungkan kekuatan performa C++ untuk logika game dan kemudahan desain UI Android (Java/XML). Dengan menggunakan sistem **Overlay**, pengembang dapat memberikan tampilan modern dan fitur-fitur baru (seperti voice chat icon) yang tidak didukung oleh mesin game asli GTA: San Andreas yang sudah tua.
