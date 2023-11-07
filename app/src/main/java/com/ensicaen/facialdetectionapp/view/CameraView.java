package com.ensicaen.facialdetectionapp.view;


import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.provider.Settings;
import android.util.DisplayMetrics;

import android.util.Log;
import android.view.View;
import android.widget.Button;


import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;

import android.Manifest;

import com.ensicaen.facialdetectionapp.R;
import com.ensicaen.facialdetectionapp.SettingsActivity;
import com.ensicaen.facialdetectionapp.controller.FaceAcquisitionListener;
import com.ensicaen.facialdetectionapp.controller.FaceAuthenticationListener;
import com.ensicaen.facialdetectionapp.controller.FaceDetectorListener;
import com.ensicaen.facialdetectionapp.controller.FrameAnalyzer;
import com.ensicaen.facialdetectionapp.controller.LivenessDetectorListener;
import com.ensicaen.facialdetectionapp.model.Profile;
import com.google.common.util.concurrent.ListenableFuture;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderCodecInfo;
import com.hbisoft.hbrecorder.HBRecorderListener;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class CameraView extends AppCompatActivity implements HBRecorderListener {
    private PreviewView previewView;
    private CameraOverlay cameraOverlay;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private String _cameraType;
    private ImageAnalysis _imageAnalysis;

    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;

    private boolean isRecording = false;

    private static final int REQUEST_CODE = 1000;
    private int screenDensity;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaRecorder mediaRecorder;
    private Button startBtn;
    private Button stopBtn;
    int ishotenabled = 0;
    public static final int RequestCode = 1009;
    HBRecorder hbRecorder;

    int timetorecord;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);


        _cameraType = getIntent().getExtras().getString("type");
        previewView = findViewById(R.id.previewView);
        cameraOverlay = findViewById(R.id.camera_overlay);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {

                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
            }
        }, ContextCompat.getMainExecutor(this));


        //Get references to UI elements
        startBtn = findViewById(R.id.button_record);
        hbRecorder = new HBRecorder(this, this);

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        }, 1000);


        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d("PermissionCheck", "Android R or higher");

            if (Environment.isExternalStorageManager()) {
                Log.d("PermissionCheck", "All files permission is already granted");
                Toast.makeText(this, "All files permission granted", Toast.LENGTH_SHORT).show();
                // You already have the permission
                mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            } else {
                Log.d("PermissionCheck", "All files permission not granted, requesting...");
                Toast.makeText(this, "Please grant all files permission", Toast.LENGTH_SHORT).show();

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCode);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, RequestCode);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RequestCode);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.FOREGROUND_SERVICE}, RequestCode);
//                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                Uri uri = Uri.fromParts("package", getPackageName(), null);
//                intent.setData(uri);
//                startActivity(intent);


            }
        } else {
            checkSupportedRecordingOptions();
            // Check if we have the WRITE_EXTERNAL_STORAGE permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // We don't have the permission, so we need to request it
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCode);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, RequestCode);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RequestCode);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.FOREGROUND_SERVICE}, RequestCode);
            } else {
                // We already have the permission
                Toast.makeText(this, "Write external storage permission is already granted.", Toast.LENGTH_SHORT).show();
            }

            Log.d("PermissionCheck", "Below Android R");
        }


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            if (Environment.isExternalStorageManager())
//            {
//
//                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                intent.setData(Uri.fromParts("package", getPackageName(), null));
//                startActivity(intent);
//
//            }
//
//
//        }

//        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
//        stopBtn = findViewById(R.id.btn_stop_recording);


        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ishotenabled == 0) {
                    timetorecord = 30;
                    Toast.makeText(CameraView.this, "Recording Started...", Toast.LENGTH_SHORT).show();

                    startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), RequestCode);

                } else {
                    Toast.makeText(CameraView.this, "Recording Stop...", Toast.LENGTH_SHORT).show();
                    hbRecorder.stopScreenRecording();
                    ishotenabled = 0;
                }

            }
        });




    }

    private void checkSupportedRecordingOptions() {

        // Examples of how to use the HBRecorderCodecInfo class to get codec info
        HBRecorderCodecInfo hbRecorderCodecInfo = new HBRecorderCodecInfo();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int mWidth = hbRecorder.getDefaultWidth();
            int mHeight = hbRecorder.getDefaultHeight();
            String mMimeType = "video/avc";
            int mFPS = 30;
            if (hbRecorderCodecInfo.isMimeTypeSupported(mMimeType)) {
                String defaultVideoEncoder = hbRecorderCodecInfo.getDefaultVideoEncoderName(mMimeType);
                boolean isSizeAndFramerateSupported = hbRecorderCodecInfo.isSizeAndFramerateSupported(mWidth, mHeight, mFPS, mMimeType, ORIENTATION_PORTRAIT);
                Log.e("EXAMPLE", "THIS IS AN EXAMPLE OF HOW TO USE THE (HBRecorderCodecInfo) TO GET CODEC INFO:");
                Log.e("HBRecorderCodecInfo", "defaultVideoEncoder for (" + mMimeType + ") -> " + defaultVideoEncoder);
                Log.e("HBRecorderCodecInfo", "MaxSupportedFrameRate -> " + hbRecorderCodecInfo.getMaxSupportedFrameRate(mWidth, mHeight, mMimeType));
                Log.e("HBRecorderCodecInfo", "MaxSupportedBitrate -> " + hbRecorderCodecInfo.getMaxSupportedBitrate(mMimeType));
                Log.e("HBRecorderCodecInfo", "isSizeAndFramerateSupported @ Width = " + mWidth + " Height = " + mHeight + " FPS = " + mFPS + " -> " + isSizeAndFramerateSupported);
                Log.e("HBRecorderCodecInfo", "isSizeSupported @ Width = " + mWidth + " Height = " + mHeight + " -> " + hbRecorderCodecInfo.isSizeSupported(mWidth, mHeight, mMimeType));
                Log.e("HBRecorderCodecInfo", "Default Video Format = " + hbRecorderCodecInfo.getDefaultVideoFormat());

                HashMap<String, String> supportedVideoMimeTypes = hbRecorderCodecInfo.getSupportedVideoMimeTypes();
                for (Map.Entry<String, String> entry : supportedVideoMimeTypes.entrySet()) {
                    Log.e("HBRecorderCodecInfo", "Supported VIDEO encoders and mime types : " + entry.getKey() + " -> " + entry.getValue());
                }

                HashMap<String, String> supportedAudioMimeTypes = hbRecorderCodecInfo.getSupportedAudioMimeTypes();
                for (Map.Entry<String, String> entry : supportedAudioMimeTypes.entrySet()) {
                    Log.e("HBRecorderCodecInfo", "Supported AUDIO encoders and mime types : " + entry.getKey() + " -> " + entry.getValue());
                }

                ArrayList<String> supportedVideoFormats = hbRecorderCodecInfo.getSupportedVideoFormats();
                for (int j = 0; j < supportedVideoFormats.size(); j++) {
                    Log.e("HBRecorderCodecInfo", "Available Video Formats : " + supportedVideoFormats.get(j));
                }
            } else {
                Log.e("HBRecorderCodecInfo", "MimeType not supported");
            }

        }


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                Toast.makeText(this, "Write external storage permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                // Permission was denied or request was cancelled
                Toast.makeText(this, "Write external storage permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // capture screen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RequestCode && resultCode == RESULT_OK) {
            hbRecorder.setOutputPath("/storage/emulated/0/");
            hbRecorder.setScreenDimensions(getResources().getDisplayMetrics().heightPixels, getResources().getDisplayMetrics().widthPixels);
            hbRecorder.recordHDVideo(true);
            hbRecorder.setMaxDuration(60);
            hbRecorder.startScreenRecording(data, resultCode);
            ishotenabled = 1;
        }


        super.onActivityResult(requestCode, resultCode, data);
    }


    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)   // viewe
                .build();

        _imageAnalysis = new ImageAnalysis.Builder().build();

        FrameAnalyzer frameAnalyzer = new FrameAnalyzer();

        if (_cameraType.equals("detection")) {
            frameAnalyzer.addFaceListener(new FaceDetectorListener(cameraOverlay, PreferenceManager.getDefaultSharedPreferences(this)));
        } else if (_cameraType.equals("acquisition")) {
            frameAnalyzer.addFaceListener(new FaceAcquisitionListener(cameraOverlay, this));
        } else if (_cameraType.equals("authentication")) {
            frameAnalyzer.addFaceListener(new FaceAuthenticationListener(cameraOverlay, this, (Profile) getIntent().getSerializableExtra("user")));
        } else if (_cameraType.equals("liveness")) {
            frameAnalyzer.addFaceListener(new LivenessDetectorListener(cameraOverlay, this));
        }

        _imageAnalysis.setAnalyzer(Runnable::run, frameAnalyzer);

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, _imageAnalysis);
    }

    public void close(int[] features) {
        _imageAnalysis.clearAnalyzer();
        Intent resultIntent = new Intent();
        resultIntent.putExtra("FEATURES_RESULT", features);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    public void close(boolean success) {
        _imageAnalysis.clearAnalyzer();
        Intent resultIntent = new Intent();
        resultIntent.putExtra("AUTHENTICATE_RESULT", success);
        setResult(RESULT_OK, resultIntent);
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }


    @Override
    public void HBRecorderOnStart() {


    }

    @Override
    public void HBRecorderOnComplete() {

    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        Log.e("HBRecorder", "Error during recording: " + reason);

    }

    @Override
    public void HBRecorderOnPause() {

    }

    @Override
    public void HBRecorderOnResume() {

    }
}
