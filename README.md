# MonoRange — Android 視覺測距儀

[![Android CI Build](https://github.com/wusui1325-dotcom/android-vision-distance/actions/workflows/android-build.yml/badge.svg)](https://github.com/wusui1325-dotcom/android-vision-distance/actions/workflows/android-build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-000000.svg)](LICENSE)

- **Package Name**：`com.wusper.vision.distance`
- **UI 風格**：現代黑白平面流線體 UI（Monochrome Flat Streamlined UI），全項目與按鈕皆配備矢量 Icon

一款不依賴 ARCore 或雲端 API 的 Android 視覺距離與高度測量開源 App，結合 **CameraX + Camera2 光學焦距參數 + IMU 陀螺儀感測器融合**，在一般 Android 手機上即可完成即時測距。

---

## 核心特色

- **現代黑白平面流線體設計**
  - 純黑（`#000000` / `#0A0A0A`）與純白（`#FFFFFF`）高對比平面美學，零陰影、細邊框（`1dp`）、膠囊流線幾何、等寬數字讀數。
  - 支援一鍵切換**曜石黑 / 極簡白**雙單色主題。
- **每個項目與按鈕皆配備專屬 Icon**
  - 頂部工具列、四種測距模式膠囊、起終點控制列、參考物體預設膠囊、鏡頭高度快調列、歷史紀錄項目、設定與校準列及對話框按鈕均具明確矢量圖示。
- **四種視覺測距模式**
  1. **地面測距（Ground Trigonometry）**：讀取旋轉向量感測器俯仰角與鏡頭離地高度 $H$，以 $D = H / \tan(\beta)$ 即時解算距離，並附帶動態水平儀準星。
  2. **兩點測距（Point-to-Point A → B）**：使用相機中心準星手動對準地面起始點 A 設定錨點，再移動準星瞄準結束點 B，結合俯仰角與方位角變化 $\Delta\psi$ 以餘弦定理 $D_{AB} = \sqrt{r_A^2 + r_B^2 - 2 r_A r_B \cos(\Delta\psi)}$ 即時解算 A→B 兩點直線距離，並支援單鍵重設 A / B 與儲存紀錄。
  3. **物體比對（Pinhole Caliper）**：透過 Camera2 自動讀取後鏡頭實體焦距與感光元件尺寸計算垂直視角（VFOV），拖曳畫面上的黑白流線卡尺框住已知尺寸目標（內建成人、房門、汽車、A4 紙、銀行卡與自訂尺寸）即時測距。
  4. **兩點測高（Two-Point Height）**：先鎖定目標底部距離，再向上瞄準目標頂端計算高度 $h = H + D \cdot \tan(\alpha)$。
- **完整校準與紀錄管理**
  - 鏡頭離地高度滑桿與快捷膠囊、焦距微調係數、一鍵水平零點校準、EMA 平滑濾波強度、四種單位切換（`m` / `cm` / `ft` / `in`）。
  - 本機儲存測距紀錄，支援單筆複製、刪除與純文字分享匯出。

---

## 專案結構

```text
app/src/main/java/com/wusper/vision/distance/
├── MainActivity.kt
├── model/MeasurementModels.kt        # 模式、單位、錨點、預設參考物與 UI 狀態
├── engine/
│   ├── DistanceCalculator.kt         # 三角測距、兩點餘弦測距、針孔測距與兩點測高
│   ├── OrientationSensorManager.kt   # 旋轉向量 / 重力感測器（俯仰、翻滾、方位角平滑）
│   └── CameraIntrinsicsHelper.kt     # Camera2 焦距與視角 (FOV) 讀取
├── data/MeasurementStore.kt          # SharedPreferences + JSON 持久化
├── viewmodel/RangefinderViewModel.kt # 狀態管理、A→B 點位鎖定與校準邏輯
└── ui/
    ├── theme/                        # 純黑白平面主題與等寬字體系統
    ├── components/                   # 流線膠囊按鈕、準星水平儀、可拖曳卡尺
    └── screens/                      # 主測距畫面、歷史紀錄彈窗、校準設定彈窗
```

---

## 建置與下載 APK

### 直接從 GitHub Actions 下載
每次推送至 `main` 分支後，GitHub Actions 會自動執行單元測試並編譯 Debug APK，可在 **Actions → Android CI Build → Artifacts (`MonoRange-debug-apk`)** 下載安裝。

### 本機編譯
需求：Android Studio Ladybug+ / JDK 17 / Android SDK 35

```bash
git clone https://github.com/wusui1325-dotcom/android-vision-distance.git
cd android-vision-distance
gradle testDebugUnitTest assembleDebug
```

---

## 授權

本專案採用 [MIT License](LICENSE) 開源授權。
