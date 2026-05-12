package com.example.appmovilstress.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appmovilstress.R
import com.google.android.material.appbar.MaterialToolbar

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun setupToolbar(toolbar: MaterialToolbar, title: String, showBackButton: Boolean = true) {
        toolbar.title = title
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(showBackButton)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        toolbar.setTitleTextColor(getColor(R.color.white))
    }
}
