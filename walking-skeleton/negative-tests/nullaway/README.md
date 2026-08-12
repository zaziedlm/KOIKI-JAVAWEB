# NullAway Negative Test

通常のsource treeにはbuildを壊すコードを残しません。

`ws-smoke-lib` の `GreetingService` に、一時的に次のmethodを追加してください。

```java
public String deliberateNullAwayViolation() {
    return null;
}
```

対象packageは `package-info.java` で `@NullMarked` なので、
NullAwayが有効なら `mvn clean verify` は失敗することを期待します。

確認後はmethodを削除し、再度 `mvn clean verify` がPASSすることを確認します。

```text
PASS
 -> deliberate violation
FAIL (NullAway)
 -> revert
PASS
```
