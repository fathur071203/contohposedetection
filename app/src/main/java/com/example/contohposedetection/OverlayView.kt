package com.example.contohposedetection
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.atan2

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    private var databaseReference: DatabaseReference

    init {
        initPaints()
        // Inisialisasi Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference("pose_detection_results")
    }

    private fun initPaints() {
        linePaint.color = ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE
        pointPaint.color = Color.BLACK
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkerResult ->
            for (landmark in poseLandmarkerResult.landmarks()) {
                for (normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                PoseLandmarker.POSE_LANDMARKS.forEach {
                    val startLandmark = poseLandmarkerResult.landmarks().get(0).get(it!!.start())
                    val endLandmark = poseLandmarkerResult.landmarks().get(0).get(it.end())

                    // Draw lines
                    canvas.drawLine(
                        startLandmark.x() * imageWidth * scaleFactor,
                        startLandmark.y() * imageHeight * scaleFactor,
                        endLandmark.x() * imageWidth * scaleFactor,
                        endLandmark.y() * imageHeight * scaleFactor,
                        linePaint
                    )

                    // Calculate and display angles for right and left hands, pinggul, and kaki
                    if (it.start() in 11..15 && it.end() in 13..17) {
                        val angleDegrees = calculateAngle(
                            startLandmark.x(),
                            startLandmark.y(),
                            poseLandmarkerResult.landmarks().get(0).get(it.end()).x(),
                            poseLandmarkerResult.landmarks().get(0).get(it.end()).y()
                        )
                        val  angleName = when (it.end()) {
                            13 -> "Sudut Bahu Kanan"
                            14 -> "Sudut Bahu Kiri"
                            15 -> "Sudut Tangan Kanan"
                            16 -> "Sudut Tangan Kiri"
                            else -> "unow2"
                        }
                        canvas.drawText(
                            "$angleName sudut: ${angleDegrees}°",
                            startLandmark.x() * imageWidth * scaleFactor,
                            startLandmark.y() * imageHeight * scaleFactor,
                            pointPaint
                        )



                    } else if (it.start() in 23..27 && it.end() in 24..28 && it.end() != 25)  {
                        // Calculate angle for pinggul or kaki
                        val angleDegrees = calculateAngle(
                            startLandmark.x(),
                            // ... (gantilah titik titik dengan indeks yang sesuai)
                            startLandmark.y(),
                            poseLandmarkerResult.landmarks().get(0).get(it.end()).x(),
                            poseLandmarkerResult.landmarks().get(0).get(it.end()).y()
                        )
                        val angleName = when (it.end()) {
                            24 -> "Sudut Pinggul Kanan"
                            26 -> "Sudut Pinggul Kiri"
                            25 -> "ini 25"
                            27 -> "Sudut Lutut Kanan"
                            28 -> "Sudut Lutut Kiri"
                            else -> " "
                        }
                        canvas.drawText(
                            "$angleName sudut: ${angleDegrees}°",
                            startLandmark.x() * imageWidth * scaleFactor,
                            startLandmark.y() * imageHeight * scaleFactor,
                            pointPaint
                        )


                    } else if ((it.start() == 32 && it.end() == 33) || (it.start() == 29 && it.end() == 30)) {
                        // Calculate angle for kaki kanan or kaki kiri
                        val angleDegrees = calculateAngle(
                            startLandmark.x(),
                            startLandmark.y(),
                            poseLandmarkerResult.landmarks().get(0).get(it.end()).x(),
                            poseLandmarkerResult.landmarks().get(0).get(it.end()).y()
                        )
                        canvas.drawText(
                            "${it} sudut: ${angleDegrees}°",
                            startLandmark.x() * imageWidth * scaleFactor,
                            startLandmark.y() * imageHeight * scaleFactor,
                            pointPaint
                        )

                        // Simpan sudut ke dalam database

                    }

                }
            }
        }
    }

    private fun calculateAngle(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val angleRadians = atan2(y2 - y1, x2 - x1)
        var angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()
        if (angleDegrees < 0) {
            angleDegrees += 360
        }
        return angleDegrees
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                kotlin.math.min(width * 1f / imageWidth, height * 1f / imageHeight)
            }

            RunningMode.LIVE_STREAM -> {
                kotlin.math.max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }

        // Save results to Firebase Realtime Database
        saveResultsToDatabase(poseLandmarkerResults)

        invalidate()
    }

    private var resultCounter: Int = 1
    private val counterLock = Object()


    private fun saveResultsToDatabase(poseLandmarkerResults: PoseLandmarkerResult) {
        var resultId: String

        synchronized(counterLock) {
            resultId = "${resultCounter++}"
        }

        val resultReference = databaseReference.child(resultId)

        for (landmarkIndex in poseLandmarkerResults.landmarks().indices) {
            val landmark = poseLandmarkerResults.landmarks()[landmarkIndex]

            for (anglePair in PoseLandmarker.POSE_LANDMARKS) {
                if (anglePair!!.end() in setOf(13, 14, 15, 16, 24, 26, 27, 28)) {
                    val startLandmark = landmark[anglePair.start()]
                    val endLandmark = landmark[anglePair.end()]

                    val angleDegrees = calculateAngle(
                        startLandmark.x(),
                        startLandmark.y(),
                        endLandmark.x(),
                        endLandmark.y()
                    )

                    val angleName = when (anglePair.end()) {
                        13 -> "Sudut Bahu Kanan"
                        14 -> "Sudut Bahu Kiri"
                        15 -> "Sudut Tangan Kanan"
                        16 -> "Sudut Tangan Kiri"
                        24 -> "Sudut Pinggul Kanan"
                        26 -> "Sudut Pinggul Kiri"
                        27 -> "Sudut Lutut Kanan"
                        28 -> "Sudut Lutut Kiri"
                        else -> continue
                    }

                    resultReference.child(angleName).setValue(angleDegrees)
                }
            }
        }

    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}
