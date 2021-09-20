/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.leakcanary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {

  private HttpRequestHelper httpRequestHelper;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);

    View button = findViewById(R.id.async_work);
    button.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        startAsyncWork();
      }
    });

    httpRequestHelper = (HttpRequestHelper) getLastNonConfigurationInstance();
    if (httpRequestHelper == null) {
      httpRequestHelper = new HttpRequestHelper(button);
    }
  }

  @Override public Object onRetainNonConfigurationInstance() {
    return httpRequestHelper;
  }

  @SuppressLint("StaticFieldLeak")
  void startAsyncWork() {
    // This runnable is an anonymous class and therefore has a hidden reference to the outer
    // class MainActivity. If the activity gets destroyed before the thread finishes (e.g. rotation),
    // the activity instance will leak.

    /**
     * 内部类拥有外部类的引用(隐藏)，编译会生产两个class文件，二进制代码的构造函数，传入外部类对象
     *
     *
     * method constructor <init>(Lcom/example/leakcanary/MainActivity;)V
     * registers 2
     * param p1, "this$0"    # Lcom/example/leakcanary/MainActivity;
     */
    // 局部变量引用work
    Runnable work = new Runnable() {
      @Override public void run() {
        // Do some slow work in background
//        SystemClock.sleep(10 * 1000);
        try {
          Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
          Log.i("LeakCanary", "e = "+e.getMessage());
          e.printStackTrace();
        }
        Log.i("LeakCanary", "异步任务做完了哈");
      }
    };
    // Thread local  可以作为gc root
    // 线程没死，回调的runnable还没有跑完，runnable， 匿名内部类外部类引用
    thread = new Thread(work);
    thread.start();
  }
  Thread thread;
  @Override
  protected void onDestroy() {
    if (thread != null) {
      thread.interrupt();
      thread = null;
      Log.i("LeakCanary", "thread = null");
    }

    super.onDestroy();
  }
}


