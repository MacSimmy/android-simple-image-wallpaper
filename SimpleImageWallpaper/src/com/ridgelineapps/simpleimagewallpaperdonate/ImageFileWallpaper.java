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

import java.lang.reflect.Method;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class ImageFileWallpaper {
    public static final boolean debug = false;
    public static final boolean pro = true;

    WallpaperService service;
    DelegatingWallpaperService.SimpleWallpaperEngine engine;
    
    String currentFileUri = "";
    Bitmap image;

    Bitmap imagePortrait;
    String currentPortraitFileUri = "";
    public boolean portraitDifferent;

    String ls_currentFileUri = "";
    Bitmap ls_image;

    Bitmap ls_imagePortrait;
    String ls_currentPortraitFileUri = "";
    public boolean ls_portraitDifferent;
    
    public boolean fillPortrait = false;
    public boolean fillLandscape = false;
    public boolean rotate = false;
    
    public Paint imageBrightnessPaint;
    public int[] imageBrighnessColors = new int[] {
        Color.argb(200, 0, 0, 0),
        Color.argb(160, 0, 0, 0),
        Color.argb(120, 0, 0, 0),
        Color.argb(80, 0, 0, 0),
        Color.argb(40, 0, 0, 0),
        Color.argb(0, 0, 0, 0),
        Color.argb(40, 255, 255, 255),
        Color.argb(80, 255, 255, 255),
        Color.argb(120, 255, 255, 255),
        Color.argb(160, 255, 255, 255),
        Color.argb(200, 255, 255, 255),
    };
    
    /*
    public int darkenIndex = 0;
    public int lightenIndex = 0;
    
    Paint lightenDarkenPaint;
    
    public int[] darkenColors = new int[] {
        Color.argb(0, 0, 0, 0),
        Color.argb(25, 0, 0, 0),
        Color.argb(50, 0, 0, 0),
        Color.argb(75, 0, 0, 0),
        Color.argb(100, 0, 0, 0),
        Color.argb(125, 0, 0, 0),
        Color.argb(150, 0, 0, 0),
        Color.argb(175, 0, 0, 0),
        Color.argb(200, 0, 0, 0),
        Color.argb(225, 0, 0, 0),
    };
    
    public int[] lightenColors = new int[] {
        Color.argb(0, 255, 255, 255),
        Color.argb(25, 255, 255, 255),
        Color.argb(50, 255, 255, 255),
        Color.argb(75, 255, 255, 255),
        Color.argb(100, 255, 255, 255),
        Color.argb(125, 255, 255, 255),
        Color.argb(150, 255, 255, 255),
        Color.argb(175, 255, 255, 255),
        Color.argb(200, 255, 255, 255),
        Color.argb(225, 255, 255, 255),
    };
    */
    
    boolean compensateForBar = false;
    
    boolean prefsLoaded = false;
    
    Paint bitmapPaint;
    
    boolean orientationSet = false;
    int currentOrientation;
    int lastOrientation;

    Bitmap.Config config = null;
    float quality = 1.0f;
    boolean unloadImages = false;
    Integer density = null;
    
    public ImageFileWallpaper(WallpaperService service, DelegatingWallpaperService.SimpleWallpaperEngine engine) {
    	this.service = service;
    	this.engine = engine;
        bitmapPaint = new Paint();
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setDither(true);
        
        try {
           DisplayMetrics metrics = service.getBaseContext().getResources().getDisplayMetrics();
           density = metrics.densityDpi;
        }
        catch(Throwable e) {
           e.printStackTrace();
        }
    }
    
    public void loadPrefs(boolean force) {
        if(force || !prefsLoaded) {
            int imageBrightnessIndex = -1;
            
            SharedPreferences prefs = engine.getPrefs();
            fillLandscape = prefs.getBoolean("image_file_fill_screen", true);
            rotate = prefs.getBoolean("image_file_rotate", false);
            
            engine.hideWhenScreenIsLocked = prefs.getBoolean("image_file_hide_if_locked", false);
            engine.differentImageWhenScreenIsLocked = prefs.getBoolean("image_file_image_if_locked", false);
            
    //        int qualityPref = 10;
    //        try {
    //           String qualityStr = prefs.getString("quality", "10");
    //           if(qualityStr != null) {
    //              qualityPref = Integer.parseInt(qualityStr);
    //           }
    //         } catch (NumberFormatException e) {
    //            e.printStackTrace();
    //        }        
    //        quality = 0.1f * qualityPref;
            
            if(pro) {
                String perfStr = prefs.getString("performance", null);
                if(perfStr != null && perfStr.equals("memory")) {
                    unloadImages = true;
                }
                else {
                    unloadImages = false;
                }
                
               String configStr = prefs.getString("config", null);
               if(configStr == null) {
                  config = null;
               }
               else if(configStr.equals("ARGB_8888")) {
                  config = Bitmap.Config.ARGB_8888;
               }
               else if(configStr.equals("RGB_565")) {
                  config = Bitmap.Config.RGB_565;
               }
               else if(configStr.equals("ARGB_4444")) {
                  config = Bitmap.Config.ARGB_4444;
               }
               else {
                  config = null;
               }
      
               String imageBrightnessStr = prefs.getString("image_brightness", "-1");
               imageBrightnessIndex = Integer.parseInt(imageBrightnessStr);
               /*
               String darkenStr = prefs.getString("darken_image", "0");
               if(!prefs.getBoolean("darken_image_cb", false)) {
                   darkenStr = "0";
               }
               
               darkenIndex = Integer.parseInt(darkenStr) / 10;

               String lightenStr = prefs.getString("lighten_image", "0");
               if(!prefs.getBoolean("lighten_image_cb", false)) {
                   lightenStr = "0";
               }
               
               lightenIndex = Integer.parseInt(lightenStr) / 10;
               */
            }
            
            currentFileUri = prefs.getString("full_image_uri", "");
            portraitDifferent = prefs.getBoolean("portrait_image_set", false);
            currentPortraitFileUri = prefs.getString("portrait_full_image_uri", "");
      
            // Temporarily removed until pref UI can be worked out
    //        if(pro && portraitDifferent) {
    //           fillPortrait = prefs.getBoolean("image_file_fill_screen_portrait", true);
    //        }
    //        else {
               fillPortrait = fillLandscape;
    //        }
            ls_currentFileUri = prefs.getString("ls_full_image_uri", "");
            ls_portraitDifferent = prefs.getBoolean("ls_portrait_image_set", false);
            ls_currentPortraitFileUri = prefs.getString("ls_portrait_full_image_uri", "");
            
            compensateForBar = prefs.getBoolean("compensate", false);
            
            prefsLoaded = true;
            
            if(imageBrightnessIndex != -1) {
                int color = imageBrighnessColors[imageBrightnessIndex];
                imageBrightnessPaint = new Paint();
                imageBrightnessPaint.setFilterBitmap(true);
                imageBrightnessPaint.setDither(true);
                imageBrightnessPaint.setARGB(Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color));
            }
            else {
                imageBrightnessPaint = null;
            }
            /*
            if(darkenIndex > 0) {
                color = darkenColors[darkenIndex];
            }
            else if(lightenIndex > 0) {
                color = lightenColors[lightenIndex];
            }
            
            if(color != 0) {
                lightenDarkenPaint = new Paint();
                lightenDarkenPaint.setFilterBitmap(true);
                lightenDarkenPaint.setDither(true);
                lightenDarkenPaint.setARGB(Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color));
            }
            else {
                lightenDarkenPaint = null;
            }
            */
            
            recycleAllImages(null);
        }
    }
    
    public void loadImages() {
        if(!unloadImages) {
            loadImage();
            loadPortraitImage();
            ls_loadImage();
            ls_loadPortraitImage();
        }
    }
    
    public void draw(Canvas canvas) {
        if(debug)
            System.out.println("Simple Image Wallpaper draw, width:" + canvas.getWidth() + " height:" + canvas.getHeight());
        
        if(canvas.getWidth() != engine.width || canvas.getHeight() != engine.height) {
            engine.postAgain = true;
            return;
        }
        
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        
        if(compensateForBar) {
            //TODO: cache
            int fullScreenWidth = -1;
            int fullScreenHeight = -1;
            try {
                WindowManager wm = (WindowManager) service.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();   
                
                try {
                    Method mGetRawH = display.getClass().getMethod("getRawHeight");
                    Method mGetRawW = display.getClass().getMethod("getRawWidth");
                    if(mGetRawH != null && mGetRawW != null) {
                        fullScreenWidth = (Integer) mGetRawW.invoke(display);
                        fullScreenHeight = (Integer) mGetRawH.invoke(display);
                    }
                }
                catch(Throwable t) {
                    //ignored, handled below
                }
                
                if(fullScreenWidth == -1 || fullScreenHeight == -1) {
                    Method mGetRealSize = Display.class.getMethod("getRealSize");
                    Point size = new Point();
                    mGetRealSize.invoke(display, new Object[]{ size });
                    if(size.x > 0 && size.y > 0) {
                        fullScreenWidth = size.x;
                        fullScreenHeight = size.y;
                    }
                }
            }
            catch(Throwable t) {
                //t.printStackTrace();
            }
            
            if(fullScreenWidth <= 0 || fullScreenHeight <= 0) {
                fullScreenWidth = 0;
                fullScreenHeight = 0;
            }
            
            if(fullScreenWidth > 0 && fullScreenHeight > 0) {
                width = fullScreenWidth;
                height = fullScreenHeight;
            }
            
            System.out.println("width=" + width + ", height=" + height + ", canvas_width=" + canvas.getWidth() + ", canvas_height=" + canvas.getHeight());
        }
        
        canvas.drawRect(0, 0, width, height, engine.background);


        int orientationNow = ((WindowManager) service.getApplication().getSystemService(Service.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
        if(!orientationSet) {
        	currentOrientation = orientationNow;
        	lastOrientation = orientationNow;
        	orientationSet = true;
        }
        
        if(orientationNow != currentOrientation) {
        	lastOrientation = currentOrientation;
        	currentOrientation = orientationNow;
        }
        
        if(debug)
            System.out.println("locked:" + engine.screenLocked + ", hide:" + engine.hideWhenScreenIsLocked + ", different_image:" + engine.differentImageWhenScreenIsLocked);
        
        if(engine.screenLocked && engine.hideWhenScreenIsLocked) {
            if(unloadImages) {
                recycleAllImages(null);
            }
        }
        else {
            Bitmap bmp;
            
            if(engine.screenLocked && engine.differentImageWhenScreenIsLocked) {
                if (ls_portraitDifferent && width < height) {
                    if(unloadImages) {
                        recycleAllImages(ls_imagePortrait);
                        ls_loadPortraitImage();
                    }
                   bmp = ls_imagePortrait;
                } else {
                    if(unloadImages) {
                        recycleAllImages(ls_image);
                        ls_loadImage();
                    }
                   bmp = ls_image;
                }                
            }
            else {
                if (portraitDifferent && width < height) {
                    if(unloadImages) {
                        recycleAllImages(imagePortrait);
                        loadPortraitImage();
                    }
                   bmp = imagePortrait;
                } else {
                    if(unloadImages) {
                        recycleAllImages(image);
                        loadImage();
                    }
                   bmp = image;
                }
            }
            
            if (bmp != null) {
                // canvas.drawBitmap(bmp, 0, 0, engine.background);
                float scaleWidth = (float) width / bmp.getWidth();
                float scaleHeight = (float) height / bmp.getHeight();
    
                float scale;
                int orientationType = service.getBaseContext().getResources().getConfiguration().orientation;
                
                if ((orientationType == Configuration.ORIENTATION_PORTRAIT && fillPortrait) ||
                    (orientationType == Configuration.ORIENTATION_LANDSCAPE && fillLandscape)) {
                    scale = Math.max(scaleWidth, scaleHeight);
                } else {
                    scale = Math.min(scaleWidth, scaleHeight);
                }
    
                int destWidth = (int) (bmp.getWidth() * scale);
                int destHeight = (int) (bmp.getHeight() * scale);
    
                int x = 0;
                int y = 0;
    
                x = (width - destWidth) / 2;
                y = (height - destHeight) / 2;
    
                Rect dest = new Rect(x, y, x + destWidth, y + destHeight);
    
                boolean rotated = false;
                if(rotate) {
                    if((width < height && destWidth > destHeight) || (width > height && destHeight > destWidth)) {
                        rotated = true;
                        int rWidth = height;
                        int rHeight = width;
    
                        scaleWidth = (float) rWidth / bmp.getWidth();
                        scaleHeight = (float) rHeight / bmp.getHeight();
            
                        if ((orientationType == Configuration.ORIENTATION_PORTRAIT && fillPortrait) ||
                            (orientationType == Configuration.ORIENTATION_LANDSCAPE && fillLandscape)) {
                            scale = Math.max(scaleWidth, scaleHeight);
                        } else {
                            scale = Math.min(scaleWidth, scaleHeight);
                        }
             
                        destWidth = (int) (bmp.getWidth() * scale);
                        destHeight = (int) (bmp.getHeight() * scale);
            
                        if((lastOrientation == Surface.ROTATION_0 && currentOrientation == Surface.ROTATION_90) ||
                           (lastOrientation == Surface.ROTATION_180 && currentOrientation == Surface.ROTATION_270) ||
                           (lastOrientation == Surface.ROTATION_90 && currentOrientation == Surface.ROTATION_180) ||
                           (lastOrientation == Surface.ROTATION_270 && currentOrientation == Surface.ROTATION_0)
                           ) {
                            canvas.rotate(270);
    
                            y = (rHeight - destHeight) / 2;
                            x = -rWidth + ((rWidth - destWidth) / 2);
                            dest = new Rect(x, y, x + destWidth, y + destHeight);
                            canvas.drawBitmap(bmp, null, dest, bitmapPaint);
                            
                            canvas.rotate(-270);
                        }
                        else {
                            canvas.rotate(90);
    
                            y = -rHeight + ((rHeight - destHeight) / 2);
                            x = (rWidth - destWidth) / 2;
                            dest = new Rect(x, y, x + destWidth, y + destHeight);
                            canvas.drawBitmap(bmp, null, dest, bitmapPaint);
                            
                            canvas.rotate(-90);
                        }
                    }
                }
                
                if(!rotated) {
                    canvas.drawBitmap(bmp, null, dest, bitmapPaint);
                }

//                if(lightenDarkenPaint != null) {
//                    canvas.drawRect(0, 0, width, height, lightenDarkenPaint);
//                }
                if (imageBrightnessPaint != null) {
                    canvas.drawRect(0, 0, width, height, imageBrightnessPaint);
                }
            }
        } 
    }
    
    public void drawBlack(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        canvas.drawRect(0, 0, width, height, engine.background);
    }
    
   public void loadImage() {
      if(currentFileUri.trim().equals("") || image != null) {
          return;
      }
     try {
       image = Utils.loadBitmap(engine.getBaseContext(), Uri.parse(currentFileUri), engine.width, engine.height, rotate, density, quality, config);
     } catch (Throwable e) {
        engine.postAgain = true;
        Log.e("ImageFileWallpaper", "Exception during loadImage", e);
     }
   }

   public void loadPortraitImage() {
      if (currentPortraitFileUri.trim().equals("") || imagePortrait != null) {
          return;
      }
     try {
       imagePortrait = Utils.loadBitmap(engine.getBaseContext(), Uri.parse(currentPortraitFileUri), engine.width, engine.height, rotate, density, quality, config);
     } catch (Throwable e) {
        engine.postAgain = true;
        Log.e("ImageFileWallpaper", "Exception during loadPortraitImage", e);
     }
   }

   public void ls_loadImage() {
       if (ls_currentFileUri.trim().equals("") || ls_image != null) {
           return;
       }
      try {
         ls_image = Utils.loadBitmap(engine.getBaseContext(), Uri.parse(ls_currentFileUri), engine.width, engine.height, rotate, density, quality, config);
      } catch (Throwable e) {
         engine.postAgain = true;
         Log.e("ImageFileWallpaper", "Exception during ls_loadImage", e);
      }
    }

    public void ls_loadPortraitImage() {
       if (ls_currentPortraitFileUri.trim().equals("") || ls_imagePortrait != null) {
           return;
       }
      try {
         ls_imagePortrait = Utils.loadBitmap(engine.getBaseContext(), Uri.parse(ls_currentPortraitFileUri), engine.width, engine.height, rotate, density, quality, config);
      } catch (Throwable e) {
         engine.postAgain = true;
         Log.e("ImageFileWallpaper", "Exception during ls_loadPortraitImage", e);
      }
    }
    
    public void cleanup() {
        ls_currentFileUri = "";
        ls_currentPortraitFileUri = "";
        currentPortraitFileUri = "";
        currentFileUri = "";
        prefsLoaded = false;
        orientationSet = false;
        engine = null;
        service = null;
        recycleAllImages(null);
    }
    
    public void recycleAllImages(Bitmap keep) {
        if(keep != image) {
            Utils.recycleBitmap(image);
            image = null;
        }
        if(keep != imagePortrait) {
            Utils.recycleBitmap(imagePortrait);
            imagePortrait = null;
        }
        if(keep != ls_image) {
            Utils.recycleBitmap(ls_image);
            ls_image = null;
        }
        if(keep != ls_imagePortrait) {
            Utils.recycleBitmap(ls_imagePortrait);
            ls_imagePortrait = null;
        }
        System.gc();
    }
}
