/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.utils;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import androidx.palette.graphics.Palette;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WallpaperUtils {

    public interface OnColorsExtractedListener {
        void onColorsExtracted(Palette palette);
    }

    private static final ExecutorService sExecutor = Executors.newSingleThreadExecutor();
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    public static void extractWallpaperColors(Context context, OnColorsExtractedListener listener) {
        sExecutor.execute(() -> {
            try {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                Drawable drawable = wallpaperManager.getDrawable();
                if (drawable instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                    Palette palette = Palette.from(bitmap).generate();
                    sHandler.post(() -> listener.onColorsExtracted(palette));
                } else {
                    sHandler.post(() -> listener.onColorsExtracted(null));
                }
            } catch (Exception e) {
                sHandler.post(() -> listener.onColorsExtracted(null));
            }
        });
    }

    private static final int MAX_WALLPAPER_PREVIEW_EDGE = 1440;

    public static Drawable getWall(Context context, boolean lockScreen) {
        WallpaperManager instance = WallpaperManager.getInstance(context);
        ParcelFileDescriptor wallpaperFile = instance.getWallpaperFile(
                lockScreen ? WallpaperManager.FLAG_LOCK : WallpaperManager.FLAG_SYSTEM);
        if (wallpaperFile == null) {
            return instance.getDrawable();
        }
        try {
            Bitmap decoded = BitmapFactory.decodeFileDescriptor(wallpaperFile.getFileDescriptor());
            if (decoded == null) {
                return instance.getDrawable();
            }
            Bitmap preview = scalePreservingAspect(decoded);
            if (preview != decoded) {
                decoded.recycle();
            }
            return new BitmapDrawable(context.getResources(), preview);
        } finally {
            try {
                wallpaperFile.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static Bitmap scalePreservingAspect(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longest = Math.max(width, height);
        if (longest <= MAX_WALLPAPER_PREVIEW_EDGE) {
            return source;
        }
        float scale = MAX_WALLPAPER_PREVIEW_EDGE / (float) longest;
        int scaledWidth = Math.max(1, Math.round(width * scale));
        int scaledHeight = Math.max(1, Math.round(height * scale));
        return Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true);
    }

    public static boolean isLiveWall(Context context) {
        return WallpaperManager.getInstance(context).getWallpaperInfo() != null;
    }
} 
