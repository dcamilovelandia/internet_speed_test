package com.tahamalas.internet_speed_test

import android.app.Activity
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.IRepeatListener
import fr.bmartel.speedtest.inter.ISpeedTestListener
import fr.bmartel.speedtest.model.SpeedTestError
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar


/** InternetSpeedTestPlugin */
public class InternetSpeedTestPlugin(internal var activity: Activity, internal var methodChannel: MethodChannel, registrar: Registrar) : MethodCallHandler {


    private var result: Result? = null
    private var speedTestSocket: SpeedTestSocket = SpeedTestSocket()


    init {
        this.methodChannel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        this.result = result
        when {
            call.method == "startListening" -> mapToCall(result, call.arguments)
            call.method == "cancelListening" -> cancelListening(call.arguments, result)
            else -> result.notImplemented()
        }
    }

    private fun mapToCall(result: Result, arguments: Any?) {
        val argsMap = arguments as Map<*, *>

        when (val args = argsMap["id"] as Int) {
            CallbacksEnum.START_DOWNLOAD_TESTING.ordinal -> startListening(args, result, "startDownloadTesting", argsMap["testServer"] as String)
            CallbacksEnum.START_UPLOAD_TESTING.ordinal -> startListening(args, result, "startUploadTesting", argsMap["testServer"] as String)
        }
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "internet_speed_test")
            channel.setMethodCallHandler(InternetSpeedTestPlugin(registrar.activity(), channel, registrar))
        }
    }

    private val callbackById: MutableMap<Int, Runnable> = mutableMapOf()

    fun startListening(args: Any, result: Result, methodName: String, testServer: String) {
        // Get callback id
        val currentListenerId = args as Int
        val runnable = Runnable {
            if (callbackById.containsKey(currentListenerId)) {
                val argsMap: MutableMap<String, Any> = mutableMapOf()
                argsMap["id"] = currentListenerId
                when (methodName) {
                    "startDownloadTesting" -> {
                        testDownloadSpeed(object : TestListener {
                            override fun onComplete(transferRate: Double) {
                                argsMap["transferRate"] = transferRate
                                argsMap["type"] = ListenerEnum.COMPLETE.ordinal
                                activity.runOnUiThread {
                                    methodChannel.invokeMethod("callListener", argsMap)
                                }
                            }

                            override fun onError(speedTestError: String, errorMessage: String) {
                                argsMap["speedTestError"] = speedTestError
                                argsMap["errorMessage"] = errorMessage
                                argsMap["type"] = ListenerEnum.ERROR.ordinal
                                activity.runOnUiThread {
                                    methodChannel.invokeMethod("callListener", argsMap)
                                }
                            }

                            override fun onProgress(percent: Double, transferRate: Double) {
                                argsMap["percent"] = percent
                                argsMap["transferRate"] = transferRate
                                argsMap["type"] = ListenerEnum.PROGRESS.ordinal
                                activity.runOnUiThread {
                                    methodChannel.invokeMethod("callListener", argsMap)
                                }
                            }
                        }, testServer)
                    }
                    "startUploadTesting" -> {
                        testUploadSpeed(object : TestListener {
                            override fun onComplete(transferRate: Double) {
                                argsMap["transferRate"] = transferRate
                                argsMap["type"] = ListenerEnum.COMPLETE.ordinal
                                activity.runOnUiThread {
                                    methodChannel.invokeMethod("callListener", argsMap)
                                }
                            }

                            override fun onError(speedTestError: String, errorMessage: String) {
                                argsMap["speedTestError"] = speedTestError
                                argsMap["errorMessage"] = errorMessage
                                argsMap["type"] = ListenerEnum.ERROR.ordinal
                                activity.runOnUiThread {
                                    methodChannel.invokeMethod("callListener", argsMap)
                                }
                            }

                            override fun onProgress(percent: Double, transferRate: Double) {
                                argsMap["percent"] = percent
                                argsMap["transferRate"] = transferRate
                                argsMap["type"] = ListenerEnum.PROGRESS.ordinal
                                activity.runOnUiThread {
                                    methodChannel.invokeMethod("callListener", argsMap)
                                }
                            }
                        }, testServer)
                    }

                }
                // Send some value to callback

            }
        }
        val thread = Thread(runnable)
        callbackById[currentListenerId] = runnable
        thread.start()
        // Return immediately
        result.success(null)
    }

    private fun testUploadSpeed(testListener: TestListener, testServer: String) {
        // add a listener to wait for speedtest completion and progress
        speedTestSocket.addSpeedTestListener(object : ISpeedTestListener {
            override fun onCompletion(report: SpeedTestReport) {
               testListener.onComplete(report.transferRateBit.toDouble())
            }

            override fun onError(speedTestError: SpeedTestError, errorMessage: String) {
                // called when a download/upload error occur
                // println("OnError: ${speedTestError.name}, $errorMessage")
                testListener.onError(errorMessage, speedTestError.name)
            }

            override fun onProgress(percent: Float, report: SpeedTestReport) {
               testListener.onProgress(percent.toDouble(), report.transferRateBit.toDouble())
            }
        })
       speedTestSocket.startFixedUpload("http://ipv4.ikoula.testdebit.info/", 1000000, 10000, 400)
    }

    private fun testDownloadSpeed(testListener: TestListener, testServer: String) {
        // add a listener to wait for speedtest completion and progress
        speedTestSocket.addSpeedTestListener(object : ISpeedTestListener {
            override fun onCompletion(report: SpeedTestReport) {
               testListener.onComplete(report.transferRateBit.toDouble())
            }

            override fun onError(speedTestError: SpeedTestError, errorMessage: String) {
                // called when a download/upload error occur
                // println("OnError: ${speedTestError.name}, $errorMessage")
                testListener.onError(errorMessage, speedTestError.name)
            }

            override fun onProgress(percent: Float, report: SpeedTestReport) {
               testListener.onProgress(percent.toDouble(), report.transferRateBit.toDouble())
            }
        })
       speedTestSocket.startFixedDownload("http://ipv4.ikoula.testdebit.info/1M.iso", 10000, 400)
    }

    private fun cancelListening(args: Any, result: Result) {
        // Get callback id
        val currentListenerId = args as Int
        // Remove callback
        callbackById.remove(currentListenerId)
        // Do additional stuff if required to cancel the listener
        result.success(null)
    }
}
