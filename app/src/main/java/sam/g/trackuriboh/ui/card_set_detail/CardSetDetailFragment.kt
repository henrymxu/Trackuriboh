package sam.g.trackuriboh.ui.card_set_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.compose.material.ExperimentalMaterialApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import sam.g.trackuriboh.R
import sam.g.trackuriboh.databinding.FragmentCardSetDetailBinding
import sam.g.trackuriboh.ui.card_set_detail.viewmodels.CardSetDetailViewModel
import sam.g.trackuriboh.ui.database.CardListFragment
import sam.g.trackuriboh.ui.search_suggestions.CardSearchSuggestionsViewModel
import sam.g.trackuriboh.utils.*

@ExperimentalMaterialApi
@AndroidEntryPoint
class CardSetDetailFragment : Fragment() {
    private val binding by viewBinding(FragmentCardSetDetailBinding::inflate)

    private val viewModel: CardSetDetailViewModel by viewModels()

    private val searchSuggestionsViewModel: CardSearchSuggestionsViewModel by viewModels()

    private val args: CardSetDetailFragmentArgs by navArgs()

    private lateinit var searchView: SearchView

    private lateinit var cardListFragment: CardListFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardListFragment = CardListFragment.newInstance(setId = args.setId)

        childFragmentManager.beginTransaction().replace(
            binding.cardSetDetailFragmentContainer.id,
            cardListFragment
        ).commit()

        initToolbar()
        initSearchSuggestions()
        initObservers()
    }

    private fun initToolbar() {
        binding.cardSetDetailToolbar.setupWithNavController(
            findNavController(),
            AppBarConfiguration.Builder().setFallbackOnNavigateUpListener {
                activity?.finish()
                true
            }.build()
        )

        createOptionsMenu()
    }

    private fun initSearchSuggestions() {
        searchView.initSearchSuggestions()

        searchView.initSearchSuggestions()

        searchSuggestionsViewModel.searchInSet(args.setId)

        searchSuggestionsViewModel.suggestionsCursor.observe(viewLifecycleOwner) {
            searchView.setSuggestionsCursor(it)
        }
    }

    private fun initObservers() {
        viewModel.cardSet.observe(viewLifecycleOwner) {
            binding.cardSetDetailToolbar.title = it.name
        }
    }

    private fun createOptionsMenu() {
        binding.cardSetDetailToolbar.apply {
            inflateMenu(R.menu.card_set_detail_toolbar)

            menu.findItem(R.id.action_search).apply {
                searchView = setIconifiedSearchViewBehaviour(object : SearchViewQueryHandler {
                    override fun handleQueryTextSubmit(query: String?) {
                        cardListFragment.search(query)

                        searchView.clearFocus()
                        binding.focusDummyView.requestFocus()
                    }

                    override fun handleQueryTextChanged(newText: String?) {
                        searchSuggestionsViewModel.getSuggestions(newText)
                    }

                    override fun handleSearchViewCollapse() {
                        cardListFragment.search(null)
                    }
                })
            }
        }
    }
}