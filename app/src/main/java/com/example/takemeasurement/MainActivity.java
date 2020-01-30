package com.example.takemeasurement;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Arrays;

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
    int canny_Min = 40;
            int canny_Max = 80;
            int hough_Maxgap = 120;
            int hough_Min = 900 ;
            int hThresh = 80;
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
        Imgproc.medianBlur(imageForAnalysis, imageForAnalysis, 7);

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

                    Log.d(TAG, "vert line X1 " + vertLines.get(numOfLine).get(0));
                    numOfLine++;
                }
                 }
            numOfLine = numOfLine - 1;   // numOfLine = number of verticle lines
            Log.d(TAG, "vert lines " + numOfLine);


                    for (int j = 0; j <= numOfLine; j++) {  // for all the verticle lines get the distance between the lines put into linDistArray
                         double x1 = vertLines.get(j).get(0),
                                y1 = vertLines.get(j).get(1),
                                x2 = vertLines.get(j).get(2),
                                y2 = vertLines.get(j).get(3);
                        Log.d(TAG, "each vert line X1 " + vertLines.get(numOfLine).get(1));

                        Point start = new Point(x1, y1);
                        Point end = new Point(x2, y2);
                         lineSlope1 = ((end.y - start.y) / (end.x - start.x));  // get the slope of the line
                        lineIntercept1 = (start.y - (lineSlope1 * start.x));  // get the intercept of the line
                        oldStart = start;
                        oldEnd = end;


                        if (j > 1) {   // measure from the old line to the new line
                            lineIntercept2 = (oldStart.y - (lineSlope1 * oldEnd.x));
                            lineDist = (Math.abs(lineIntercept2 - lineIntercept1)) / (Math.sqrt((lineSlope1 * lineSlope1) + 1));
                            lineDistArray.add(lineDist);
                            Log.d(TAG, "line dist " + lineDist);
                        }
                    }


            for (int k = 0; k < lineDistArray.size(); k++) {  // for all the line distances totals, average the similar ones

                if ((k >= 1) && ((Math.abs((lineDistArray.get(k-1))-(lineDistArray.get(k))) > 10))) {
                    total = total + Math.abs(lineDistArray.get(k));
                    goodDist++;
                    Log.d(TAG, "linedistarray" + lineDistArray.get(k));
                }

            }

                lineDist = (total /(goodDist-1)/7);
            TextView mainView = (TextView) findViewById(R.id.textView1);
            mainView.setText(String.format("Dist: %.2f", lineDist));
            TextView newView = (TextView) findViewById(R.id.textView2);
            newView.setText(Integer.toString(numOfLine));

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

