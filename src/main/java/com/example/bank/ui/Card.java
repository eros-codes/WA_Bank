package com.example.bank.ui;

/** A small reusable view (a tile in a list) that shows one item. */
public interface Card<T> {
    void show(T item);
}
