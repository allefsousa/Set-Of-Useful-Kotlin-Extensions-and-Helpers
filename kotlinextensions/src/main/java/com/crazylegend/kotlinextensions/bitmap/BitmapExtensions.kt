package com.crazylegend.kotlinextensions.bitmap

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.FileProvider
import com.crazylegend.kotlinextensions.file.getRealPath
import com.crazylegend.kotlinextensions.file.outAsBitmap
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


/**
 * Created by Hristijan on 2/1/19 to long live and prosper !
 */


data class FileAndUri(var file: File?, var path: String?)


@RequiresPermission(allOf = [WRITE_EXTERNAL_STORAGE])
fun Bitmap.createFileFromBitmap(
    mediaDir: String, imageExtension: String,
    compressionFormat: Bitmap.CompressFormat, compressionQuality: Int
): FileAndUri {

    val fileToReturn: File?


    val file: File?
    val uuid = UUID.randomUUID().toString()
    val directory = File(Environment.getExternalStorageDirectory(), mediaDir)


    file = if (directory.exists()) {
        File(directory, uuid.plus(imageExtension))

    } else {
        directory.mkdirs()

        File(directory, uuid.plus(imageExtension))

    }


    /*Single.fromCallable {
        Bitmap()
    }.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess {


    }*/


    val fos = FileOutputStream(file)
    this@createFileFromBitmap.compress(compressionFormat, compressionQuality, fos)
    fos.flush()
    fos.close()

    fileToReturn = file


    return FileAndUri(fileToReturn, fileToReturn.name)

}

@Throws(FileNotFoundException::class, SecurityException::class)
fun Uri.getBitmap(contentResolver: ContentResolver): Bitmap? {
    return MediaStore.Images.Media.getBitmap(contentResolver, this)
}


@RequiresApi(26)
fun Bitmap.toIcon(): Icon = Icon.createWithBitmap(this)

fun Bitmap.toDrawable(resources: Resources) = BitmapDrawable(resources, this)


fun Bitmap.toByteArray(compressFormat: Bitmap.CompressFormat, quality: Int): Single<ByteArray>? {

    val bos = ByteArrayOutputStream()

    return Single.fromCallable {
        this.compress(compressFormat, quality, bos)
        bos.toByteArray()
    }.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).doAfterSuccess {
        bos.flush()
        bos.close()
    }

}

fun Activity.createColoredBitmapFullScreen(color: Int): Bitmap {


    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)

    val width = displayMetrics.widthPixels
    val height = displayMetrics.heightPixels
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint()
    paint.color = color
    paint.style = Paint.Style.FILL
    canvas.drawPaint(paint)


    return bitmap
}


fun Activity.createColoredBitmap(color: Int, width: Int, height: Int): Bitmap {

    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint()
    paint.color = color
    paint.style = Paint.Style.FILL
    canvas.drawPaint(paint)


    return bitmap
}


fun ByteArray.getBitmap(): Single<Bitmap>? {

    return Single.fromCallable {
        BitmapFactory.decodeByteArray(this, 0, this.size)
    }.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread())

}


@Throws(FileNotFoundException::class, IllegalArgumentException::class)
fun Context.getUriForFile(filePath: String, authority: String): Uri? {
    return FileProvider.getUriForFile(this, authority, File(filePath))
}

fun ContentResolver.getBitmap(imageUri: Uri): Bitmap {
    return MediaStore.Images.Media.getBitmap(this, imageUri)
}


/**
 * Resize Bitmap to specified height and width.
 */
fun Bitmap.resize(newWidth: Number, newHeight: Number): Bitmap {
    val width = width
    val height = height
    val scaleWidth = newWidth.toFloat() / width
    val scaleHeight = newHeight.toFloat() / height
    val matrix = Matrix()
    matrix.postScale(scaleWidth, scaleHeight)
    if (width > 0 && height > 0) {
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
    return this
}


/**
 * Mthod to save Bitmap to specified file path.
 */
fun Bitmap.saveFile(path: String) {
    val f = File(path)
    if (!f.exists()) {
        f.createNewFile()
    }
    val stream = FileOutputStream(f)
    compress(Bitmap.CompressFormat.PNG, 100, stream)
    stream.flush()
    stream.close()
}

/**
 * Returns Bitmap Width And Height Presented as a Pair of two Int where pair.first is width and pair.second is height
 */
val Bitmap.size: Pair<Int, Int>
    get() {
        return Pair(width, height)
    }

/**
 * get Pixels from Bitmap Easily
 */
operator fun Bitmap.get(x: Int, y: Int) = getPixel(x, y)


/**
 * set Pixels to Bitmap Easily
 */
operator fun Bitmap.set(x: Int, y: Int, pixel: Int) = setPixel(x, y, pixel)


/**
 * Crop image easily.
 * @param r is the Rect to crop from the Bitmap
 *
 * @return cropped #android.graphics.Bitmap
 */
fun Bitmap.crop(r: Rect) =
    if (Rect(0, 0, width, height).contains(r)) Bitmap.createBitmap(this, r.left, r.top, r.width(), r.height()) else null


/**
 * rotate a Bitmap with a ease
 */
fun Bitmap.rotateTo(angle: Float, recycle: Boolean = true): Bitmap {
    val matrix = Matrix()
    matrix.setRotate(angle)
    val newBitmap = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (recycle && isRecycled.not() && newBitmap != this)
        recycle()
    return newBitmap
}


/**
 * Greyscale the Image.
 */
fun Bitmap.toGrayScale(recycle: Boolean): Bitmap? {
    val ret = Bitmap.createBitmap(width, height, config)
    val canvas = Canvas(ret)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)
    val colorMatrixColorFilter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = colorMatrixColorFilter
    canvas.drawBitmap(this, 0f, 0f, paint)
    if (recycle && !isRecycled && ret != this) recycle()
    return ret
}


/**
 * Blend the Bitmap Corners to Round with Given radius
 */
fun Bitmap.toRoundCorner(radius: Float, borderSize: Float = 0f, @ColorInt borderColor: Int = 0, recycle: Boolean = true): Bitmap {
    val width = width
    val height = height
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val ret = Bitmap.createBitmap(width, height, config)
    val shader = BitmapShader(this, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    paint.shader = shader
    val canvas = Canvas(ret)
    val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
    val halfBorderSize = borderSize / 2f
    rectF.inset(halfBorderSize, halfBorderSize)
    canvas.drawRoundRect(rectF, radius, radius, paint)
    if (borderSize > 0) {
        paint.shader = null
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderSize
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawRoundRect(rectF, radius, radius, paint)
    }
    if (recycle && !isRecycled && ret != this) recycle()
    return ret
}


/**
 * Makes the Bitmap Round with given params
 */
fun Bitmap.toRound(borderSize: Float = 0f, borderColor: Int = 0, recycle: Boolean = true): Bitmap {
    val width = width
    val height = height
    val size = Math.min(width, height)
    val paint = Paint(ANTI_ALIAS_FLAG)
    val ret = Bitmap.createBitmap(width, height, config)
    val center = size / 2f
    val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
    rectF.inset((width - size) / 2f, (height - size) / 2f)
    val matrix = Matrix()
    matrix.setTranslate(rectF.left, rectF.top)
    if (width != height) {
        matrix.preScale(size.toFloat() / width, size.toFloat() / height)
    }
    val shader = BitmapShader(this, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    shader.setLocalMatrix(matrix)
    paint.shader = shader
    val canvas = Canvas(ret)
    canvas.drawRoundRect(rectF, center, center, paint)
    if (borderSize > 0) {
        paint.shader = null
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderSize
        val radius = center - borderSize / 2f
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
    }
    if (recycle && !isRecycled && ret != this) recycle()
    return ret
}

fun Bitmap.toUriJpeg(context: Context, title: String, description: String): Uri {
    val bytes = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 100, bytes)
    val path = MediaStore.Images.Media.insertImage(context.contentResolver, this, title, description)
    return Uri.parse(path)
}

fun Bitmap.toUriPng(context: Context, title: String, description: String): Uri {
    val bytes = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, bytes)
    val path = MediaStore.Images.Media.insertImage(context.contentResolver, this, title, description)
    return Uri.parse(path)
}

fun Bitmap.toUriWebp(context: Context, title: String, description: String): Uri {
    val bytes = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.WEBP, 100, bytes)
    val path = MediaStore.Images.Media.insertImage(context.contentResolver, this, title, description)
    return Uri.parse(path)
}

fun Bitmap.makeCircle(): Bitmap? {
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val color = -0xbdbdbe
    val paint = Paint()
    val rect = Rect(0, 0, width, height)

    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    paint.color = color

    canvas.drawCircle(width.div(2f), height.div(2f), width.div(2f), paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, rect, rect, paint)

    return output
}

@Throws(FileNotFoundException::class)
fun Uri.toBitmap(context: Context): Bitmap {
    return MediaStore.Images.Media.getBitmap(context.contentResolver, this)
}

@Throws(FileNotFoundException::class)
fun Uri.toDrawable(context: Context): Drawable {
    val inputStream = context.contentResolver.openInputStream(this)
    return Drawable.createFromStream(inputStream, this.toString())
}

/**
 * Corrects the rotation of a bitmap based on the EXIF tags in the file as specified by the URI
 */
@SuppressLint("NewApi")
private fun Context.correctBitmapRotation(initialBitmap: Bitmap, inputUri: Uri): Bitmap {
    var bitmap = initialBitmap
    try {
        val exif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val exifInterface = contentResolver.openInputStream(inputUri).use { inputStream ->
                ExifInterface(inputStream)
            }
            exifInterface
        } else {
            ExifInterface(if (inputUri.scheme == "file") inputUri.path else inputUri.getRealPath(this))
        }
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                bitmap = initialBitmap.rotate(90)
                initialBitmap.recycle()
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                bitmap = initialBitmap.rotate(180)
                initialBitmap.recycle()
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                bitmap = initialBitmap.rotate(270)
                initialBitmap.recycle()
            }
        }
        //When choosing from the photos app on my phone it throws a IllegalArgumentException
        //saying that the filename is null.  But in this instance we don't need to change the
        //orientation and the bitmap is not null.
    } catch (e: IOException) {
        e.printStackTrace()
    } catch (e: NullPointerException) {
        e.printStackTrace()
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
    }
    return bitmap
}

/**
 * Saves a bitmap to a file with a certain compression level between 0 and 100.
 */
private fun saveBitmap(outputFile: File, bitmap: Bitmap, compression: Int) {
    var out: FileOutputStream? = null
    try {
        Log.d("ImageFileManipulation", bitmap.toString())
        Log.d("ImageFileManipulation", outputFile.toString())
        out = FileOutputStream(outputFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, compression, out)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            if (out != null) {
                out.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}

/**
 * Rotates a bitmap, creating a new bitmap.  Beware of memory allocations.
 */
fun Bitmap.rotate(degrees: Int): Bitmap {
    val matrix = Matrix()
    val w = width
    val h = height

    matrix.postRotate(degrees.toFloat())

    return Bitmap.createBitmap(this, 0, 0, w, h, matrix, true)
}

private fun lessResolution(context: Context, fileUri: Uri, width: Int, height: Int): Bitmap? {
    var inputStream: InputStream? = null
    try {
        val options = BitmapFactory.Options()

        // First decode with inJustDecodeBounds=true to check dimensions
        options.inJustDecodeBounds = true

        inputStream = context.contentResolver.openInputStream(fileUri)
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream!!.close()

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, width, height)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false

        inputStream = context.contentResolver.openInputStream(fileUri)
        val returnValue = BitmapFactory.decodeStream(inputStream, null, options)
        inputStream!!.close()

        return returnValue

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            inputStream?.close()
        } catch (e: Exception) {
            /*squish*/
        }

    }
    return null
}


/**
 * Decodes an image byte array into a bitmap, sizing it down to be just bigger than [reqWidth] and [reqHeight] while retaining powers of 2 scaling.
 */
fun BitmapFactory_decodeByteArraySized(array: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap {
    val measureOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(array, 0, array.size, measureOptions)
    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(measureOptions, reqWidth, reqHeight)
    }
    return BitmapFactory.decodeByteArray(array, 0, array.size, options)
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {

    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        // Calculate ratios of height and width to requested height and width
        val heightRatio = Math.ceil(height.toDouble() / reqHeight.toDouble()).toInt()
        val widthRatio = Math.ceil(width.toDouble() / reqWidth.toDouble()).toInt()

        // Choose the smallest ratio as inSampleSize value, this will guarantee
        // a final image with both dimensions be just smaller than or equal to the
        // requested height and width.
        inSampleSize = if (heightRatio > widthRatio) heightRatio else widthRatio
    }
    return inSampleSize
}

@Suppress("NOTHING_TO_INLINE")
private inline fun calculateInSampleSizeMax(options: BitmapFactory.Options, maxWidth: Int, maxHeight: Int): Int {
    var inSampleSize = 1

    if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
        // Calculate ratios of height and width to requested height and width
        val heightRatio = Math.ceil(options.outHeight / maxHeight.toDouble()).toInt()
        val widthRatio = Math.ceil(options.outWidth / maxWidth.toDouble()).toInt()

        // Choose the bigger ratio as inSampleSize value, this will guarantee
        // a final image with both dimensions smaller than or equal to the
        // requested height and width.
        inSampleSize = if (heightRatio > widthRatio) heightRatio else widthRatio
    }
    return inSampleSize
}

fun Bitmap.toRoundCorner(radius :Float) :Bitmap? {
    val width = this.width
    val height = this.height
    val bitmap = Bitmap.createBitmap(width, height, this.config)
    val paint = Paint()
    val canvas = Canvas(bitmap)
    val rect = Rect(0, 0, width, height)

    paint.isAntiAlias = true
    canvas.drawRoundRect(RectF(rect), radius, radius, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, rect, rect, paint)

    this.recycle()
    return bitmap
}

fun Context.saveBitmapToFile(bitmap :Bitmap) :File? {
    val file = getOutputMediaFile()
    file.outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }
    return file
}

fun File.saveBitmapToFile(bitmap :Bitmap) :File? {
    this.outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }
    return this
}

private fun Context.getOutputMediaFile() :File {
    val picName = UUID.randomUUID().toString().replace("-".toRegex(), "") + ".jpg"
    val folder = this.getExternalFilesDir(null)
    if(!folder.isDirectory) {
        folder.mkdirs()
    }

    return File(folder, picName)
}

fun drawableToBitmap(drawable :Drawable) :Bitmap {
    if(drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    var width = drawable.intrinsicWidth
    width = if(width > 0) width else 1
    var height = drawable.intrinsicHeight
    height = if(height > 0) height else 1
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}

fun Context.requestMediaScanner(url :String) {
    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    val contentUri = Uri.fromFile(File(url))
    mediaScanIntent.data = contentUri
    this.sendBroadcast(mediaScanIntent)
}

fun downloadBitmap(imageUrl :String) :Bitmap? {
    var bitmap :Bitmap? = null
    val url = URL(imageUrl)
    val conn = url.openConnection() as HttpURLConnection

    if(conn.responseCode == HttpURLConnection.HTTP_OK) {
        bitmap = conn.inputStream.outAsBitmap()
    }
    conn.disconnect()

    return bitmap
}

fun Bitmap.resize(width :Int, height :Int, mode :ResizeMode = ResizeMode.AUTOMATIC, isExcludeAlpha :Boolean = false) :Bitmap {
    var mWidth = width
    var mHeight = height
    var mMode = mode
    val sourceWidth = this.width
    val sourceHeight = this.height

    if(mode == ResizeMode.AUTOMATIC) {
        mMode = calculateResizeMode(sourceWidth, sourceHeight)
    }

    if(mMode == ResizeMode.FIT_TO_WIDTH) {
        mHeight = calculateHeight(sourceWidth, sourceHeight, width)
    } else if(mMode == ResizeMode.FIT_TO_HEIGHT) {
        mWidth = calculateWidth(sourceWidth, sourceHeight, height)
    }
    val config = if(isExcludeAlpha) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
    return Bitmap.createScaledBitmap(this, mWidth, mHeight, true).copy(config, true)
}

private fun calculateResizeMode(width :Int, height :Int) :ResizeMode =
        if(ImageOrientation.getOrientation(width, height) === ImageOrientation.LANDSCAPE) {
            ResizeMode.FIT_TO_WIDTH
        } else {
            ResizeMode.FIT_TO_HEIGHT
        }

private fun calculateWidth(originalWidth :Int, originalHeight :Int, height :Int) :Int = Math.ceil(originalWidth / (originalHeight.toDouble() / height)).toInt()
private fun calculateHeight(originalWidth :Int, originalHeight :Int, width :Int) :Int = Math.ceil(originalHeight / (originalWidth.toDouble() / width)).toInt()
enum class ResizeMode {
    AUTOMATIC, FIT_TO_WIDTH, FIT_TO_HEIGHT, FIT_EXACT
}

private enum class ImageOrientation {
    PORTRAIT, LANDSCAPE;

    companion object {
        fun getOrientation(width :Int, height :Int) :ImageOrientation =
                if(width >= height) ImageOrientation.LANDSCAPE else ImageOrientation.PORTRAIT
    }
}