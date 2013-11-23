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

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

public class Utils {
    public static boolean isKitKat() {
       return true;
    }
   
    public static Bitmap loadBitmap(Context context, Uri imageURI, int width, int height, boolean rotateIfNecessary, Integer density, float quality, Bitmap.Config config) throws FileNotFoundException {
       System.gc();
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        if(density != null) {
           // ?
           o.inScreenDensity = density;
           o.inTargetDensity = density;
        }
        
        if(config != null) {
           o.inPreferredConfig = config;
        }
        
        Bitmap bmp = null;
        if(isKitKat()) {
           ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(imageURI, "r");
           FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
           bmp = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, o);
           try {
              parcelFileDescriptor.close();           
           }
           catch(IOException e) {
              e.printStackTrace();
           }
        }
        else {
           InputStream is = context.getContentResolver().openInputStream(imageURI);
           bmp = BitmapFactory.decodeStream(is, null, o);
           try {
              is.close();
          } catch (Exception e) {
              // TODO: put all in logs
              e.printStackTrace();
          }
        }
        
        int imageWidth = o.outWidth;
        int imageHeight = o.outHeight;
        
        int longSide = Math.max(width, height);
        int imageLongSide = Math.max(o.outWidth, o.outHeight);
        int shortSide = Math.min(width, height);
        int imageShortSide = Math.min(o.outWidth, o.outHeight);
         
        int scale=1;
        // Option 1
//        if (o.outHeight > longSide || o.outWidth > longSide) {
//            scale = (int) Math.pow(2, (int) Math.round(Math.log(longSide / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
//        }
        // Option 2
//        while(true) {
//        	if(rotateIfNecessary) {
//	        	if(imageLongSide / 2 < longSide) {
//	        		break;
//	        	}
//	        	
//	        	if(imageShortSide / 2 < shortSide) {
//	        		break;
//	        	}
//        	}
//        	else {
//	        	if(imageWidth / 2 < width) {
//	        		break;
//	        	}
//	        	
//	        	if(imageHeight / 2 < height) {
//	        		break;
//	        	}
//        	}
//
//        	imageLongSide /= 2;
//        	imageShortSide /= 2;
//        	imageWidth /= 2;
//        	imageHeight /= 2;
//            scale *= 2;
//        }
        // Option 3
        if(rotateIfNecessary) {
          if (imageLongSide > imageShortSide) {
              scale = Math.round((float) imageShortSide / (float) shortSide);
          } else {
              scale = Math.round((float) imageLongSide / (float) longSide);
          }           
        }
        else {
           if (imageWidth > imageHeight) {
              scale = Math.round((float) imageHeight / (float) height);
          } else {
              scale = Math.round((float) imageWidth / (float) width);
          }
        }
        
//        scale *= quality;

        // Decode with inSampleSize
        o = new BitmapFactory.Options();
        o.inSampleSize = scale;
        o.inDither = true;
//        o.inPreferQualityOverSpeed = true;
//        o.inPurgeable = true;
//        o.inInputShareable = false;
        if(density != null) {
           // ?
           o.inScreenDensity = density;
           o.inTargetDensity = density;
        }

        if(config != null) {
           o.inPreferredConfig = config;
        }
        
        int retries = 0;
        boolean success = false;
        
        InputStream is = null;
        
        while(!success) {
            try {
               Utils.recycleBitmap(bmp);
               bmp = null;
               System.gc();
               
               if(isKitKat()) {
                  ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(imageURI, "r");
                  FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                  bmp = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, o);
                  try {
                     parcelFileDescriptor.close();           
                  }
                  catch(IOException e) {
                     e.printStackTrace();
                  }
               }
               else {               
                  // TODO: don't load stream twice?
                   is = context.getContentResolver().openInputStream(imageURI);
                   
           //        System.out.println("s:" + scale + " o:" + o.outWidth + ", " + o.outHeight + " **************************** decoding:" + imageURI);
                   
                   bmp = BitmapFactory.decodeStream(is, null, o);
               }
                success = true;
            }
            catch(OutOfMemoryError e) {
                e.printStackTrace();
                if(retries++ >= 15) {
                   throw e;
                }
                scale *= 1.2;
                o = new BitmapFactory.Options();
                o.inSampleSize = scale;
                o.inPurgeable = true;
                o.inInputShareable = false;
            }
            finally
            {
                try {
                   if(is != null) {
                      is.close();
                   }
                } catch (Exception e) {
                    // TODO: put all in logs
                    e.printStackTrace();
                }
            }
        }
        return bmp;
    }

    public static Point getBitmapSize(Context context, Uri imageURI) throws FileNotFoundException {
        System.gc();
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        InputStream is = null;
        
        try
        {
           is = context.getContentResolver().openInputStream(imageURI);
           context.getContentResolver().openInputStream(imageURI);
           Bitmap bitmap = BitmapFactory.decodeStream(is, null, o);
           Point size = new Point(o.outWidth, o.outHeight);
           recycleBitmap(bitmap);
           return size;
         }
         finally
         {
             try {
                 is.close();
             } catch (Exception e) {
                 // TODO: put all in logs
                 e.printStackTrace();
             }
         }
    }
    
    public static void recycleBitmap(Bitmap image) {
       try {
          if (image != null && !image.isRecycled()) {
             image.recycle();
          }
       } catch (Throwable e) {
          e.printStackTrace();
       }       
    }    
    
    public static Paint createPaint(int r, int g, int b) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setARGB(255, r, g, b);
        return paint;
    }
}
