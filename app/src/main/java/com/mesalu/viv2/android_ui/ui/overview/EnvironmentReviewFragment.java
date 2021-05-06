package com.mesalu.viv2.android_ui.ui.overview;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mesalu.viv2.android_ui.R;

public class EnvironmentReviewFragment extends Fragment {

    private EnvironmentReviewViewModel mViewModel;

    public static EnvironmentReviewFragment newInstance() {
        return new EnvironmentReviewFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_environment_review, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(EnvironmentReviewViewModel.class);
        // TODO: Use the ViewModel
    }

}