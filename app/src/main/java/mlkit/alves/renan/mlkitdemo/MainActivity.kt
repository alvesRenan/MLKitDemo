package mlkit.alves.renan.mlkitdemo

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.TextView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.progressDialog
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() , View.OnClickListener{

    private lateinit var fileFinalPath: File
    private lateinit var uriFinale: Uri
    private lateinit var b: Dialog

    private val permissions = arrayOf(WRITE_EXTERNAL_STORAGE, CAMERA)
    private val PERMISSION_REQ_CODE = 100
    private var isPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnBrowse.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.btnBrowse) {
            openMenu()
        } else if (v?.id == R.id.txtCamera) {
            if (checkPermission()) {
                when {
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.M -> {
                        fileFinalPath = File(Environment.getExternalStorageDirectory(), "Image_${System.currentTimeMillis()}.jpg")

                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(fileFinalPath))
                        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivityForResult(cameraIntent, 2)
                    }
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.N || Build.VERSION.SDK_INT == Build.VERSION_CODES.O -> {
                        fileFinalPath = File(Environment.getExternalStorageDirectory(), "Image_${System.currentTimeMillis()}.jpg")

                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        uriFinale = FileProvider.getUriForFile(applicationContext, "$packageName.provider", fileFinalPath)
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriFinale)
                        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivityForResult(cameraIntent, 2)
                    }
                    else -> {
                        fileFinalPath = File(Environment.getExternalStorageDirectory(), "Image_${System.currentTimeMillis()}.jpg")
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(fileFinalPath))
                        startActivityForResult(cameraIntent, 2)
                    }
                }
            }
        } else if (v?.id == R.id.txtGallery) {
            if (checkPermission()) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    intent.type = "image/*"
                    startActivityForResult(intent, 1)
                } else {
                    startActivityForResult(intent, 1)
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun openMenu() {
        val dialogBuilder = AlertDialog.Builder(this)
                .setTitle("Choose image from:")

        val rv = layoutInflater.inflate(R.layout.choose_from_dialog, null)
        dialogBuilder.setView(rv)

        val textCamera = rv.findViewById<TextView>(R.id.txtCamera)
        val textGallery = rv.findViewById<TextView>(R.id.txtGallery)

        textCamera.setOnClickListener(this)
        textGallery.setOnClickListener(this)

        b = dialogBuilder.create()
        b.show()
    }

    private fun checkPermission() : Boolean {
        var permissionsGranted = false

        if (ActivityCompat.checkSelfPermission(this, this.permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, PERMISSION_REQ_CODE)
            }
        } else {
            permissionsGranted = true
        }

        return permissionsGranted
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQ_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    isPermissionGranted = true
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                        openDialog()
                    } else {
                        if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {

                        } else {
                            val snackbar = Snackbar.make(btnBrowse, "Permission Denied", Snackbar.LENGTH_LONG)
                            snackbar.setAction("Retry") {
                                openSettings()
                            }
                            snackbar.show()
                        }
                    }
                }
        }
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    private fun openDialog() {
        val builder = AlertDialog.Builder(this)
                .setTitle("Need Permissions")
                .setMessage("Please allow this app to use this permission.")
                .setNeutralButton("OK") { click, _ ->
                    click?.dismiss()
                    checkPermission()
                }

        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                1 -> try {
                    b.dismiss()
                    val selectedImage = data!!.data
                    managedImageFromUri(selectedImage)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                2 -> try {
                    b.dismiss()
                    val pictureUri = Uri.fromFile(fileFinalPath)
                    val file = File(pictureUri.path)
                    val path = file.absolutePath
                    val bmOptions = BitmapFactory.Options()
                    val bitmap = BitmapFactory.decodeFile(path, bmOptions)
                    imgPreview.setImageBitmap(bitmap)
                    onDeviceRecognizeText(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun managedImageFromUri(selectedImage: Uri?) {
        try {
            val bitmap: Bitmap? = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImage)
            imgPreview.setImageBitmap(bitmap)
            onDeviceRecognizeText(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onDeviceRecognizeText(bitmap: Bitmap?) {
        val image = FirebaseVisionImage.fromBitmap(bitmap!!)
        val detector = FirebaseVision.getInstance().visionTextDetector

        val progress = progressDialog("Processing the image...")
        progress.show()

        detector.detectInImage(image)
                .addOnCompleteListener { it ->
                    var detectedText = ""
                    var countMatches = 0
                    val pattern = Pattern.compile("(^[a-zA-Z]{3}-? ?\\d{4}\$)")

                    it.result.blocks.forEach {
                        it.lines.forEach {
                            for (i in it.elements.indices) {
                                val matcher = pattern.matcher(it.elements[i].text)
                                if (matcher.find()) {
                                    countMatches += 1
                                    detectedText += "${matcher.group()}\n"
                                }

                                // more details about the text
//                                detectedText += if (matcher.find()) {
//                                    "Placa detectada = ${matcher.group()}\n"
//                                } else {
//                                    "Texto detectado = ${it.elements[i].text}\n"
//                                }
                            }
                        }
                    }

                    runOnUiThread {
                        txtOutput.text = "Recorrências de placas na imagem: $countMatches\n Placas detectadas:\n $detectedText"
                        progress.dismiss()
                    }
                }
                .addOnFailureListener {
                    runOnUiThread {
                        progress.dismiss()
                        alert("Something went wrong", "=(").show()
                    }
                }
    }

}
