package sam.g.trackuriboh.ui_database.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import sam.g.trackuriboh.R
import sam.g.trackuriboh.data.db.relations.ProductWithCardSetAndSkuIds
import sam.g.trackuriboh.databinding.ItemCardBinding
import javax.inject.Inject

class CardListAdapter @Inject constructor()
    : PagingDataAdapter<ProductWithCardSetAndSkuIds, CardListAdapter.CardViewHolder>(CARD_COMPARATOR) {

    private var mOnItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onCardItemClick(cardId: Long)
        fun onViewPricesItemClick(skuIds: List<Long>)
        fun onOpenTCGPlayerClick(cardId: Long)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        mOnItemClickListener = onItemClickListener
    }

    companion object {
        private val CARD_COMPARATOR = object : DiffUtil.ItemCallback<ProductWithCardSetAndSkuIds>() {
            override fun areItemsTheSame(oldItem: ProductWithCardSetAndSkuIds, newItem: ProductWithCardSetAndSkuIds): Boolean =
                oldItem.product.id == newItem.product.id

            override fun areContentsTheSame(oldItem: ProductWithCardSetAndSkuIds, newItem: ProductWithCardSetAndSkuIds): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CardViewHolder {
        return CardViewHolder(ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val cardItem = getItem(position)
        if (cardItem != null) {
            holder.bind(cardItem)
        }
    }

    inner class CardViewHolder(private val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            with(binding) {
                root.setOnClickListener {
                    mOnItemClickListener?.onCardItemClick(
                        getItem(bindingAdapterPosition)?.product?.id ?: throw IllegalArgumentException("card is null")
                    )
                }

                itemCardViewPricesButton.setOnClickListener {
                    mOnItemClickListener?.onViewPricesItemClick(
                        getItem(bindingAdapterPosition)?.skuIds ?: throw IllegalArgumentException("skuIds is null")
                    )
                }

                itemCardTcgplayerButton.setOnClickListener {
                    mOnItemClickListener?.onOpenTCGPlayerClick(
                        getItem(bindingAdapterPosition)?.product?.id ?: throw IllegalArgumentException("card is null")
                    )
                }

            }
        }

        internal fun bind(item: ProductWithCardSetAndSkuIds) {
            Glide.with(itemView)
                .load(item.product.imageUrl)
                .placeholder(R.drawable.img_cardback)
                .fitCenter()
                .into(binding.itemCardImage)

            binding.apply {
                itemCardTitleTextview.text = item.product.name
                itemCardNumberRarityTextview.text = itemView.resources.getString(
                    R.string.item_card_number_rarity_text,
                    item.product.number,
                    item.product.rarity
                )
                itemCardSetNameTextview.text = item.cardSet.name
            }
        }
    }
}