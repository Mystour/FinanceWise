import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.starry.greenstash.ui.screens.recognition.RecognitionViewModel

class RecognitionViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecognitionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecognitionViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
