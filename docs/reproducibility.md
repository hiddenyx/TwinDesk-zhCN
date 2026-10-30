# 可复现性验证

目的：证明本仓库的中文版 APK 是在上游 `v2.4.0` 源码基础上仅做字符串修改后构建的，
构建链路与官方包同源，没有夹带其他代码。

## 结论

本机用上游 `v2.4.0` 源码 + 相同构建配置编译出的**英文原版** APK，
与官方 GitHub Release 的 `TwinDesk-2.4.0.apk` 对比，以下条目 **SHA256 完全一致**：

| 条目 | 官方包 | 本机构建 |
|---|---|---|
| classes.dex | `503663f509be735c434e1cd186205dacdfcadeb2ef139548298c29de40e9ec00` | 同左 |
| classes2.dex | `878931ffc9b9fdd05aff34bc1d373f7b335661caf7c875b386bf7d6235c26e89` | 同左 |
| resources.arsc | `55f557be3f904319c1b4c0c289c6d795fca80fd5263bc34cd66093a9014f11b3` | 同左 |
| AndroidManifest.xml | `fa44fe6768f59517c38221d03854de06fdd8ae4818834c7e16b9e2cb7326ea0b` | 同左 |
| lib/arm64-v8a/libandroidx.graphics.path.so | `41e9a793c43a0f4fddb19e33f346bace464f30f888ba7b9eaf96294ea115bfb6` | 同左 |

整包 SHA256 不同，原因是签名（不同 debug keystore）与 zip 容器元数据不同。

## 文件哈希

| 文件 | SHA256 |
|---|---|
| 官方 TwinDesk-2.4.0.apk | `0ae4fe22af45fa1da1c4759234c0cbf38380c77e21c1130697d5c52324eda583` |
| 本仓库英文源码编译版 | `7835885b9093afad4e4076a4c2f677f3bac810c982f0ece9c21533c263a4fe42` |
| 本仓库中文源码编译版 | `25266dba17123f25c77ee73da0fb88e970227c1c4819e38c3789a628808b9937` |

## 复现方式

```bash
# 1) 取上游源码
git clone --depth 1 --branch v2.4.0 https://github.com/InukaAsith/TwinDesk.git upstream

# 2) 用本仓库的 android/ 覆盖（其中已是修改后的源码；英文原版请用上游原文件）
#    或者：从上游源码开始，按 android/patches/ 里的脚本顺序执行汉化
#    顺序：patch_source.js → patch_source3.js → 4 → 5 → 6 → 7 → 8

# 3) 构建
cd android && ./gradlew assembleStandardRelease

# 4) 验证内容一致性
unzip -p app/build/outputs/apk/standard/release/app-standard-release.apk classes.dex | sha256sum
```

## 校验中文版改动范围（建议命令）

```bash
# 与官方包裹对比，只有这些条目不同：资源、dex、Manifest、assets/web 下的中文脚本
unzip -v 官方.apk > /tmp/a.txt
unzip -v 中文版.apk > /tmp/b.txt
diff /tmp/a.txt /tmp/b.txt
```