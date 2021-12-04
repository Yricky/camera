# camera
## 基于Camera2API的一个相机
## 实现功能：
- 切换前后相机
- 拍照（文件存在/storage/sdcard0/Android/data/com.yricky.camera/files/pic里）
## 未实现的功能
- 录制（录制surface的MediaRecorder在prepare阶段表现严重依赖设备，在模拟器上表现正常，在kyrin990-HarmonyOS2.0设备上stopRecord时崩溃，在高通骁龙865-HarmonyOS2.0设备上prepare阶段崩溃）
