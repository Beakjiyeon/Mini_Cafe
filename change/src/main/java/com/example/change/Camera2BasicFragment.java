package com.example.change;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

//import com.example.camera2basic.face.AboutPerson;
//import com.example.camera2basic.face.ExecuteWithFace;
//import com.example.camera2basic.setting.AppSetting;

import com.example.change.face.AboutPerson;
import com.example.change.face.ExecuteWithFace;
import com.example.change.setting.AppSetting;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

//import android.support.v7.app.AlertDialog;


public class Camera2BasicFragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback{

    Button btn; EditText editText; EditText editName;
    View alertView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

//        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        // Fragment Xml 파일의 <RecyclerView> 태그 획득
//        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.menuRecyclerView);

        View view = inflater.inflate(R.layout.fragment_camera2_basic, container, false);

        btn = (Button) view.findViewById(R.id.picture);
        editText = (EditText) view.findViewById(R.id.edit);
//        editName = (EditText) view.findViewById(R.id.edit_name);

        alertView = inflater.inflate(R.layout.alertview_name, null); // 이게 되려나 ?
        //test
//        getEditName(); // 처음에는 잘 뜨는데 다시 누르면 안뜨고 그러지 않았나?
        editName = (EditText) alertView.findViewById(R.id.edit_name);

        showAlertDialog();


        //editText.setHi

        return view;
    }
    //end onCreate view

    public void addTextToEditText(String str){ // 진행사항 출력하는 함수
        String tempStr = editText.getText().toString() + "\n" + str; // 개행하고 붙인다
        editText.setText(tempStr);

/*
        new AlertDialog.Builder(getContext())
                .setTitle("카메라에 얼굴이 나오게 해주세요!")
                .setPositiveButton("네", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
*/

    }
    //end method

    void showAlertDialog(){
        // 대화상자 출력한다
        new AlertDialog.Builder(getContext())
                .setTitle("얼굴 등록 하셨나요?")
                //.setMessage("카메라에 얼굴이 나오게 해주세요!")
                .setPositiveButton("네", new DialogInterface.OnClickListener() {
                    // 등록함
                    public void onClick(DialogInterface dialog, int which) {

                        new AlertDialog.Builder(getContext())
                                .setTitle("카메라에 얼굴이 나오게 해주세요!?")
                                .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        AppSetting.registeredPersonFlag=true;
//                            addTextToEditText("등록했습니다");
                                        takePicture(); // 사진 찍는다 > 콜백3

                                    }
                                }).show();

                    }
                }).setNegativeButton("아뇨", new DialogInterface.OnClickListener(){
            // 등록안함
            public void onClick(DialogInterface dialog, int which) {
                AppSetting.registeredPersonFlag=false;
//                            addTextToEditText("등록안했어요");
                // 대화상자 출력한다
                new AlertDialog.Builder(getContext())
                        .setTitle("이름 등록")
                        .setView(alertView)
                        //.setMessage("10번 사진 찍습니다!")
                        .setPositiveButton("등록", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // 가져온다
                                name = editName.getText().toString();
                                // 이름 받고 이름 토대로 사람 생성
                                new AboutPerson.CreatePersonTask(name,Camera2BasicFragment.this).execute("");

                                new AlertDialog.Builder(getContext())
                                        .setTitle("10번 사진 찍습니다!\n카메라에 얼굴이 나오게 해주세요!")
                                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                                // 10번 스레드
                                                startCaptureThread();
                                            }
                                        }).show();

                            }
                        }).show();
            }

        }).show();

    }

    String name; // 바로 아래서 사용한다 : 대화상자
    // texture view 관련 생명주기 인터페이스 구현
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);

        }// goto open camera

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }// goto confiure transform

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    Handler capturehandler = new Handler();

    public void startCaptureThread(){
        new CaptureThread().start(); // post 라서 스레드 start 가능 ?
    }

    public /*static*/ class CaptureThread extends Thread { // 시간차를 두고 사진 10번 캡처하는 스레드

        @Override
        public void run() {
            super.run();

            for(int i=10;i>0;i--){ //10번 반복

                final int count = i;

                try{
                    sleep(1500);
                }catch (Exception e){Log.e("   thread error","");}

                takePicture();

                capturehandler.post(new Runnable() { // 이 스레드는 단순히 확인용. 지워도 오케
                    @Override
                    public void run() {
                        addTextToEditText(count+""); // 일단 놔둔다
                    }
                });
                //end handler - post

            }
            //end for
            AppSetting.trainRequestFlag = true;
            takePicture();
        }
        //end method
    }
    //end class

    // from: on surface texture available. 뷰 w,h를 전달받는듯 ?
    private void openCamera(int width, int height) {

        // 카메라 퍼미션부터 확인
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestCameraPermission();
            return;
        }

        setUpCameraOutputs(width, height); // 어떤걸 가로,세로 설정하는거지 ? 프리뷰,텍스쳐뷰 등
        configureTransform(width, height);

        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try {

            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
        //end try catch

    }
    //end method

    // from : onSurfaceTextureSizeChanged,  openCamera
    private void configureTransform(int viewWidth, int viewHeight) { // 음...?

        Activity activity = getActivity();

        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        //end if

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
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
        //end if else

        mTextureView.setTransform(matrix);
    }
    //end method

    private ImageReader mImageReader;
    private Handler mBackgroundHandler;
    private int mSensorOrientation; // 카메라 센서 방향
    private static final String TAG = "Camera2BasicFragment";
    private static final int MAX_PREVIEW_WIDTH = 1920; // 누구맘 ?
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private Size mPreviewSize;
    private AutoFitTextureView mTextureView;
    private boolean mFlashSupported;
    private String mCameraId;

    // from: open camera
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {// 매개는 무슨 역할 ?

        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try {

            for (String cameraId : manager.getCameraIdList()) { // 모든 카메라 루프

                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample. 나는 front 만 쓸거다
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) { // front > back 변경
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                // 왜 가장 큰 이미지를 쓰는 건데 ?
                Size largest = Collections.max( // 최대 크기 추출
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());


                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, 2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                // 콜백3에 전달하는 Handler는 무슨 역할을 하나 ?


                // Find out if we need to swap dimension to get the preview size relative to sensor coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION); // 센서 방향 얻
                boolean swappedDimensions = false; // 초기화

                switch (displayRotation) { // 디바이스 방향

                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize); // 화면 크기 ?

                int rotatedPreviewWidth = width; // 매개 width, height
                int rotatedPreviewHeight = height;

                int maxPreviewWidth = displaySize.x; // 디바이스 화면 크기
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) { // 가로,세로 변경되면
                    rotatedPreviewWidth = height; // width <-> height
                    rotatedPreviewHeight = width;

                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                // 최대 크기 벗어나는지 확인 후 조절
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }



                // 적절한 사이즈로 프리뷰 설정 <- 이 부분을 조절해야 하나 ?
                // 프리뷰 크기 == texture 크기
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), // 크기 리스트 전달
                        rotatedPreviewWidth, rotatedPreviewHeight, // 초기 texture 뷰 크기 전달 ?
                        maxPreviewWidth, maxPreviewHeight,  // 최대 크기 전달
                        largest); // 최대 Size 전달
                Log.e("   선택된 카메라 크기", mPreviewSize.getWidth()+"  "+mPreviewSize.getHeight());
                // 선택된 카메라 크기로 모든게 결정된다 아 몰라
//
/*
                mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                        ImageFormat.JPEG, 1);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);
*/

                // 우리가 설정한 크기대로 texture view 크기를 조정하나보다
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio( // 설정한 프리뷰 크기로 texture 크기 변경
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available; // boolean

                mCameraId = cameraId; // 멤버에 카메라 식별자 할당

                return; //끝
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }
    //end method

    // from: set up camera outputs
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, // 초기 texture
                                          int maxWidth, int maxHeight, // 화면 크기
                                          Size aspectRatio) { // 크기목록 중 최대

        // 프리뷰 크기에 대해 큰 크기
        List<Size> bigEnough = new ArrayList<>();
        // 프리뷰 크기에 대해 작은 크기
        List<Size> notBigEnough = new ArrayList<>();

        int w = aspectRatio.getWidth(); // 최대 크기
        int h = aspectRatio.getHeight();

//        Log.e("   화면 크기: ",maxWidth+"  "+maxHeight);
//        Log.e("   camera list length", ""+choices.length); // 카메라 리스트 길이 궁금해서
        for (Size option : choices) {  // 크기 목록 루프

//            Log.e("   카메라 크기: ", option.getWidth()+"  "+option.getHeight());

            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight && // 화면 최대 보다 작고

                    option.getHeight() == option.getWidth() * h / w) { // 가로세로 비율 고려

                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) { // texture 크기 고려

                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
                //end if else
            }
            //end if
        }
        //end for

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());

        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());

        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
        //end if else
//        return choices[0];

//        return Collections.min(Arrays.asList(choices), new CompareSizesByArea()); // test 제일 작은거 반환
    }
    //end method

    // from: set up camera outputs. 콜백3
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {

//            mBackgroundHandler.post( new ImageSaver(reader.acquireNextImage(), mFile)); // 파일에 저장안하니까 수정 !
//            mBackgroundHandler.post(  new ImageSend(reader.acquireNextImage()) );
            mBackgroundHandler.post(  new ImageSend( reader.acquireLatestImage() ) );

        }
    };

    InputStream inputStream;
    Handler handler = new Handler(Looper.getMainLooper());
    private class ImageSend implements Runnable {

        private Image mImage;

        public ImageSend(Image image) {
            mImage = image;
        }
        //end 생성자

        @Override
        public void run() {

            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()]; // capacity()
//            byte[] bytes = new byte[buffer.capacity()]; // capacity()
            buffer.get(bytes);

            // 가설: input stream 은 한 번 사용하면 사라진다. 휘발성.
            // 그러면, byte[] bytes를 전달하며 input stream으로 변환하여 사용하면 어떨까 ?
            inputStream = new ByteArrayInputStream(bytes);

            // 최초 대화상자에서 플래그 설정했다
            if(AppSetting.registeredPersonFlag == true){
                // 등록된 사람이면
                new ExecuteWithFace.DetectAndIdentifyTask(bytes, Camera2BasicFragment.this).execute(inputStream);
            }else{
                // 처음온 사람이면 10번 사진 등록하겠지 최소 등록횟수 찾아봐야 하나 ? 아 정확도 출력하라고..
                new ExecuteWithFace.DetectAndAddFaceTask(bytes,Camera2BasicFragment.this).execute();
            }
/* 지우지마요 */
//            new ExecuteWithFace.DetectAndIdentifyTask(bytes, Camera2BasicFragment.this).execute(inputStream);
//            new ExecuteWithFace.DetectAndAddFaceTask(bytes,Camera2BasicFragment.this).execute();
//            new ExecuteWithFace.AddFaceTask(/*임시*/UUID.fromString("3a7643fe-bf9a-4dcf-8d1f-26abe657b5a9"),
//                    inputStream, /*faces[0], */Camera2BasicFragment.this).execute();

            mImage.close();
        }
        //end method
    }
    //end class

    // from : 콜백3.
    // 이미지 사후 처리 ?
    private /*static*/ class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            // 파일에 저장하나봐
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
    //end class

    // from: set up camera outpus
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            // 대화상자 반환 : 분문 + 버튼1
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }
    //end class

    // from: set up camera output
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow

            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
    //end class

    private static final String FRAGMENT_DIALOG = "dialog";

    // from: open camera
    private void requestCameraPermission() {
        // if else 가 뭘 위한건지는 모르겠다
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // 대화상자 출력
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }
    //end method

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    // from: request caemra permission
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();

            // 대화상자 출력하는듯 ?  :본문 + 버튼2
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)

                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })

                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }
    //end class

    // 자원 점유 관련
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private CameraDevice mCameraDevice;

    //from: open camera
    // 카메라 관련. 콜백1
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release(); // 락을 릴리즈 ?
            mCameraDevice = cameraDevice;

            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    private CaptureRequest mPreviewRequest;

    //from: 콜백1 - on opened  반복적으로 프리뷰 출력한다
    private void createCameraPreviewSession() {

        try {

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            // 버퍼 초기화를 프리뷰 크기에 맞춘다

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() { //콜백1은 익명 객체

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }
                            //end if

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;

                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();

                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                                // 반복해서 프리뷰 출력한다

                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                            //end try catch
                        }
                        //end method

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
            //end create capture session

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //end try catch
    }
    //end method

    //from: create camera review session, unlockFocus,  captureStillPicture
    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {

        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }
    //end method

    @Override  // onclick
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.picture: {
//                new CaptureThread().start(); // 10번 스레드 기동
                editText.setText("");
                takePicture();
                //btn.setEnabled(false);
                //takePicture();
                break;
            }
        }
    }
    //end onclick

    //from : onclick
    private void takePicture() {
        lockFocus();
    }
    //end method

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_PREVIEW = 0;
    private int mState = STATE_PREVIEW;
    private CameraCaptureSession mCaptureSession;

    //from : take picuture
    private void lockFocus() { // 사진 찍는 함수 !

        try {

            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK; // capture 전에 mState 변경 > 콜백2에서 분기

            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // 원래는 listener, handler 모두 null 을 전달했었다

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //end try catch
    }
    //end method

    //from: create capture session
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {

            switch (mState) {

                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }
        //end method process

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };
    //end 콜백2 아니다

    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            // capture 완료하면 호출되는 메소드 ?
            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override // 눈을떠 성공했다잖아
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("사진 capture");
                    Log.d(TAG, mFile.toString());
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    //end

    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }
    //end

    private void unlockFocus() { //capture() !!
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    //end method

    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    //end method

    private static final int STATE_PICTURE_TAKEN = 4;
    private File mFile;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    //end method

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
    //end method

    //from on pause
    private void closeCamera() {

        try {

            mCameraOpenCloseLock.acquire();
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

        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }
    //end method

    private HandlerThread mBackgroundThread;

    //from: on pause
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    //end method

    private void showToast(final String text) {
        final Activity activity = getActivity();

        if (activity != null) {

            activity.runOnUiThread(new Runnable() { // get activity() 해서 run on ui thread()
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
        //end if
    }
    //end method

    public static Camera2BasicFragment newInstance() {
        return new Camera2BasicFragment();
    }
    //end new

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        view.findViewById(R.id.picture).setOnClickListener(this);
//        view.findViewById(R.id.info).setOnClickListener(this);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
    }
    //end on view created

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFile = new File(getActivity().getExternalFilesDir(null), "pic.jpg");
    }
    //end method

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }
    //end on resune

    //from: on resume
    private void startBackgroundThread() {

        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();

        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    //end start background thread

}//end Fragment
