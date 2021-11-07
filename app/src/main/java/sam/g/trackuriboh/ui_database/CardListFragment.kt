package sam.g.trackuriboh.ui_database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import sam.g.trackuriboh.data.db.relations.ProductWithSetInfo
import sam.g.trackuriboh.databinding.FragmentCardListBinding
import sam.g.trackuriboh.ui_database.adapters.CardListAdapter
import sam.g.trackuriboh.ui_database.viewmodels.BaseSearchViewModel
import sam.g.trackuriboh.ui_database.viewmodels.CardListViewModel
import sam.g.trackuriboh.viewBinding
import javax.inject.Inject

@AndroidEntryPoint
class CardListFragment : BaseSearchListFragment<ProductWithSetInfo>(), CardListAdapter.OnItemClickListener {
    @Inject
    lateinit var mAdapter: CardListAdapter

    private val binding by viewBinding(FragmentCardListBinding::inflate)

    private val mViewModel: CardListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()

        this.search(mViewModel.currentQueryValue())
    }

    override fun onItemClick(cardId: Long) {
        hideSoftKeyboard()

        val action = DatabaseFragmentDirections.actionDatabaseFragmentToCardDetailFragment(cardId)
        findNavController().navigate(action)
    }

    override fun getViewModel(): BaseSearchViewModel<ProductWithSetInfo> {
        return mViewModel
    }

    override fun getListView(): RecyclerView {
        return binding.cardList
    }

    override fun getAdapter(): PagingDataAdapter<ProductWithSetInfo, out RecyclerView.ViewHolder> {
        return mAdapter
    }

    private fun initRecyclerView() {
        mAdapter.setOnItemClickListener(this)
        binding.cardList.apply {
            this.layoutManager = LinearLayoutManager(context)
            this.adapter = mAdapter
        }
    }
}
