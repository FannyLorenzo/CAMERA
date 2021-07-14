package com.example.android.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.nio.ByteBuffer;
import java.security.cert.CertPathBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {
        private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

        ///To make the photo appear vertically
        static {
            ORIENTATIONS.append(Surface.ROTATION_0, 90);
            ORIENTATIONS.append(Surface.ROTATION_90, 0);
            ORIENTATIONS.append(Surface.ROTATION_180, 270);
            ORIENTATIONS.append(Surface.ROTATION_270, 180);
        }

        private SurfaceView mSurfaceView;
        private SurfaceHolder mSurfaceHolder;
        private ImageView iv_show;
        private CameraManager mCameraManager;//Camera Manager
        private Handler childHandler, mainHandler;
        private String mCameraID;//Camera Id 0 is the last 1 is the former
        private ImageReader mImageReader;
        private CameraCaptureSession mCameraCaptureSession;
        private CameraDevice mCameraDevice;
    private CaptureRequest mCaptureRequestBuilder;
    private TextureView mTextureView;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    Button btnCamara;
    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
        btnCamara = findViewById(R.id.btnCamara);
        initVIew();

        /// boton

        btnCamara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //initVIew();
                takePicture();
            }
        });
        }

        /**
         * Initialization
         */
        private void initVIew() {
            iv_show = (ImageView) findViewById(R.id.iv_show_camera2_activity);
            //mSurfaceView
            mSurfaceView = (SurfaceView) findViewById(R.id.surface_view_camera2_activity);
            mSurfaceView.setOnClickListener(this);
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.setKeepScreenOn(true);
            // mSurfaceView adds a callback
            mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) { //SurfaceView creation
                    // Initialize Camera
                    initCamera2();
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) { //SurfaceView destroyed
                    // Release Camera resources
                    if (null != mCameraDevice) {
                        mCameraDevice.close();
                        MainActivity.this.mCameraDevice = null;
                    }
                }
            });
        }

        /**
         * Initialize Camera2
         */
       // @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        private void initCamera2() {
            HandlerThread handlerThread = new HandlerThread("Camera2");
            handlerThread.start();
            childHandler = new Handler(handlerThread.getLooper());
            mainHandler = new Handler(getMainLooper());
            mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT;//front camera
            mImageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG,1);
            mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() { //You can process the temporary photo taken here, for example, write local
                @Override
                public void onImageAvailable(ImageReader reader) {
                   // mCameraDevice.close();
                    mSurfaceView.setVisibility(View.GONE);
                    iv_show.setVisibility(View.VISIBLE);
                    // Get photo data
                    Image image = reader.acquireNextImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);//Save the byte array from the buffer
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bitmap != null) {
                        iv_show.setImageBitmap(bitmap);
                    }
                }
            }, mainHandler);
                 // Get camera management
                    mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                         // Open the camera
                mCameraManager.openCamera(mCameraID, stateCallback, mainHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }


        /**
         * Camera creation monitor
         */
        private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {//Open the camera
                mCameraDevice = camera;
                //Open preview
                takePreview();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {//Close the camera
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    MainActivity.this.mCameraDevice = null;
                }
            }

            @Override
            public void onError(CameraDevice camera, int error) {//An error occurred
                Toast.makeText(MainActivity.this, "camera failed to open", Toast.LENGTH_SHORT).show();
            }
        };

        /**
         * Start preview
         */
        private void takePreview() {
            try {
                // Create the CaptureRequest.Builder needed for the preview
                final CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                         // Surface of the SurfaceView as the target of CaptureRequest.Builder
                previewRequestBuilder.addTarget(mSurfaceHolder.getSurface());
                // Create a CameraCaptureSession that is responsible for managing the processing of preview requests and photo requests
                mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface(), mImageReader.getSurface()), new CameraCaptureSession.StateCallback() // ③
                {
                    @Override
                    public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                        if (null == mCameraDevice) return;
                        // When the camera is ready, start displaying the preview
                        mCameraCaptureSession = cameraCaptureSession;
                        try {
                            // auto focus
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            // turn on the flash
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                            // show preview
                            CaptureRequest previewRequest = previewRequestBuilder.build();
                            mCameraCaptureSession.setRepeatingRequest(previewRequest, null, childHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                        Toast.makeText(MainActivity.this, "Configuration failed", Toast.LENGTH_SHORT).show();
                    }
                }, childHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        /**
         * Click event
         */
        @Override
        public void onClick(View v) {
            takePicture();
        }

        /**
         * taking pictures
         */
        private void takePicture() {
            if (mCameraDevice == null) return;
            // Create a CaptureRequest.Builder for taking photos
            final CaptureRequest.Builder captureRequestBuilder;
            try {
                captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                         // The imageReader surface as the target of CaptureRequest.Builder
                captureRequestBuilder.addTarget(mImageReader.getSurface());
                // auto focus
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // automatic exposure
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                // Get the phone direction
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                // Calculate the direction of the photo based on the device orientation
                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
                //photograph
                CaptureRequest mCaptureRequest = captureRequestBuilder.build();
                mCameraCaptureSession.capture(mCaptureRequest, null, childHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        //Start playing
        public void start() {
           /*
            try {
                Log.e(getAttributionTag(),"start");
                mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, childHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }*/
        }

        //Pause playback
        public void stop() {
            try {
                Log.e(getAttributionTag(),"stop");
                mCameraCaptureSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

         // Close playback
        public void close() {
            if (mTextureView != null) {
                mTextureView = null;

            }
            // mPreviewed=false;
            if (childHandler != null) {
                childHandler.removeCallbacksAndMessages(null);
                childHandler = null;
            }
            if (mHandler != null)
                mHandler.removeCallbacksAndMessages(null);
            mHandler = null;

            if (mHandlerThread != null) {
                mHandlerThread.quit();
                mHandlerThread = null;
            }

            if (null != mCameraCaptureSession) {
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        }
    }