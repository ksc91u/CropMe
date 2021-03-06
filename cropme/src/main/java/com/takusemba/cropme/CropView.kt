package com.takusemba.cropme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import com.bumptech.glide.Glide
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * CropView
 *
 * @author takusemba
 * @since 05/09/2017
 */
class CropView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr), Croppable {

    private var horizontalAnimator: MoveAnimator? = null
    private var verticalAnimator: MoveAnimator? = null
    private var scaleAnimator: ScaleAnimator? = null

    private var actionDetector: ActionDetector? = null

    private val percentWidth: Float
    private val percentHeight: Float
    private val maxScale: Int
    private var restriction: RectF? = null
    private val backgroundAlpha: Int
    private val withBorder: Boolean

    private var uri: Uri? = null
    private var scale: ScaleXY? = null
    private var point: PointF? = null

    var onCropChangeListener: OnCropChangeListener? = null

    var cropChangeSubject: PublishSubject<Pair<Uri, CropInfo>> = PublishSubject.create()
    var disposable: CompositeDisposable = CompositeDisposable()

    init {
        val a = getContext().obtainStyledAttributes(attrs, R.styleable.CropView)

        percentWidth = a.getFraction(R.styleable.CropView_cropme_result_width, DEFAULT_BASE, DEFAULT_PBASE, DEFAULT_PERCENT_WIDTH)
        if (percentWidth < MIN_PERCENT || MAX_PERCENT < percentWidth) {
            throw IllegalArgumentException("sr_result_width must be set from 0% to 100%")
        }

        percentHeight = a.getFraction(R.styleable.CropView_cropme_result_height, DEFAULT_BASE, DEFAULT_PBASE, DEFAULT_PERCENT_HEIGHT)
        if (percentHeight < MIN_PERCENT || MAX_PERCENT < percentHeight) {
            throw IllegalArgumentException("sr_result_height must be set from 0% to 100%")
        }

        maxScale = a.getInt(R.styleable.CropView_cropme_max_scale, DEFAULT_MAX_SCALE)
        if (maxScale < MIN_SCALE || MAX_SCALE < maxScale) {
            throw IllegalArgumentException("sr_max_scale must be set from 1 to 5")
        }

        backgroundAlpha = (a.getFraction(R.styleable.CropView_cropme_background_alpha, DEFAULT_BASE, DEFAULT_PBASE, DEFAULT_BACKGROUND_ALPHA) * COLOR_DENSITY).toInt()
        if (percentWidth < MIN_PERCENT || MAX_PERCENT < percentWidth) {
            throw IllegalArgumentException("sr_background_alpha must be set from 0% to 100%")
        }

        withBorder = a.getBoolean(R.styleable.CropView_cropme_with_border, DEFAULT_WITH_BORDER)

        a.recycle()

        init()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposable.dispose()
    }

    private fun init() {

        startActionDetector()
        addLayouts()

        cropChangeSubject.throttleLast(100, TimeUnit.MILLISECONDS).subscribe { pair ->
            onCropChangeListener?.onCropChange(pair.first, pair.second)
        }.addTo(disposable)

        viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {

                val target = findViewById<CropImageView>(R.id.cropme_image_view)
                val overlayView = findViewById<CropOverlayView>(R.id.cropme_overlay)

                val resultWidth = width * percentWidth
                val resultHeight = height * percentHeight

                restriction = RectF((width - resultWidth) / 2f, (height - resultHeight) / 2f,
                        (width + resultWidth) / 2f, (height + resultHeight) / 2f)

                horizontalAnimator = HorizontalMoveAnimatorImpl(target, restriction, maxScale)
                verticalAnimator = VerticalMoveAnimatorImpl(target, restriction, maxScale)
                scaleAnimator = ScaleAnimatorImpl(target, maxScale)

                target.setResultRect(restriction)
                overlayView.setAttrs(restriction, backgroundAlpha, withBorder)

                viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
        val target = findViewById<CropImageView>(R.id.cropme_image_view)
        viewTreeObserver.addOnDrawListener {

            val targetRect = Rect()
            target.getHitRect(targetRect)

            updateCropInfo()
        }
    }

    private fun startActionDetector() {
        actionDetector = ActionDetector(context, object : ActionListener {

            override fun onScaled(scale: Float) {
                this@CropView.scale = scaleAnimator!!.scale(scale)
            }

            override fun onScaleEnded() {
                this@CropView.scale = scaleAnimator!!.reScaleIfNeeded()
                updateCropInfo()
            }

            override fun onMoved(dx: Float, dy: Float) {
                val x = horizontalAnimator!!.move(dx)
                val y = verticalAnimator!!.move(dy)

                this@CropView.point = PointF(x, y)
            }

            override fun onFlinged(velocityX: Float, velocityY: Float) {
                horizontalAnimator!!.fling(velocityX)
                verticalAnimator!!.fling(velocityY)
            }

            override fun onMoveEnded() {
                if (horizontalAnimator!!.isNotFlinging) {
                    val x = horizontalAnimator!!.reMoveIfNeeded(0f)
                    this@CropView.point = PointF(x, this@CropView.point?.y ?: 0f)
                }

                if (verticalAnimator!!.isNotFlinging) {
                    val y = verticalAnimator!!.reMoveIfNeeded(0f)
                    this@CropView.point = PointF(this@CropView.point?.x ?: 0f, y)
                }

                updateCropInfo()
            }
        })
        setOnTouchListener { v, event ->
            actionDetector!!.detectAction(event)
            true
        }
    }

    private fun updateCropInfo(){
        uri?.let {
            cropChangeSubject.onNext(Pair(it, this@CropView.cropInfo()))
        }
    }

    private fun cropInfo(): CropInfo {
        val target = findViewById<CropImageView>(R.id.cropme_image_view)
        val targetRect = Rect()
        target.getHitRect(targetRect)

        val scale = if (this@CropView.scale == null) ScaleXY(1.0f, 1.0f) else ScaleXY(this@CropView.scale!!.x, this@CropView.scale!!.y)
        val point = if (this@CropView.point == null) null else PointF(this@CropView.point!!.x, this@CropView.point!!.y)
        return CropInfo(scale, point, targetRect, restriction)
    }

    private fun addLayouts() {
        val imageView = CropImageView(context)
        imageView.id = R.id.cropme_image_view
        val imageParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        imageView.scaleType = ImageView.ScaleType.FIT_XY
        imageView.adjustViewBounds = true
        addView(imageView, imageParams)

        val overlayView = CropOverlayView(context)
        overlayView.id = R.id.cropme_overlay
        val overlayParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        addView(overlayView, overlayParams)
    }

    override fun setUri(uri: Uri) {
        setUri(uri, null, null, null)
    }

    override fun setUri(uri: Uri, scale: ScaleXY?, offsetX: Float?, offsetY: Float?) {
        val image = findViewById<ImageView>(R.id.cropme_image_view)
        this.uri = uri
        Glide.with(context)
                .load(uri).into(image)
        image.requestLayout()
        if (scale != null && offsetX != null && offsetY != null) {
            image.postDelayed({
                image.scaleX = scale.x
                image.scaleY = scale.x
                image.translationY = offsetY
                image.translationX = offsetX
            }, 100)
            this.scale = ScaleXY(scale.x, scale.y)
            this.point = PointF(offsetX, offsetY)
            updateCropInfo()
        } else {
            image.postDelayed({
                image.scaleX = 1.0f
                image.scaleY = 1.0f
                image.translationX = 0f
                image.translationY = 0f
            }, 100)
            this.scale = ScaleXY(1.0f, 1.0f)
            this.point = PointF(0f, 0f)
            updateCropInfo()
        }
    }

    override fun setBitmap(bitmap: Bitmap) {
        val image = findViewById<ImageView>(R.id.cropme_image_view)
        image.setImageBitmap(bitmap)
        image.requestLayout()
    }

    override fun crop(listener: OnCropListener) {
        val target = findViewById<CropImageView>(R.id.cropme_image_view)
        val targetRect = Rect()
        target.getHitRect(targetRect)
        var bitmap = (target.drawable as BitmapDrawable).bitmap
        bitmap = Bitmap.createScaledBitmap(bitmap, targetRect.width(), targetRect.height(), false)
        var leftOffset = (restriction!!.left - targetRect.left).toInt()
        var topOffset = (restriction!!.top - targetRect.top).toInt()
        val rightOffset = (targetRect.right - restriction!!.right).toInt()
        val bottomOffset = (targetRect.bottom - restriction!!.bottom).toInt()
        var width = restriction!!.width().toInt()
        var height = restriction!!.height().toInt()

        if (leftOffset < 0) {
            width += leftOffset
            leftOffset = 0
        }
        if (topOffset < 0) {
            height += topOffset
            topOffset = 0
        }
        if (rightOffset < 0) {
            width += rightOffset
        }
        if (bottomOffset < 0) {
            height += bottomOffset
        }
        if (width < 0 || height < 0) {
            listener.onFailure()
            return
        }

        val result = Bitmap.createBitmap(bitmap, leftOffset, topOffset, width, height)
        if (result != null) {
            listener.onSuccess(result)
        } else {
            listener.onFailure()
        }
    }

    companion object {

        private val DEFAULT_BASE = 1
        private val DEFAULT_PBASE = 1

        private val MIN_PERCENT = 0
        private val MAX_PERCENT = 1

        private val DEFAULT_PERCENT_WIDTH = 0.8f
        private val DEFAULT_PERCENT_HEIGHT = 0.8f

        private val DEFAULT_MAX_SCALE = 2
        private val MIN_SCALE = 1
        private val MAX_SCALE = 5

        private val DEFAULT_BACKGROUND_ALPHA = 0.8f
        private val COLOR_DENSITY = 255f

        private val DEFAULT_WITH_BORDER = true
    }
}
