param(
    [int]$PostgresPort = 55432
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path "$PSScriptRoot\..\..").Path
$containerName = "koiki-ws-flyway-$PID"
$jarPath = Join-Path $repoRoot "walking-skeleton\ws-flyway-two-tier\target\ws-flyway-two-tier-0.0.1-SNAPSHOT.jar"

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME must point to JDK 21."
}

$java = Join-Path $env:JAVA_HOME "bin\java.exe"
if (-not (Test-Path -LiteralPath $java)) {
    throw "java.exe was not found under JAVA_HOME: $env:JAVA_HOME"
}

Push-Location $repoRoot
try {
    & .\mvnw.cmd -pl walking-skeleton/ws-flyway-two-tier -am clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Initial Maven build failed."
    }

    docker run --name $containerName `
        -e POSTGRES_DB=koiki_ws `
        -e POSTGRES_USER=koiki `
        -e POSTGRES_PASSWORD=koiki `
        -p "${PostgresPort}:5432" `
        -d postgres:17-alpine | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL container startup failed."
    }

    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        docker exec $containerName pg_isready -U koiki -d koiki_ws | Out-Null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) {
        throw "PostgreSQL did not become ready."
    }

    $env:WS_FLYWAY_JDBC_URL = "jdbc:postgresql://localhost:$PostgresPort/koiki_ws"
    $env:WS_EXPECTED_KOIKI_VERSION = "1"
    & $java -jar $jarPath
    if ($LASTEXITCODE -ne 0) {
        throw "Initial migration validation failed."
    }

    & .\mvnw.cmd -pl walking-skeleton/ws-flyway-two-tier -am clean package `
        -DskipTests -Plater-koiki-release
    if ($LASTEXITCODE -ne 0) {
        throw "Later KOIKI release build failed."
    }

    $env:WS_EXPECTED_KOIKI_VERSION = "2"
    & $java -jar $jarPath
    if ($LASTEXITCODE -ne 0) {
        throw "Later KOIKI V2 migration validation failed."
    }

    docker exec $containerName psql -U koiki -d koiki_ws -Atc `
        "SELECT 'koiki=' || max(version) FROM koiki_flyway_history WHERE success UNION ALL SELECT 'customer=' || max(version) FROM flyway_schema_history WHERE success;"
    if ($LASTEXITCODE -ne 0) {
        throw "History table query failed."
    }
}
finally {
    Remove-Item Env:WS_FLYWAY_JDBC_URL -ErrorAction SilentlyContinue
    Remove-Item Env:WS_EXPECTED_KOIKI_VERSION -ErrorAction SilentlyContinue
    docker rm -f $containerName | Out-Null
    Pop-Location
}
