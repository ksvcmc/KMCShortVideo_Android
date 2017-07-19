# 金山魔方短视频API文档
## 项目背景
金山魔方是一个多媒体能力提供平台，通过统一接入API、统一鉴权、统一计费等多种手段，降低客户接入多媒体处理能力的代价，提供多媒体能力供应商的效率

## 安装
  app目录:
+ **app**:示例工程
+ **app/libs**: 魔方sdk包 libkmcshortvideo.jar，以及厂家sdk包

## SDK包总体介绍
+ KMCMaterial 素材类(主题、音乐、滤镜)
+ KMCShortVideo为短视频编辑类
+ KMCAuthManager 提供鉴权功能
+ KMCMaterialManager 提供素材列表查下、素材下载等功能


## SDK使用指南
#### 1. KMCAuthManager  
+ **鉴权**  
  本sdk包采用鉴权加密方式，需要通过商务渠道拿到授权的token信息，方可以使用，具体请咨询商务。  
鉴权函数如下，其中auth为ak信息，date为过期时间。  

```java
/**
 * @param context
 * @param auth token
 * @param listener 注册结果的回调
 */
void authorize(Context context, String token, AuthResultListener listener)；
```

#### 2. KMCShortVideo
+ **初始化**
```java
/**
  * init the sdk with activity
  * this function load the sdk ,so must be called before setContentView
  */
void init(Activity activity);
```

+ **release**
```java
 /**
   * release
   * this function should only be called when you don't want to use sdk anymore
   */
 void release();
```

+ **attachWindow**
```java
 /**
   * attach window
   * @param window 用于预览的窗口
   * @param videoConfig 视频参数
   * @param audioConfig 音频参数
   * @return true if success
   */
 boolean attachWindow(NvsLiveWindow window, KMCVideoConfig videoConfig,
                                 KMCAudioConfig audioConfig);
```

+ **insertVideoClip**
```java
 /**
   * insert video or photo at given index
   */
 boolean insertVideoClip(String filePath, int index)
```

+ **removeVideoClip**
```java
 boolean removeVideoClip(int index)
```

+ **removeAllVideoClips**
```java
 boolean removeAllVideoClips()
```

+ **play**
```java
 /**
   * play the video from startTime to endTime
   */
 void play(long startTime, long endTime)
```

+ **stop**
```java
 /**
   * stop
   */
 void stop()
```

+ **seekTo**
```java
 /**
   * seek to time， 单位微秒
   */
 void seekTo(long time)
```

+ **getDuration**
```java
 /**
   * 获取视频长度， 单位微秒
   */
 long getDuration(long time)
```

+ **getCurrentPlaybackTime**
```java
 /**
   *  获取当前播放时间，单位微秒
   */
 long getCurrentPlaybackTime(long time)
```

+ **getState**
```java
  /** get sdk state
    * STATE_STOPPED STATE_PLAYBACK STATE_SEEKING STATE_COMPILE
    */
 int getState()
```

+ **应用素材**
  素材下载完成后，然后调用KMCShortVideo的applyMaterial即可将素材应用于短视频
```java
/**
 激活素材
 @param material        需要展示的素材
 */
void applyMaterial(KMCArMaterial material);
```

+ **修改封面文案**
当应用了主题，且主题封面中有文字的时候，可以修改封面文案
```java
/**
  * 修改主题封面
  */
void setThemeTitleCaptionText(String text);
```

+ **getTrimIn**
```java
/**
  * 获取裁剪起始点(单位微秒)
  */
long getTrimIn();
```

+ **getTrimOut**
```java
/**
  * 获取裁剪终点(单位微秒)
  */
long getTrimOut();
```

+ **changeTrimInPoint**
```java
/**
  * 设置裁剪起始点(单位微秒)
  */
void changeTrimInPoint(int index, long trimIn);
```

+ **changeTrimOutPoint**
```java
/**
  * 设置裁剪终点(单位微秒)
  */
void changeTrimoutPoint(int index, long trimOut);
```

+ **getSpeed**
```java
/**
  * 获取的播放速度
  * 默认值为1，表示按正常速度播放;小于1的值表示慢放;大于1的值表示快放
  */
double getSpeed(int index);
```

+ **changeSpeed**
```java
/**
  * 设置播放速度
  */
void changeSpeed(int index, double speed);
```

+ **getRotation**
```java
/**
  * 获取视频旋转角度角度，默认值为0
  */
double getRotation(int index);
```

+ **changeRotation**
```java
/**
  * 设置旋转角度
  */
void changeRotation(int index, double rotate);
```

+ **getSaturation**
```java
/**
  * 饱和度
  * 默认值为1，取值范围0-10
  */
double getSaturation(int index);
```

+ **changeSaturation**
```java
/**
  * 设置饱和度
  */
void changeSaturation(int index, double saturation);
```

+ **getContrast**
```java
/**
  * 对比度
  * 默认值为1，取值范围0-10
  */
double getContrast(int index);
```

+ **changeContrast**
```java
/**
  * 设置对比度
  */
void changeContrast(int index, double contrast);
```

+ **getBrightness**
```java
/**
  * 亮度
  * 默认值为1，取值范围0-10
  */
double getContrast(int index);
```

+ **getBrightness**
```java
/**
  * 设置亮度
  */
void changeBrightness(int index, double bright);
```


+ **compile**
```java
/**
  * compile
  * @param startTime 开始时间。startTime取值范围在[0,timeline.duration - 1],传入其他值无效。
  * @param endTime 结束时间。endTime取值范围在(startTime,timeline.duration],同样传入其他值无效
  * @param outputFilePath 生成输出的文件路径
  * @param videoResolutionGrade 	生成文件输出的视频分辨率
  * @param videoBitrateGrade 生成文件输出的视频码率
  * @param flags 生成文件输出的特殊标志，如果没有特殊需求，请填写0
  * return 返回true，则启动打包成功；false，则打包启动失败
  * 注意：生成文件是异步操作
  */
boolean compile(long 	startTime,
                long 	endTime,
                String 	outputFilePath,
                int 	videoResolutionGrade,
                int 	videoBitrateGrade,
                int 	flags)
```

+ **getVideoConfig**
```java
/**
  * 获取视频的参数
  */
KMCVideoConfig getVideoConfig(String filePath);
```

+ **getAudioConfig**
```java
/**
  * 获取视频的参数
  */
KMCAudioConfig getAudioConfig(String filePath);
```

#### 3. KMCAuthManager
+ **拉取素材列表息**
 客户可以在控制台把贴纸放入一个group里面，sdk通过groupID进行拉取，相关函数为：
```java
void fetchMaterials(final Context context, final int materialType, 
                    final FetchMaterialListener listener)；
```
拉取成功后，资源索引文件，包括贴纸的下载地址，缩略图的下载地址，贴纸的手势ID,手势描述信息等，可以在此处设置UI相关信息。

+ **查询贴纸是否已经下载到本地**
```java
boolean isMaterialDownloaded(KMCMaterial material);
```

+ **下载素材**
素材大小不固定，大的可能几M，小的可能几十K,相关函数：
```java
void downloadMaterial(final Context context, final KMCMaterial material,
                      final DownloadMaterialListener listener);
```
## 接入流程
![金山魔方接入流程](https://raw.githubusercontent.com/wiki/ksvcmc/KMCFilter_Android/all.jpg "金山魔方接入流程")
## 接入步骤  
1.登录[金山云控制台]( https://console.ksyun.com)，选择视频服务-金山魔方
![步骤1](https://raw.githubusercontent.com/wiki/ksvcmc/KMCFilter_Android/step1.png "接入步骤1")

2.在金山魔方控制台中挑选所需服务。
![步骤2](https://raw.githubusercontent.com/wiki/ksvcmc/KMCFilter_Android/step2.png "接入步骤2")

3.点击申请试用，填写申请资料。
![步骤3](https://raw.githubusercontent.com/wiki/ksvcmc/KMCFilter_Android/step3.png "接入步骤3")

![步骤4](https://raw.githubusercontent.com/wiki/ksvcmc/KMCFilter_Android/step4.png "接入步骤4")

4.待申请审核通过后，金山云注册时的邮箱会收到邮件及试用token。
![步骤5](https://raw.githubusercontent.com/wiki/ksvcmc/KMCFilter_Android/step5.png "接入步骤5")

5.下载安卓/iOS版本的SDK集成进项目。
![步骤6](https://raw.githubusercontent.com/wiki/ksvcmc/KMCFilter_Android/step6.png "接入步骤6")

6.参照文档和DEMO填写TOKEN，就可以Run通项目了。
7.试用中或试用结束后，有意愿购买该服务可以与我们的商务人员联系购买。
（商务Email:KSC-VBU-KMC@kingsoft.com）
## 反馈与建议  
主页：[金山魔方](https://docs.ksyun.com/read/latest/142/_book/index.html)  
邮箱：ksc-vbu-kmc-dev@kingsoft.com  
QQ讨论群：574179720 [视频云技术交流群]

