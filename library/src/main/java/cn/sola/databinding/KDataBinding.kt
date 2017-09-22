package cn.sola.databinding;

import android.view.View
import android.widget.TextView
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by sola_tmy on 2017/9/8.
 * KDataBinding is a simple library to bind a property and a View
 *
 * You can use it like this
 * When bind to a property: var intValue by canBeBind(key, defaultValue)
 * When bind to ReadWriteProperty: var readWriteProperty by MyReadWriteProperty().canBeBind(key)
 *
 * Then you can bind to a view like this
 * View.bind(key){value -> doSomeThing(value)}
 * TextView.bindText(key) //this will do textView.setText(value)
 */
abstract class KDataBinding<R, T> : ReadWriteProperty<R, T>{

    val list = arrayListOf<()->Unit>()

    open fun addListener(view:View, doWhat: () -> Unit){
        list.add(doWhat)
        doWhat()
        view.addOnAttachStateChangeListener(object :View.OnAttachStateChangeListener{
            override fun onViewDetachedFromWindow(v: View?) {
                list.remove(doWhat)
            }

            override fun onViewAttachedToWindow(v: View?) {}
        })
    }

    abstract fun value(): T?

}

/**
 *
 */
class PropertyBinding<R, T>(var realValue: T): KDataBinding<R, T>() {
    override fun value(): T { return realValue }

    override fun getValue(thisRef: R, property: KProperty<*>): T { return realValue }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        this.realValue = value
        list.forEach { it() }
    }

}

/**
 *
 */
class EmptyBinding<R, T>:KDataBinding<R, T>() {
    override fun value(): T { throw Exception("This exception should never throw") }

    override fun addListener(view: View, doWhat: () -> Unit){}

    override fun getValue(thisRef: R, property: KProperty<*>): T { throw Exception("This exception should never throw") }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {}

}

class DelegateBinding<R, T>(val parent: ReadWriteProperty<R, T>): KDataBinding<R, T>(){

    var lastValue:T? = null

    override fun value(): T? {
        return lastValue
    }


    override fun getValue(thisRef: R, property: KProperty<*>): T { return parent.getValue(thisRef, property) }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        parent.setValue(thisRef, property, value)
        list.forEach { it() }
    }

}

val map = hashMapOf<String, KDataBinding<*, *>>()

inline fun getBinding(key:String): KDataBinding<*, *>{
    try {
        if (map[key] == null) {
            throw NullPointerException("")
        } else {
            return map[key]!!
        }
    } catch (e: Throwable){
        e.printStackTrace()
        return EmptyBinding<Any, Any>()
    }
}

inline fun TextView.bindText(key:String) {
    val dataBinding: KDataBinding<*, *> = getBinding(key)
    val todo = { this.text = dataBinding.value().toString() }
    dataBinding.addListener(this, todo)
}

inline fun View.bind(key:String, noinline doWhat:(Any?)-> Unit) {
    val dataBinding: KDataBinding<*, *> = getBinding(key)
    val todo = { doWhat(dataBinding.value())}
    dataBinding.addListener(this, todo)
}

inline fun View.bind(key1:String, key2:String, noinline doWhat:(Any?, Any?)-> Unit) {
    val dataBinding1: KDataBinding<*, *> = getBinding(key1)
    val dataBinding2: KDataBinding<*, *> = getBinding(key2)

    val todo = { doWhat(dataBinding1.value(), dataBinding2.value()) }

    dataBinding1.addListener(this, todo)
    dataBinding2.addListener(this, todo)
}

inline fun  ReadWriteProperty<*, *>.canBeBind(key:String): ReadWriteProperty<*, *> {
    return DelegateBinding(this).apply { map.put(key, this) }
}

inline fun <R, T> canBeBind(key:String, value:T): ReadWriteProperty<R, T> {
    return PropertyBinding<R, T>(value).apply { map.put(key, this) }
}