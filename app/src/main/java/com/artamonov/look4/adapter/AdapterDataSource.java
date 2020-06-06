package com.artamonov.look4.adapter;

import androidx.annotation.Nullable;

public interface AdapterDataSource<T> {
    int getCount();

    @Nullable
    T get(int position);
}
