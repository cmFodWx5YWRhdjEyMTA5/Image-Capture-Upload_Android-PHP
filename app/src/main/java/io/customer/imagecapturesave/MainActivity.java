package io.customer.imagecapturesave;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {
   ImageView imgUser;
   int MY_PERMISSIONS_REQUEST_CAMERA=1111;
   ProgressBar mProgressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imgUser=findViewById(R.id.imgUser);
        mProgressBar=findViewById(R.id.pbProcessing);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},  MY_PERMISSIONS_REQUEST_CAMERA);
                }

            }
        }

    }

    public void launch_cam(View view) {
     dispatchTakePictureIntent();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // main logic
        }
    }

    int REQUEST_IMAGE_CAPTURE=3456;
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,"com.example.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");
            // imgUser.setImageBitmap(imageBitmap);
            setPic();
            galleryAddPic();
            Log.d(TAG, "onActivityResult: "+mCurrentPhotoPath);
        }
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = imgUser.getWidth();
        int targetH = imgUser.getHeight();

        // Get the dimensions of the bitmap
        Log.d(TAG, "setPic: "+mCurrentPhotoPath);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        imgUser.setImageBitmap(bitmap);
    }
    String TAG="IMG_TAG";

    public void uploadPhoto(View view) {
        String url ="https://file-uploads-walteranyika.c9users.io/upload.php";
        AsyncHttpClient client =new AsyncHttpClient();
        RequestParams params=new RequestParams();
        File file=new File(mCurrentPhotoPath);
        try {
           params.put("fileToUpload",file);
           mProgressBar.setVisibility(View.VISIBLE);
           client.post(url, params, new TextHttpResponseHandler() {
               @Override
               public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                   mProgressBar.setVisibility(View.INVISIBLE);

                   Toast.makeText(MainActivity.this, "Failed to communicate with the server", Toast.LENGTH_SHORT).show();
               }

               @Override
               public void onSuccess(int statusCode, Header[] headers, String responseString) {
                   mProgressBar.setVisibility(View.INVISIBLE);

                   Toast.makeText(MainActivity.this, "Server said "+responseString, Toast.LENGTH_SHORT).show();
               }
           });


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "File was not found", Toast.LENGTH_SHORT).show();
        }


    }
    //PHP CODE TO RECEIVE UPLOADED FILE AND SAVE IN FOLDER
    /*<?php
         if($_SERVER['REQUEST_METHOD']=='POST')
         {
                 $target_dir = "uploads/";
                 //$target_file = $target_dir . basename($_FILES["fileToUpload"]["name"]);
                 $path = $_FILES['fileToUpload']['name'];
                 $ext = pathinfo($path, PATHINFO_EXTENSION);
                 $x=rand(100000,10000000);
                 $y=rand(100000,10000000);
                 $new_name=$x."_".$y.".".$ext;
                 $target_file = $target_dir .$new_name;
                 //fill in with correct credentilas

                 if (move_uploaded_file($_FILES["fileToUpload"]["tmp_name"], $target_file))
                 {
                     echo json_encode(array('response'=>'Uploaded Succesfully.'));
                 }else
                 {
                   echo json_encode(array('response'=>'Sorry, there was an error uploading your file.'));
                 }
         }
         else
         {
           echo json_encode(array('response'=>'No Data was sent'));
         }
        ?>*/
}
