package com.wj.audio;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import com.wj.audio.utils.RxPermissions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import rx.functions.Action1;
import rx.observers.AssertableSubscriber;
import rx.observers.SafeSubscriber;

public class MainActivity extends AppCompatActivity {

  @InjectView(R.id.uri)
  Button    uri;
  @InjectView(R.id.videoview)
  Button    videoview;
  @InjectView(R.id.wj_vv)
  VideoView wjVv;

  private Context context;
  //权限申请
  private static final String PACKAGE_URL_SCHEME = "package:";
  //项目中文件名称
  private static final String assetsName         = "music.mp4";
  //sd卡中的路径
  private              String sdPath             = "";
  //sd卡中的名称
  private              String sdName             = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.inject(this);
    context = this;
    sdPath = Environment.getExternalStorageDirectory().getPath() + "/wj";
    sdName = Environment.getExternalStorageDirectory().getPath() + "/wj/music.mp4";
    copyMusicFile(assetsName, sdPath, sdName);
  }

  @OnClick({ R.id.uri, R.id.videoview })
  public void onViewClicked(View view) {
    RxPermissions.getInstance(context)
        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .subscribe(new Action1<Boolean>() {
          @Override
          public void call(Boolean aBoolean) {
            if (aBoolean) {
              switch (view.getId()) {
                case R.id.uri:
                  showByUri();
                  break;
                case R.id.videoview:
                  showByVideoView();
                  break;
              }
            } else {
              new MaterialDialog.Builder(context).title("帮助")
                  .content(R.string.string_help_text)
                  .positiveText("退出")
                  .negativeText("设置")
                  .onNegative((dialog, which) -> startAppSettings())
                  .show();
            }
          }
        });
  }

  // 启动应用的设置
  private void startAppSettings() {
    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.setData(Uri.parse(PACKAGE_URL_SCHEME + getPackageName()));
    startActivity(intent);
  }

  /**
   * 使用VideoView展示
   */
  private void showByVideoView() {
    Uri uri = Uri.parse(sdName);
    wjVv.setMediaController(new MediaController(context));
    wjVv.setVideoURI(uri);
  }

  /**
   * 使用自带视频播放器播放
   */
  private void showByUri() {
    Uri uri = Uri.parse(sdName);
    Intent intentUri = new Intent(Intent.ACTION_VIEW);
    intentUri.setDataAndType(uri, "video/mp4");
    startActivity(intentUri);
  }

  /**
   * 将文件拷贝到sd卡中
   *
   * @param oldPath 项目中的文件路径
   * @param newPath 要拷贝到sd卡的文件夹路径
   * @param fileName 要拷贝到sd卡的文件路径 包含到文件名
   */
  public void copyMusicFile(String oldPath, String newPath, String fileName) {
    try {
      InputStream is = context.getAssets().open(oldPath);
      File file = new File(newPath);
      if (!file.exists()) {//如果文件夹不存在，则创建新的文件夹
        file.mkdirs();
      }
      File fileCopy = new File(fileName);
      FileOutputStream fos = new FileOutputStream(fileCopy);
      byte[] buffer = new byte[1024];
      int byteCount = 0;
      while ((byteCount = is.read(buffer)) != -1) {// 循环从输入流读取
        // buffer字节
        fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
      }
      fos.flush();// 刷新缓冲区
      is.close();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
