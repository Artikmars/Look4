package com.artamonov.look4.utils;

import android.text.Editable;
import android.text.TextWatcher;

public class PostTextChangeWatcher implements TextWatcher {

    private PostTextChangedListener mListener;

    public PostTextChangeWatcher(PostTextChangedListener listener) {
        super();
        mListener = listener;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (mListener != null) mListener.textChanged(s.toString());
    }

    public interface PostTextChangedListener {
        void textChanged(String text);
    }
}
