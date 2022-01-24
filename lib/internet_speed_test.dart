import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:internet_speed_test/callbacks_enum.dart';
import 'package:tuple/tuple.dart';

typedef void CancelListening();
typedef void DoneCallback(double transferRate, SpeedUnit unit);
typedef void ProgressCallback(
  double percent,
  double transferRate,
  SpeedUnit unit,
);
typedef void ErrorCallback(String errorMessage, String speedTestError);

class InternetSpeedTest {
  static const MethodChannel _channel =
      const MethodChannel('internet_speed_test');

  Map<int, Tuple3<ErrorCallback, ProgressCallback, DoneCallback>>
      _callbacksById = new Map();

  DateTime _startTime = DateTime.now();
  int _fileSize = 1000000;

  _finishTest(MethodCall call){
    int testTime = DateTime.now().difference(_startTime).inMicroseconds;
    double speed = (8 * _fileSize) / testTime;
    // print("Measured Download Speed: $downloadSpeed");
    _callbacksById[call.arguments["id"]]!.item3(speed, SpeedUnit.Mbps);
    _callbacksById.remove(call.arguments["id"]);
  }

  Future<void> _methodCallHandler(MethodCall call) async {
    switch (call.method) {
      case 'callListener':
        if (call.arguments["id"] as int ==
            CallbacksEnum.START_DOWNLOAD_TESTING.index) {
          if (call.arguments['type'] == ListenerEnum.COMPLETE.index) {
            _finishTest(call);
          } else if (call.arguments['type'] == ListenerEnum.ERROR.index) {
            _callbacksById[call.arguments["id"]]!.item1(
                call.arguments['errorMessage'],
                call.arguments['speedTestError']);
            _callbacksById.remove(call.arguments["id"]);
          } else if (call.arguments['type'] == ListenerEnum.PROGRESS.index) {
            if(call.arguments['percent'].toDouble() == 100.0) _finishTest(call);
          }
        } else if (call.arguments["id"] as int ==
            CallbacksEnum.START_UPLOAD_TESTING.index) {
          if (call.arguments['type'] == ListenerEnum.COMPLETE.index) {
            _finishTest(call);
          
          } else if (call.arguments['type'] == ListenerEnum.ERROR.index) {
            _callbacksById[call.arguments["id"]]!.item1(
                call.arguments['errorMessage'],
                call.arguments['speedTestError']);
          } else if (call.arguments['type'] == ListenerEnum.PROGRESS.index) {
            if(call.arguments['percent'].toDouble() == 100.0) _finishTest(call);
          }
        }
//        _callbacksById[call.arguments["id"]](call.arguments["args"]);
        break;
      default:
        print(
            'TestFairy: Ignoring invoke from native. This normally shouldn\'t happen.');
    }

    _channel.invokeMethod("cancelListening", call.arguments["id"]);
  }

  Future<CancelListening> _startListening(
      Tuple3<ErrorCallback, ProgressCallback, DoneCallback> callback,
      CallbacksEnum callbacksEnum,
      String testServer,
      {Map<String, dynamic>? args,
      int fileSize = 1000000}) async {
    _startTime = DateTime.now();
    _fileSize = fileSize;
    _channel.setMethodCallHandler(_methodCallHandler);
    int currentListenerId = callbacksEnum.index;
    _callbacksById[currentListenerId] = callback;
    await _channel.invokeMethod(
      "startListening",
      {
        'id': currentListenerId,
        'args': args,
        'testServer': testServer,
        'fileSize': fileSize,
      },
    );
    return () {
      _channel.invokeMethod("cancelListening", currentListenerId);
      _callbacksById.remove(currentListenerId);
    };
  }

  Future<CancelListening> startDownloadTesting(
      {required DoneCallback onDone,
      required ProgressCallback onProgress,
      required ErrorCallback onError,
      int fileSize = 200000,
      String testServer = 'http://ipv4.ikoula.testdebit.info/1M.iso'}) async {
      
    return await _startListening(Tuple3(onError, onProgress, onDone),
        CallbacksEnum.START_DOWNLOAD_TESTING, testServer,
        fileSize: fileSize);
  }

  Future<CancelListening> startUploadTesting({
    required DoneCallback onDone,
    required ProgressCallback onProgress,
    required ErrorCallback onError,
    int fileSize = 200000,
    String testServer = 'http://ipv4.ikoula.testdebit.info/',
  }) async {
    return await _startListening(Tuple3(onError, onProgress, onDone),
        CallbacksEnum.START_UPLOAD_TESTING, testServer,
        fileSize: fileSize);
  }
}
