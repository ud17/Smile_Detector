package com.example.smiledetector;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.theartofdev.edmodo.cropper.CropImage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FrameProcessor {

    private Facing facing = Facing.FRONT;
    private CameraView cameraView;
    private RecyclerView recyclerView;
    private ArrayList<FaceDetectionModel> faceDetectionModelArrayList;
    private BottomSheetBehavior bottomSheetBehavior;
    private ImageView imageView;
    private Button toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        faceDetectionModelArrayList = new ArrayList<>();
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));

        imageView = findViewById(R.id.face_detection_image_view);

        cameraView = findViewById(R.id.face_detection_camera_view);
        toggle = findViewById(R.id.face_detection_camera_button);
        FrameLayout bottomSheetButton = findViewById(R.id.bottom_sheet_button);

        recyclerView = findViewById(R.id.bottom_sheet_recyclerView);

        cameraView.setFacing(facing);
        cameraView.setLifecycleOwner(MainActivity.this);
        /* cameraView.addFrameProcessor(MainActivity.this);*/


        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int id = view.getId();

                if (id == toggle.getId()){
                    if (cameraView.getFacing() == facing){
                        cameraView.setFacing(Facing.BACK);
                    }else {
                        cameraView.setFacing(Facing.FRONT);
                    }

                }
            }
        });

        bottomSheetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity().start(MainActivity.this);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recyclerView.setAdapter(new FaceDetectionRecyclerView(MainActivity.this , faceDetectionModelArrayList));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK){
                Uri imageUri = result.getUri();

                try {
                    analyseImage(MediaStore.Images.Media.getBitmap(getContentResolver() , imageUri));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void analyseImage(final Bitmap bitmap) {
        if (bitmap == null){
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            return;
        }

        imageView.setImageBitmap(null);
        faceDetectionModelArrayList.clear();

        recyclerView.getAdapter().notifyDataSetChanged();               //RecyclerView rebuilds everything because everything above has been cleared!
        //Notifying the adapter about the changes in the recycler View

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); //because at this point we don't really have anything to show hence the view is collapsed

        showProgress();

        FirebaseVisionImage firebaseVisionImage =  FirebaseVisionImage.fromBitmap(bitmap); // transforming images into something that the vision api can process into

        FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS).build();


        FirebaseVisionFaceDetector faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options); // face detector to detect face and passing options

        faceDetector.detectInImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
            @Override
            public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                //The firebaseVisionFaces returns the number of faces detected inside the image that has been passed!
                // all these images will be in the form of coordinates and numbers and bits so we create a bitmap that can be mutated and we can see actual faces and images
                Bitmap mutableImage = bitmap.copy(Bitmap.Config.ARGB_8888 , true);

                detectFaces(firebaseVisionFaces , mutableImage);    //method that will detect faces by using canvas that will draw faces on screen
                hideProgress();

                imageView.setImageBitmap(mutableImage);

                recyclerView.getAdapter().notifyDataSetChanged();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "There was an error", Toast.LENGTH_SHORT).show();
                hideProgress();
            }
        });


    }

    private void detectFaces(List<FirebaseVisionFace> firebaseVisionFaces, Bitmap bitmap) {
        if (firebaseVisionFaces == null || bitmap == null){
            Toast.makeText(this, "Error !", Toast.LENGTH_SHORT).show();
            return;
        }

        Canvas canvas = new Canvas(bitmap);   //Canvas is a tool that is used to draw something on the app screen and hence we pass bitmap that needs to be drawn on the screen

        Paint facePaint = new Paint();  // simple pencil to draw the face
        facePaint.setColor(Color.GREEN);
        facePaint.setStyle(Paint.Style.STROKE);   //setting the style of facePaint
        facePaint.setStrokeWidth(5f);  //setting the width of the tip or point of the pencil

        Paint faceTextPaint = new Paint();
        faceTextPaint.setColor(Color.BLUE);
        faceTextPaint.setTextSize(30f);
        faceTextPaint.setTypeface(Typeface.SANS_SERIF);

        Paint faceLandmarkPaint = new Paint();                    //Landmarks are for detecting actual features on the face such as eyes, nose, ears etc
        faceLandmarkPaint.setColor(Color.RED);
        faceLandmarkPaint.setStyle(Paint.Style.FILL);
        faceLandmarkPaint.setStrokeWidth(8f);


        for (int i = 0 ; i < firebaseVisionFaces.size() ; i++){
            canvas.drawRect(firebaseVisionFaces.get(i).getBoundingBox() , facePaint);
            canvas.drawText("Face " + i ,(firebaseVisionFaces.get(i).getBoundingBox().centerX()- (firebaseVisionFaces.get(i).getBoundingBox().width() /2) + 8f )
                    , (firebaseVisionFaces.get(i).getBoundingBox().centerY() + (firebaseVisionFaces.get(i).getBoundingBox().height() /2 ) - 8f ) , facePaint );

            FirebaseVisionFace face = firebaseVisionFaces.get(i);  //getting one face at a time and drawing it with the help of canvas
            if ( face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE) != null){
                FirebaseVisionFaceLandmark leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);

                if (leftEye == null){
                    Toast.makeText(this, "Left eye object is null !", Toast.LENGTH_SHORT).show();
                    return;
                }
                canvas.drawCircle(leftEye.getPosition().getX() , leftEye.getPosition().getY() , 8f , faceLandmarkPaint);
            }
            if ( face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE) != null){
                FirebaseVisionFaceLandmark RIGHT_EYE = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);

                if (RIGHT_EYE == null){
                    Toast.makeText(this, "Right Eye object is null !", Toast.LENGTH_SHORT).show();
                    return;
                }
                canvas.drawCircle(RIGHT_EYE.getPosition().getX() , RIGHT_EYE.getPosition().getY() , 8f , faceLandmarkPaint);
            }
            if ( face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE) != null){
                FirebaseVisionFaceLandmark NOSE_BASE = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);

                if (NOSE_BASE == null){
                    Toast.makeText(this, "Nose object is null !", Toast.LENGTH_SHORT).show();
                    return;
                }
                canvas.drawCircle(NOSE_BASE.getPosition().getX() , NOSE_BASE.getPosition().getY() , 8f , faceLandmarkPaint);
            }
            if ( face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR) != null){
                FirebaseVisionFaceLandmark LEFT_EAR = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);

                if (LEFT_EAR == null){
                    Toast.makeText(this, "Nose object is null !", Toast.LENGTH_SHORT).show();
                    return;
                }
                canvas.drawCircle(LEFT_EAR.getPosition().getX() , LEFT_EAR.getPosition().getY() , 8f , faceLandmarkPaint);
            }
            if ( face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR) != null){
                FirebaseVisionFaceLandmark RIGHT_EAR = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR);

                if (RIGHT_EAR == null){
                    Toast.makeText(this, "Nose object is null !", Toast.LENGTH_SHORT).show();
                    return;
                }
                canvas.drawCircle(RIGHT_EAR.getPosition().getX() , RIGHT_EAR.getPosition().getY() , 8f , faceLandmarkPaint);
            }
            if ( face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT) != null && face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM) != null && face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT) != null){
                FirebaseVisionFaceLandmark MOUTH_LEFT = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT);
                FirebaseVisionFaceLandmark MOUTH_BOTTOM = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM);
                FirebaseVisionFaceLandmark MOUTH_RIGHT = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT);

                if (MOUTH_LEFT == null){
                    Toast.makeText(this, "Nose object is null !", Toast.LENGTH_SHORT).show();
                    return;
                }
                canvas.drawLine(MOUTH_LEFT.getPosition().getX() , MOUTH_LEFT.getPosition().getY() , MOUTH_BOTTOM.getPosition().getX() , MOUTH_BOTTOM.getPosition().getY(), faceLandmarkPaint);

                canvas.drawLine(MOUTH_BOTTOM.getPosition().getX() , MOUTH_BOTTOM.getPosition().getY() , MOUTH_RIGHT.getPosition().getX() , MOUTH_RIGHT.getPosition().getY() , faceLandmarkPaint);

                faceDetectionModelArrayList.add(new FaceDetectionModel(i , "Smiling probability : " + face.getSmilingProbability()));
                faceDetectionModelArrayList.add(new FaceDetectionModel(i , "Left eye open probability : " + face.getLeftEyeOpenProbability()));
                faceDetectionModelArrayList.add(new FaceDetectionModel(i , "Right eye open probability : " + face.getRightEyeOpenProbability()));
            }//end of for loop
        }


    }

    private void showProgress() {
        findViewById(R.id.bottom_sheet_button_image).setVisibility(View.GONE);
        findViewById(R.id.bottom_sheet_progress_bar).setVisibility(View.VISIBLE);
    }

    private void hideProgress(){
        findViewById(R.id.bottom_sheet_button_image).setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_sheet_progress_bar).setVisibility(View.GONE);
    }

    @Override
    public void process(@NonNull Frame frame) {

        final int width = frame.getSize().getWidth();
        final int height = frame.getSize().getHeight();

        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder() // this tell the system how the image should be constructed !
                .setWidth(width)
                .setHeight(height)
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setRotation(
                        (facing == Facing.FRONT)
                        ? FirebaseVisionImageMetadata.ROTATION_270 :
                                FirebaseVisionImageMetadata.ROTATION_90
                )
                .build();

        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromByteArray((byte[]) frame.getData(), metadata);

        FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()
                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .build();

        FirebaseVisionFaceDetector faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options);

        faceDetector.detectInImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
            @Override
            public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                imageView.setImageBitmap(null);

                Bitmap bitmap = Bitmap.createBitmap(height , width , Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bitmap);

                Paint dotPaint = new Paint();
                dotPaint.setColor(Color.RED);
                dotPaint.setStyle(Paint.Style.FILL);
                dotPaint.setStrokeWidth(3f);

                Paint linePaint = new Paint();
                linePaint.setColor(Color.GREEN);
                linePaint.setStyle(Paint.Style.STROKE);
                linePaint.setStrokeWidth(2f);

                for (FirebaseVisionFace face : firebaseVisionFaces){
                    List<FirebaseVisionPoint> faceCountors = face.getContour(FirebaseVisionFaceContour.FACE).getPoints();

                    for (int i = 0 ; i < faceCountors.size() ; i++){
                        FirebaseVisionPoint faceContour = null;
                        if (i != faceCountors.size()-1){
                            faceContour = faceCountors.get(i);

                            canvas.drawLine(faceContour.getX() , faceContour.getY() , faceCountors.get(i +1).getX() , faceCountors.get(i + 1).getY(),linePaint);
                        }else {
                            canvas.drawLine(faceContour.getX() , faceContour.getY() , faceCountors.get(0).getX() , faceCountors.get(0).getY(),linePaint);
                        }

                        canvas.drawCircle(faceContour.getX(),faceContour.getY() ,3f , dotPaint);
                    }//End inner loop
                }//End outer loop
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                imageView.setImageBitmap(null);
            }
        });

    }

 /*   @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}