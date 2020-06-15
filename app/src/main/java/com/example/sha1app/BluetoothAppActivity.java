package com.example.sha1app;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;



public class BluetoothAppActivity extends AppCompatActivity implements AdapterView .OnItemClickListener{
    private static final int CAMERA_REQUEST_CODE = 1;
    BluetoothAdapter bluetoothAdapter;
    private static final String TAG = "MyActivity";

    //variabilele unde salvam numele si MAC-ul dispozitivului cu care se face legatura

    public static TextView btMenu;

    String phoneDeviceMAC;

    Button btnAttachAddress;

    Button btnSendSignature;

    // aceste doua variabile sunt pentru semnatura digitala primita de la pc
    TextView digitalSignature;
    StringBuilder signature;

    private String strDigitalSignature;

    BluetoothConnectionService mBluetoothConnection;

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    //string in care se primeste codul hash si data imaginii
    String dataToSend;

    //acest TextView ne informeaza daca s-a selectat vreo imagine, iar codul hash a fost generat
    TextView sha1BT;

    //UUID-ul generat de pe site
    private static final UUID MY_UUID_INSECURE =

            UUID.fromString("d92d14ef-e714-45d2-9f2d-03ad011befa9");
    BluetoothDevice mBTDevice;
    public DeviceListAdapter mDeviceListAdapter;

    //lista unde apar dispozitivele gasite de bluetooth
    ListView lvNewDevices;

    //Tesing
    private String cameraFilePath;
    private String imgAbsPath;
    //Testing


    // Create a BroadcastReceiver for ACTION_FOUND

    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(bluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */

    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {



        @Override

        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        Toast.makeText(getApplicationContext(),"Connected",Toast.LENGTH_SHORT).show();
                        break;

                }
            }
        }

    };

    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */

    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {

        @Override

        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");
            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }

    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */

    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {

        @Override

        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    Toast.makeText(getApplicationContext(),"Bonded",Toast.LENGTH_SHORT).show();
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;

                }
                //case2: creating a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                    Toast.makeText(getApplicationContext(),"Bonding",Toast.LENGTH_SHORT).show();
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                    Toast.makeText(getApplicationContext(),"Not bonded",Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

/**
    protected void onDestroy() {

        Log.d(TAG, "onDestroy: called.");

        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
        super.onDestroy();

    }
*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_app);

        btMenu = findViewById(R.id.textView4);

        //se instantiaza variabilele pentru semnatura digitala primita de la pc
        digitalSignature = (TextView) findViewById(R.id.digitalSignature);
        signature = new StringBuilder();
        strDigitalSignature = new String("None");

        //intentul prin care se primesc datele despre codul hash
        Intent sha1BTintent = getIntent();
        String message = sha1BTintent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        String message2 = sha1BTintent.getStringExtra(MainActivity.EXTRA_MESSAGE2);
        String message3 = sha1BTintent.getStringExtra(MainActivity.EXTRA_MESSAGE3);
        dataToSend = message + "..." + message2 + readFromFile(this);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);


        btnSendSignature = findViewById(R.id.sendSignature);
        btnAttachAddress = findViewById(R.id.attachMAC);



        registerReceiver(mBroadcastReceiver4, filter);

        //se instantiaza TextView-u care ne arata daca codul hash a fost generat sau nu
        sha1BT = findViewById(R.id.textView3);
        sha1BT.setText(message3);
        if(sha1BT.getText().equals("Data has been collected"))
        {
            sha1BT.setBackgroundColor(Color.GREEN);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,new IntentFilter("incomingMessage"));


        //ListView-u cu dispozitivele decoperite
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        lvNewDevices.setOnItemClickListener(BluetoothAppActivity.this);
        mBTDevices = new ArrayList<>();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    //aici primim mesajele de la pc sau de la alt dispozitiv

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
            signature.append(text);
            digitalSignature.setText(signature.toString());
            if( signature.toString().equals("Valid Signature")) {
                digitalSignature.setBackgroundColor(Color.GREEN);
                //btnSendSignature.setVisibility(View.VISIBLE);
            }
            else
            {
                digitalSignature.setBackgroundColor(Color.RED);
                //btnSendSignature.setVisibility(View.VISIBLE);
            }
        }
    };

    //inceperea conexiunii prin apasarea butonului Start Connection
    public void startc(View view)
    {
        startConnection();
    }

    //trimiterea datelor prin apasarea butonului Send
    public void sendd(View view)
    {
        if(dataToSend == null)
        {
            Toast.makeText(getApplicationContext(),"Please select a photo",Toast.LENGTH_SHORT).show();
        }
        else {
            byte[] bytes = dataToSend.getBytes(Charset.defaultCharset());
            mBluetoothConnection.write(bytes);
            Toast.makeText(getApplicationContext(), "The hash value has been sent", Toast.LENGTH_SHORT).show();
        }
    }


    //create method for starting connection

//***remember the conncction will fail and app will crash if you haven't paired first

    public void startConnection(){
        startBTConnection(mBTDevice,MY_UUID_INSECURE);
    }

    /**
     * starting chat service method
     */

    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");
        mBluetoothConnection.startClient(device,uuid);
        btMenu.setText("Bluetooth Menu - Connected");
        btMenu.setBackgroundColor(Color.GREEN);
    }

    //atunci cand se pasa butonul de pornire sau oprire Bluetooth
    public void enableDisableBT(View view)
    {
        if(bluetoothAdapter == null){
            Toast.makeText(getApplicationContext(),"Nu are capabilități Bluetooth!",Toast.LENGTH_SHORT).show();
        }

        if(!bluetoothAdapter.isEnabled()){
            Toast.makeText(getApplicationContext(),"Activare BT.",Toast.LENGTH_SHORT).show();
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

        if(bluetoothAdapter.isEnabled()){
            Toast.makeText(getApplicationContext(),"Dezactivare BT.",Toast.LENGTH_SHORT).show();
            bluetoothAdapter.disable();
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        }

        //butonul pentru setarea dispozitivului ca fiind descoperibil
    public void btnEnableDisable_Discoverable(View view) {
        Toast.makeText(getApplicationContext(),"Making device discoverable for 300 seconds.",Toast.LENGTH_SHORT).show();
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        IntentFilter intentFilter = new IntentFilter(bluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2,intentFilter);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void btnDiscover(View view) {
        Toast.makeText(getApplicationContext(),"In căutarea dispozitivelor neasociate.",Toast.LENGTH_SHORT).show();

        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Închidere descoperire",Toast.LENGTH_SHORT).show();
            //check BT permissions in manifest
            checkBTPermissions();
            bluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }

        if(!bluetoothAdapter.isDiscovering()){

            //check BT permissions in manifest
            checkBTPermissions();
            bluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     *
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
                    this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
                }
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    //atunci când se apasa pe un dispozitiv descoperit dupa cautare
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //se închide modul de descoperire pentru a reduce consumul de memorie
        bluetoothAdapter.cancelDiscovery();
        Log.d(TAG, "onItemClick: Ai apăsat pe un dispozitiv.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();
        //pentru verificarea semnăturii digitale în cazul unui transfer unui transfer
        phoneDeviceMAC = deviceAddress;
        btnAttachAddress.setVisibility(View.VISIBLE);

        Log.d(TAG, "onItemClick: Numele dispozitivului " + deviceName);
        Log.d(TAG, "onItemClick: Adresa MAC a dispozitivului = " + deviceAddress);
        //se realizează asocierea
        //NOTE: necesită API 17+
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Se încearcă a se asocia cu " + deviceName);
            mBTDevices.get(i).createBond();
            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(BluetoothAppActivity.this);
        }
    }


    //trimiterea semnaturii digitale te la un telefon la altul

    public void sendSignature(View view)
    {
        if(getStrDigitalSignature() == null)
        {
            Toast.makeText(getApplicationContext(),"No signature",Toast.LENGTH_SHORT).show();
        }
        else {
            byte[] bytes = getStrDigitalSignature().getBytes(Charset.defaultCharset());
            mBluetoothConnection.write(bytes);
            Toast.makeText(getApplicationContext(), "The signature has been sent", Toast.LENGTH_SHORT).show();
        }
    }

    //get si set pentru strDigitalSignrature

    public String getStrDigitalSignature()
    {
        return strDigitalSignature;
    }

    public void setStrDigitalSignature(String newSgn)
    {
        strDigitalSignature = newSgn;
    }

    //butonul de intoarcere

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    // IN TESTARE

    public void attachAddress(View view)
    {
        writeToFile("-"+phoneDeviceMAC, this);
        Toast.makeText(getApplicationContext(),"The phone address is attached to hash!",Toast.LENGTH_SHORT).show();

    }

    private void writeToFile(String data,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("address.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String readFromFile(Context context) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("address.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }







    /**Version in test
     * Trying to take a photo with camera
     */

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        //This is the directory in which the file will be created. This is the default location of Camera photos
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for using again
        cameraFilePath = "file://" + image.getAbsolutePath();
        imgAbsPath = image.getAbsolutePath();
        return image;
    }

    public void captureFromCamera(View view) {
        if(checkCameraHardware(this)==true && isStoragePermissionGranted()==true) {
            try {

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", createImageFile()));
                startActivityForResult(intent, CAMERA_REQUEST_CODE);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public  boolean isStoragePermissionGranted()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // codul returneaza RESULT_OK doar daca o imagine este selectata
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode) {
                case CAMERA_REQUEST_CODE:
                    //imageView.setImageURI(Uri.parse(cameraFilePath));
                    Bitmap bmp = BitmapFactory.decodeFile(imgAbsPath);

                    int[] pixels = new int[bmp.getHeight()*bmp.getWidth()];
                    bmp.getPixels(pixels,0,bmp.getWidth(),0,0,bmp.getWidth()-1,bmp.getHeight()-1);

                    dataToSend = null;
                    dataToSend = encryptThisIntegerArray(pixels);
                    sha1BT.setText(dataToSend);

                    byte[] bytes = dataToSend.getBytes(Charset.defaultCharset());
                    mBluetoothConnection.write(bytes);

                    break;
                //testing
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




}





