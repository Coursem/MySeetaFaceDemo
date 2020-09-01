离线人脸识别升级版SeetaFace2


注意:添加so文件、将模型文件复制到SD卡

1. **so文件下载**:
armeabi-v7a： libSeetaFaceDetector2.so 、 libSeetaFaceLandmarker2.so 、 libSeetaFaceRecognizer2.so 、 libseetanet2.so  
将SO文件放置在jniLibs下的armeabi-v7a的文件夹下，并且在app的build文件下：
```
defaultConfig {
        ...
        ndk {
            abiFilters 'armeabi-v7a'
        }
    }
```

2. **模型文件下载**:
dat模型文件seetaface ：SeetaFaceDetector2.0.ats 、 SeetaFaceRecognizer2.0.ats 、 SeetaPointDetector2.0.pts5.ats  
将三个ats文件放置在SD卡，根目录下的seetaface目录下，可以放在assets资源目录下，然后复制到SD卡，或者去服务器下载



//设置模型文件在SD卡的位置
SeetaHelper.ROOT_CACHE = Environment.getExternalStorageDirectory().toString() + File.separator + "seeta"
//设置assets目录中的模型文件
SeetaHelper.ROOT_ASSETS = "seetaface"
```


> 参考：
>> https://github.com/seetafaceengine/SeetaFace2
>> https://github.com/seetaface/SeetaFaceEngine2/blob/master/example/android/README.md
