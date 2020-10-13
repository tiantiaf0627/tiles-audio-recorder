package com.domain.src.main.java.com.jelly.domain;


import io.reactivex.Observable;

/**
 * Created by Tiantian on 05/11/17.
 */

abstract class UseCase<T> {
    public abstract Observable<T> buildObservable();

    public Observable<T> execute() {
        return buildObservable();
    }
}
