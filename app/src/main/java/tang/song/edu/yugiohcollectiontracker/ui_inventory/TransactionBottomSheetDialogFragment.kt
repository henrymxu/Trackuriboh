package tang.song.edu.yugiohcollectiontracker.ui_inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.bottom_sheet_transaction.*
import tang.song.edu.yugiohcollectiontracker.R
import tang.song.edu.yugiohcollectiontracker.data.db.relations.CardWithSetInfo
import tang.song.edu.yugiohcollectiontracker.databinding.BottomSheetTransactionBinding
import tang.song.edu.yugiohcollectiontracker.viewBinding
import java.util.*

@AndroidEntryPoint
class TransactionBottomSheetDialogFragment : BottomSheetDialogFragment(), Toolbar.OnMenuItemClickListener {
    private val mViewModel: TransactionBottomSheetDialogViewModel by viewModels()

    private val args: TransactionBottomSheetDialogFragmentArgs by navArgs()

    private val binding by viewBinding(BottomSheetTransactionBinding::inflate)

    private lateinit var mCard: CardWithSetInfo

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.setOnShowListener {
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
                BottomSheetBehavior.from(it).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.getCardDetailsById(args.cardId).observe(viewLifecycleOwner) {
            mCard = it.also {
                binding.newTransactionNameEdittext.setText(it.card.name)
            }

            initDropdowns()
        }

        initToolbar()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            R.id.action_save_transaction -> {
                return true
            }
            else -> false
        }
    }

    private fun initToolbar() {
        binding.newTransactionToolbar.apply {
            inflateMenu(R.menu.transaction_dialog_toolbar_menu)
            setNavigationOnClickListener { dismiss() }
            setOnMenuItemClickListener(this@TransactionBottomSheetDialogFragment)
        }
    }

    private fun initDropdowns() {
        initCardNumberDropdown()
        initTransactionTypeDropdown()
        initPlatformDropdown()
    }

    private fun initCardNumberDropdown() {
        val cardNumberList = mutableSetOf<String>()
        mCard.sets.forEach { cardNumberList.add(it.cardNumber) }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cardNumberList.toList())
        binding.newTransactionCardNumberTextview.apply {
            setAdapter(adapter)

            setOnItemClickListener { _, _, _, _ ->
                setRarityDropdown(this.text.toString())
            }
        }
    }

    private fun initTransactionTypeDropdown() {
        val transactionTypeList = resources.getStringArray(R.array.transaction_types)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, transactionTypeList)
        binding.newTransactionTypeTextview.setAdapter(adapter)
    }

    private fun initPlatformDropdown() {
        val platformTypeList = resources.getStringArray(R.array.platform_types).toList()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, platformTypeList)
        binding.newTransactionPlatformTextview.setAdapter(adapter)
    }

    private fun setRarityDropdown(cardNumber: String) {
        val rarityList = mutableListOf<String>()

        mCard.sets.forEach { if (it.cardNumber == cardNumber && it.rarity != null) {
            rarityList.add(it.rarity)
        } }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, rarityList)
        binding.newTransactionRarityTextview.apply {
            setAdapter(adapter)

            if (rarityList.size == 1) {
                setText(rarityList[0], false)
            }
        }
    }
}
