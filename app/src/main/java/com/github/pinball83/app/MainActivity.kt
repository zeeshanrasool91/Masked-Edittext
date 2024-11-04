package com.github.pinball83.app

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.pinball83.maskededittext.MaskedEditText
import com.github.pinball83.maskededittext.MaskedEditText.IconCallback

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val maskedEditText = findViewById<MaskedEditText>(R.id.masked_edit_text)
        maskedEditText.setIconCallback(object : IconCallback {
            override fun onIconPushed(unmaskedText: String?) {
                Log.d(TAG, "onIconPushed: $unmaskedText")
                maskedEditText.setMaskedText("          ")
            }
        })
        val maskedEditText1 = MaskedEditText.Builder(this)
            .mask("8 (***) *** **-**")
            .notMaskedSymbol("*")
            .icon(R.drawable.ic_account_circle)
            .format("[1][2][3] [4][5][6]-[7][8]-[10][9]") //we change format output text, swap last two digit
            .iconCallback(object : IconCallback {
                override fun onIconPushed(unmaskedText: String?) {
                    Log.d(TAG, "onIconPushed: ")
                    Toast.makeText(
                        this@MainActivity,
                        String.format("Unmasked formatted text %s", unmaskedText),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
            .build()
        maskedEditText1.inputType = InputType.TYPE_CLASS_NUMBER
        val editText1 = MaskedEditText.Builder(this)
            .mask("8 (***) *** **-**")
            .notMaskedSymbol("*")
            .icon(android.R.drawable.ic_menu_close_clear_cancel)
            .build()
        editText1.inputType = InputType.TYPE_CLASS_NUMBER
        editText1.isEnabled = false
        editText1.setMaskedText("1234567891")
        val editText2 = MaskedEditText.Builder(this)
            .mask("8 (***) *** **-**")
            .notMaskedSymbol("*")
            .build()
        editText2.inputType = InputType.TYPE_CLASS_NUMBER
        editText2.isEnabled = false
        editText2.setMaskedText("9263998787")
        val secondEditText = MaskedEditText.Builder(this)
            .mask("Q***************")
            .notMaskedSymbol("*")
            .build()
        val secondEditText1 = MaskedEditText.Builder(this)
            .mask("**-****-*********")
            .notMaskedSymbol("*")
            .build()
        val thirdEditText = MaskedEditText.Builder(this)
            .mask("*****")
            .notMaskedSymbol("*")
            .build()
        thirdEditText.inputType = InputType.TYPE_CLASS_NUMBER
        val thirdEditText1 = MaskedEditText.Builder(this)
            .mask("***4***")
            .notMaskedSymbol("*")
            .build()
        thirdEditText1.setMaskedText("888488")
        thirdEditText1.inputType = InputType.TYPE_CLASS_NUMBER
        val fordEditText = MaskedEditText.Builder(this)
            .mask("TSH***************")
            .notMaskedSymbol("*")
            .build()
        fordEditText.inputType = InputType.TYPE_CLASS_NUMBER

        // check without mask
        val fordEditText1 = MaskedEditText.Builder(this).build()
        fordEditText1.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val fordEditText2 = MaskedEditText.Builder(this).build()
        fordEditText2.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        val fordEditText3 = MaskedEditText.Builder(this).build()
        fordEditText3.inputType = InputType.TYPE_CLASS_NUMBER
        val layout = findViewById<LinearLayout>(R.id.container)
        layout.addView(maskedEditText1)
        layout.addView(editText1)
        layout.addView(editText2)
        layout.addView(secondEditText)
        layout.addView(secondEditText1)
        layout.addView(thirdEditText)
        layout.addView(thirdEditText1)
        layout.addView(fordEditText)
        layout.addView(fordEditText1)
        layout.addView(fordEditText2)
        layout.addView(fordEditText3)
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
