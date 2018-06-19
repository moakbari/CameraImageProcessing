/*
*  ImageProcessing.cpp
*/

#include <jni.h>
#include <opencv2/contrib/detection_based_tracker.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc_c.h>
#include <opencv2/imgproc/imgproc.hpp>
#include "opencv2/highgui/highgui.hpp"

#include <fstream>

#include <string>
#include <vector>

#include <android/log.h>

using namespace std;
using namespace cv;


Mat * mCanny = NULL;

enum Processing_Type_List{
	Calculate_Face_Detection = 0,
	Calculte_Canny = 1,
};

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_com_example_cameraimageprocessing_CameraPreview_nativeCreateFaceHaarClassifier
(JNIEnv * jenv, jclass, jstring jFileName)
{
    const char* jnamestr = jenv->GetStringUTFChars(jFileName, NULL);
    string stdFileName(jnamestr);
    jlong result = 0;
    result = (jlong) new CascadeClassifier(stdFileName);
    return result;
}


JNIEXPORT void JNICALL Java_com_example_cameraimageprocessing_CameraPreview_nativeStartHaarDetection
(JNIEnv* env, jobject thiz,
		jint width, jint height,
		jbyteArray NV21FrameData,
		jintArray outPixels,
		jlong jObjept)
{
	jbyte * pNV21FrameData = env->GetByteArrayElements(NV21FrameData, 0);
	jint * poutPixels = env->GetIntArrayElements(outPixels, 0);

	Mat mResult(height, width, CV_8UC4, (unsigned char *)poutPixels);
	float resizeFactor = 0.2;
	int scaledHeight = round(resizeFactor*height);
	int scaledWidth = round(resizeFactor*width);


	Mat mGrayInp(height, width, CV_8UC1, (unsigned char *)pNV21FrameData);
	//Mat mGray1(round(resizeFactor*height), round(resizeFactor*width), CV_8UC1);
	Mat mGray(scaledHeight, scaledWidth, CV_8UC1);
	resize(mGrayInp, mGray, Size(), resizeFactor, resizeFactor);
	equalizeHist(mGray, mGray);

	vector<Rect> rectFaces;

	((CascadeClassifier*)jObjept)->detectMultiScale(mGray, rectFaces, 1.1, 3, CV_HAAR_FIND_BIGGEST_OBJECT|CV_HAAR_SCALE_IMAGE, Size(30,30));


	Mat mOutput(scaledWidth, scaledWidth, CV_8UC4);

	//resize(mGray, mOutput, mOutput.size(), 0, 0);
	//cvtColor(mOutput, mResult, CV_GRAY2RGBA);

	cvtColor(mGray, mOutput, CV_GRAY2RGBA);
	//for (int i = 0; i < rectFaces.size(); i++){
	if (rectFaces.size() != 0){
		Rect_<int> face= (Rect_<int>) rectFaces[0];
		//Rect_<int> eyeArea((int)(face.x + face.width/9), (int)(face.y + (face.height/4)), (int) (face.width - 2*face.width/8) ,(int)( face.height/3.5));
		Rect_<int> rightEyeArea((int)(face.x+ face.width/8), (int)(face.y + (face.height/4)), (int)(face.width/2.5) ,(int)(face.height/4));
		//face.x = (1/resizeFactor)*face.x;
		//face.y = (1/resizeFactor)*face.y;
		//face.height = (1/resizeFactor)*face.height;
		//face.width = (1/resizeFactor)*face.width;
		//rectangle(mOutput, face, Scalar(0,255,0, 255) ,1);
		rectangle(mOutput, rightEyeArea, Scalar(255,0,0, 255) ,1);
	}
	resize(mOutput, mResult, mResult.size(), 0, 0);

	env->ReleaseByteArrayElements(NV21FrameData, pNV21FrameData, 0);
	env->ReleaseIntArrayElements(outPixels, poutPixels, 0);
}

JNIEXPORT void JNICALL Java_com_example_cameraimageprocessing_CameraPreview_nativeStopHaarDetection
(JNIEnv * jenv, jclass, jlong thiz)
{
       delete (CascadeClassifier*)thiz;
}







JNIEXPORT jlong JNICALL Java_com_example_cameraimageprocessing_CameraPreview_nativeCreateObject
(JNIEnv * jenv, jclass, jstring jFileName)
{
    const char* jnamestr = jenv->GetStringUTFChars(jFileName, NULL);
    string stdFileName(jnamestr);
    jlong result = 0;

    DetectionBasedTracker::Parameters DetectorParams;
	/*DetectorParams.maxObjectSize = 400;
	DetectorParams.maxTrackLifetime = 20;
	DetectorParams.minDetectionPeriod = 7;
	DetectorParams.minNeighbors = 4;
	DetectorParams.minObjectSize = 40;
	DetectorParams.scaleFactor = 1.1;*/

    result = (jlong)new DetectionBasedTracker(stdFileName, DetectorParams);
    return result;
}

JNIEXPORT void JNICALL Java_com_example_cameraimageprocessing_CameraPreview_nativeStartDetection
(JNIEnv * jenv, jclass, jlong thiz)
{
       ((DetectionBasedTracker*)thiz)->run();
}

JNIEXPORT void JNICALL Java_com_example_cameraimageprocessing_CameraPreview_nativeStopDetection
(JNIEnv * jenv, jclass, jlong thiz)
{
       ((DetectionBasedTracker*)thiz)->stop();
}


JNIEXPORT jboolean JNICALL  Java_com_example_cameraimageprocessing_CameraPreview_nativeFaceDetection
(
		JNIEnv* env, jobject thiz,
		jint width, jint height,
		jbyteArray NV21FrameData,
		jintArray outPixels,
		jlong jObjept)
{
	jbyte * pNV21FrameData = env->GetByteArrayElements(NV21FrameData, 0);
	jint * poutPixels = env->GetIntArrayElements(outPixels, 0);

	float resizeFactor = 0.5;
	Mat mGrayInp(height, width, CV_8UC1, (unsigned char *)pNV21FrameData);
	Mat mGray1(round(resizeFactor*height), round(resizeFactor*width), CV_8UC1);
	Mat mGray(round(resizeFactor*height), round(resizeFactor*width), CV_8UC1);
	resize(mGrayInp, mGray1, Size(), resizeFactor, resizeFactor);
	equalizeHist(mGray1, mGray);
	//Mat mRgb;
	//cvtColor(mGray1, mRgb, CV_GRAY2RGBA);
	//Mat mGray;
	//cvtColor(mRgb, mGray, CV_RGBA2GRAY);



	//DetectionBasedTracker detectionTrackerObj(stdFileName, DetectorParams);
	//detectionTrackerObj.run();
	vector<Rect> rectFaces;

	//detectionTrackerObj.process(mGray);
	//detectionTrackerObj.getObjects(rectFaces);
	((DetectionBasedTracker*)jObjept)->process(mGray);
	((DetectionBasedTracker*)jObjept)->getObjects(rectFaces);
	Mat mOutput(height, width, CV_8UC1);
	Mat mResult(height, width, CV_8UC4, (unsigned char *)poutPixels);
	resize(mGray, mOutput, mOutput.size(), 0, 0);
	cvtColor(mOutput, mResult, CV_GRAY2RGBA);
	//Point pt1(140,40);
	//Point pt2(200,140);

	//if (rectFaces.size() != 0){

	//Rect_<int> face(pt1, pt2);
	for (int i = 0; i < rectFaces.size(); i++){
		Rect_<int> face= (Rect_<int>) rectFaces[i];
		face.x = (1/resizeFactor)*face.x;
		face.y = (1/resizeFactor)*face.y;
		face.height = (1/resizeFactor)*face.height;
		face.width = (1/resizeFactor)*face.width;
		Rect_<int> rightEyeArea((int)(face.x+ face.width/8), (int)(face.y + (face.height/4)), (int)(face.width/2.5) ,(int)(face.height/4));
		rectangle(mResult, face, Scalar(0,255,0, 255) ,3);
		rectangle(mResult, rightEyeArea, Scalar(255,0,0, 255) ,3);

	}

	//detectionTrackerObj.stop();

	env->ReleaseByteArrayElements(NV21FrameData, pNV21FrameData, 0);
	env->ReleaseIntArrayElements(outPixels, poutPixels, 0);
	return true;
}

JNIEXPORT jboolean JNICALL  Java_com_example_cameraimageprocessing_CameraPreview_ImageProcessing
(
		JNIEnv* env, jobject thiz,
		jint width, jint height,
		jbyteArray NV21FrameData,
		jintArray outPixels)
{
	jbyte * pNV21FrameData = env->GetByteArrayElements(NV21FrameData, 0);
	jint * poutPixels = env->GetIntArrayElements(outPixels, 0);

	if ( mCanny == NULL )
	{
		mCanny = new Mat(height, width, CV_8UC1);
	}
	Mat mGray(height, width, CV_8UC1, (unsigned char *)pNV21FrameData);
	Mat mResult(height, width, CV_8UC4, (unsigned char *)poutPixels);
	IplImage srcImg = mGray;
	IplImage CannyImg = *mCanny;
	IplImage ResultImg = mResult;
	cvCanny(&srcImg, &CannyImg, 80, 100, 3);
	cvCvtColor(&CannyImg, &ResultImg, CV_GRAY2BGRA);
	env->ReleaseByteArrayElements(NV21FrameData, pNV21FrameData, 0);
	env->ReleaseIntArrayElements(outPixels, poutPixels, 0);
	return true;
}

#ifdef __cplusplus
}
#endif
