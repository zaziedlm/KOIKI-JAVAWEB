# koiki-starters

KOIKI Spring Boot StarterのCanonical ownership locationです。

Runtime Starterは所定Phaseの検証でOwnershipとPublic APIを確定してから追加し、空Maven Moduleを
先行作成しません。Phase 2では、P2-A1で`koiki-starter-security`、P2-B1で`koiki-starter-audit`を
Architecture Ownerのcontract承認後に正式Starterとして追加しています。P2-B2では
`koiki-starter-identity`、P2-B3ではSession利用applicationだけが導入するoptionalな
`koiki-starter-session-jdbc`を同じ手順で追加しています。
