package com.wj.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import butterknife.ButterKnife;
import butterknife.InjectView;
import java.io.IOException;

/**
 * Created by Hannah on 2018/6/12.
 */

public class SurfaceActivity extends AppCompatActivity
    implements SurfaceHolder.Callback, MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnPreparedListener,
    MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnVideoSizeChangedListener {

  @InjectView(R.id.sfv)
  SurfaceView sfv;

  private Context       context;
  private String        dataPath;
  private SurfaceHolder holder;
  private MediaPlayer   player;
  private Display       defaultDisplay;

  @Override
  protected void onCreate(
      @Nullable
          Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_surface);
    ButterKnife.inject(this);
    context = this;

    //获取数据
    dataPath = getIntent().getStringExtra("dataPath");

    //给SurfaceView添加CallBack监听
    holder = sfv.getHolder();
    holder.addCallback(this);
    //为了可以播放视频或者使用Camera预览，我们需要指定其Buffer类型
    holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    //下面开始实例化MediaPlayer对象
    player = new MediaPlayer();
    player.setOnCompletionListener(this);
    player.setOnErrorListener(this);
    player.setOnInfoListener(this);
    player.setOnPreparedListener(this);
    player.setOnSeekCompleteListener(this);
    player.setOnVideoSizeChangedListener(this);

    //设置资源
    try {
      player.setDataSource(dataPath);
    } catch (IOException e) {
      e.printStackTrace();
    }

    defaultDisplay = this.getWindowManager().getDefaultDisplay();
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {
    Log.v("SurfaceActivity:LOG", "创建");
    // 当SurfaceView中的Surface被创建的时候被调用
    //在这里我们指定MediaPlayer在当前的Surface中进行播放
    player.setDisplay(holder);
    //在指定了MediaPlayer播放的容器后，我们就可以使用prepare或者prepareAsync来准备播放了
    player.prepareAsync();
  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    Log.v("SurfaceActivity:LOG", "改变");
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    Log.v("SurfaceActivity:LOG", "销毁");
  }

  @Override
  public void onCompletion(MediaPlayer mediaPlayer) {
    Log.v("SurfaceActivity:LOG", "onCompletion 播放完成");
    finish();
  }

  @Override
  public boolean onError(MediaPlayer player, int whatError, int extra) {
    Log.v("SurfaceActivity:LOG", "onError");
    switch (whatError) {
      case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
        Log.v("Play Error:::", "MEDIA_ERROR_SERVER_DIED");
        break;
      case MediaPlayer.MEDIA_ERROR_UNKNOWN:
        Log.v("Play Error:::", "MEDIA_ERROR_UNKNOWN");
        break;
      default:
        break;
    }
    return false;
  }

  @Override
  public boolean onInfo(MediaPlayer mediaPlayer, int whatInfo, int i1) {
    //当一些特定信息出现或者警告时触发
    switch (whatInfo) {
      case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
        break;
      case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
        break;
      case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
        break;
      case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
        break;
    }
    return false;
  }

  @Override
  public void onPrepared(MediaPlayer mediaPlayer) {
    Log.v("SurfaceActivity:LOG", "onPrepared 准备");
    //当prepare完成后，该方法触发，在这里我们播放视频
    int videoWidth = mediaPlayer.getVideoWidth();
    int videoHeight = mediaPlayer.getVideoHeight();
    if (videoWidth > defaultDisplay.getWidth() || videoHeight > defaultDisplay.getHeight()) {
      //如果video的宽或者高超出了当前屏幕的大小，则要进行缩放
      //计算缩放比例
      float scaleW = videoWidth / defaultDisplay.getWidth();
      float scaleH = videoHeight / defaultDisplay.getHeight();

      //获取最大的那个
      float max = Math.max(scaleW, scaleH);
      videoWidth = (int) Math.ceil(videoWidth / max);
      videoHeight = (int) Math.ceil(videoHeight / max);

      //设置宽高参数
      sfv.setLayoutParams(new LinearLayout.LayoutParams(videoWidth, videoHeight));
      //开始播放视频
      player.start();
    }
  }

  @Override
  public void onSeekComplete(MediaPlayer mediaPlayer) {
    //seek操作完成时触发
    Log.v("SurfaceActivity:LOG", "onSeekComplete");
  }

  @Override
  public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i1) {
    // 当Surface尺寸等参数改变时触发
    Log.v("SurfaceActivity:LOG", "onVideoSizeChanged 尺寸发生变化");
  }
}
