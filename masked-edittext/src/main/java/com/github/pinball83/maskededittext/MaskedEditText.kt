package com.github.pinball83.maskededittext

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnTouchListener
import android.view.inputmethod.InputMethodManager
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.graphics.drawable.DrawableCompat
import com.thrd.maskededittext.R
import java.util.regex.Pattern

class MaskedEditText : AppCompatEditText, OnTouchListener, OnFocusChangeListener {
    private var context: Context? = null
    private var mask: String? = null
    private var notMaskedSymbol: String? = null
    private var deleteChar: String? = null
    private var replacementChar: String? = null
    private var format: String? = null
    var isRequired = false
    private var maskIconCallback: MaskIconCallback? = null
    private var iconCallback: IconCallback? = null
    private var maskIcon: Drawable? = null
    private val listValidCursorPositions = ArrayList<Int>()
    private var firstAllowedPosition = 0
    private var lastAllowedPosition = 0
    private var onFocusChangeListener: OnFocusChangeListener? = null
    private var filteredMask: String? = null
    private var maskedInputFilter: MaskedInputFilter? = null

    constructor(context: Context) : super(context) {
        init(context, "", "", null, null, null, null, null)
    }

    private constructor(
        context: Context,
        mask: String?,
        notMaskedSymbol: String?,
        format: String?,
        @DrawableRes maskIcon: Int,
        iconCallback: IconCallback?
    ) : super(context) {
        var drawable: Drawable? = null
        if (maskIcon != -1) drawable = this.resources.getDrawable(maskIcon)
        init(context, mask, notMaskedSymbol, null, format, drawable, null, iconCallback)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        init(context, "", "", attrs, null, null, null, null)
    }

    private fun init(
        context: Context,
        mask: String?,
        notMaskedSymbol: String?,
        attrs: AttributeSet?,
        format: String?,
        maskIcon: Drawable?,
        maskIconCallback: MaskIconCallback?,
        iconCallback: IconCallback?
    ) {
        this.context = context
        this.mask = mask
        this.notMaskedSymbol = notMaskedSymbol
        this.maskIcon = maskIcon
        this.maskIconCallback = maskIconCallback
        this.iconCallback = iconCallback
        this.format = format
        initByAttributes(context, attrs)
        initMaskIcon()
        this.isLongClickable = false
        this.isSingleLine = true
        this.isFocusable = true
        this.isFocusableInTouchMode = true
    }

    @SuppressLint("RestrictedApi")
    private fun initByAttributes(context: Context, attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.MaskedEditText, 0, 0)
        if (TextUtils.isEmpty(mask) && TextUtils.isEmpty(notMaskedSymbol)) {
            notMaskedSymbol = a.getString(R.styleable.MaskedEditText_notMaskedSymbol)
            mask = a.getString(R.styleable.MaskedEditText_mask)
            val maskedIconRes = a.getResourceId(R.styleable.MaskedEditText_maskIcon, 0)
            if (maskedIconRes > 0) {
                val dm = AppCompatDrawableManager.get()
                val drawableIcon = dm.getDrawable(context, maskedIconRes)
                if (drawableIcon != null) {
                    val wrappedDrawable = DrawableCompat.wrap(drawableIcon)
                    val drawableIconColor =
                        a.getColor(R.styleable.MaskedEditText_maskIconColor, currentHintTextColor)
                    DrawableCompat.setTint(wrappedDrawable, drawableIconColor)
                    maskIcon = wrappedDrawable
                }
            }
        }
        if (!TextUtils.isEmpty(mask) && !TextUtils.isEmpty(notMaskedSymbol)) {
            deleteChar = a.getString(R.styleable.MaskedEditText_deleteChar)
            if (deleteChar == null) deleteChar = " "
            replacementChar = a.getString(R.styleable.MaskedEditText_replacementChar)
            if (replacementChar == null) replacementChar = " "
            val format = a.getString(R.styleable.MaskedEditText_format)
            if (format == null && this.format == null) this.format = "" else if (!TextUtils.isEmpty(
                    format
                ) && this.format == null
            ) this.format = format
            initListValidCursorPositions(mask, notMaskedSymbol)
            filteredMask = mask!!.replace(notMaskedSymbol!!, replacementChar!!)
            this.setText(filteredMask, BufferType.NORMAL)
            maskedInputFilter = MaskedInputFilter()
            this.filters = arrayOf<InputFilter>(maskedInputFilter!!)
        }
        var inputType = a.getInteger(R.styleable.MaskedEditText_android_inputType, -1)
        inputType = inputType
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initMaskIcon() {
        if (maskIcon != null) {
            maskIcon!!.setBounds(0, 0, maskIcon!!.intrinsicHeight, maskIcon!!.intrinsicHeight)
            val compoundDrawables = compoundDrawables
            setCompoundDrawables(
                compoundDrawables[0],
                compoundDrawables[1],
                maskIcon,
                compoundDrawables[3]
            )
        }
        super.setOnFocusChangeListener(this)
        super.setOnTouchListener(this)
    }

    private fun initListValidCursorPositions(mask: String?, charSequence: String?) {
        val chars = mask!!.toCharArray()
        val maskedSymbol = charSequence!![0]
        for (i in 0 until mask.length) {
            if (chars[i] == maskedSymbol) {
                listValidCursorPositions.add(i)
            }
        }
        firstAllowedPosition = listValidCursorPositions[0]
        lastAllowedPosition = listValidCursorPositions[listValidCursorPositions.size - 1]
    }

    override fun setInputType(type: Int) {
        var type = type
        if (type == -1) {
            type = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        if (type == InputType.TYPE_CLASS_NUMBER || type == InputType.TYPE_NUMBER_FLAG_SIGNED || type == InputType.TYPE_NUMBER_FLAG_DECIMAL || type == InputType.TYPE_CLASS_PHONE) {
            val symbolExceptions = symbolExceptions
            this.keyListener = DigitsKeyListener.getInstance("0123456789.$symbolExceptions")
        } else {
            super.setInputType(type)
        }
    }

    private val symbolExceptions: String
        /**
         * Generate symbol exception for inputType = number
         */
        private get() {
            if (TextUtils.isEmpty(filteredMask)) return ""
            val maskSymbolException = StringBuilder()
            for (c in filteredMask!!.toCharArray()) {
                if (!Character.isDigit(c) && maskSymbolException.indexOf(c.toString()) == -1) {
                    maskSymbolException.append(c)
                }
            }
            maskSymbolException.append(replacementChar)
            return maskSymbolException.toString()
        }
    val unmaskedText: String
        get() {
            val text = super.getText()
            if (mask != null && !mask!!.isEmpty()) {
                val unMaskedText: Editable = SpannableStringBuilder()
                for (index in listValidCursorPositions) {
                    if (text != null) {
                        unMaskedText.append(text[index])
                    }
                }
                return if (format != null && !format!!.isEmpty()) formatText(
                    unMaskedText.toString(),
                    format!!
                ) else unMaskedText.toString().trim { it <= ' ' }
            }
            return text.toString().trim { it <= ' ' }
        }

    fun setMaskedText(input: String?) {
        if (input != null) {
            val filteredInputBuilder = StringBuilder(input)
            if (filteredInputBuilder.length < listValidCursorPositions.size) {
                while (filteredInputBuilder.length < listValidCursorPositions.size) {
                    filteredInputBuilder.append(deleteChar)
                }
            } else if (filteredInputBuilder.length > listValidCursorPositions.size) {
                filteredInputBuilder.replace(
                    listValidCursorPositions.size,
                    filteredInputBuilder.length,
                    ""
                )
            }
            val buffer = StringBuilder(filteredInputBuilder)
            val text = this.text
            if (text != null) {
                for (i in 0 until mask!!.length) {
                    if (!listValidCursorPositions.contains(i)) {
                        buffer.insert(i, mask!![i].toString())
                    }
                }
                maskedInputFilter!!.setTextSetup(true)
                this.setText(buffer.toString())
                maskedInputFilter!!.setTextSetup(false)
            }
        }
    }

    private fun formatText(input: String, pattern: String): String {
        val p = Pattern.compile("(\\[[\\d]+])")
        val m = p.matcher(pattern)
        val sb = StringBuffer()
        while (m.find()) {
            m.appendReplacement(sb, getSymbol(input, m.group()))
        }
        return sb.toString()
    }

    private fun getSymbol(input: String, group: String): String {
        val i = Integer.valueOf(group.replace("[", "").replace("]", ""))
        return input.toCharArray()[i - 1].toString()
    }

    /**
     * Use builder
     */
    @Deprecated("")
    fun setFormat(format: String?) {
        this.format = format
    }

    /**
     * Use builder
     */
    @Deprecated("")
    fun setMask(mask: String?) {
        this.mask = mask
    }

    override fun setOnFocusChangeListener(onFocusChangeListener: OnFocusChangeListener) {
        this.onFocusChangeListener = onFocusChangeListener
    }

    override fun setOnTouchListener(onTouchListener: OnTouchListener) {}
    override fun onFocusChange(view: View, hasFocus: Boolean) {
        if (onFocusChangeListener != null) {
            onFocusChangeListener!!.onFocusChange(view, hasFocus)
        }
        if (hasFocus) {
            this.setSelection(firstAllowedPosition)
            this.requestFocus()
        }
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val x = event.x.toInt()
        if (maskIcon != null && maskIcon!!.isVisible && x > width - paddingRight - maskIcon!!.intrinsicWidth) {
            if (event.action == MotionEvent.ACTION_UP) {
                if (maskIconCallback != null) maskIconCallback!!.onIconPushed()
                if (iconCallback != null) iconCallback!!.onIconPushed(unmaskedText)
            }
            return true
        }
        if ((event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_DOWN) && TextUtils.isEmpty(
                unmaskedText
            )
        ) {
            this.setSelection(firstAllowedPosition)
            this.requestFocus()
            val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                ?: return false
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            return true
        }
        return false
    }

    /**
     * Use setIconCallback method
     */
    @Deprecated("")
    fun setMaskIconCallback(maskIconCallback: MaskIconCallback?) {
        this.maskIconCallback = maskIconCallback
    }

    /**
     * Use IconCallback interface
     */
    @Deprecated("")
    interface MaskIconCallback {
        fun onIconPushed()
    }

    fun setIconCallback(iconCallback: IconCallback?) {
        this.iconCallback = iconCallback
    }

    interface IconCallback {
        fun onIconPushed(unmaskedText: String?)
    }

    private inner class MaskedInputFilter : InputFilter {
        private var isUserInput = true
        private var textSetup = false
        override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned,
            dstart: Int,
            dend: Int
        ): CharSequence {
            if (textSetup) return source
            if (source !is SpannableStringBuilder) {
                val filteredStringBuilder = StringBuilder()
                // defaultMaskedSymbols == array that tells us which symbols should be replaced by default
                // and which symbols are part of mask
                val defaultMaskedSymbols = BooleanArray(dend - dstart + 1)
                for (i in 0..dend - dstart) {
                    defaultMaskedSymbols[i] = isCharAllowed(dstart + i)
                }
                for (i in start until end) {
                    val currentChar = source[i]
                    if (defaultMaskedSymbols[0]) {
                        isUserInput = false
                        this@MaskedEditText.text!!.replace(dstart, dstart + 1, "")
                        isUserInput = true
                        filteredStringBuilder.append(currentChar)
                        var index: Int
                        index = if (!isCharAllowed(dstart + 1)) dstart + 1 else dstart
                        skipSymbol(index)
                    } else {
                        if (dstart != mask!!.length) {
                            var index: Int
                            index = if (!isCharAllowed(dstart)) dstart + 1 else dstart
                            val position = skipSymbol(index)
                            this@MaskedEditText.text!!.replace(
                                position,
                                position,
                                Character.toString(currentChar)
                            )
                        }
                    }
                }
                if (isUserInput && TextUtils.isEmpty(source)) { //deletion detection
                    if (dend != 0) {
                        for (i in 0 until dend - dstart) {
                            if (defaultMaskedSymbols[i]) {
                                filteredStringBuilder.append(deleteChar)
                            } else {
                                filteredStringBuilder.append(mask!![dstart + i])
                            }
                        }
                        skipSymbolAfterDeletion(dstart)
                    }
                }
                return filteredStringBuilder.toString()
            }
            return source
        }

        private fun skipSymbol(index: Int): Int {
            var position = getNextAvailablePosition(index, false)
            if (position > lastAllowedPosition) position = lastAllowedPosition
            setSelection(position)
            return position
        }

        private fun skipSymbolAfterDeletion(index: Int) {
            val position = getNextAvailablePosition(index, true)
            setSelection(position)
        }

        private fun getNextAvailablePosition(index: Int, isDeletion: Boolean): Int {
            return if (listValidCursorPositions.contains(index)) {
                val i = listValidCursorPositions.indexOf(index)
                val iterator: ListIterator<Int> =
                    listValidCursorPositions.listIterator(i)
                if (isDeletion) {
                    if (iterator.hasPrevious()) return iterator.previous() + 1
                } else {
                    if (iterator.hasNext()) return iterator.next()
                }
                index
            } else {
                findCloserIndex(index, isDeletion)
            }
        }

        private fun findCloserIndex(index: Int, isDeletion: Boolean): Int {
            val iterator: ListIterator<Int>
            return if (isDeletion) {
                iterator = listValidCursorPositions.listIterator(listValidCursorPositions.size - 1)
                while (iterator.hasPrevious()) {
                    val previous = iterator.previous()
                    if (previous <= index) return previous + 1
                }
                firstAllowedPosition
            } else {
                if (index > firstAllowedPosition) {
                    iterator = listValidCursorPositions.listIterator()
                    while (iterator.hasNext()) {
                        val next = iterator.next()
                        if (next >= index) return next - 1
                    }
                    lastAllowedPosition
                } else {
                    firstAllowedPosition
                }
            }
        }

        private fun isCharAllowed(index: Int): Boolean {
            return index < mask!!.length && mask!![index] == notMaskedSymbol!!.toCharArray()[0]
        }

        fun setTextSetup(textSetup: Boolean) {
            this.textSetup = textSetup
        }
    }

    class Builder(private val context: Context) {
        private var mask: String? = null
        private var notMaskedSymbol: String? = null
        private var icon = -1
        private var iconCallback: IconCallback? = null
        private var format: String? = null
        fun mask(mask: String?): Builder {
            this.mask = mask
            return this
        }

        fun notMaskedSymbol(notMaskedSymbol: String?): Builder {
            this.notMaskedSymbol = notMaskedSymbol
            return this
        }

        fun icon(@DrawableRes maskIcon: Int): Builder {
            icon = maskIcon
            return this
        }

        fun iconCallback(maskIconCallback: IconCallback?): Builder {
            iconCallback = maskIconCallback
            return this
        }

        fun format(format: String?): Builder {
            this.format = format
            return this
        }

        fun build(): MaskedEditText {
            return MaskedEditText(context, mask, notMaskedSymbol, format, icon, iconCallback)
        }
    }
}
