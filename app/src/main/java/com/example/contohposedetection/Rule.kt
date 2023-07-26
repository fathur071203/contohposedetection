package com.example.contohposedetection

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import kotlinx.coroutines.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import android.graphics.drawable.VectorDrawable;



data class AngleData(
    val sudutBahuKanan: Float,
    val sudutBahuKiri: Float,
    val sudutLututKiri: Float,
    val sudutLututKanan: Float,
    val sudutPinggulKiri: Float,
    val sudutPinggulKanan: Float,
    val sudutTanganKanan: Float,
    val sudutTanganKiri: Float
)

@Composable
fun Dataderajat() {
    val databaseReference = Firebase.database.reference.child("pose_detection_results")
    LaunchedEffect(Unit) {
        val dataListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChildren()) {
                    val childrenSnapshots = snapshot.children
                    childrenSnapshots.forEach { childSnapshot ->
                        val xValue = childSnapshot.key?.toFloatOrNull() ?: 0.0f
                        val angles = AngleData(
                            sudutBahuKanan =  childSnapshot.child("Sudut Bahu Kanan").getValue(Double::class.java)
                                ?.toFloat() ?: 0.0f,
                            sudutBahuKiri =   childSnapshot.child("Sudut Bahu Kiri").getValue(Double::class.java)
                                ?.toFloat() ?: 0.0f,
                            sudutLututKiri = childSnapshot.child("Sudut Lutut Kiri").getValue(Double::class.java)
                                ?.toFloat() ?: 0.0f,
                            sudutLututKanan = childSnapshot.child("Sudut Lutut Kanan").getValue(Double::class.java)
                                ?.toFloat() ?: 0.0f,
                            sudutPinggulKiri = childSnapshot.child("Sudut Pinggul Kiri").getValue(Double::class.java)
                                ?.toFloat() ?: 0.0f,
                            sudutPinggulKanan = childSnapshot.child("Sudut Pinggul Kanan").getValue(Double::class.java)
                                ?.toFloat() ?: 0.0f,
                            sudutTanganKanan =  childSnapshot.child("Sudut Tangan Kanan").getValue(Double::class.java)
                                ?.toFloat() ?: 0.0f,
                            sudutTanganKiri =   childSnapshot.child("Sudut Tangan Kiri").getValue(Double::class.java)
                                ?.toFloat() ?: 0.0f
                        )

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle if there's an error retrieving data from the database
            }
        }
        databaseReference.addValueEventListener(dataListener)

    }




}

object AngleProcessor {
    val angleLiveData: MutableLiveData<String> = MutableLiveData()
    fun processAngles(angles: AngleData, context: Context) {
        var angleResult = ""
        if (angles.sudutTanganKanan in 90.0..180.0) {
            angleResult = "gerakansempurna"
        } else if (angles.sudutTanganKanan in 0.0..90.0) {
            angleResult = "tidaksempurna"
        } else {
            angleResult = "tidakbergerak"
        }
        angleLiveData.postValue(angleResult)
    }
}

