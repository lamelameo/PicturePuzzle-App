package com.example.lamelameo.picturepuzzle;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PauseMenu.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PauseMenu#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PauseMenu extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private OnFragmentInteractionListener resumeListener, quitListener;

    public PauseMenu() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PauseMenu.
     */
    public static PauseMenu newInstance() {
        PauseMenu fragment = new PauseMenu();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View fragmentView = inflater.inflate(R.layout.fragment_pause_menu, container, false);
        // set on click listeners for buttons in the menu
        final Button resumeButton = fragmentView.findViewById(R.id.resumeButton);
        final Button quitButton = fragmentView.findViewById(R.id.newPuzzle);

        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send callback to game activity - should remove fragment
                resumeButton();
            }
        });

        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send callback to game activity - should finish the activity which sends app back to main activity
                newPuzzle();
            }
        });
        return fragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        quitListener = (OnFragmentInteractionListener)context;
        try {
            resumeListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void resumeButton() {
        resumeListener.onClickResume();
    }

    public void newPuzzle() {
        quitListener.onClickNewPuzzle();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        // activity must implement code to define what happens when clicking the buttons in the menu
        void onClickResume();
        void onClickNewPuzzle();
    }
}
