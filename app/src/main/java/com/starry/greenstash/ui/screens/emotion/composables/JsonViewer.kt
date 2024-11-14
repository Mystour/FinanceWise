//package com.starry.greenstash.ui.screens.emotion.composables
//
//import android.view.View
//import android.widget.HorizontalScrollView
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.AndroidView
//import com.google.gson.GsonBuilder
//import com.google.gson.JsonSyntaxException
//import com.yuyh.jsonviewer.library.JsonRecyclerView
//
//@Composable
//fun JsonViewer(jsonString: String) {
//    val context = LocalContext.current
//    val gson = remember { GsonBuilder().setPrettyPrinting().create() }
//
//    AndroidView(
//        factory = {
//            HorizontalScrollView(context).apply {
//                layoutParams = LayoutParams(
//                    LayoutParams.MATCH_PARENT,
//                    LayoutParams.MATCH_PARENT
//                )
//                fillViewport = true
//                orientation = HorizontalScrollView.HORIZONTAL
//
//                addView(JsonRecyclerView(context).apply {
//                    id = View.generateViewId()
//                    layoutParams = LayoutParams(
//                        LayoutParams.MATCH_PARENT,
//                        LayoutParams.WRAP_CONTENT
//                    )
//                })
//            }
//        },
//        update = { jsonRecyclerView ->
//            val prettyJson = try {
//                gson.toJson(gson.fromJson(jsonString, Any::class.java))
//            } catch (e: JsonSyntaxException) {
//                "Invalid JSON: ${e.message}"
//            }
//            jsonRecyclerView.bindJson(prettyJson)
//        },
//        modifier = Modifier.fillMaxWidth()
//    )
//}
