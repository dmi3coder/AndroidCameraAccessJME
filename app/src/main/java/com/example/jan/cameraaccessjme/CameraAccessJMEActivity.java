package com.example.jan.cameraaccessjme;

import com.jme3.app.AndroidHarness;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;

import java.nio.ByteBuffer;
import java.util.List;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jme3.app.SimpleApplication;
import com.jme3.system.android.AndroidConfigChooser.ConfigType;
import com.jme3.texture.Image;

/**
 * Created by Jan on 30/12/2015.
 */
public class CameraAccessJMEActivity extends AndroidHarness {


    private static final String TAG = "CameraAccessJMEActivity";
    private Camera mCamera;
    private CameraPreview mPreview;
    private int mDesiredCameraPreviewWidth = 640;
    private final int MY_PERMISSION_REQUEST_USE_CAMERA = 200;
    private LocationManager locationManager;
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Toast.makeText(CameraAccessJMEActivity.this, "Location changed", Toast.LENGTH_SHORT).show();
            if (app != null) {
                ((CameraAccessJME) app).setUserLocation(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private byte[] mPreviewBufferRGB565 = null;
    java.nio.ByteBuffer mPreviewByteBufferRGB565;
    // the actual size of the preview images
    int mPreviewWidth;
    int mPreviewHeight;
    // If we have to convert the camera preview image into RGB565 or can use it
    // directly
    private boolean pixelFormatConversionNeeded = true;

    private boolean stopPreview = false;
    Image cameraJMEImageRGB565;

    // Implement the interface for getting copies of preview frames
    private final Camera.PreviewCallback mCameraCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera c) {
            if (c != null && stopPreview == false) {
                mPreviewByteBufferRGB565.clear();
                // Perform processing on the camera preview data.
                if (pixelFormatConversionNeeded) {
                    yCbCrToRGB565(data, mPreviewWidth, mPreviewHeight,
                            mPreviewBufferRGB565);
                    mPreviewByteBufferRGB565.put(mPreviewBufferRGB565);
                } else {
                    mPreviewByteBufferRGB565.put(data);
                }
                cameraJMEImageRGB565.setData(mPreviewByteBufferRGB565);
                if ((CameraAccessJME) app != null) {
                    ((CameraAccessJME) app)
                            .setTexture(cameraJMEImageRGB565);
                }
            }
        }
    };

    // Retrieve an instance of the Camera object.
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            // get a Camera instance
            c = Camera.open(0);
        } catch (Exception e) {
            // Camera is does not exist or is already in use
            Log.e(TAG, "Camera not available or in use.");

        }
        // return NULL if camera is unavailable, otherwise return the Camera
        // instance
        return c;
    }

    // configure camera parameters like preview size
    private void initializeCameraParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        // Get a list of supported preview sizes.
        List<Camera.Size> sizes = mCamera.getParameters()
                .getSupportedPreviewSizes();
        int currentWidth = 0;
        int currentHeight = 0;
        boolean foundDesiredWidth = false;
        for (Camera.Size s : sizes) {
            if (s.width == mDesiredCameraPreviewWidth) {
                currentWidth = s.width;
                currentHeight = s.height;
                foundDesiredWidth = true;
                break;
            }
        }
        if (foundDesiredWidth) {
            parameters.setPreviewSize(currentWidth, currentHeight);
        }
        // we also want to use RGB565 directly
        List<Integer> pixelFormats = parameters.getSupportedPreviewFormats();
        for (Integer format : pixelFormats) {
            if (format == ImageFormat.RGB_565) {
                Log.d(TAG, "Camera supports RGB_565");
                pixelFormatConversionNeeded = false;
                parameters.setPreviewFormat(format);
                break;
            }
        }
        if (pixelFormatConversionNeeded) {
            Log.e(TAG,
                    "Camera does not support RGB565 directly. Need conversion");
        }
        mCamera.setParameters(parameters);

        // register our callback function to get access to the camera preview
        // frames
        preparePreviewCallbackBuffer();
        if (mCamera == null) {
            Log.e(TAG, "Camera not available");
        } else {
            // Create our Preview view and set it as the content of our
            // activity.
            mPreview = new CameraPreview(this, mCamera, mCameraCallback);
            // We do not want to display the Camera Preview view at startup - so
            // we resize it to 1x1 pixel.
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(1, 1);
            addContentView(mPreview, lp);
        }
    }

    public CameraAccessJMEActivity() {
        // Set the application class to run
        appClass = "com.example.jan.cameraaccessjme.CameraAccessJME";
        // Try ConfigType.FASTEST; or ConfigType.LEGACY if you have problems
        eglConfigType = ConfigType.BEST;
        // Exit Dialog title & message
        exitDialogTitle = "Exit?";
        exitDialogMessage = "Press Yes";
        // Enable verbose logging
        eglConfigVerboseLogging = false;
        // Choose screen orientation
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        // Invert the MouseEvents X (default = true)
        mouseEventsInvertX = true;
        // Invert the MouseEvents Y (default = true)
        mouseEventsInvertY = true;
    }

    // We override AndroidHarness.onCreate() to be able to add the SurfaceView
    // needed for camera preview
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onResume() {
        super.onResume();
        stopPreview = false;
        // Create an instance of Camera
        mCamera = getCameraInstance();
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_USE_CAMERA);
        } else {
            // initialize camera parameters
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, locationListener);
            initializeCameraParameters();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_USE_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        &&grantResults[1] == PackageManager.PERMISSION_GRANTED
                        &&grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    initializeCameraParameters();
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, locationListener);
                    Toast.makeText(CameraAccessJMEActivity.this, "permissions granted :*", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        stopPreview = true;
        super.onPause();
        // Make sure to release the camera immediately on pause.
        releaseCamera();
        // remove the SurfaceView
        ViewGroup parent = (ViewGroup) mPreview.getParent();
        parent.removeView(mPreview);
    }


    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            // Release the camera.
            mCamera.release();
            mCamera = null;
        }
    }

    // prepares the Camera preview callback buffers.
    public void preparePreviewCallbackBuffer() {
        int pformat;
        pformat = mCamera.getParameters().getPreviewFormat();
        Log.e(TAG, "PREVIEW format: " + pformat);
        // Get pixel format information to compute buffer size.
        PixelFormat info = new PixelFormat();
        PixelFormat.getPixelFormatInfo(pformat, info);
        // The actual preview width and height.
        // They can differ from the requested width mDesiredCameraPreviewWidth
        mPreviewWidth = mCamera.getParameters().getPreviewSize().width;
        mPreviewHeight = mCamera.getParameters().getPreviewSize().height;
        int bufferSizeRGB565 = mPreviewWidth * mPreviewHeight * 2 + 4096;
        //Delete buffer before creating a new one.
        mPreviewBufferRGB565 = null;
        mPreviewBufferRGB565 = new byte[bufferSizeRGB565];
        mPreviewByteBufferRGB565 = ByteBuffer.allocateDirect(mPreviewBufferRGB565.length);
        cameraJMEImageRGB565 = new Image(Image.Format.RGB565, mPreviewWidth,
                mPreviewHeight, mPreviewByteBufferRGB565);
    }

    public static void yCbCrToRGB565(byte[] YCBCRs, int width, int height,
                                     byte[] rgbs) {
        // the end of the luminance data
        final int lumEnd = width * height;
        // points to the next luminance value pair
        int lumPtr = 0;
        // points to the next chromiance value pair
        int chrPtr = lumEnd;
        // points to the next byte output pair of RGB565 value
        int outPtr = 0;
        // the end of the current luminance scanline
        int lineEnd = width;

        while (true) {

            // skip back to the start of the chromiance values when necessary
            if (lumPtr == lineEnd) {
                if (lumPtr == lumEnd)
                    break; // we've reached the end
                // division here is a bit expensive, but's only done once per
                // scanline
                chrPtr = lumEnd + ((lumPtr >> 1) / width) * width;
                lineEnd += width;
            }

            // read the luminance and chromiance values
            final int Y1 = YCBCRs[lumPtr++] & 0xff;
            final int Y2 = YCBCRs[lumPtr++] & 0xff;
            final int Cr = (YCBCRs[chrPtr++] & 0xff) - 128;
            final int Cb = (YCBCRs[chrPtr++] & 0xff) - 128;
            int R, G, B;

            // generate first RGB components
            B = Y1 + ((454 * Cb) >> 8);
            if (B < 0)
                B = 0;
            else if (B > 255)
                B = 255;
            G = Y1 - ((88 * Cb + 183 * Cr) >> 8);
            if (G < 0)
                G = 0;
            else if (G > 255)
                G = 255;
            R = Y1 + ((359 * Cr) >> 8);
            if (R < 0)
                R = 0;
            else if (R > 255)
                R = 255;
            // NOTE: this assume little-endian encoding
            rgbs[outPtr++] = (byte) (((G & 0x3c) << 3) | (B >> 3));
            rgbs[outPtr++] = (byte) ((R & 0xf8) | (G >> 5));

            // generate second RGB components
            B = Y2 + ((454 * Cb) >> 8);
            if (B < 0)
                B = 0;
            else if (B > 255)
                B = 255;
            G = Y2 - ((88 * Cb + 183 * Cr) >> 8);
            if (G < 0)
                G = 0;
            else if (G > 255)
                G = 255;
            R = Y2 + ((359 * Cr) >> 8);
            if (R < 0)
                R = 0;
            else if (R > 255)
                R = 255;
            // NOTE: this assume little-endian encoding
            rgbs[outPtr++] = (byte) (((G & 0x3c) << 3) | (B >> 3));
            rgbs[outPtr++] = (byte) ((R & 0xf8) | (G >> 5));
        }
    }
}