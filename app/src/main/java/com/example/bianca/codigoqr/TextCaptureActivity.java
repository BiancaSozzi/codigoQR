package com.example.bianca.codigoqr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.Locale;

/**
 * Created by bianc on 12/12/2017.
 */

public class TextCaptureActivity extends AppCompatActivity {
    private SurfaceView cameraView;
    private TextView textBlockContent;
    private CameraSource cameraSource;
    private static final String TAG = "TextCaptureActivity";
    private final int  MY_PERMISSION_REQUEST_CAMERA  = 1;
    private static boolean TEXTO_DESEADO = false;
    private TextToSpeech tts;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.text_capture);

        cameraView = findViewById(R.id.surface_view);
        textBlockContent = findViewById(R.id.text_value);

       createCameraSource();

       textBlockContent.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               openActivity();
           }
       });


    }

    @SuppressLint("InlinedApi")
    private void createCameraSource(){
        Context context = getApplicationContext();

        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();

        if(!textRecognizer.isOperational()){
            Log.w(TAG, "Detector dependencies are not yet available");
            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

            if(hasLowStorage){
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_SHORT).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

       //Setear processor a textRecognizer
        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                final SparseArray<TextBlock> items = detections.getDetectedItems();
                if(items.size() != 0){
                    textBlockContent.post(new Runnable() {
                        @Override
                        public void run() {
                            StringBuilder value = new StringBuilder();
                            for (int i = 0; i < items.size(); i++){
                                TextBlock item = items.valueAt(i);
                                value.append(item.getValue());
                                value.append("\n");
                                if(item.getValue().contains("The quick")){
                                    Log.i("TEXTO","Es el texto buscado");
                                    TEXTO_DESEADO= true;
                                    Log.i("TEXTO_DESEADO", Boolean.toString(TEXTO_DESEADO));
                                }else{
                                    TEXTO_DESEADO = false;
                                    Log.i("TEXTO_DESEADO", Boolean.toString(TEXTO_DESEADO));
                                }
                            }
                            textBlockContent.setText(value.toString());
                        }
                    });
                }


            }
        });

        cameraSource = new CameraSource.Builder(context,textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1280, 1024)
                .setRequestedFps(15.0f)
                .setAutoFocusEnabled(true)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //Verificar los permisos de la camara
                if(ActivityCompat.checkSelfPermission(TextCaptureActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED){
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA));
                        requestPermissions(new String[]{Manifest.permission.CAMERA},
                                MY_PERMISSION_REQUEST_CAMERA);
                    }
                    return;
                }else{
                    try{
                        cameraSource.start(cameraView.getHolder());
                    }catch (IOException ie){
                        Log.e("CAMERA SOURCE", ie.getMessage());
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    private void openActivity(){
        if(TEXTO_DESEADO){
            Intent detectorQR = new Intent(getApplicationContext(), detectorQR.class);
            startActivity(detectorQR);
        }else{
            Toast.makeText(getApplicationContext(),"Este texto no es el adecuado", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        createCameraSource();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraSource.release();
    }
}
