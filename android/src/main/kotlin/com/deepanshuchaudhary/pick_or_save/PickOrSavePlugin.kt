package com.deepanshuchaudhary.pick_or_save

import android.util.Log

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** PickOrSavePlugin */
class PickOrSavePlugin : FlutterPlugin, ActivityAware, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    private var pickOrSave: PickOrSave? = null
    private var pluginBinding: FlutterPlugin.FlutterPluginBinding? = null
    private var activityBinding: ActivityPluginBinding? = null

    companion object {
        const val LOG_TAG = "PickOrSavePlugin"
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(LOG_TAG, "onAttachedToEngine - IN")

        if (pluginBinding != null) {
            Log.w(LOG_TAG, "onAttachedToEngine - already attached")
        }

        pluginBinding = flutterPluginBinding

        val messenger = pluginBinding?.binaryMessenger
        doOnAttachedToEngine(messenger!!)

        Log.d(LOG_TAG, "onAttachedToEngine - OUT")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(LOG_TAG, "onDetachedFromEngine")
        doOnDetachedFromEngine()
    }

    // note: this may be called multiple times on app startup
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.d(LOG_TAG, "onAttachedToActivity")
        doOnAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        Log.d(LOG_TAG, "onDetachedFromActivity")
        doOnDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Log.d(LOG_TAG, "onReattachedToActivityForConfigChanges")
        doOnAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Log.d(LOG_TAG, "onDetachedFromActivityForConfigChanges")
        doOnDetachedFromActivity()
    }

    private fun doOnAttachedToEngine(messenger: BinaryMessenger) {
        Log.d(LOG_TAG, "doOnAttachedToEngine - IN")

        this.channel = MethodChannel(messenger, "pick_or_save")
        this.channel.setMethodCallHandler(this)

        Log.d(LOG_TAG, "doOnAttachedToEngine - OUT")
    }

    private fun doOnDetachedFromEngine() {
        Log.d(LOG_TAG, "doOnDetachedFromEngine - IN")

        if (pluginBinding == null) {
            Log.w(LOG_TAG, "doOnDetachedFromEngine - already detached")
        }
        pluginBinding = null

        this.channel.setMethodCallHandler(null)

        Log.d(LOG_TAG, "doOnDetachedFromEngine - OUT")
    }

    private fun doOnAttachedToActivity(activityBinding: ActivityPluginBinding?) {
        Log.d(LOG_TAG, "doOnAttachedToActivity - IN")

        this.activityBinding = activityBinding

        Log.d(LOG_TAG, "doOnAttachedToActivity - OUT")
    }

    private fun doOnDetachedFromActivity() {
        Log.d(LOG_TAG, "doOnDetachedFromActivity - IN")

        if (pickOrSave != null) {
            activityBinding?.removeActivityResultListener(pickOrSave!!)
            pickOrSave = null
        }
        activityBinding = null

        Log.d(LOG_TAG, "doOnDetachedFromActivity - OUT")
    }


    override fun onMethodCall(call: MethodCall, result: Result) {
        Log.d(LOG_TAG, "onMethodCall - IN , method=${call.method}")
        if (pickOrSave == null) {
            if (!createPickOrSave()) {
                result.error("init_failed", "Not attached", null)
                return
            }
        }
        when (call.method) {
            "pickFiles" -> pickOrSave!!.pickFile(
                result,
                fileExtensionsFilter = parseMethodCallArrayOfStringArgument(
                    call,
                    "fileExtensionsFilter"
                ),
                mimeTypeFilter = parseMethodCallArrayOfStringArgument(call, "mimeTypeFilter"),
                localOnly = call.argument("localOnly") as Boolean? == true,
                copyFileToCacheDir = call.argument("copyFileToCacheDir") as Boolean? != false,
                filePickingType = parseMethodCallFilePickingTypeArgument(call)
                    ?: FilePickingType.SINGLE
            )
            "saveFiles" -> pickOrSave!!.saveFile(
                result,
                sourceFilesPaths = parseMethodCallArrayOfStringArgument(call, "sourceFilesPaths"),
                data = parseMethodCallArrayOfByteArgument(call, "data"),
                filesNames = parseMethodCallArrayOfStringArgument(call, "filesNames"),
                mimeTypesFilter = parseMethodCallArrayOfStringArgument(call, "mimeTypesFilter"),
                localOnly = call.argument("localOnly") as Boolean? == true,
            )
            "fileMetaData" -> pickOrSave!!.fileMetaData(
                result,
                sourceFileUri = call.argument("sourceFileUri"),
                sourceFilePath = call.argument("sourceFilePath")
            )
            "cancelFilesSaving" -> pickOrSave!!.cancelFilesSaving()
            else -> result.notImplemented()
        }
    }

    private fun createPickOrSave(): Boolean {
        Log.d(LOG_TAG, "createPickOrSave - IN")

        var pickOrSave: PickOrSave? = null
        if (activityBinding != null) {
            pickOrSave = PickOrSave(
                activity = activityBinding!!.activity
            )
            activityBinding!!.addActivityResultListener(pickOrSave)
        }
        this.pickOrSave = pickOrSave

        Log.d(LOG_TAG, "createPickOrSave - OUT")

        return pickOrSave != null
    }

    private fun parseMethodCallArrayOfStringArgument(
        call: MethodCall,
        arg: String
    ): Array<String>? {
        if (call.hasArgument(arg)) {
            return call.argument<ArrayList<String>>(arg)?.toTypedArray()
        }
        return null
    }

    private fun parseMethodCallArrayOfByteArgument(
        call: MethodCall,
        arg: String
    ): Array<ByteArray>? {
        if (call.hasArgument(arg)) {
            return call.argument<ArrayList<ByteArray>>(arg)?.toTypedArray()
        }
        return null
    }

    private fun parseMethodCallFilePickingTypeArgument(call: MethodCall): FilePickingType? {
        val arg = "filePickingType"
        if (call.hasArgument(arg)) {
            return if (call.argument<String>(arg)?.toString() == "FilePickingType.multiple") {
                FilePickingType.MULTIPLE
            } else {
                FilePickingType.SINGLE
            }
        }
        return null
    }
}
