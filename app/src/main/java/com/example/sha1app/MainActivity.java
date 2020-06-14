package com.example.sha1app;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.media.ExifInterface;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.sha1app.MESSAGE";//extra la codul hash
    public static final String EXTRA_MESSAGE2 = "com.example.sha1app.MESSAGE2";//extra la data
    public static final String EXTRA_MESSAGE3 = "com.example.sha1app.MESSAGE3";//extra atunci cand n a fost selectat nimic
    static final int GALLERY_REQUEST_CODE = 1;
    ImageView imageView;
    TextView sha1image;
    TextView dateText;
    private static final String TAG = "MyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        sha1image = findViewById(R.id.textView2);
        dateText = findViewById(R.id.textView);

    }

//pentru selectarea din galerie a fotografiilor
    public void pickFromGallery(View view){
        Intent intent = new Intent (Intent.ACTION_PICK);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg","image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes);
        startActivityForResult(intent,GALLERY_REQUEST_CODE);
    }
//onActivityResult la selectarea imaginilor
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // codul returneaza RESULT_OK doar daca o imagine este selectata
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode) {
                case GALLERY_REQUEST_CODE:
                    //data.getData returneaza continutul URI pentru imaginea selectata
                    Uri selectedImage = data.getData();
                    String suri = selectedImage.toString();
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };

                    // preia cursorul
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);

                    // se muta cursorul pe primul rand
                    cursor.moveToFirst();

                    //preia indexul coloanei  MediaStore.Images.Media.DATA

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);

                    //preia valoarea String a coloanei

                    String imgDecodableString = cursor.getString(columnIndex);
                    cursor.close();

                    //preia data imaginii
                    final File file = new File(imgDecodableString);


                    //criptarea metadate-lor

                    /*
                    Imaginea este salvata intr-0 variabila de tip bitmap, apoi hashing-ul are loc asupra unui vector care contine
                    valoarea tuturor bitilor din imagine, la care se adauga si data exacta cand a fost realizat fisierul pozei
                     */


                    Bitmap bmp = BitmapFactory.decodeFile(imgDecodableString);

                    int[] pixels = new int[bmp.getHeight()*bmp.getWidth()];
                    bmp.getPixels(pixels,0,bmp.getWidth(),0,0,bmp.getWidth()-1,bmp.getHeight()-1);




                    String date;

                    ExifInterface intf = null;
                    try {
                        intf = new ExifInterface(imgDecodableString);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }

                    if(intf == null) {
                        date = "nu are data";
                    }


                    date = intf.getAttribute(ExifInterface.TAG_DATETIME);

                    String idata1 = encryptThisIntegerArray(pixels);
                    String idata2 = encryptThisString(date);

                    String sha1images = encryptThisString(idata1+idata2);

                    sha1image.setText(sha1images);


                    imageView.setImageBitmap(bmp);
                    dateText.setText("Date: " + date);

                    break;
            }

    }
    //algoritmul de criptare a sirului de intregi
    public static String encryptThisIntegerArray(int[] input)
    {
        try {
            // metoda getInstance() preia algoritmul SHA-1
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            //se converteste sirul de intregi intr-unul de octeti
            ByteBuffer byteBuffer = ByteBuffer.allocate(input.length * 4);
            IntBuffer intBuffer = byteBuffer.asIntBuffer();
            intBuffer.put(input);
            byte[] array = byteBuffer.array();

            // metoda digest() este apelata pentru a calcula hash-ul intrarii
            //si returneaza un sir de octeti
            byte[] messageDigest = md.digest(array);

            // octetii se convertesc intr-o reprezentare cu semn pozitiv
            BigInteger no = new BigInteger(1, messageDigest);

            // amprenta este convertita in caractere ascii
            String hashtext = no.toString(16);

            // se adauga zerouri pentru a completa cei 32 de biti
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            // returneaza codul hash
            return hashtext;
        }

        // pentru preluarea algoritmilor inexistenti
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    //algoritmul de criptare a stringului
    public static String encryptThisString(String input)
    {
        try {
            // metoda getInstance() preia algoritmul SHA-1
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            // metoda digest() este apelata pentru a calcula hash-ul intrarii
            //si returneaza o matrice de biti


            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            // Add preceding 0s to make it 32 bit
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            // return the HashText
            return hashtext;
        }

        // pentru preluarea algoritmilor inexistenti
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    //butonul prin care deschide o noua activitate
    //pentru a transfera codul hash si data imaginii
    public void bluetoothButton(View view){
        Intent intent = new Intent(this,BluetoothAppActivity.class);
        String message = sha1image.getText().toString();
        String message2 = dateText.getText().toString();
        String message3 = "Waiting for data";

        if(dateText.getText().toString().length() > 6)
        {
            message3 = "Data has been collected";

        }

        intent.putExtra(EXTRA_MESSAGE,message);
        intent.putExtra(EXTRA_MESSAGE2,message2);
        intent.putExtra(EXTRA_MESSAGE3, message3);
        startActivity(intent);
    }


}
