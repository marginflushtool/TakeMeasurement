package com.example.takemeasurement;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.security.Policy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.Arrays;

 import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.hardware.camera2.CameraMetadata.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_APPROXIMATE;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static org.opencv.android.Utils.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button btnTakePic;
    Button btnBlurPic;
    Button btnEdgeLinePic;
    Button btnTakeMeasure;
    Button btnRefine;
    ImageView imageView;
    String pathTofile;
    Mat analysis;
    int canny_Min = 30;
            int canny_Max = 70;
            int hough_Maxgap = 80;
            int hough_Min = 950 ;
            int hThresh = 70;
            int ksize = 5;
            int iterations = 1;
    double lineDist = 2.00;
    double lineSlope1 = 0.0;
    double lineSlope2 =0.0;
    double lineIntercept1 = 0.0;
    double lineIntercept2 = 0.0;
    boolean writeLine = false;
    int numOfLine = 0;
    double total = 0;
    int goodDist = 0;
    ArrayList<Double> lineDistArray = new ArrayList<>() ;
    ArrayList<ArrayList<Double>> vertLines = new ArrayList<ArrayList<Double>>();
    List<Double> line = new ArrayList<Double>(4);
    double focalLength = 0.0;

    private static final String TAG = MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");

        }

        analysis = new Mat();
        setContentView(R.layout.activity_main); //
        btnTakePic = findViewById(R.id.btnTakePic);
        btnBlurPic = findViewById(R.id.btnBlurPic);
        btnEdgeLinePic = findViewById(R.id.btnEdgeLinePic);
        btnRefine = findViewById(R.id.btnRefine);

        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        btnTakePic.setOnClickListener((View.OnClickListener) MainActivity.this);
        btnBlurPic.setOnClickListener((View.OnClickListener) MainActivity.this);
        btnEdgeLinePic.setOnClickListener((View.OnClickListener) MainActivity.this);
        btnRefine.setOnClickListener((View.OnClickListener) MainActivity.this);


        imageView = findViewById(R.id.image);
        imageView.setRotation(90);


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                BitmapFactory.Options op = new BitmapFactory.Options();
                op.inPreferredConfig = Bitmap.Config.ARGB_8888;

                Bitmap bitmap = BitmapFactory.decodeFile(pathTofile);


                bitmapToMat(bitmap, analysis);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    private void dispatchPictureTakerAction() {
          Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
 //         CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
   //         CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
     //         focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE);

        //        Log.d(TAG, "focal leng  " + focalLength);
//        Log.d(TAG, "Focal length:"+Highgui.CV_CAP_PROP_ANDROID_FOCAL_LENGTH);

        if (takePic.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            photoFile = createPhotoFile();

            if (photoFile != null) {
                pathTofile = photoFile.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this, "com.example.takemeasurement.fileprovider", photoFile);
                takePic.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePic, 1);
            }

        }

    }

    private File createPhotoFile() {
        String name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(name, ".jpg", storageDir);
        } catch (IOException e) {
            Log.d("mylog", "Excep: " + e.toString());
        }
        return image;
    }


    private void blurPic(Mat imageForAnalysis) {

        Imgproc.cvtColor(imageForAnalysis, imageForAnalysis, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.medianBlur(imageForAnalysis, imageForAnalysis, 11);

  //      Imgproc.dilate(imageForAnalysis, imageForAnalysis, new Mat(), new Point(-1, -1),iterations);
  //      Imgproc.erode(imageForAnalysis, imageForAnalysis, new Mat(), new Point(-1, -1),iterations);
 //       Imgproc.Laplacian(imageForAnalysis, imageForAnalysis, CvType.CV_8U);

        analysis = imageForAnalysis;

        Bitmap bitmap = Bitmap.createBitmap(imageForAnalysis.cols(), imageForAnalysis.rows(), Bitmap.Config.ARGB_8888);
        matToBitmap(imageForAnalysis, bitmap);
        imageView = findViewById(R.id.image);

        imageView.setImageBitmap(bitmap);
        imageView.setRotation(85);
    }

    private void refinePic(){
            if (numOfLine > 2) {                // blur pic - erode further - increase houghlinep threshold
                ksize = ksize + 3;
                iterations = iterations + 1;
                hough_Maxgap = hough_Maxgap / 2;
                hough_Min = hough_Min * 2;

            }
            else {
                ksize = 1;
                iterations = 1;
                hough_Maxgap = hough_Maxgap * 2;
                hough_Min = hough_Min / 2;
                // decrease threshold - decrease blur
            }
            blurPic(analysis);
            edgeLinePic(analysis);

    }

    private void drawImage(Mat imageDraw) {
        Bitmap bitmap = Bitmap.createBitmap(imageDraw.cols(), imageDraw.rows(), Bitmap.Config.ARGB_8888);
        matToBitmap(imageDraw, bitmap);
        imageView = findViewById(R.id.image);
        imageView.setImageBitmap(bitmap);
        imageView.setRotation(80);
    }



        private void edgeLinePic(Mat imageForAnalysis) {

        Mat lines = new Mat();

        int lineCount = 0;
        int vertLineCount = 0;
        Point oldStart = new Point(2, 0);
        Point oldEnd = new Point(2, 0);

        Imgproc.Canny(imageForAnalysis, imageForAnalysis, canny_Min, canny_Max);
       Imgproc.dilate(imageForAnalysis, imageForAnalysis, new Mat(), new Point(-1, -1),iterations);
 //      Imgproc.erode(imageForAnalysis, imageForAnalysis, new Mat(), new Point(-1, -1),iterations);
       drawImage(imageForAnalysis);

            Log.d(TAG, "canny done");

        Imgproc.HoughLinesP(imageForAnalysis, lines, 1, Math.PI / 180, hThresh, hough_Min, hough_Maxgap);
        Log.d(TAG, "HoughP done");


            for (int i = 0; i < lines.rows(); i++) {
                lineCount++;
                double[] vec = lines.get(i, 0);
                double x1 = vec[0],
                        y1 = vec[1],
                        x2 = vec[2],
                        y2 = vec[3];

                Log.d(TAG, "X1 " + x1);
                Point start = new Point(x1, y1);
                Point end = new Point(x2, y2);

                lineSlope1 = ((end.y - start.y) / (end.x - start.x));  // get the slope of the line
                lineIntercept1 = (start.y - (lineSlope1 * start.x));  // get the intercept of the line
                Log.d(TAG, "lineslope1  " + lineSlope1);
                if ((Math.abs(lineSlope1) < 0.5)) {   //collect all verticle lines into VertLines Mat
                    vertLines.add(new ArrayList<Double>());
                    drawImage(imageForAnalysis);
                    Imgproc.line(imageForAnalysis, start, end, new Scalar(255, 255, 255), 10); //write the vertical line
                    Log.d(TAG, "line written");
                    //save verticle lines
   //                 line.add(x1);
     //                line.add(y1);
       //             line.add(x2);
         //           line.add(y2);
                    vertLines.get(numOfLine).add(0,x1);
                    vertLines.get(numOfLine).add(1,y1);
                    vertLines.get(numOfLine).add(2,x2);
                    vertLines.get(numOfLine).add(3,y2);

 //                   Log.d(TAG, "vert line X1 " + vertLines.get(numOfLine).get(0));
                    numOfLine++;
                }
                 }
            numOfLine = numOfLine - 1;   // numOfLine = number of verticle lines
            Log.d(TAG, "vert lines " + numOfLine);
            for (int h = 0; h<= numOfLine; h++) {
                double rx1 = vertLines.get(h).get(0),
                        ry1 = vertLines.get(h).get(1),
                        rx2 = vertLines.get(h).get(2),
                        ry2 = vertLines.get(h).get(3);
 //               Log.d(TAG, "ref vert line X1 " + vertLines.get(numOfLine).get(1));

                Point refStart = new Point(rx1, ry1);
                Point refEnd = new Point(rx2, ry2);


                for (int j = 0; j <= numOfLine; j++) {  // for all the verticle lines get the distance between the lines put into linDistArray
                    double x1 = vertLines.get(j).get(0),
                            y1 = vertLines.get(j).get(1),
                            x2 = vertLines.get(j).get(2),
                            y2 = vertLines.get(j).get(3);
 //                   Log.d(TAG, "inner vert line X1 " + vertLines.get(numOfLine).get(1));

                    Point start = new Point(x1, y1);
                    Point end = new Point(x2, y2);
                    lineSlope1 = ((end.y - start.y) / (end.x - start.x));  // get the slope of the line
                    lineIntercept1 = (start.y - (lineSlope1 * start.x));  // get the intercept of the line
                    //                      oldStart = start;
                    //                      oldEnd = end;


                    if (j > 1) {   // measure from the ref line to the new line
                        lineIntercept2 = (refStart.y - (lineSlope1 * refEnd.x));
                        lineDist = (Math.abs(lineIntercept2 - lineIntercept1)) / (Math.sqrt((lineSlope1 * lineSlope1) + 1));
                        lineDistArray.add(lineDist);
                        Log.d(TAG, "line dist " + lineDist);
                    }
                }
            }
            Collections.sort(lineDistArray); // Sort distances min to max
            ArrayList<Double> delta = new ArrayList<Double>(); //array of deltas in line distances
            double max = 1;
            double bogey = 0; //get max value in distances to set as bogey
            double lastMax = 0;

            Log.d(TAG, "size of line dist array " + lineDistArray.size());

            for (int n = 0; n < (lineDistArray.size()-1); n++) {  // for all the line distances totals, average binominal values
                delta.add(lineDistArray.get(n + 1) - max);  // find the jump in array and then average after that
                max = delta.get(n);
                if (max > lastMax) {
                    lastMax = max;
                }
                }


            bogey = lastMax;

                 Log.d(TAG, "distances in delta " + delta.size());

            Log.d(TAG, "bogey " + bogey);

            goodDist = 0;
             for (int m = 0; m < delta.size(); m++ ) { // use bogey to find index.
                 if (delta.get(m) < bogey) {
                     goodDist++;
                     Log.d(TAG, "index for small vs large" + goodDist);

                 }

             }
                for (int l = goodDist; l < lineDistArray.size(); l++) {  //sum up all the larger sized and average them
                    lineDist = lineDist + lineDistArray.get(l);
                }
                lineDist = lineDist / (lineDistArray.size()-goodDist);
            Log.d(TAG, "linedist avg" + lineDist);



                lineDist = (lineDist/86);
            TextView mainView = (TextView) findViewById(R.id.textView1);
            mainView.setText(String.format("Dist: %.2f", lineDist));
            TextView newView = (TextView) findViewById(R.id.textView2);
            newView.setText(Integer.toString(numOfLine));
            numOfLine = 0;
            lineDist = 0;
            lineDistArray.removeAll(lineDistArray);
            vertLines.removeAll(vertLines);

        }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnTakePic:
                Log.d(TAG, "Picture button Pressed");
                dispatchPictureTakerAction();
                break;
            case R.id.btnBlurPic:
                Log.d(TAG, "Blur button Pressed");
                blurPic(analysis);
                break;
            case R.id.btnEdgeLinePic:
                Log.d(TAG, "Edge button Pressed");
                edgeLinePic (analysis);
                break;
            case R.id.btnRefine:
                Log.d(TAG, "Refine button Pressed");
                refinePic();
                break;
                /*
                    case R.id.btnTakeMeasure:
                    */

        }
    }
}

