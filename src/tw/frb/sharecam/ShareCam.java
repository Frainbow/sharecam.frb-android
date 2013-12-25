package tw.frb.sharecam;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

public class ShareCam {

    private static final String TAG = "ShareCam";
    private Camera mCamera;
    private CameraPreview mPreview;
    private FrameLayout mFrameLayout;

    public byte[] jpeg;

    public ShareCam(Context context) {
        // Create an instance of Camera
        if (checkCameraHardware(context)) {
            mCamera = getCameraInstance();
        }

        // Create our Preview view and set it as the content of our activity.
        if (mCamera != null) {
            mPreview = new CameraPreview(context, mCamera);
            mFrameLayout = (FrameLayout)((MainActivity)context).findViewById(R.id.flCamera);
            mFrameLayout.addView(mPreview);
        }
    }

    public void release() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void takePicture() {
        if (mCamera != null) {
            mCamera.takePicture(null, null, new PictureCallback() {
                public void onPictureTaken(byte[] data, Camera camera) {
                    jpeg = data;
                    mCamera.startPreview();
                }
            });
        }
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
        private static final String TAG = "CameraPreview";
        private SurfaceHolder mHolder;
        private Camera mCamera;
        private Camera.Parameters mParams;
        private List<Camera.Size> sizes;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;
            mParams = mCamera.getParameters();
            sizes = mParams.getSupportedPictureSizes();

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mParams.setPictureSize(sizes.get(sizes.size() - 1).width, sizes.get(sizes.size() - 1).height);
                mCamera.setParameters(mParams);
                mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallback(this);
                mCamera.setDisplayOrientation(90);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null) {
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
            } catch (Exception e) {
              // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setParameters(mParams);
                mCamera.setPreviewDisplay(mHolder);
                mCamera.setPreviewCallback(this);
                mCamera.setDisplayOrientation(90);
                mCamera.startPreview();
            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }

        @Override
        public void onPreviewFrame(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub
            
        }
    }
}
