package com.bhthai.maskdetection

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.softbankrobotics.facemaskdetection.FaceMaskDetection
import com.softbankrobotics.facemaskdetection.capturer.BottomCameraCapturer
import com.softbankrobotics.facemaskdetection.detector.AizooFaceMaskDetector

class FaceMaskDetector(private val context: Context,private val lifecycleOwner: LifecycleOwner) {

    private var detector = AizooFaceMaskDetector(context)
    private val capture = BottomCameraCapturer(context,lifecycleOwner)
    private val faceMaskDetection = FaceMaskDetection(detector,capture)

    fun startDetectingFaceMask(){

        faceMaskDetection.start { faces->
            faces.forEach {
                when{
                    (it.confidence < 0.5) && it.hasMask ->{
                        Log.i("FaceMaskDetector", "FaceMaskDetected")
                    }
                    (it.confidence < 0.5) && !it.hasMask ->{
                        Log.i("FaceMaskDetector", "FaceMask Not Found ")
                    }
                }
            }
        }
    }


}