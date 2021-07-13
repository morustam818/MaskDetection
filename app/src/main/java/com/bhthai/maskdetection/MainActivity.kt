package com.bhthai.maskdetection

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.design.activity.RobotActivity
import com.camerakit.type.CameraFacing
import com.softbankrobotics.facemaskdetection.FaceMaskDetection
import com.softbankrobotics.facemaskdetection.capturer.BottomCameraCapturer
import com.softbankrobotics.facemaskdetection.capturer.TopCameraCapturer
import com.softbankrobotics.facemaskdetection.detector.AizooFaceMaskDetector
import com.softbankrobotics.facemaskdetection.utils.OpenCVUtils
import com.softbankrobotics.facemaskdetection.utils.TAG
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : RobotActivity(),RobotLifecycleCallbacks {
    companion object{
        const val CAMERA_PERMISSION_REQUEST_CODE = 1
    }
//    private lateinit var faceMaskDetector: FaceMaskDetector
    private var detection: FaceMaskDetection? = null
    private var detectionFuture: Future<Unit>? = null
    private var shouldBeRecognizing = false
    private val useTopCamera = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        OpenCVUtils.loadOpenCV(this)
//        faceMaskDetector = FaceMaskDetector(this,this)
        if (cameraPermissionAlreadyGranted()){
            QiSDK.register(this,this)
        }else{
            requestPermissionForCamera()
        }
    }


    private fun requestPermissionForCamera() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(
                this,
                R.string.permissions_needed,
                Toast.LENGTH_LONG
            ).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun cameraPermissionAlreadyGranted(): Boolean {
        val resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return resultCamera == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            var cameraPermissionGranted = true

            for (grantResult in grantResults) {
                cameraPermissionGranted = cameraPermissionGranted and
                        (grantResult == PackageManager.PERMISSION_GRANTED)
            }
            if (cameraPermissionGranted) {
                QiSDK.register(this, this)
            } else {
                Toast.makeText(
                    this,
                    R.string.permissions_needed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onRobotFocusGained(qiContext: QiContext?) {
        val detector = AizooFaceMaskDetector(this)
        val capturer = if (useTopCamera) {
            TopCameraCapturer(qiContext!!)
        } else {
            BottomCameraCapturer(this, this)
        }
        detection = FaceMaskDetection(detector,capturer)
        shouldBeRecognizing = true
        btnStart?.setOnClickListener {
            detectionFuture?.requestCancellation()
            startDetecting()
        }
    }

    override fun onRobotFocusLost() {
        Log.w(TAG, "Robot focus lost")
        detectionFuture?.cancel(true)
    }

    override fun onRobotFocusRefused(reason: String?) {
        Log.e(TAG, "Robot focus refused because $reason")
    }

    override fun onResume() {
        super.onResume()
        OpenCVUtils.loadOpenCV(this)
        shouldBeRecognizing = true
    }

    override fun onPause() {
        super.onPause()
        shouldBeRecognizing = false
        detectionFuture?.requestCancellation()
        detectionFuture = null
    }

    override fun onDestroy() {
        super.onDestroy()
        detectionFuture?.requestCancellation()
        QiSDK.unregister(this,this)
    }

    private fun startDetecting() {
        Toast.makeText(
            this,
            "DetectionStarted Please wait",
            Toast.LENGTH_LONG
        ).show()
        ivResult?.visibility = View.GONE
        btnStart?.visibility = View.GONE
        pBar?.visibility = View.VISIBLE
        try {
            detectionFuture = detection?.start { faces ->
                // Filter and sort the faces so that they're left to right and certain enough
                val sortedFaces = faces
//                    .filter { (it.confidence > 0.5) }
                    .sortedBy { -it.bb.left }
                Log.i(TAG, "Filtered faces ${faces.size}, ->  ${sortedFaces.size}")
//            setFaces(sortedFaces)
                // Now update the logic
                val seesWithMask = sortedFaces.any { it.hasMask }
                val seesWithoutMask = sortedFaces.any { !it.hasMask }
                val numPeople = sortedFaces.size
//            updateState(seesWithMask, seesWithoutMask, numPeople)
                Toast.makeText(
                    this,
                    "NumberOfPeopleFound $numPeople",
                    Toast.LENGTH_LONG
                ).show()
                Log.i(TAG, "startDetecting: $seesWithMask $seesWithoutMask $numPeople")
                if (seesWithMask){
                    updateUI(true)
                }else if (seesWithoutMask){
                    updateUI(false)
                }
            }
        }catch (e:Exception){
            Toast.makeText(
                this,
                "FoundException ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
//        detectionFuture?.thenConsume {
//            Log.i(
//                TAG,
//                "Detection future has finished: success=${it.isSuccess}, cancelled=${it.isCancelled}"
//            )
//            if (shouldBeRecognizing) {
//                Log.w(TAG, "Stopped, but it shouldn't have - starting it again")
//                startDetecting()
//            }
//        }
    }
    private fun updateUI(isMaskDetected: Boolean){
        Toast.makeText(
            this,
            "UserHasWearAMask $isMaskDetected",
            Toast.LENGTH_LONG
        ).show()
        if (isMaskDetected){
            pBar?.visibility = View.GONE
            ivResult?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.baseline_done_24))
            ivResult?.visibility = View.VISIBLE
        }else{
            pBar?.visibility = View.GONE
            ivResult?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.baseline_close_24))
            ivResult?.visibility = View.VISIBLE
        }
        btnStart?.visibility = View.VISIBLE
    }
}