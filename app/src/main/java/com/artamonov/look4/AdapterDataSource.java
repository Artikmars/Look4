package com.artamonov.look4;

import androidx.annotation.Nullable;

public interface AdapterDataSource<T> {
    int getCount();

    @Nullable
    T get(int position);
}
