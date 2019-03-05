package com.crazylegend.kotlinextensions.livedata

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*


/**
 * Created by Hristijan on 2/1/19 to long live and prosper !
 */

//app compat activity
inline fun <reified T : ViewModel> AppCompatActivity.compatProvider(): T {
    return ViewModelProviders.of(this).get(T::class.java)
}

inline fun <reified T : AndroidViewModel> AppCompatActivity.compatProvider(factory: ViewModelProvider.Factory): T {
    return ViewModelProviders.of(this, factory).get(T::class.java)
}


//fragment
inline fun <reified T : ViewModel> Fragment.fragmentProvider(): T {

    return ViewModelProviders.of(this).get(T::class.java)
}

inline fun <reified T : AndroidViewModel> Fragment.fragmentProvider(factory: ViewModelProvider.Factory): T {

    return ViewModelProviders.of(this, factory).get(T::class.java)
}

//fragment activity
inline fun <reified T : ViewModel> FragmentActivity.fragmentActivityProvider(): T {

    return ViewModelProviders.of(this).get(T::class.java)
}

inline fun <reified T : AndroidViewModel> FragmentActivity.fragmentActivityProvider(factory: ViewModelProvider.Factory): T {

    return ViewModelProviders.of(this, factory).get(T::class.java)
}


//shared model
inline fun <reified T : ViewModel> Fragment.sharedProvider(): T {

    return ViewModelProviders.of(requireActivity()).get(T::class.java)
}

inline fun <reified T : AndroidViewModel> Fragment.sharedProvider(factory: ViewModelProvider.Factory): T {

    return ViewModelProviders.of(requireActivity(), factory).get(T::class.java)
}



/**
 * Converts a LiveData to a SingleLiveData (exactly similar to LiveData.first()
 */
fun <T> LiveData<T>.toSingleLiveData(): SingleLiveData<T> = first()

/**
 * Converts a LiveData to a MutableLiveData with the initial value set by this LiveData's value
 */
fun <T> LiveData<T>.toMutableLiveData(): MutableLiveData<T> {
    val liveData =  MutableLiveData<T>()
    liveData.value = this.value
    return liveData
}

/**
 * Creates a LiveData that emits the initialValue immediately.
 */
fun <T> liveDataOf(initialValue: T): MutableLiveData<T> {
    return emptyLiveData<T>().apply { value = initialValue }
}

/**
 * Creates a LiveData that emits the value that the `callable` function produces, immediately.
 */
fun <T> liveDataOf(callable: () -> T): LiveData<T> {
    val returnedLiveData = MutableLiveData<T>()
    returnedLiveData.value = callable.invoke()
    return returnedLiveData
}

/**
 * Creates an empty LiveData.
 */
fun <T> emptyLiveData(): MutableLiveData<T> {
    return MutableLiveData()
}

/**
 * Emits at most 1 item and returns a SingleLiveData
 */
fun <T> LiveData<T>.first(): SingleLiveData<T> {
    return SingleLiveData(this)
}


/**
 * Emits the first n valueus
 */
fun <T> LiveData<T>.take(count:Int): LiveData<T> {
    val mutableLiveData: MediatorLiveData<T> = MediatorLiveData()
    var takenCount = 0
    mutableLiveData.addSource(this) {
        if(takenCount<count) {
            mutableLiveData.value = it
            takenCount++
        } else {
            mutableLiveData.removeSource(this)
        }
    }
    return mutableLiveData
}

/**
 * Takes until a certain predicate is met, and does not emit anything after that, whatever the value.
 */
inline fun <T> LiveData<T>.takeUntil(crossinline predicate : (T?)->Boolean): LiveData<T> {
    val mutableLiveData: MediatorLiveData<T> = MediatorLiveData()
    var metPredicate = predicate(value)
    mutableLiveData.addSource(this) {
        if(predicate(it)) metPredicate = true
        if(!metPredicate) {
            mutableLiveData.value = it
        } else {
            mutableLiveData.removeSource(this)
        }
    }
    return mutableLiveData
}


/**
 * Skips the first n values
 */
fun <T> LiveData<T>.skip(count:Int): LiveData<T> {
    val mutableLiveData: MediatorLiveData<T> = MediatorLiveData()
    var skippedCount = 0
    mutableLiveData.addSource(this) {
        if(skippedCount>=count) {
            mutableLiveData.value = it
        }
        skippedCount++
    }
    return mutableLiveData
}



/**
 * Skips all values until a certain predicate is met (the item that actives the predicate is also emitted)
 */
inline fun <T> LiveData<T>.skipUntil(crossinline predicate : (T?)->Boolean): LiveData<T> {
    val mutableLiveData: MediatorLiveData<T> = MediatorLiveData()
    var metPredicate = false
    mutableLiveData.addSource(this) {
        if(metPredicate || predicate(it)) {
            metPredicate=true
            mutableLiveData.value = it
        }
    }
    return mutableLiveData
}

/**
 * Emits only the values that are not null
 */
fun <T> LiveData<T>.nonNullValues(): NonNullLiveData<T> {
    return NonNullLiveData(this)
}


/**
 * Emits the default value when a null value is emitted
 */
fun <T> LiveData<T>.defaultIfNull(default:T):LiveData<T>{
    val mutableLiveData:MediatorLiveData<T> = MediatorLiveData()
    mutableLiveData.addSource(this) {
        mutableLiveData.value = it ?: default
    }
    return mutableLiveData
}

/**
 * Emits the items that are different from all the values that have been emitted so far
 */
fun <T> LiveData<T>.distinct(): LiveData<T> {
    val mutableLiveData: MediatorLiveData<T> = MediatorLiveData()
    val dispatchedValues = mutableListOf<T?>()
    mutableLiveData.addSource(this) {
        if(!dispatchedValues.contains(it)) {
            mutableLiveData.value = it
            dispatchedValues.add(it)
        }
    }
    return mutableLiveData
}



/**
 * Emits the items that are different from the last item
 */
fun <T> LiveData<T>.distinctUntilChanged(): LiveData<T> {
    val mutableLiveData: MediatorLiveData<T> = MediatorLiveData()
    var latestValue : T? = null
    mutableLiveData.addSource(this) {
        if(latestValue!=it) {
            mutableLiveData.value = it
            latestValue = it
        }
    }
    return mutableLiveData
}


