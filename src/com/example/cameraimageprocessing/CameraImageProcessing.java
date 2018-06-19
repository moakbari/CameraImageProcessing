package com.example.cameraimageprocessing;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class CameraImageProcessing extends Activity {

	private static final String TAG = "CameraImageProcessing::Activity";
	private static final int Calculate_Face_Detection = 0;
	private static final int Calculte_Canny = 1;
	private static final int Calculate_Face_Detection2 = 2;
//	private static final int Switch_Camera = 2;
	

	private CameraPreview mPreview;
	private FrameLayout mainLayout;
	public ImageView processedFrameView;
	private int mProcessingTypeSelected;
	private String mClassifierPath;
 	
	public CameraImageProcessing() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		// Hide the window title.
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
		setContentView(R.layout.activity_camera_image_processing);
		
	
		
		mPreview = new CameraPreview(this);
		processedFrameView = new ImageView(this);
		mainLayout = (FrameLayout) findViewById(R.id.cameraimageprocessing_preview);
		processedFrameView.setScaleType(ImageView.ScaleType.FIT_XY);
		mainLayout.addView(mPreview);
		mainLayout.addView(processedFrameView);

		mProcessingTypeSelected = 0;
		//Size previewSize = mPreview.getCameraPreviewSize();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_camera_image_processing, menu);
		return true;
	}

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        
        switch (item.getItemId()){
        	case R.id.menu_face_detection:
        		mProcessingTypeSelected =  Calculate_Face_Detection;
        		mPreview.StopHaarDetection();
        		mPreview.StartDetection();
         		break;
        	case R.id.menu_face_detection2:
        		mProcessingTypeSelected =  Calculate_Face_Detection2;
        		mPreview.StopDetection();
        		mPreview.StartHaarDetection();
        		break;
        	case R.id.menu_canny:
        		mProcessingTypeSelected = Calculte_Canny;
        		mPreview.StopDetection();
        		mPreview.StopHaarDetection();
        		break;

        	case R.id.menu_switch_camera:
        		//mItemSelected = Switch_Camera;
       			mPreview.SwitchCamera();
           		break;
        	default:
        		return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    protected void onResume() {
        // Open the default i.e. the first rear facing camera.
        mPreview.MyStartPreview();
        super.onResume();
    }

    @Override
    protected void onPause() {
    	mPreview.MyStopPreview();
        super.onPause();
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
     
        //releaseCamera();
    }
    
    protected void onDestroy(){
    	mPreview.StopDetection();
    	super.onDestroy();
    }
    
    public ImageView getProcessedImageHandle(){
    	return processedFrameView;
    }
    
    public int getProcessingTypeSelected(){
    	return mProcessingTypeSelected;
    }
    
    public String getClassifierPath(){
    	return mClassifierPath;
    }
  
}
