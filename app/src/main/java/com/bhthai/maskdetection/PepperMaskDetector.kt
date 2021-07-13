package com.bhthai.maskdetection

import android.util.Log
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.softbankrobotics.facemaskdetection.FaceMaskDetection
import com.softbankrobotics.facemaskdetection.capturer.TopCameraCapturer
import com.softbankrobotics.facemaskdetection.detector.AizooFaceMaskDetector
import com.softbankrobotics.facemaskdetection.utils.TAG

class PepperMaskDetector(qiContext: QiContext, detector: AizooFaceMaskDetector) {

    private var detection: FaceMaskDetection? = null
    private var detectionFuture: Future<Unit>? = null
    private var capturer : TopCameraCapturer? = null

    init {
        capturer = TopCameraCapturer(qiContext)
        detection = FaceMaskDetection(detector,capturer!!)
    }

    private fun startDetecting() {
        detectionFuture = detection?.start { faces ->
            // Filter and sort the faces so that they're left to right and certain enough
            val sortedFaces = faces
                .filter { (it.confidence > 0.5) }
                .sortedBy { -it.bb.left }
            Log.i(TAG, "Filtered faces ${faces.size}, ->  ${sortedFaces.size}")
//            setFaces(sortedFaces)
            // Now update the logic
            val seesWithMask = sortedFaces.any { it.hasMask }
            val seesWithoutMask = sortedFaces.any { !it.hasMask }
            val numPeople = sortedFaces.size
//            updateState(seesWithMask, seesWithoutMask, numPeople)
            Log.i(TAG, "startDetecting: $seesWithMask $seesWithoutMask $numPeople")
        }
        detectionFuture?.thenConsume {
            Log.i(
                TAG,
                "Detection future has finished: success=${it.isSuccess}, cancelled=${it.isCancelled}"
            )
        }
    }
}