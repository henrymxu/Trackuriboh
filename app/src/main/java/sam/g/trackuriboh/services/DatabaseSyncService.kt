package sam.g.trackuriboh.services

import android.util.Log
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import sam.g.trackuriboh.data.db.AppLocalCache
import sam.g.trackuriboh.data.network.responses.CardResponse
import sam.g.trackuriboh.data.network.responses.CardSetResponse
import sam.g.trackuriboh.data.network.services.CardSetApiService
import sam.g.trackuriboh.data.network.services.CatalogApiService
import sam.g.trackuriboh.data.network.services.ProductApiService
import javax.inject.Inject

private const val PAGINATION_LIMIT_SIZE = 100
private const val MAX_PARALLEL_REQUESTS = 20
private const val REQUEST_INTERVAL_DELAY = 2000L

@ViewModelScoped
class DatabaseSyncService @Inject constructor(
    private val cardSetApiService: CardSetApiService,
    private val productApiService: ProductApiService,
    private val catalogApiService: CatalogApiService,
    private val appLocalCache: AppLocalCache,
) {

    suspend fun syncDatabase(): Flow<DatabaseSyncState> = flow {
        try {
            emit(DatabaseSyncState.LOADING(0))

            val cardResponse = productApiService.getCards(offset = 11700)
            val cardSetResponse = cardSetApiService.getSets()
            val cardRarityResponse = catalogApiService.getCardRarities()
            val printingResponse = catalogApiService.getPrintings()
            val conditionResponse = catalogApiService.getConditions()

            appLocalCache.run {
                insertCardRarities(cardRarityResponse.results.map { it.toDatabaseEntity() })
                insertPrintings(printingResponse.results.map { it.toDatabaseEntity() })
                insertConditions(conditionResponse.results.map { it.toDatabaseEntity() })
            }

            paginateAndPopulateDatabase(
                ::getCardSetList,
                ::insertCardSets,
                cardSetResponse.totalItems,
                this@flow,
            )

            paginateAndPopulateDatabase(
                ::getCardList,
                ::insertCards,
                cardResponse.totalItems,
                this@flow,
            )

            emit(DatabaseSyncState.SUCCESS)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            emit(DatabaseSyncState.FAILURE(throwable.message))
        } finally {
            emit(DatabaseSyncState.IDLE)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun getCardList(offset: Int, limit: Int): List<CardResponse.CardItem> =
        productApiService.getCards(offset, limit).results

    private suspend fun getCardSetList(offset: Int, limit: Int): List<CardSetResponse.CardSetItem> =
        cardSetApiService.getSets(offset, limit).results

    private suspend fun insertCardSets(cardSets: List<CardSetResponse.CardSetItem>): List<Long> =
        appLocalCache.insertCardSets(cardSets.map { it.toDatabaseEntity() })

    private suspend fun insertCards(cards: List<CardResponse.CardItem>): List<Long> {
        val result = appLocalCache.insertProducts(cards.map { it.toDatabaseEntity() })
        cards.forEach { cardItem -> cardItem.skus?.let {
            appLocalCache.insertProductSkus(cardItem.skus.map { it.toDatabaseEntity().also { Log.d("SKU", it.toString()) } })
        } }

        return result
    }

    private suspend fun <T> paginateAndPopulateDatabase(
        apiServiceCall: suspend (offset: Int, limit: Int) -> List<T>,
        databaseInsert: suspend (List<T>) -> List<Long>,
        totalCount: Int,
        flow: FlowCollector<DatabaseSyncState>,
    ) {

        // Each bach should make MAX_PARALLEL_REQUESTS number of requests and fetch PAGINATION_LIMIT_SIZE number of items
        val batchOffsetIncrements = minOf(totalCount, MAX_PARALLEL_REQUESTS * PAGINATION_LIMIT_SIZE)

        for (batchOffset in 0 until totalCount step batchOffsetIncrements) {
            coroutineScope {
                val requestBatch = (
                        batchOffset until minOf(batchOffset + batchOffsetIncrements, totalCount)
                        step PAGINATION_LIMIT_SIZE
                    ).map { curOffset ->
                        async {
                        val itemList = apiServiceCall(curOffset, PAGINATION_LIMIT_SIZE)

                        databaseInsert(itemList)
                    }
                }

                requestBatch.awaitAll()
                delay(REQUEST_INTERVAL_DELAY)
                flow.emit(DatabaseSyncState.LOADING((batchOffset.toDouble() / totalCount * 100).toInt()))
            }
        }
    }

    sealed class DatabaseSyncState {
        object IDLE : DatabaseSyncState()
        data class LOADING(val progress: Int) : DatabaseSyncState()
        object SUCCESS : DatabaseSyncState()
        data class FAILURE(val msg: String?) : DatabaseSyncState()
    }
}
