package com.example.bianca.codigoqr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class detectorQR extends AppCompatActivity {

    private CameraSource cameraSource;
    private SurfaceView cameraView;
    private final int  MY_PERMISSION_REQUEST_CAMERA  = 1;
    private String token = "";
    private String tokenanterior = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detector_qr);

        cameraView = findViewById(R.id.camera_view);
        initQR();

    }

    public void initQR(){

        //Crear el detector QR - BarcodeDetector permite reconocer còdigos de barra y còdigos QR
        final BarcodeDetector barcodeDetector= new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        //Crear la camara fuente - Camera Source permite obtener frames de la camara del dispositivo y posteriormente analizarlo
        cameraSource = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true)
                .build();

        //Listener del ciclo de vida de la camara
        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //Verificar si el usuario dio permisos para la camara

                if(ActivityCompat.checkSelfPermission(detectorQR.this, Manifest.permission.CAMERA)
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

                /*
                if (ContextCompat.checkSelfPermission(getApplicationContext(),android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                    try{
                        cameraSource.start(cameraView.getHolder());
                    }catch (IOException ie){
                        Log.e("CAMERA SOURCE", ie.getMessage());
                    }
                }else{
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_camera),Toast.LENGTH_SHORT).show();                }
            */
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        //preparar el detector QR
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                /*El resultado se devuelve en un SparseArray. Cuando devuelve una respuesta
                barcodes.size() es distinto de 0.
                 */
                if(barcodes.size() != 0){
                    //Obtener el dato devuelto por el QR, (texto - entero - URL)
                    token = barcodes.valueAt(0).displayValue.toString();

                    //Verificar que el token anterior no sea igual al actual
                    //Util para evitar multiples llamadas empleando el mismo token
                    if(!token.equals(tokenanterior)){
                        //guardar el ultimo token procesado
                        tokenanterior = token;
                        Log.i("Token", token);

                        if(URLUtil.isValidUrl(token)){
                            //Si es una URL valida abre el navegador
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(token));
                            startActivity(browserIntent);
                        }else{
                            //definir como se va a mostrar el contenido del QR, en este caso abre un dialogo para compartir
                           /* Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.putExtra(Intent.EXTRA_TEXT, token);
                            shareIntent.setType("text/plain");
                            startActivity(shareIntent);*/

                           //Mostrar un toast con el contenido del qr
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(),token,Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                synchronized (this){
                                    wait(5000);
                                    //limpiar el token
                                    tokenanterior="";
                                }

                            }catch (InterruptedException e){
                                Log.e("Error", "Waiting didn't work!");
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }

            /*
            En el código anterior puede notar que al momento de detectar una lectura
            del código QR por el móvil se realiza una serie de pasos que va desde la
            obtención del mismo mediante barcodes.valueAt(0).displayValue.toString();,
            la validación (y liberación con el Thread) para evitar leer el mismo QR de forma consecutiva
            (el lector QR siempre sigue ejecutándose independientemente de si ha procesado un QR o no)
            y luego mediante URLUtil.isValidUrl(token) verificamos si es una URL el token obtenido,
            y en ese caso abre en navegador predefinido de nuestro teléfono, en caso contrario simplemente mostramos el
            dialog para compartir contenido por las distintas aplicaciones que tengamos en nuestro teléfono.
            */

        });

    }
}
