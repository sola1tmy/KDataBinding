package com.mtn.iceman3d.printer.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/**
 * Created by sola_tmy on 2017/9/8.
 * KDataBinding is a simple library to bind a property and a View
 *
 * You can use it like this
 * When bind to a property: var intValue by canBeBind(this::intValue, 10)
 * When bind to ReadWriteProperty: var readWriteProperty by MyReadWriteProperty().canBeBind(this, this::readWriteProperty)
 *
 * Then you can bind to a view like this
 * View.bind(key){value -> doSomeThing(value)}
 * TextView.bindText(key) //this will do textView.setText(value)
 */
abstract class KDataBinding<in R, T> : ReadWriteProperty<R, T>{

    val list = arrayListOf<()->Unit>()

    open fun bindListener(view:View, doWhat: () -> Unit){
        DEFAULT_HANDLER.post { doWhat()}
        list.add({ DEFAULT_HANDLER.post { doWhat() }})

        view.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener{
            override fun onViewDetachedFromWindow(v: View?) {
                list.remove(doWhat)
            }

            override fun onViewAttachedToWindow(v: View?) {}
        })
    }

    abstract fun value(): T?
}

val TAG = "KDataBinding"
val DEFAULT_HANDLER:Handler = Handler(Looper.getMainLooper())
val DATA_BINDING_MAP = hashMapOf<KProperty<*>, KDataBinding<*, *>>()


/**
 * For properties
 */
class PropertyBinding<T>(private var realValue: T): KDataBinding<Any?, T>() {

    override fun value(): T = realValue

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = realValue

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.realValue = value
        list.forEach { it() }
    }

}

/**
 * When you want to get a value which not called canBeBind
 * An EmptyBinding will return
 */
class EmptyBinding<T>: KDataBinding<Any, T>() {
    override fun value(): T { throw Exception("This exception should never be thrown") }

    override fun bindListener(view: View, doWhat: () -> Unit){}

    override fun getValue(thisRef: Any, property: KProperty<*>): T { throw Exception("This exception should be never thrown") }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        Log.e(TAG, "You've bind a view with a property which can not be bind")
    }

}

/**
 * For expand existing delegate
 */
class DelegateBinding<in R, T>(private val parent: ReadWriteProperty<R, T>, private val thisRef: R, private val property: KProperty<*>): KDataBinding<R, T>(){

    override fun value(): T? = parent.getValue(thisRef, property)

    override fun getValue(thisRef: R, property: KProperty<*>): T = parent.getValue(thisRef, property)

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        parent.setValue(thisRef, property, value)
        list.forEach { it() }
    }

}

internal fun <T> getBinding(property: KProperty<T>): KDataBinding<*, T> =
        if (DATA_BINDING_MAP[property] == null)  EmptyBinding() else (DATA_BINDING_MAP[property] as KDataBinding<*, T>)

fun TextView.bindText(property: KProperty<*>) {
    val dataBinding: KDataBinding<*, *> = getBinding(property)
    val todo = { this.text = dataBinding.value().toString() }
    dataBinding.bindListener(this, todo)
}

fun <T: View, R> T.bind(property: KProperty<R>, doWhat:T.(R?)-> Unit) {
    val dataBinding: KDataBinding<*, R> = getBinding(property)
    val todo = { doWhat(dataBinding.value())}
    dataBinding.bindListener(this, todo)
}

fun <T: View, R1, R2> T.bind(p1: KProperty<R1>, p2: KProperty<R2>, doWhat: T.(R1?, R2?) -> Unit) {
    val dataBinding1: KDataBinding<*, R1> = getBinding(p1)
    val dataBinding2: KDataBinding<*, R2> = getBinding(p2)

    val todo = { doWhat(dataBinding1.value(), dataBinding2.value())}
    dataBinding1.bindListener(this, todo)
    dataBinding2.bindListener(this, todo)
}

fun <T: View, R1, R2, R3> T.bind(p1: KProperty<R1>, p2: KProperty<R2>, p3: KProperty<R3>, doWhat: T.(R1?, R2?, R3?) -> Unit) {
    val dataBinding1: KDataBinding<*, R1> = getBinding(p1)
    val dataBinding2: KDataBinding<*, R2> = getBinding(p2)
    val dataBinding3: KDataBinding<*, R3> = getBinding(p3)

    val todo = { doWhat(dataBinding1.value(), dataBinding2.value(), dataBinding3.value())}
    dataBinding1.bindListener(this, todo)
    dataBinding2.bindListener(this, todo)
    dataBinding3.bindListener(this, todo)
}

fun <R, T> ReadWriteProperty<R, T>.canBeBind(thisRef: R, property: KProperty<*>): ReadWriteProperty<R, T> =
        DelegateBinding(this, thisRef, property).apply { DATA_BINDING_MAP.put(property, this) }

fun <T> canBeBind(property: KProperty<*>, value: T) =
        PropertyBinding(value).apply { DATA_BINDING_MAP.put(property, this) }