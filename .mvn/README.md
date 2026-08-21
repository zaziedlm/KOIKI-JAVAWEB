# Maven Wrapper

Repositoryに含める公式Maven WrapperはApache Maven Wrapper Pluginから生成します。

```sh
mvn wrapper:wrapper -Dmaven=3.9.16 -Dtype=bin
```

生成後は`mvnw`、`mvnw.cmd`および`wrapper/maven-wrapper.properties`をCommitし、
全ての正式buildでWrapperを使用します。Wrapperを更新するときは上記の公式コマンドで再生成し、
生成差分とMaven versionをreviewしてください。

`bin`型は公式`maven-wrapper.jar`もRepositoryへ含めます。Phase 1aではWindowsを主要な開発・CI
検証環境とし、3.3.4の`only-script`型で確認されたPowerShellのsymlink判定不具合を回避するため、
生成scriptへ独自patchを加えず`bin`型を採用します。
