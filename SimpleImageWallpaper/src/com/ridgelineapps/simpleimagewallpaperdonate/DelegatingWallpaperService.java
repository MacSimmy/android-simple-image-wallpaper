 /*
 * Copyright (C) 2012 Android Simple Image Wallpaper (http://code.google.com/p/android-simple-image-wallpaper/)
 * 
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published 
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ridgelineapps.simpleimagewallpaperdonate;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public class DelegatingWallpaperService extends WallpaperService {
    public static final boolean debug = false;
    
    
    @Override
    public Engine onCreateEngine() {
        return new SimpleWallpaperEngine();
    }

    public class SimpleWallpaperEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        boolean postAgain = false;
        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                
                if(hideWhenScreenIsLocked || differentImageWhenScreenIsLocked) {
                    KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                    screenLocked = keyguardManager.inKeyguardRestrictedInputMode();
                    if(debug)
                        System.out.println("Simple Image Wallpaper: locked == " + screenLocked);
                }
                
                SurfaceHolder holder = getSurfaceHolder();
                Canvas canvas = null;
                try {
                    //TODO: post to black screen while images are being loaded to avoid showing old wallpaper for a split second
                    wallpaper.loadPrefs(false);
                    wallpaper.loadImages();
                    if (visible) {
                        canvas = holder.lockCanvas();
                        if (canvas != null) {
                            wallpaper.draw(canvas);
                        }
                    }
                } finally {
                    if (canvas != null) {
                        holder.unlockCanvasAndPost(canvas);
                    }
                }
                handler.removeCallbacks(drawRunner);
                
            	if(postAgain) {
            	    postAgain = false;
                    handler.postDelayed(drawRunner, retryDelay * 100L);
                    retryDelay *= 2;
            	}
            	else {
            		retryDelay = 1;
            	}
            }
        };
        
        int retryDelay = 1;

        boolean hideWhenScreenIsLocked = false;
        boolean differentImageWhenScreenIsLocked = false;
        boolean screenLocked = false;
        
        int width;
        int height;
        
        ImageFileWallpaper wallpaper;
        private boolean visible = false;
        
        BroadcastReceiver mReceiver;       
        

        public Paint background;

        public SimpleWallpaperEngine() {
            wallpaper = new ImageFileWallpaper(DelegatingWallpaperService.this, this);
            background = Utils.createPaint(0, 0, 0);
            getPrefs().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            
            IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            mReceiver = new ScreenReceiver();
            registerReceiver(mReceiver, filter);        
        }

        public Context getBaseContext() {
            return DelegatingWallpaperService.this.getBaseContext();
        }
        
        public synchronized void onSharedPreferenceChanged(SharedPreferences shared, String key) {
        	wallpaper.loadPrefs(true);
        }

        public SharedPreferences getPrefs() {
            return PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if(debug)
                System.out.println("Simple Image Wallpaper: onVisibilityChanged():" + visible);
            
//            if(((PowerManager) getSystemService(Context.POWER_SERVICE)).isScreenOn()) {
//                if(debug)
//                    System.out.println("Simple Image Wallpaper: screenOn == true");                
//            }
            
            super.onVisibilityChanged(visible);
            this.visible = visible;
            removeAndPost();
//            try {
//                if(!visible) {
//                    SurfaceHolder holder = getSurfaceHolder();
//                    Canvas canvas = null;
//                    try {
//                        canvas = holder.lockCanvas();
//                        if (canvas != null) {
//                            wallpaper.drawBlack(canvas);
//                        }
//                    } finally {
//                        if (canvas != null) {
//                            holder.unlockCanvasAndPost(canvas);
//                        }
//                    }
//                }
//            }
//            catch(Throwable t) {
//                t.printStackTrace();
//            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            if(debug)
                System.out.println("Simple Image Wallpaper: onSurfaceDestroyed()");
            super.onSurfaceDestroyed(holder);
            this.visible = false;
           	handler.removeCallbacks(drawRunner);
        }
        
        @Override
		public void onDestroy() {
            if(debug)
                System.out.println("Simple Image Wallpaper: onDestroy()");
            try {
               if(mReceiver != null) {
                  unregisterReceiver(mReceiver);
               }
            }
            catch(Exception e) {
               e.printStackTrace();
            }
            
            visible = false;
           	wallpaper.cleanup();
           	handler.removeCallbacks(drawRunner);
            if(getPrefs() != null)
                getPrefs().unregisterOnSharedPreferenceChangeListener(this);
            super.onDestroy();
		}

        @Override
        public void onDesiredSizeChanged(int desiredWidth, int desiredHeight) {
            super.onDesiredSizeChanged(desiredWidth, desiredHeight);
            if(debug)
                System.out.println("Simple Image Wallpaper: onDesiredSizeChanged, width:" + desiredWidth + " height:" + desiredHeight);
        }
        
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            if(debug)
                System.out.println("Simple Image Wallpaper: onSurfaceChanged, width:" + width + " height:" + height);

//            if (width > 0 && height > 0 && width != this.width || height != this.height) {
                this.width = width;
                this.height = height;
                removeAndPost();
//            }
        }

        public void removeAndPost() {
            handler.removeCallbacks(drawRunner);
            if (visible) {
                handler.post(drawRunner);
            }
        }
        
        public class ScreenReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    if(debug)
                        System.out.println("Simple Image Wallpaper: screen off");
                    removeAndPost();
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    if(debug)
                        System.out.println("Simple Image Wallpaper: screen on");
                    removeAndPost();
                } else 
                if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                    if(debug)
                        System.out.println("Simple Image Wallpaper: user present");
                    removeAndPost();
                }
            }
        }
        
    }
}
