package com.example.cameraimageprocessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

	private static final int Calculate_Face_Detection = 0;
	private static final int Calculte_Canny = 1;
	private static final int Calculate_Face_Detection2 = 2;
	private static final String TAG = "CameraPreview";
	
	
	private Camera mCamera;
	private SurfaceHolder mHolder;
 	private boolean previewIsRunning;
	private int currentCameraId;
	int numberOfCameras;
	int previewImageFormat;
	private boolean isProcessingUnderway = false;
	private byte[] currentFrameData = null;
	private Size mPreviewSize = null;
	private CameraImageProcessing mainActivityContext;
	private Matrix rotationMatrix;
	private int mProcessingTypeSelected;
	private long mNativeObj = 0;
	private Bitmap mBitmap = null;
	private Bitmap mProcessedBitmap = null;
	int[] mPixels = null;
	
	private long mHaarFaceClassifierNativeobj = 0;
	//private long mHaarRightEyeClassifierNativeobj = 0;

 	Handler mHandler = new Handler(Looper.getMainLooper());

    @SuppressWarnings("deprecation")	
	public CameraPreview(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		Log.i(TAG, "Instantiated new " + this.getClass());
		mainActivityContext = (CameraImageProcessing) context;
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        previewIsRunning = false;
		
        // Find the ID of the default camera
		currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            
        rotationMatrix = new Matrix();
        initializeRotationMatrix();
        //mProcessingTypeSelected = mainActivityContext.getProcessingTypeSelected();
        mNativeObj = 0;
        InitializeDetectionBasedTracker();
           
	}
    
    private File LoadClassifierFile(int rawFileId, String fileName){
        InputStream is = mainActivityContext.getResources().openRawResource(rawFileId);
        File cascadeDir = mainActivityContext.getDir("cascade", Context.MODE_PRIVATE);
        File mCascadeFile = new File(cascadeDir, fileName);
        FileOutputStream os = null;
		try {
			os = new FileOutputStream(mCascadeFile);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        byte[] buffer = new byte[4096];
        int bytesRead;
        try {
			while ((bytesRead = is.read(buffer)) != -1) {
			    try {
					os.write(buffer, 0, bytesRead);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return mCascadeFile;

    }
    
    private void InitializeDetectionBasedTracker(){
    	File mClassifierFileFace =	LoadClassifierFile(R.raw.lbpcascade_frontalface, "lbpcascade_frontalface.xml");
   		mNativeObj = nativeCreateObject(mClassifierFileFace.getAbsolutePath());
   		
    	nativeStartDetection(mNativeObj);
    }
    
    private void CreateHaarClassifier(){
    	File mClassifierFileFace =	LoadClassifierFile(R.raw.haarcascade_frontalface_alt, "haarcascade_frontalface_alt.xml");
    	//File mClassifierFileRightEye =	LoadClassifierFile(R.raw.haarcascade_righteye_2splits, "haarcascade_righteye_2splits.xml");
    	mHaarFaceClassifierNativeobj = nativeCreateFaceHaarClassifier(mClassifierFileFace.getAbsolutePath());
    	//mHaarRightEyeClassifierNativeobj = nativeCreateRightEyeHaarClassifier(mClassifierFileRightEye.getAbsolutePath());
    }
    
    public Size getCameraPreviewSize(){
    	return mPreviewSize;
    }
	
    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            //mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();
        }
    }	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.i(TAG, "surfaceCreated():");
		try{
			mCamera = getCameraInstance(currentCameraId);
			mCamera.setPreviewDisplay(holder);
			mCamera.setPreviewCallback(this);
			setCameraDisplayOrientation((Activity) getContext(), currentCameraId, mCamera);
		} catch(IOException e){
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
		
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "surfaceDestroyed():");
		// TODO Auto-generated method stub
        if (mCamera != null) {
        	mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
	}



	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		Log.i(TAG, "surfaceChanged():");
		try{
			mCamera.setPreviewDisplay(holder);
			mCamera.setPreviewCallback(this);
		} catch(IOException e){
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}

		Parameters parameters = mCamera.getParameters();
		previewImageFormat = parameters.getPreviewFormat();
		mPreviewSize = parameters.getPreviewSize();
		
		if(mBitmap == null){
			mBitmap = Bitmap.createBitmap(mPreviewSize.width, mPreviewSize.height, Bitmap.Config.ARGB_4444);
			mPixels = new int[mPreviewSize.width * mPreviewSize.height];
		}
		
		mCamera.startPreview();
	
	}


	// if this is called in onResume, the surface might not have been created yet
	// so check that the camera has been set up too.
	public void MyStartPreview() {
		// TODO Auto-generated method stub
	    if (!previewIsRunning && (mCamera != null)) {
	    	mCamera.startPreview();
	        previewIsRunning = true;
	    }
	}

	private Camera getCameraInstance(int cId) {
		if (mCamera != null){
			mCamera.release();
			mCamera = null;
		}
		Camera c = null;
		try{
			c = Camera.open(cId);
		}
		catch (Exception e){
			Log.e(getContext().getString(R.string.app_name), "failed to open Camera");
	        e.printStackTrace();
			
		}
		// TODO Auto-generated method stub
		return c;
	}


	public void MyStopPreview() {
		// TODO Auto-generated method stub
		if (previewIsRunning && (mCamera != null)){
			mCamera.stopPreview();
			previewIsRunning = false;
		}
		
	}
	
	public void SwitchCamera(){
		
		if (mCamera != null){
	    	mCamera.setPreviewCallback(null);
	        mCamera.stopPreview();
	        mCamera.release();
	        mCamera = null;
	        previewIsRunning = false;
		}

		switch (currentCameraId){
			case Camera.CameraInfo.CAMERA_FACING_FRONT:
				currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
				break;
			case Camera.CameraInfo.CAMERA_FACING_BACK:
				currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
				break;
			default:
				break;
		}
		try{
			rotationMatrix = new Matrix();
			initializeRotationMatrix();
			mCamera = getCameraInstance(currentCameraId);
			mCamera.setPreviewDisplay(mHolder);
			mCamera.setPreviewCallback(this);
			setCameraDisplayOrientation((Activity) getContext(), currentCameraId, mCamera);
		} catch(IOException e){
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
		MyStartPreview();
	}

	
    public static void setCameraDisplayOrientation(Activity activity,
	         int cameraId, android.hardware.Camera camera) {
	     android.hardware.Camera.CameraInfo info =
	             new android.hardware.Camera.CameraInfo();
	     android.hardware.Camera.getCameraInfo(cameraId, info);
	     int rotation = activity.getWindowManager().getDefaultDisplay()
	             .getRotation();
	     int degrees = 0;
	     switch (rotation) {
	         case Surface.ROTATION_0: degrees = 0; break;
	         case Surface.ROTATION_90: degrees = 90; break;
	         case Surface.ROTATION_180: degrees = 180; break;
	         case Surface.ROTATION_270: degrees = 270; break;
	     }

	     int result;
	     if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	         result = (info.orientation + degrees) % 360;
	         result = (360 - result) % 360;  // compensate the mirror
	     } else {  // back-facing
	         result = (info.orientation - degrees + 360) % 360;
	     }
	     camera.setDisplayOrientation(result);
	 }

   
	@Override
	public void onPreviewFrame(byte[] arg0, Camera arg1) {
		// TODO Auto-generated method stub
		// At preview mode, the frame data will pushed to here.
		if (previewImageFormat == ImageFormat.NV21)
        {
			//We only accept the NV21(YUV420) format.
			if ( !isProcessingUnderway )
			{
				isProcessingUnderway = true;
				currentFrameData = arg0;
				mHandler.post(StartImageProcessing);
				//StartImageProcessing();
			}
        }

	}
	
	private void initializeRotationMatrix(){
	    switch(currentCameraId){
	    	case Camera.CameraInfo.CAMERA_FACING_FRONT:
	    		//rotationMatrix.postRotate(90);
	    		rotationMatrix.preScale(-1, 1);
	    		break;
	    	case Camera.CameraInfo.CAMERA_FACING_BACK:
	    		//rotationMatrix.postRotate(90);
	    		break;
	    	default:
	    		break;
	    }
	}
	
	public boolean IsDetectorRunning(){
		if (mNativeObj == 0){
			return false;
		}
		else{
			return true;
		}
	}
	
	public void StartDetection(){
		
		if (!IsDetectorRunning()){
			InitializeDetectionBasedTracker();			
		}
		
	}
	
	public void StopDetection(){
		
		if (IsDetectorRunning()){
		nativeStopDetection(mNativeObj);
		mNativeObj = 0;
		}
		
	}
	
	public void StartHaarDetection(){
		
		if (mHaarFaceClassifierNativeobj == 0){
			CreateHaarClassifier();			
		}
		
	}
	
	public void StopHaarDetection(){
		
		if (mHaarFaceClassifierNativeobj != 0){
		nativeStopHaarDetection(mHaarFaceClassifierNativeobj);
		mHaarFaceClassifierNativeobj = 0;
		}
		
	}
	//
	// Native JNI 
	//
	
	//public static native long nativeCreateRightEyeHaarClassifier(String cascadeName);
	
	public static native long nativeCreateFaceHaarClassifier(String cascadeName);
	public static native void nativeStartHaarDetection(int width, int height, 
    		byte[] NV21FrameData, int [] pixels, long mHaarFaceClassifierNativeobj);
	public static native void nativeStopHaarDetection(long thiz);
	
    public static native long nativeCreateObject(String cascadeName);
    public static native void nativeStartDetection(long thiz);
    public static native void nativeStopDetection(long thiz);
    public native boolean nativeFaceDetection(int width, int height, 
    		byte[] NV21FrameData, int [] pixels, long mNativeObj);
    public native boolean ImageProcessing(int width, int height, 
    		byte[] NV21FrameData, int [] pixels);
    static
    {
        System.loadLibrary("ImageProcessing");
    }
    
    private Runnable StartImageProcessing = new Runnable() 
    {
       public void run()
    //public void StartImageProcessing()
        {
         if (mPreviewSize != null){
        		Log.i("CameraImageProcessing", "StartImageProcessing():");
        		//Log.i("mPreviewSize.width", Integer.toString(mPreviewSize.width));
        		//Log.i("mPreviewSize.height", Integer.toString(mPreviewSize.height));
        		//Bitmap bitmap = Bitmap.createBitmap(mPreviewSize.width, mPreviewSize.height, Bitmap.Config.ARGB_8888);
        		//Bitmap processedbitmap = Bitmap.createBitmap(mPreviewSize.height, mPreviewSize.width, Bitmap.Config.ARGB_8888);
				//int[] pixels = new int[mPreviewSize.width * mPreviewSize.height];
		        mProcessingTypeSelected = mainActivityContext.getProcessingTypeSelected();
		        switch(mProcessingTypeSelected){
		            case(Calculate_Face_Detection):
		            	nativeFaceDetection(mPreviewSize.width, mPreviewSize.height, currentFrameData, mPixels, mNativeObj);
		                break;
		            case(Calculate_Face_Detection2):
		            	nativeStartHaarDetection(mPreviewSize.width, mPreviewSize.height, currentFrameData, mPixels, mHaarFaceClassifierNativeobj);
		            	break;
		            case(Calculte_Canny):
		    			ImageProcessing(mPreviewSize.width, mPreviewSize.height, currentFrameData, mPixels);
		            	break;
		            default:
		            	break;
		        }

		        mBitmap.setPixels(mPixels, 0, mPreviewSize.width, 0, 0, mPreviewSize.width, mPreviewSize.height);
    			//Log.i("bitmap.width", Integer.toString(bitmap.getWidth()));
        		//Log.i("bitmap.height", Integer.toString(bitmap.getHeight()));

        		//Canvas canvas = new Canvas (processedbitmap);
        		//canvas.drawBitmap(bitmap, matrix, new Paint());
		        mProcessedBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mPreviewSize.width, mPreviewSize.height, rotationMatrix, true);
        		//processedbitmap = bitmap;
    			//Log.i("processedbitmap.width", Integer.toString(processedbitmap.getWidth()));
        		//Log.i("processedbitmap.height", Integer.toString(processedbitmap.getHeight()));
    			mainActivityContext.getProcessedImageHandle().setImageBitmap(mProcessedBitmap);
    			isProcessingUnderway = false;
         	}
        }
    };
}