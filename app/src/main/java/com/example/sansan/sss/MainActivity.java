package com.example.sansan.sss;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sansan.sss.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //    实例变量
    public CountDownTimer countDownTimer;
    public Context context;
    public static final int PHOTO_REQUEST_CAREMA = 1;// 拍照
    public static final int CROP_PHOTO = 2;//照相截图
    public static final int LOCAL_PHOTO = 3;//获取本地图像
    public static final int TURN_AD_PAGE=4;//跳转广告界面
    private Button Bt_takepicture;
    private Button Bt_turn2Ad;
    private Button Bt_compare;
    private ImageView image_show_takepicture;
    private ImageView image_show_localpicture;
    public Facenet facenet;
    public Bitmap bitmap1;
    public Bitmap bitmap2;
    private Uri imageUri;
    public static File tempFile;
    public MTCNN mtcnn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mtcnn = new MTCNN(getAssets());
        facenet = new Facenet(getAssets());
        Bt_takepicture = (Button) findViewById(R.id.bt1);
        Bt_turn2Ad = (Button) findViewById(R.id.bt2);
        Bt_compare = (Button) findViewById(R.id.bt3);
        image_show_takepicture = (ImageView) findViewById(R.id.picture);
        image_show_localpicture = (ImageView) findViewById(R.id.localpicture);
//    按键监听
        Bt_takepicture.setOnClickListener(this);
        Bt_turn2Ad.setOnClickListener(this);
        Bt_compare.setOnClickListener(this);
        image_show_localpicture.setOnClickListener(this);
    }
    //    点击操作
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt1://打开摄像头
                openCamera(this);
                Log.i("Main", "[*] aaa");
                break;
//            case R.id.bt2://跳转广告
//                Intent startSecondActivity = new Intent(MainActivity.this, secondActivity.class);
//                startActivity(startSecondActivity);
//                break;
            case R.id.bt3://对比两张照片
                long t1 = System.currentTimeMillis();
                try {
                    double score = compareFaces();
                    showScore(score, System.currentTimeMillis() - t1);
                } catch (Exception e) {
                    Log.i("Main", "[*]error" + e);
                }
                break;
            case R.id.localpicture://打开本地相册获取照片
                Intent pickLocalPicture = new Intent(Intent.ACTION_PICK, null);
                pickLocalPicture.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(pickLocalPicture, LOCAL_PHOTO);
                break;

        }
    }
    //  获得activityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PHOTO_REQUEST_CAREMA:
                if (resultCode == RESULT_OK) {
                    Intent intent = new Intent("com.android.camera.action.CROP");
                    intent.setDataAndType(imageUri, "image/*");
                    intent.putExtra("scale", true);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, CROP_PHOTO); // 启动裁剪程序
                }
                break;
            case CROP_PHOTO://相机获取图像
                if (resultCode == RESULT_OK) {
                    try {
//                        读到图像
                        bitmap1 = BitmapFactory.decodeStream(getContentResolver()
                                .openInputStream(imageUri));

                        bitmap1=Utils.resize(bitmap1,1000);
                        image_show_takepicture.setImageBitmap(bitmap1);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case LOCAL_PHOTO ://获取本地图像
                if (resultCode == RESULT_OK) {
                    try {
//                        读到图像
                        //bitmap2 = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        bitmap2= MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                        bitmap2=Utils.resize(bitmap2,1000);
                        image_show_localpicture.setImageBitmap(bitmap2);
                    } catch (Exception e) {
                        Log.d("MainActivity","[*]"+e);
                        e.printStackTrace();
                    }
                }


        }
    }
    //打开本地摄像头
    public void openCamera(Activity activity) {
        //獲取系統版本
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        // 激活相机
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 判断存储卡是否可以用，可用进行存储
        if (hasSdcard()) {
            SimpleDateFormat timeStampFormat = new SimpleDateFormat(
                    "yyyy_MM_dd_HH_mm_ss");
            String filename = timeStampFormat.format(new Date());
            tempFile = new File(Environment.getExternalStorageDirectory(),
                    filename + ".jpg");
            if (currentapiVersion < 24) {
                // 从文件中创建uri
                imageUri = Uri.fromFile(tempFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            } else {
                //兼容android7.0 使用共享文件的形式
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(MediaStore.Images.Media.DATA, tempFile.getAbsolutePath());
                //检查是否有存储权限，以免崩溃
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    //申请WRITE_EXTERNAL_STORAGE权限
                    Toast.makeText(this, "请开启存储权限", Toast.LENGTH_SHORT).show();
                    return;
                }
                //TODO else弹出框询问允许是否使用存储
                //
                imageUri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            }
        }
        // 开启一个带有返回值的Activity，请求码为PHOTO_REQUEST_CAREMA
        activity.startActivityForResult(intent, PHOTO_REQUEST_CAREMA);
    }
    //判断sdcard是否被挂载
    public static boolean hasSdcard() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }
    //两个照片对比score
    public double compareFaces(){
        Bitmap bm1=Utils.copyBitmap(bitmap1);
        Bitmap bm2=Utils.copyBitmap(bitmap2);
        Rect rect1=mtcnn.getBiggestFace(bitmap1,40);
        Rect rect2=mtcnn.getBiggestFace(bitmap2,40);
        if (rect1==null) return -1;
        if (rect2==null) return -2;
        //MTCNN检测到的人脸框，再上下左右扩展margin个像素点，再放入facenet中。
        int margin=20; //20这个值是facenet中设置的。自己应该可以调整。
        Utils.rectExtend(bitmap1,rect1,margin);
        Utils.rectExtend(bitmap2,rect2,margin);

        //要比较的两个人脸，加厚Rect
        Utils.drawRect(bitmap1,rect1,1+bitmap1.getWidth()/100 );
        Utils.drawRect(bitmap2,rect2,1+bitmap2.getWidth()/100 );
        //(2)裁剪出人脸(只取第一张)
        Bitmap face1=Utils.crop(bitmap1,rect1);
        Bitmap face2=Utils.crop(bitmap2,rect2);
        //(显示人脸)
        image_show_takepicture.setImageBitmap(bitmap1);
        image_show_localpicture.setImageBitmap(bitmap2);
        //(3)特征提取
        FaceFeature ff1=facenet.recognizeImage(face1);
        FaceFeature ff2=facenet.recognizeImage(face2);
        bitmap1=bm1;
        bitmap2=bm2;
        //(4)比较
        return ff1.compare(ff2);
    }
    //显示两个照片socre
    public void showScore(double score,long time){
        TextView textView=(TextView)findViewById(R.id.textView);
        textView.setText("[*]人脸检测+识别 运行时间:"+time+"\n");
        Log.i("Mainactivity","[*]score="+score);
        if (score<-1e-4){
            if (score<-1.5)textView.append("[*]图二检测不到人脸");
            else textView.append("[*]图一检测不到人脸");
        }else{
            textView.append("[*]二者相似度为:"+score+" [可设为小于1.1为同一个人]");
        }
    }






}







//        b1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Toast.makeText(MainActivity.this, "人脸识别", Toast.LENGTH_SHORT).show();
//            }
//        });

















//        Button b2 = (Button) findViewById(R.id.bt2);
//        b2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent =new Intent(MainActivity.this,secondActivity.class);
//                startActivity(intent);
//            }
//        });
//    }


//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        switch (ev.getAction()){
//            case MotionEvent.ACTION_DOWN:
//                //有按下动作时取消定时
//                if (countDownTimer != null){
//                    countDownTimer.cancel();
//                }
//                break;
//            case MotionEvent.ACTION_UP:
//                //抬起时启动定时
//                startAD();
//                break;
//        }
//        return super.dispatchTouchEvent(ev);
//    }
//    //跳转广告
//    public void startAD() {
//        if (countDownTimer == null) {
//            countDownTimer = new CountDownTimer(advertisingTime, 1000) {
//                @Override
//                public void onTick(long millisUntilFinished) {
//
//                }
//
//                @Override
//                public void onFinish() {
//                    //定时完成后的操作
//                    //跳转到广告页面
//                    startActivity(new Intent(context,secondActivity.class));
//                }
//            };
//            countDownTimer.start();
//        } else {
//            countDownTimer.start();
//        }
//    }
//    @Override
//    protected void onResume() {
//        super.onResume();
//        //显示主界面时启动定时
//        startAD();
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        //主界面不在前台时停止计时
//        if (countDownTimer != null){
//            countDownTimer.cancel();
//        }
////    }
//}
