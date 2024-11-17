package com.starry.greenstash.ui.screens.emotion.composables

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.paulvarry.jsonviewer.JsonViewer
import org.json.JSONArray

@Composable
fun JsonViewer(jsonString: String) {
    val context = LocalContext.current
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    AndroidView(
        factory = {
            JsonViewer(context).apply {
                id = View.generateViewId()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = { jsonViewer ->
            val prettyJson = try {
                gson.toJson(gson.fromJson(jsonString, JSONArray::class.java))
            } catch (e: JsonSyntaxException) {
                "Invalid JSON: ${e.message}"
            }
            jsonViewer.setJson(prettyJson)
        },
        modifier = Modifier.fillMaxWidth()
    )
}
