package com.yricky.camera_base.activity

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.StateCallback
import android.media.CamcorderProfile
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.view.SurfaceView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.yricky.camera_base.R
import com.yricky.camera_base.utils.getCameraManager
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * @author Yricky
 * @date 2021/12/4
 */
class CameraActivity:AppCompatActivity() {
    companion object{
        val executor by lazy{
            ThreadPoolExecutor(1,8,10L, TimeUnit.MILLISECONDS, LinkedBlockingQueue(12))
        }
    }
    var mediaRecorder:MediaRecorder? = null

    lateinit var surfaceView: SurfaceView

    var cameraInst:CameraDevice? = null
    var useBackCam = false
    var rec = false
    var picView:ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                R.layout.activity_camera_h
            }else{
                R.layout.activity_camera_v
            })
        surfaceView = findViewById(R.id.sfv_preview)
        findViewById<ImageView>(R.id.btn_cap).setOnClickListener {
            //takePicture()
            if(rec)
                stopRec("${System.currentTimeMillis()}.mp4")
            else
                startRec()
        }
        findViewById<ImageView>(R.id.btn_switch).setOnClickListener {
            if(!rec){
                useBackCam = !useBackCam
                prepareCamera()
            }

        }
        picView = findViewById(R.id.btn_pic)
    }

    override fun onResume() {
        super.onResume()
        prepareCamera()
    }


    private fun prepareCamera(){
        val cameraId = if(useBackCam) "0" else "1"
        cameraInst?.close()
        surfaceView.post {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(File(externalCacheDir,"cache.mp4").absolutePath)
                setVideoEncodingBitRate(10000000)
                setVideoFrameRate(30)
                setVideoSize(surfaceView.width, surfaceView.height)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setPreviewDisplay(surfaceView.holder.surface)
                prepare()
            }
            //mediaRecorder.setVideoSize(surfaceView.width,surfaceView.height)
            val targets = listOf(mediaRecorder?.surface)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                finish()
            }else{
                getCameraManager().openCamera(cameraId,object :StateCallback(){
                    override fun onOpened(camera: CameraDevice) {
                        camera.createCaptureSession(targets, object: CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                val captureRequest = session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                captureRequest.addTarget(mediaRecorder!!.surface)
                                session.setRepeatingRequest(captureRequest.build(), null, null)
                            }
                            override fun onConfigureFailed(session: CameraCaptureSession) = Unit
                        }, null)
                        cameraInst = camera

                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        cameraInst = null
                        camera.close()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                    }
                },null)
            }

        }
    }

    private fun takePicture(){
        val imageReader:ImageReader = ImageReader.newInstance(surfaceView.width,surfaceView.height,ImageFormat.JPEG,1)
        imageReader.setOnImageAvailableListener({
            executor.execute {
                val buf = it.acquireNextImage().planes[0].buffer
                val data = ByteArray(buf.remaining())
                buf.get(data)
                val bitmap = BitmapFactory.decodeByteArray(data,0,data.size)
                picView?.post {
                    picView?.setContentAnimate(bitmap)
                }
                FileOutputStream(
                    File(getExternalFilesDir("pic"),"${System.currentTimeMillis()}.png")
                ).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
                }
            }
            prepareCamera()
        },
            Handler(mainLooper)
        )
        cameraInst?.createCaptureSession(listOf(imageReader.surface), object: CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                val captureRequest = session.device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                captureRequest.addTarget(imageReader.surface)
                session.capture(captureRequest.build(),null,null)
            }
            override fun onConfigureFailed(session: CameraCaptureSession) = Unit
        }, null)
    }

    private fun startRec(){
        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        rec = true
        mediaRecorder?.start()

    }

    private fun stopRec(fName:String){
        mediaRecorder?.stop()
        mediaRecorder?.reset()
        rec = false
        File(externalCacheDir,"cache.mp4")
            .renameTo(File(getExternalFilesDir("video"),fName))
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }


    private fun ImageView.setContentAnimate(bitmap:Bitmap){
        scaleX = 0f
        scaleY = 0f
        animate().apply {
            scaleX(1f)
            scaleY(1f)
            start()
        }
        setImageBitmap(bitmap)
    }
}