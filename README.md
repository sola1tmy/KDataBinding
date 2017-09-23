# KDataBinding
Kotlin single file library to bind value and view.

### How to use
````
Config.intValue = canBeBind(this::intValue, 10)
TextView.bindText(Config::intValue)

Config.hasNewVersion:Boolean = SharedPreferenceReadWriteProperty().canBeBind(this, this::hasNewVersion)
ImageView.bind(Config::hasNewVersion) { bool ->
  setImageResource(if(bool) R.drawable.new : R.drawable.empty)
}
````
