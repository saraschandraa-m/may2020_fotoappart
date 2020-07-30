package com.appstone.fotoappart;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.configuration.CameraConfiguration;
import io.fotoapparat.error.CameraErrorListener;
import io.fotoapparat.exception.camera.CameraException;
import io.fotoapparat.parameter.Flash;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;
import io.fotoapparat.result.BitmapPhoto;
import io.fotoapparat.result.PhotoResult;
import io.fotoapparat.result.WhenDoneListener;
import io.fotoapparat.selector.AspectRatioSelectorsKt;
import io.fotoapparat.selector.FlashSelectorsKt;
import io.fotoapparat.selector.FocusModeSelectorsKt;
import io.fotoapparat.selector.LensPositionSelectorsKt;
import io.fotoapparat.selector.SelectorsKt;
import io.fotoapparat.selector.SensorSensitivitySelectorsKt;
import io.fotoapparat.view.CameraView;
import io.fotoapparat.view.FocusView;

import static io.fotoapparat.selector.FocusModeSelectorsKt.continuousFocusPicture;
import static io.fotoapparat.selector.LensPositionSelectorsKt.back;
import static io.fotoapparat.selector.LensPositionSelectorsKt.front;
import static io.fotoapparat.selector.PreviewFpsRangeSelectorsKt.highestFps;
import static io.fotoapparat.selector.ResolutionSelectorsKt.highestResolution;

public class MainActivity extends AppCompatActivity {

    private CameraView cameraView;
    private FocusView focusView;
    private ImageView mIvPreviewImage;

    private Fotoapparat fotoapparat;


    private boolean isFacingFront = false;

    private CameraConfiguration cameraConfiguration;

    private CameraConfiguration defaultConfiguration = CameraConfiguration
            .builder()
            .photoResolution(AspectRatioSelectorsKt.standardRatio(
                    highestResolution()
            ))
            .focusMode(SelectorsKt.firstAvailable(
                    continuousFocusPicture(),
                    FocusModeSelectorsKt.autoFocus(),
                    FocusModeSelectorsKt.fixed()
            ))
            .flash(SelectorsKt.firstAvailable(
                    FlashSelectorsKt.autoRedEye(),
                    FlashSelectorsKt.autoFlash(),
                    FlashSelectorsKt.torch(),
                    FlashSelectorsKt.off()
            ))
            .previewFpsRange(highestFps())
            .sensorSensitivity(SensorSensitivitySelectorsKt.highestSensorSensitivity())
            .frameProcessor(new CameraFrameProcessor())
            .build();


    private CameraConfiguration flashOnConfiguration =
            CameraConfiguration
                    .builder()
                    .photoResolution(AspectRatioSelectorsKt.standardRatio(
                            highestResolution()
                    ))
                    .focusMode(SelectorsKt.firstAvailable(
                            continuousFocusPicture(),
                            FocusModeSelectorsKt.autoFocus(),
                            FocusModeSelectorsKt.fixed()
                    ))
                    .flash(SelectorsKt.firstAvailable(
                            FlashSelectorsKt.on()
                    ))
                    .previewFpsRange(highestFps())
                    .sensorSensitivity(SensorSensitivitySelectorsKt.highestSensorSensitivity())
                    .frameProcessor(new CameraFrameProcessor())
                    .build();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.cameraview);
        focusView = findViewById(R.id.focusview);

        ImageView mIvCapture = findViewById(R.id.iv_btn_capture);
        ImageView mIvFlipCamera = findViewById(R.id.iv_switch_camera);
        ImageView mIvFlashOn = findViewById(R.id.iv_flash_on);
        mIvPreviewImage = findViewById(R.id.iv_preview_img);

        mIvCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fotoapparat != null) {
                    PhotoResult photoResult = fotoapparat.takePicture();

                    photoResult.toBitmap().whenDone(new WhenDoneListener<BitmapPhoto>() {
                        @Override
                        public void whenDone(@Nullable BitmapPhoto bitmapPhoto) {
                            mIvPreviewImage.setImageBitmap(bitmapPhoto.bitmap);
                        }
                    });
                }
            }
        });

        mIvFlipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fotoapparat != null) {
                    fotoapparat.switchTo(isFacingFront ? back() : front(), cameraConfiguration);
                    isFacingFront = !isFacingFront;
                }
            }
        });

        mIvFlashOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fotoapparat != null) {
                    fotoapparat.updateConfiguration(flashOnConfiguration);
                    cameraConfiguration = flashOnConfiguration;
                }
            }
        });

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
        } else {
            retrieveImagesFromDevice();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fotoapparat != null) {
            fotoapparat.start();
        } else {
            initiateCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fotoapparat != null) {
            fotoapparat.stop();
        }
    }


    private void initiateCamera() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1000);
        } else {
            setupFotoappart();
        }
    }

    private void setupFotoappart() {
        fotoapparat = Fotoapparat
                .with(MainActivity.this)
                .into(cameraView)
                .focusView(focusView)
                .previewScaleType(ScaleType.CenterCrop)
                .lensPosition(back())
                .frameProcessor(new CameraFrameProcessor())
                .cameraErrorCallback(new CameraErrorListener() {
                    @Override
                    public void onError(CameraException e) {
                        Toast.makeText(MainActivity.this, "Error in creating camera", Toast.LENGTH_LONG).show();
                    }
                })
                .build();

        fotoapparat.start();

        cameraConfiguration = defaultConfiguration;

    }

    private void retrieveImagesFromDevice() {


        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;


        String[] albumProj = {MediaStore.Images.Media._ID,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME};


        ArrayList<GalleryPicker> galleryPickers = new ArrayList<>();
        ArrayList<String> galleryName = new ArrayList<>();

        Cursor albumCursor = getApplicationContext().getContentResolver().query(uri, albumProj, null, null, null);

        for (albumCursor.moveToFirst(); !albumCursor.isAfterLast(); albumCursor.moveToNext()) {
            String albumName = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
            String albumID = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Images.Media._ID));

            Log.i("Album Name: " + albumName, "Album ID: " + albumID);

            if (!galleryName.contains(albumName)) {
                GalleryPicker galleryPicker = new GalleryPicker();
                galleryPicker.albumID = albumID;
                galleryPicker.albumName = albumName;

                galleryPickers.add(galleryPicker);
                galleryName.add(albumName);
            }
        }

        String[] proj = {MediaStore.Images.Media.DATA};

        String[] selectionIDs = new String[1];
        selectionIDs[0] = "7869";

        Cursor retrieveCursor = getApplicationContext().getContentResolver().query(uri, proj, MediaStore.Images.Media.BUCKET_ID+"=?", selectionIDs, null);


        ArrayList<String> images = new ArrayList<>();
        if (retrieveCursor != null) {
            for (retrieveCursor.moveToFirst(); !retrieveCursor.isAfterLast(); retrieveCursor.moveToNext()) {
                String image = retrieveCursor.getString(retrieveCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                images.add(image);
            }
        }

        if (images != null && images.size() > 0) {
            Glide.with(MainActivity.this).load(images.get(0)).into(mIvPreviewImage);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupFotoappart();
            } else {
                Toast.makeText(MainActivity.this, "User Denied Permission", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 1001) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                retrieveImagesFromDevice();
            }
        }
    }

    private class CameraFrameProcessor implements FrameProcessor {

        @Override
        public void process(Frame frame) {

        }
    }
}