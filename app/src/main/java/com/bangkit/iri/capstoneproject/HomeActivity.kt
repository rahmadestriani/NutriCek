package com.bangkit.iri.capstoneproject

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bangkit.iri.capstoneproject.databinding.ActivityHomeBinding
import com.bangkit.iri.capstoneproject.ml.MobilenetV110224Quant
import com.bangkit.iri.capstoneproject.ml.TfHubModel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    lateinit var bitmap: Bitmap
    lateinit var photoFile: File

    companion object {
        const val REQUEST_UPLOAD = 100
        const val REQUEST_CAM = 101
        const val FILE_NAME = "file_name"
    }

    //Button Animations
    private val rotateOpen: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.rotate_open_anim
        )
    }
    private val rotateClose: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.rotate_close_anim
        )
    }
    private val fromBottom: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.from_bottom_anim
        )
    }
    private val toBottom: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.to_bottom_anim
        )
    }
    private var clicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = FirebaseFirestore.getInstance()

        val fileName = "models.txt"
        val inputString = application.assets.open(fileName).bufferedReader().use { it.readText() }
        //var modelNames = inputString.split("\n")

        binding.addBtn.setOnClickListener {
            onAddButtonClicked()

        }


        binding.fabCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photoFile = getPhotoFile(FILE_NAME)

            val fileProvider = FileProvider.getUriForFile(
                this,
                "com.bangkit.iri.capstoneproject.fileprovider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

            //opening camera app
            if (intent.resolveActivity(this.packageManager) != null) {
                startActivityForResult(intent, REQUEST_CAM)
            } else {
                Toast.makeText(this, "Camera tidak bisa dibuka", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fabUpload.setOnClickListener {
            var intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"

            startActivityForResult(intent, REQUEST_UPLOAD)
        }

        //PROBLEMNNYA ADA DI SINI :3
        binding.fabSearch.setOnClickListener {
            //val resizedPic = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            //val model = MobilenetV110224Quant.newInstance(this)

            val model = TfHubModel.newInstance(this)

// Creates inputs for reference.
            /*val inputFeature0 = TensorBuffer.createFixedSize(
                intArrayOf(1, 224, 224, 3),
                DataType.UINT8
            )
            val tbuffer = TensorImage.fromBitmap(resizedPic)
            var byteBuffer = tbuffer.buffer
            inputFeature0.loadBuffer(byteBuffer)*/


            val image = TensorImage.fromBitmap(bitmap)

// Runs model inference and gets result.
           /* val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            var maxPredict = getMaxPredict(outputFeature0.floatArray)*/

            val outputs = model.process(image)
            val probability = outputs.probabilityAsCategoryList.maxByOrNull { it.score }

            val prediction = probability!!.label

            binding.tvTitleMain.setText(prediction)

            val energy = findViewById(R.id.energy) as TextView
            val protein = findViewById(R.id.protein) as TextView
            val fats = findViewById(R.id.fats) as TextView
            val carbohydrate = findViewById(R.id.carbohydrate) as TextView
            val VitA = findViewById(R.id.VitA) as TextView
            val VitB1 = findViewById(R.id.VitB1) as TextView
            val VitC = findViewById(R.id.VitC) as TextView

            val docRef = db.collection("nurtisi")
            val query = docRef.whereEqualTo("Name",prediction)
            query.get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        Log.d("exist", "${document.id} => ${document.data}")
                        energy.text = document.getString("Energy")
                        protein.text = document.getString("Protein")
                        fats.text = document.getString("Fats")
                        carbohydrate.text = document.getString("Carbohydrate")
                        VitA.text = document.getString("VitA")
                        VitB1.text = document.getString("VitB1")
                        VitC.text = document.getString("VitC")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("error", "get failed with ", exception)
                }

// Releases model resources if no longer used.
            model.close()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            REQUEST_UPLOAD -> {
                var uri: Uri? = data?.data
                binding.imgMain.setImageURI(data?.data)
                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            }
            REQUEST_CAM -> {
                val takenImage = BitmapFactory.decodeFile(photoFile.absolutePath)
                binding.imgMain.setImageBitmap(takenImage)
                bitmap = takenImage
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    //getphoto
    private fun getPhotoFile(fileName: String): File {
        val storageDirection = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val result = File.createTempFile(fileName, ".jpg", storageDirection)
        return result
    }

    //get max prediction
    private fun getMaxPredict(arr: FloatArray): Int {
        var ind = 0
        var min = 0.0f

        for (i in 0..1000) {
            if (arr[i] > min) {
                min = arr[i]
                ind = i
            }
        }
        return ind
    }

    //BUTTONS
    private fun onAddButtonClicked() {
        setButton(clicked)
        clicked = !clicked

    }

    private fun setButton(clicked: Boolean) {
        if (!clicked) {
            //VISIBLE
            binding.fabCamera.visibility = View.VISIBLE
            binding.fabUpload.visibility = View.VISIBLE
            binding.fabSearch.visibility = View.VISIBLE

            //ANIMATION
            binding.addBtn.startAnimation(rotateOpen)
            binding.fabCamera.startAnimation(fromBottom)
            binding.fabUpload.startAnimation(fromBottom)
            binding.fabSearch.startAnimation(fromBottom)

            //CLICKABLE
            binding.fabCamera.isClickable = true
            binding.fabUpload.isClickable = true
            binding.fabSearch.isClickable = true
        } else {
            //VISIBLE
            binding.fabCamera.visibility = View.INVISIBLE
            binding.fabUpload.visibility = View.INVISIBLE
            binding.fabSearch.visibility = View.INVISIBLE

            //ANIMATION
            binding.addBtn.startAnimation(rotateClose)
            binding.fabCamera.startAnimation(toBottom)
            binding.fabUpload.startAnimation(toBottom)
            binding.fabSearch.startAnimation(toBottom)

            //CLICKABLE
            binding.fabCamera.isClickable = false
            binding.fabUpload.isClickable = false
            binding.fabSearch.isClickable = false
        }
    }

}