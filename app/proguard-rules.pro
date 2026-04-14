# Regras para evitar crash na versão de produção (Release)

# Media3 / ExoPlayer
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# --- CORREÇÃO PARA O CRASH DAS FOTOS (WorkManager) ---
-keep class androidx.work.** { *; }
-keep class androidx.startup.** { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.work.**
-dontwarn androidx.startup.**

# Guava (usado pelo Media3)
-dontwarn com.google.common.**
-keep class com.google.common.** { *; }

# Suas classes do projeto
-keep class br.com.britoinformatica.www.riopardofm.** { *; }
-keep class br.com.britoinformatica.www.radioriopardomg.** { *; }
