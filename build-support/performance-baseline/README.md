# Performance Baseline

Phase 1b CP9のTooling-owned・非配布harnessである。同一fixture binaryをbare Spring BootとKOIKI適用の
2 applicationから起動し、同一PC内のpaired resultだけを参考比較する。

```powershell
pwsh -NoProfile -File build-support/performance-baseline/verify-performance-baseline.ps1
```

短縮検証には`-Smoke`を付ける。性能数値はquality gateではなく、fingerprint、raw、aggregateのschemaと
再実行可能性を検査対象とする。
