package com.app.tryoutfurniture

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.PixelCopy
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.ar.sceneform.ArSceneView
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class PhotoSaver(private val activity : Activity) {

    private fun generateFileName() : String?{
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.absolutePath +
                "/TryOutFurniture/${date}_screenshot.jpg"
    }

    private fun saveImageBitmap(bm:Bitmap,fileName : String){

        val file = File(fileName)
        if(!file.parentFile.exists()){
            file.parentFile.mkdirs()
        }

        try{
            //save bitmap here...
            val outputStream = FileOutputStream(fileName) //to store images
            saveDataToGallery(bm,outputStream)
            //to show files in gallery
            MediaScannerConnection.scanFile(activity, arrayOf(fileName),null,null)
        }
        catch (e: IOException){
            Toast.makeText(activity, "Failed to store bitmap", Toast.LENGTH_SHORT).show()
        }
    }

    //for api level 29 or above
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageBitmap(bm: Bitmap){
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,"${date}_screenshot.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH,"DCIM/TryOutFurniture")
        }

        val uri = activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues)
        activity.contentResolver.openOutputStream(uri ?: return).use {outputStream ->
            outputStream?.let {
                try{
                    saveDataToGallery(bm,it)
                }
                catch (e:IOException){
                    Toast.makeText(activity, "Failed to store bitmap", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveDataToGallery(bm: Bitmap,outputStream: OutputStream){
        val outputData = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG,100,outputData) //store bytes in outputData
        outputData.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
    }

    /*
        The view in our AR scene is a child of Surface view, we can use function pixelCopy() which
        will get pixels fro m surface view to make image and store in gallery
     */
    fun takePhoto(arScene: ArSceneView){

        val bitmap = Bitmap.createBitmap(arScene.width,arScene.height,Bitmap.Config.ARGB_8888)
        val handlerThread = HandlerThread("PixelCopy")
        handlerThread.start()

        //enqueue requests to copy pixels
        PixelCopy.request(arScene, bitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                //screenshot created
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    val fileName = generateFileName()
                    saveImageBitmap(bitmap, fileName ?: return@request)
                } else {
                    saveImageBitmap(bitmap)
                }
                activity.runOnUiThread {
                    Toast.makeText(activity, "Photo saved Successfully", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                activity.runOnUiThread {
                    Toast.makeText(activity, "Failed to take photo", Toast.LENGTH_SHORT).show()
                }
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper)) //looper of handler keep the thread alive
    }


}