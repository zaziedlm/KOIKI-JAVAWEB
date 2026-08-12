# Maven Wrapper

公式Maven WrapperはApache Maven Wrapper Pluginから生成します。

```sh
mvn wrapper:wrapper -Dmaven=3.9.16 -Dtype=only-script
```

期待設定は `wrapper/maven-wrapper.properties.template` を参照してください。
