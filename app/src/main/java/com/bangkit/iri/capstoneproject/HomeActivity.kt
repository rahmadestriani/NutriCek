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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bangkit.iri.capstoneproject.databinding.ActivityHomeBinding
import com.bangkit.iri.capstoneproject.ml.AutoModel15ClassesTfhub2
import com.google.firebase.firestore.FirebaseFirestore
import org.tensorflow.lite.support.image.TensorImage
import java.io.File

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var bitmap: Bitmap
    private lateinit var photoFile: File

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
                Toast.makeText(this, "Camera Application won't start", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fabUpload.setOnClickListener {
            var intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"

            startActivityForResult(intent, REQUEST_UPLOAD)
        }

        binding.fabSearch.setOnClickListener {

            val model = AutoModel15ClassesTfhub2.newInstance(this)

            val image = TensorImage.fromBitmap(bitmap)
            val outputs = model.process(image)

            val probability = outputs.probabilityAsCategoryList.maxByOrNull { it.score }
            val prediction = probability?.label

            binding.tvTitleMain.text = prediction

            val docRef = db.collection("nurtisi")
            val query = docRef.whereEqualTo("Name", prediction)
            query.get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            Log.d("exist", "${document.id} => ${document.data}")
                            binding.energy.text = document.getString("Energy")
                            binding.protein.text = document.getString("Protein")
                            binding.fats.text = document.getString("Fats")
                            binding.carbohydrate.text = document.getString("Carbohydrate")
                            binding.VitA.text = document.getString("VitA")
                            binding.VitB1.text = document.getString("VitB1")
                            binding.VitC.text = document.getString("VitC")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.d("MyPrediction", "Prediction Failed :  ", e)
                    }
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

    //buttons
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