# ArchUnit External Consumer Fixture

Root Reactorには含めない独立Maven projectです。

`koiki-archunit-rules`をtest依存として利用し、Customer moduleから別moduleの
`internal` packageを参照した場合に、ADR番号・影響・修正方法を含むArchitecture
Test failureが発生することを検証します。

このfixtureは意図的に違反しているため、通常の`mvn test`は失敗が正解です。

```powershell
.\walking-skeleton\archunit-external-consumer\verify-external-consumer.ps1
```
