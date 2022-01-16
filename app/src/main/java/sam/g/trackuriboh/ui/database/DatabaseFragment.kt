package sam.g.trackuriboh.ui.database

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.work.WorkInfo
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import sam.g.trackuriboh.*
import sam.g.trackuriboh.MainActivity.Companion.ACTION_SET_BOTTOM_NAV_ENABLED
import sam.g.trackuriboh.databinding.FragmentDatabaseBinding
import sam.g.trackuriboh.ui.common.utils.UiState
import sam.g.trackuriboh.ui.database.adapters.DatabaseStateAdapter
import sam.g.trackuriboh.ui.database.viewmodels.DatabaseViewModel
import sam.g.trackuriboh.ui.search_suggestions.CardSearchSuggestionsViewModel
import sam.g.trackuriboh.ui.search_suggestions.CardSetSearchSuggestionsViewModel
import sam.g.trackuriboh.utils.*
import sam.g.trackuriboh.workers.DatabaseUpdateCheckWorker
import sam.g.trackuriboh.workers.WORKER_PROGRESS_KEY

/**
 * !IMPORTANT
 * The SearchView is very buggy and the behaviour is sometimes hard to manage. When updating Hilt,
 * the keyboard starting flickering when returning back from another fragment (this is because
 * Navigation uses replace instead of add for fragment transactions, so the fragment is recreated
 * when returning). To workaround this
 * we'll use activities to contain destinations that come from here.
 */
@AndroidEntryPoint
class DatabaseFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(FragmentDatabaseBinding::inflate)

    private lateinit var searchView: SearchView

    private val viewModel: DatabaseViewModel by viewModels()

    private val cardSearchSuggestionsViewModel: CardSearchSuggestionsViewModel by viewModels()

    private val cardSetSearchSuggestionsViewModel: CardSetSearchSuggestionsViewModel by viewModels()

    private lateinit var stateAdapter: DatabaseStateAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViewPagerBackPressBehaviour(binding.databaseViewPager)

        initToolbar()
        initTabLayoutWithViewPager()
        initObservers()
        initDatabaseSyncObservers()
        initSearchSuggestions()
        initFragmentResultListeners()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_database_update_check -> {
                viewModel.checkForDatabaseUpdates()

                true
            }
            R.id.action_database_download -> {
                context?.createAlertDialog(
                    title = getString(R.string.database_download_prompt_title),
                    message = getString(R.string.database_download_prompt_message),
                    positiveButtonText = getString(R.string.lbl_download),
                    negativeButtonText = getString(R.string.lbl_cancel),
                    onPositiveButtonClick = { _, _ -> viewModel.downloadDatabase() }
                )?.show()

                true
            }
            else -> false
        }
    }

    private fun initTabLayoutWithViewPager() {
        binding.databaseViewPager.apply {
            adapter = DatabaseStateAdapter(this@DatabaseFragment).also {
                stateAdapter = it
            }

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    when(position) {
                        DatabaseViewModel.Page.CARDS.position -> {
                            viewModel.setCurrentPage(DatabaseViewModel.Page.CARDS)
                        }
                        DatabaseViewModel.Page.CARD_SETS.position -> {
                            viewModel.setCurrentPage(DatabaseViewModel.Page.CARD_SETS)
                        }
                    }
                }
            })
        }

        TabLayoutMediator(binding.databaseTabLayout, binding.databaseViewPager) { tab, position ->
            tab.text = when (position) {
                DatabaseViewModel.Page.CARDS.position -> getString(R.string.tab_card_title)
                DatabaseViewModel.Page.CARD_SETS.position -> getString(R.string.tab_set_title)
                else -> null
            }
        }.attach()
    }

    private fun initToolbar() {
        binding.databaseToolbar.setupAsTopLevelDestinationToolbar()

        createOptionsMenu()
    }

    private fun initDatabaseSyncObservers() {
        viewModel.databaseDownloadState.observe(viewLifecycleOwner) { event ->
            handleWorkUpdated(
                workEvent = event,
                onLoading = { uiState ->
                    uiState.data?.progress?.getInt(WORKER_PROGRESS_KEY, 0)?.let { progress ->
                        setProgress(progress, uiState.message)
                    }
                },
                onSuccess = { uiState ->
                    uiState.message?.let { showSnackbar(it, SnackbarType.SUCCESS) }
                },
                onFailure = { uiState ->
                    uiState.message?.let { showSnackbar(it, SnackbarType.ERROR) }
                },
                onCancelled = { uiState ->
                    uiState.message?.let { showSnackbar(it, SnackbarType.INFO) }
                }
            )
        }

        viewModel.databaseUpdateState.observe(viewLifecycleOwner) { event ->
            handleWorkUpdated(
                workEvent = event,
                onLoading = { uiState ->
                    setProgress(indeterminate = true, progress = -1, message = uiState.message)
                },
                onSuccess = { uiState ->
                    uiState.message?.let { showSnackbar(it, SnackbarType.SUCCESS) }
                },
                onFailure = { uiState ->
                    uiState.message?.let { showSnackbar(it, SnackbarType.ERROR) }
                },
                onCancelled = { uiState ->
                    uiState.message?.let { showSnackbar(it, SnackbarType.INFO) }
                }
            )
        }

        viewModel.databaseUpdateCheckState.observe(viewLifecycleOwner) { event ->
            handleWorkUpdated(
                workEvent = event,
                onLoading = { uiState ->
                    setProgress(indeterminate = true, progress = -1, message = uiState.message)
                },
                onSuccess = { uiState ->
                    if (uiState.data != null) {
                        context?.createAlertDialog(
                            title = getString(R.string.database_update_check_prompt_title),
                            message = uiState.message,
                            positiveButtonText = getString(R.string.lbl_update),
                            onPositiveButtonClick = { _, _ ->
                                viewModel.updateDatabase(
                                    uiState.data?.outputData?.getLongArray(DatabaseUpdateCheckWorker.UPDATE_CARD_SET_IDS_RESULT)
                                )
                            }
                        )?.show()
                    } else {
                        uiState.message?.let { showSnackbar(it, SnackbarType.INFO) }
                    }

                },
                onFailure = { uiState ->
                    uiState.message?.let { showSnackbar(it, SnackbarType.ERROR) }
                },
                onCancelled = { uiState ->
                    uiState.message?.let { showSnackbar(it, SnackbarType.INFO) }
                }
            )
        }
    }

    private fun handleWorkUpdated(
        workEvent: SingleEvent<UiState<WorkInfo>>,
        onLoading: (uiState: UiState<WorkInfo>) -> Unit,
        onSuccess: (uiState: UiState<WorkInfo>) -> Unit,
        onFailure: (uiState: UiState<WorkInfo>) -> Unit,
        onCancelled: (uiState: UiState<WorkInfo>) -> Unit,
    ) {
        if (workEvent.hasBeenHandled) {
            return
        }

        val savedStateHandle = findNavController().getBackStackEntry(R.id.mainGraph).savedStateHandle

        when (val uiState = workEvent.getContent()) {
            is UiState.Loading -> {
                showLoading()
                binding.databaseToolbar.menu.setEnabled(false)
                savedStateHandle.set(ACTION_SET_BOTTOM_NAV_ENABLED, false)
                onLoading(uiState)
            }
            else -> {
                workEvent.handleEvent()
                showContent()
                binding.databaseToolbar.menu.setEnabled(true)
                savedStateHandle.set(ACTION_SET_BOTTOM_NAV_ENABLED, true)

                when (uiState) {
                    is UiState.Failure -> {
                        if (uiState.data?.state == WorkInfo.State.CANCELLED) {
                            onCancelled(uiState)
                        } else {
                            onFailure(uiState)
                        }
                    }
                    is UiState.Success -> onSuccess(uiState)
                    else -> return
                }
            }
        }
    }

    private fun initSearchSuggestions() {
        searchView.initSearchSuggestions()

        cardSearchSuggestionsViewModel.suggestionsCursor.observe(viewLifecycleOwner) {
            searchView.setSuggestionsCursor(it)
        }

        cardSetSearchSuggestionsViewModel.suggestionsCursor.observe(viewLifecycleOwner) {
            searchView.setSuggestionsCursor(it)
        }
    }

    private fun setProgress(progress: Int, message: String? = null, indeterminate: Boolean = false) {
        with(binding.databaseSyncProgressTextview) {
            show(!indeterminate)
            text = getString(R.string.progress_percent, progress)

        }

        with(binding.databaseSyncInfoTextview) {
            visibility = View.VISIBLE
            binding.databaseSyncInfoTextview.text = message
        }

        with(binding.databaseSyncProgressIndicator) {
            isIndeterminate = indeterminate

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setProgress(progress, true)
            } else {
                this.progress = progress
            }
        }
    }

    private fun showLoading() {
        binding.apply {
            databaseTabLayout.visibility = View.GONE
            databaseViewPager.visibility = View.GONE

            databaseSyncProgressIndicator.show()
            databaseSyncProgressTextview.visibility = View.VISIBLE

            databaseSyncInfoTextview.visibility = View.VISIBLE

            databaseViewPager.adapter = null
        }
    }

    private fun showContent() {
        binding.apply {
            databaseTabLayout.visibility = View.VISIBLE
            databaseViewPager.visibility = View.VISIBLE

            databaseSyncProgressIndicator.hide()
            databaseSyncProgressTextview.visibility = View.GONE
            databaseSyncInfoTextview.visibility = View.GONE

            if (databaseViewPager.adapter == null) {
                databaseViewPager.adapter = stateAdapter
            }
        }

    }

    private fun createOptionsMenu() {
        binding.databaseToolbar.apply {
            inflateMenu(R.menu.database_toolbar)

            menu.findItem(R.id.action_search).apply {
                searchView = setIconifiedSearchViewBehaviour(object : SearchViewQueryHandler {
                    override fun handleQueryTextSubmit(query: String?) {
                        performSearch(query)
                    }

                    override fun handleQueryTextChanged(newText: String?) {
                        when(viewModel.currentPage.value) {
                            DatabaseViewModel.Page.CARDS -> cardSearchSuggestionsViewModel.getSuggestions(newText)
                            DatabaseViewModel.Page.CARD_SETS -> cardSetSearchSuggestionsViewModel.getSuggestions(newText)
                            null -> return
                        }
                    }

                    override fun handleSearchViewExpanded() {

                        // Hide other menu items when searchview expands
                        menu.children.forEach {
                            it.isVisible = it.itemId == itemId
                        }
                    }

                    override fun handleSearchViewCollapse() {
                        // Show other menu items when searchview collapses
                        menu.children.forEach {
                            it.isVisible = true
                        }
                        resetLists()
                    }
                })
            }

            setOnMenuItemClickListener(this@DatabaseFragment)
        }
    }

    /** Queries on the current displayed fragment **/
    private fun performSearch(query: String?) {
        val currentFragment = childFragmentManager.findViewPagerFragment(binding.databaseViewPager.currentItem.toLong())

        if (currentFragment is BaseSearchListFragment<*>) {
            currentFragment.search(query)
        }

        searchView.clearFocus()
        binding.focusDummyView.requestFocus()
    }

    private fun resetLists() {
        repeat(stateAdapter.itemCount) {
            val currentFragment = childFragmentManager.findViewPagerFragment(stateAdapter.getItemId(it))

            if (currentFragment is BaseSearchListFragment<*> && currentFragment.lastQueryValue() != null) {
                currentFragment.search(null)
            }
        }
    }

    private fun initObservers() {
        viewModel.currentPage.observe(viewLifecycleOwner) {
            // If they page change while we're showing suggestions, remove the current cursor
            searchView.setSuggestionsCursor(null)
        }
    }

    /*
     * Fragment Result API. See https://developer.android.com/guide/fragments/communicate#fragment-result.
     * MAIN NOTE: You can only set one listener for a given key.
     *
     * Another way is to use NavBackStackEntry https://developer.android.com/guide/navigation/navigation-programmatic#returning_a_result
     */
    private fun initFragmentResultListeners() {
        childFragmentManager.apply {
            setFragmentResultListener(CardListFragment.FRAGMENT_RESULT_REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
                val cardId = bundle.getLong(CardListFragment.CARD_ID_DATA_KEY)

                findNavController().safeNavigate(
                    MainGraphDirections.actionGlobalCardDetailActivity(cardId = cardId)
                )
            }

            setFragmentResultListener(CardSetListFragment.FRAGMENT_RESULT_REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
                val setId = bundle.getLong(CardSetListFragment.SET_ID_DATA_KEY)

                findNavController().safeNavigate(
                    MainGraphDirections.actionGlobalCardDetailActivity(setId = setId)
                )
            }
        }
    }
}
