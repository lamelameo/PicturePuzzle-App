package com.example.lamelameo.picturepuzzle.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.lamelameo.picturepuzzle.data.MainViewModel
import com.example.lamelameo.picturepuzzle.databinding.FragmentPauseBinding

/**
 * A simple [Fragment] subclass.
 * Use the [PauseFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PauseFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var mBinding: FragmentPauseBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var mContainer: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (container != null) {
            mContainer = container
        }
        // Inflate the layout for this fragment
        mBinding = FragmentPauseBinding.inflate(layoutInflater, container, false)
        // set on click listeners for buttons in the menu
        mBinding.resumeButton.setOnClickListener { mainViewModel.resumeGame() }
        mBinding.newPuzzle.setOnClickListener { mainViewModel.finishGame() }
        return mBinding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PauseFragment.
         */
        @JvmStatic
        fun newInstance(viewModel: MainViewModel) =
            PauseFragment().apply {
                mainViewModel = viewModel
            }
    }

    private fun closeFragment() {
        mainViewModel.resumeGame()
        parentFragmentManager.beginTransaction().remove(this).commit()
        mContainer.visibility = View.INVISIBLE; mContainer.isClickable = false
    }


}