package com.ctrl.myseetaface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.SeetaRect;
import com.seeta.sdk.util.SeetaHelper;
import com.seeta.sdk.util.SeetaUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CompareTwoFaceActivity extends AppCompatActivity {
    private final String TAG = "chy";
    private static final String TAG_PREVIEW = "预览";
    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 270);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 90);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private String mCameraId;

    private Size mPreviewSize;

    private ImageReader mImageReader;

    private CameraDevice mCameraDevice;

    private CameraCaptureSession mCaptureSession;

    private CaptureRequest mPreviewRequest;

    private CaptureRequest.Builder mPreviewRequestBuilder;

    private TextureView ttv;

    private Surface mPreviewSurface;
    private ImageView mImage;
    private Button mCheck;
    private ProgressDialog pd;
    private Bitmap mudelBitmap;
    private Bitmap bitmap;

    private SeetaImageData seetaImageData;
    private SeetaRect[] seetaRects;
    private boolean hasFace = false;
    private ArrayList<Float> similarity = new ArrayList<>();
    private SeetaHelper mSeetaHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_two_face);
        ttv = findViewById(R.id.ttv);
        mImage = findViewById(R.id.img);
        mCheck = findViewById(R.id.btn_check);
        ttv.setSurfaceTextureListener(textureListener);
        pd = new ProgressDialog(this);
        pd.setTitle("提示");
        pd.setMessage("正在初始化...");
        pd.setCanceledOnTouchOutside(false);
        showDia();
        new Thread(new Runnable() {
            @Override
            public void run() {
                mSeetaHelper = SeetaHelper.getInstance();
                initCheckModel();
                cancelDia();
            }
        }).start();
        mCheck.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                capture();
            }
        });

    }

    //1.创建对比对象
    private void initCheckModel() {
        mudelBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.chy);
        discernTheFace();

    }

    //检测模板中人脸
    private void discernTheFace() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                seetaImageData = SeetaUtil.ConvertToSeetaImageData(mudelBitmap);
                seetaRects = mSeetaHelper.faceDetector2.Detect(seetaImageData);
                if (null == seetaRects || seetaRects.length == 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(CompareTwoFaceActivity.this, "没有检测到人脸1", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.i("detectFace", "没有检测到人脸");
                    hasFace = false;
                }
                hasFace = true;
            }
        }).start();
    }

    //对比校验
    private void compare2Image() {
        similarity.clear();
        SeetaImageData seetaImageData2 = SeetaUtil.ConvertToSeetaImageData(bitmap);
        SeetaRect[] seetaRects2 = mSeetaHelper.faceDetector2.Detect(seetaImageData2);
        if (null == seetaRects2 || seetaRects2.length == 0) {
            Toast.makeText(this, "没有检测到人脸2", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.e("chy", "到校验了!!");
        //对比模型数据
        SeetaPointF[] seetaPoints = mSeetaHelper.pointDetector2.Detect(seetaImageData, seetaRects[0]);
        Log.e("chy", "seetaPoints:" + seetaPoints.length);
        //照片数据
        SeetaPointF[] seetaPoints2 = mSeetaHelper.pointDetector2.Detect(seetaImageData2, seetaRects2[0]);
        Log.e("chy", "seetaPoints2:" + seetaPoints2.length);
        float sim = mSeetaHelper.faceRecognizer2.Compare(seetaImageData, seetaPoints, seetaImageData2, seetaPoints2);
        Log.e("chy", "sim:" + sim);
        similarity.add(sim);
        Log.e("chy", "similarity.size:" + similarity.size());
//        cancelDia();
        if (similarity.size() > 0) {
            if (similarity.get(0) > 0.7) {
                Toast.makeText(CompareTwoFaceActivity.this, "验证通过", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(CompareTwoFaceActivity.this, "验证失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(CompareTwoFaceActivity.this, "验证失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDia() {
        if (pd != null && !pd.isShowing()) {
            pd.show();
        }
    }

    private void cancelDia() {
        if (pd != null && pd.isShowing()) {
            pd.dismiss();
        }
    }

    //    <------------------------Camera2的相关操作---------------------------------------------------------------------------->
// Surface状态回调
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
            configureTransform(width, height);
            openCamera();
        }


        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    // 摄像头状态回调
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            //开启预览
            startPreview();
        }


        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.i(TAG, "CameraDevice Disconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "CameraDevice Error");
        }
    };
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {

        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPause() {
        closeCamera();
        super.onPause();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupCamera(int width, int height) {
        // 获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // 遍历所有摄像头
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                // 默认打开后置摄像头 - 忽略前置摄像头
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;
                // 获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraId = cameraId;
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openCamera() {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //检查权限
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //打开相机，第一个参数指示打开哪个摄像头，第二个参数stateCallback为相机的状态回调接口，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
//            manager.openCamera(mCameraId, stateCallback, null);
            manager.openCamera("1", stateCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeCamera() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == ttv || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        ttv.setTransform(matrix);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startPreview() {
        setupImageReader();
        SurfaceTexture mSurfaceTexture = ttv.getSurfaceTexture();
        //设置TextureView的缓冲区大小
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //获取Surface显示预览数据
        mPreviewSurface = new Surface(mSurfaceTexture);
        try {
            getPreviewRequestBuilder();
            //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，
            // 第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，
            // 第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mCaptureSession = session;
                    repeatPreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void repeatPreview() {
        mPreviewRequestBuilder.setTag(TAG_PREVIEW);
        mPreviewRequest = mPreviewRequestBuilder.build();
        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mCaptureSession.setRepeatingRequest(mPreviewRequest, mPreviewCaptureCallback, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setupImageReader() {
        //前三个参数分别是需要的尺寸和格式，最后一个参数代表每次最多获取几帧数据
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 1);
        }
        //监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
        //该监听只会在拍照的时候进行回调!!!
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.i(TAG, "Image Available!");
                Image image = reader.acquireLatestImage();
                Toast.makeText(getBaseContext(), "验证开始", Toast.LENGTH_LONG).show();
                // 开启线程异步保存图片
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ByteBuffer buffer = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                            buffer = image.getPlanes()[0].getBuffer();
                        }
                        byte[] data = new byte[buffer.remaining()];
                        Log.e("chy", "该加载了!!");
                        buffer.get(data);
                        File imageFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/myPicture.jpg");


                        FileOutputStream fos = null;
                        FileInputStream fis = null;
                        try {
                            fos = new FileOutputStream(imageFile);
                            fos.write(data, 0, data.length);
                            fis = new FileInputStream(imageFile);
                            bitmap = BitmapFactory.decodeStream(fis);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e("chy", "马上就到校验了!!");
//                                    Glide.with(getBaseContext()).load(imageFile).into(mImage);
//                                    mImage.setVisibility(View.VISIBLE);
//                                    ttv.setVisibility(View.GONE);
                                    compare2Image();
                                }
                            });
                            image.close();

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (fis != null) {
                                try {
                                    fis.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }).start();

            }
        }, null);
    }

    // 选择sizeMap中大于并且最接近width和height的size
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }


    // 创建预览请求的Builder（TEMPLATE_PREVIEW表示预览请求）
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getPreviewRequestBuilder() {
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //设置预览的显示界面
        mPreviewRequestBuilder.addTarget(mPreviewSurface);
        MeteringRectangle[] meteringRectangles = mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AF_REGIONS);
        if (meteringRectangles != null && meteringRectangles.length > 0) {
            Log.d(TAG, "PreviewRequestBuilder: AF_REGIONS=" + meteringRectangles[0].getRect().toString());
        }
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
    }

    // 拍照
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void capture() {
        try {
            //首先我们创建请求拍照的CaptureRequest
            final CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //获取屏幕方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mCaptureBuilder.addTarget(mPreviewSurface);
                mCaptureBuilder.addTarget(mImageReader.getSurface());
                //设置拍照方向
                mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
                //停止预览
                mCaptureSession.stopRepeating();
                //开始拍照，然后回调上面的接口重启预览，因为mCaptureBuilder设置ImageReader作为target，
                // 所以会自动回调ImageReader的onImageAvailable()方法保存图片
                CameraCaptureSession.CaptureCallback captureCallback = null;
                captureCallback = new CameraCaptureSession.CaptureCallback() {

                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        repeatPreview();
                    }
                };
                mCaptureSession.capture(mCaptureBuilder.build(), captureCallback, null);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static class ImageSaver implements Runnable {
        private Image mImage;
        public File imageFile;

        public ImageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            ByteBuffer buffer = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                buffer = mImage.getPlanes()[0].getBuffer();
            }
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            imageFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/myPicture.jpg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(imageFile);
                fos.write(data, 0, data.length);


            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}