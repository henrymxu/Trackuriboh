package tang.song.edu.yugiohcollectiontracker.ui_card_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import tang.song.edu.yugiohcollectiontracker.data.repository.CardRepository

class CardDetailViewModel(private val cardRepository: CardRepository) : ViewModel() {
    fun getCardDetailsById(cardId: Long) = liveData {
        emit(cardRepository.getCardDetails(cardId))
    }
}