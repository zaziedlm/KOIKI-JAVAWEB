[CmdletBinding()]
param(
    [ValidatePattern('^koiki-reference-[a-z0-9][a-z0-9-]{0,62}$')]
    [string]$ContainerName = 'koiki-reference-postgres',

    [ValidatePattern('^[A-Za-z][A-Za-z0-9_]{0,62}$')]
    [string]$Database = 'koiki_reference',

    [ValidatePattern('^[A-Za-z][A-Za-z0-9_]{0,62}$')]
    [string]$DatabaseUser = 'koiki',

    [switch]$ConfirmDisposable
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not $ConfirmDisposable) {
    throw 'Refusing to seed without -ConfirmDisposable. Use only the disposable local PostgreSQL from the Reference run guide.'
}

function Invoke-Docker {
    param([Parameter(Mandatory)][string[]]$Arguments)

    $output = & docker @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw ($output -join "`n")
    }
    return @($output)
}

function Invoke-Postgres {
    param([Parameter(Mandatory)][string]$Sql)

    $arguments = @(
        'exec', '--interactive', $ContainerName,
        'psql', '--username', $DatabaseUser, '--dbname', $Database,
        '--no-psqlrc', '--quiet', '--tuples-only', '--no-align',
        '--set', 'ON_ERROR_STOP=1'
    )
    $output = $Sql | & docker @arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw ($output -join "`n")
    }
    return @($output | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

$containerState = @(Invoke-Docker -Arguments @(
    'inspect', '--format', '{{.State.Running}}|{{.Config.Image}}', $ContainerName)
)
if ($containerState.Count -ne 1 -or $containerState[0] -notmatch '^true\|postgres:17(?:-alpine)?$') {
    throw "Container '$ContainerName' must be a running PostgreSQL 17 container. Observed: $($containerState -join ', ')"
}

$passwordBytes = [byte[]]::new(24)
$randomNumberGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $randomNumberGenerator.GetBytes($passwordBytes)
} finally {
    $randomNumberGenerator.Dispose()
}
$demoPassword = 'A9!' + [Convert]::ToBase64String($passwordBytes).TrimEnd('=').Replace('+', 'A').Replace('/', 'B')

$sqlTemplate = @'
BEGIN;

DO $$
BEGIN
    IF to_regclass('public.koiki_user') IS NULL
        OR to_regclass('public.koiki_password_credential') IS NULL
        OR to_regclass('public.kkref_department') IS NULL
        OR to_regclass('public.kkref_expense_request') IS NULL THEN
        RAISE EXCEPTION 'Required Framework and Reference migrations have not been applied.';
    END IF;

    IF EXISTS (SELECT 1 FROM koiki_user)
        OR EXISTS (SELECT 1 FROM koiki_password_credential)
        OR EXISTS (SELECT 1 FROM koiki_role)
        OR EXISTS (SELECT 1 FROM koiki_permission)
        OR EXISTS (SELECT 1 FROM koiki_user_role)
        OR EXISTS (SELECT 1 FROM koiki_role_permission)
        OR EXISTS (SELECT 1 FROM koiki_login_attempt)
        OR EXISTS (SELECT 1 FROM koiki_external_identity_link)
        OR EXISTS (SELECT 1 FROM kkref_department)
        OR EXISTS (SELECT 1 FROM kkref_expense_category)
        OR EXISTS (SELECT 1 FROM kkref_user_department_assignment)
        OR EXISTS (SELECT 1 FROM kkref_expense_approver_scope)
        OR EXISTS (SELECT 1 FROM kkref_expense_request)
        OR EXISTS (SELECT 1 FROM kkref_expense_line) THEN
        RAISE EXCEPTION 'Demo seed requires a clean disposable database; existing Identity or Reference data was found.';
    END IF;
END $$;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO koiki_user
    (user_id, email, canonical_email, status, version, created_at, updated_at)
VALUES
    ('b2000000-0000-0000-0000-000000000001',
     'p3-demo-user@example.test', 'p3-demo-user@example.test', 'ACTIVE', 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO koiki_password_credential
    (user_id, encoded_password, locked_until, version, created_at, updated_at)
VALUES
    ('b2000000-0000-0000-0000-000000000001',
     '{bcrypt}' || crypt('__DEMO_PASSWORD__', gen_salt('bf', 10)), NULL, 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO koiki_role
    (role_id, role_code, version, created_at, updated_at)
VALUES
    ('b2000000-0000-0000-0000-000000000100', 'P3_DEMO_REVIEWER', 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO koiki_permission
    (permission_id, permission_code, version, created_at, updated_at)
VALUES
    ('b2000000-0000-0000-0000-000000000101', 'EXPENSE:APPLY', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b2000000-0000-0000-0000-000000000102', 'EXPENSE:APPROVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b2000000-0000-0000-0000-000000000103', 'EXPENSE:SETTLE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b2000000-0000-0000-0000-000000000104', 'IDENTITY:ADMIN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b2000000-0000-0000-0000-000000000105', 'MASTER:ADMIN', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO koiki_user_role (user_id, role_id)
VALUES
    ('b2000000-0000-0000-0000-000000000001',
     'b2000000-0000-0000-0000-000000000100');

INSERT INTO koiki_role_permission (role_id, permission_id)
SELECT 'b2000000-0000-0000-0000-000000000100', permission_id
FROM koiki_permission
WHERE permission_code IN (
    'EXPENSE:APPLY', 'EXPENSE:APPROVE', 'EXPENSE:SETTLE',
    'IDENTITY:ADMIN', 'MASTER:ADMIN');

INSERT INTO kkref_department
    (department_id, department_code, department_name, active, version, created_at, updated_at)
VALUES
    ('b3000000-0000-0000-0000-000000000001',
     'DEMO', 'Demo Department', true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO kkref_expense_category
    (expense_category_id, expense_category_code, expense_category_name,
     active, version, created_at, updated_at)
VALUES
    ('b3000000-0000-0000-0000-000000000002',
     'TRAVEL', 'Travel Expense', true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO kkref_user_department_assignment
    (user_id, department_id, version, created_at, updated_at)
VALUES
    ('b2000000-0000-0000-0000-000000000001',
     'b3000000-0000-0000-0000-000000000001', 0,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO kkref_expense_approver_scope
    (approver_user_id, department_id, created_at)
VALUES
    ('b2000000-0000-0000-0000-000000000001',
     'b3000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP);

INSERT INTO kkref_expense_request
    (expense_request_id, applicant_user_id, department_id, claimed_amount,
     status, decision_reason, version, created_at, updated_at)
VALUES
    ('b3000000-0000-0000-0000-000000000101',
     'b2000000-0000-0000-0000-000000000001',
     'b3000000-0000-0000-0000-000000000001',
     1200, 'DRAFT', NULL, 0, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    ('b3000000-0000-0000-0000-000000000102',
     'b2000000-0000-0000-0000-000000000001',
     'b3000000-0000-0000-0000-000000000001',
     2400, 'SUBMITTED', NULL, 1, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    ('b3000000-0000-0000-0000-000000000103',
     'b2000000-0000-0000-0000-000000000001',
     'b3000000-0000-0000-0000-000000000001',
     3600, 'APPROVED', NULL, 2, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day');

INSERT INTO kkref_expense_line
    (expense_line_id, expense_request_id, expense_category_id, usage_date,
     description, purpose, amount)
VALUES
    ('b3000000-0000-0000-0000-000000000201',
     'b3000000-0000-0000-0000-000000000101',
     'b3000000-0000-0000-0000-000000000002',
     CURRENT_DATE - 3, 'Demo draft expense', 'Browser demonstration', 1200),
    ('b3000000-0000-0000-0000-000000000202',
     'b3000000-0000-0000-0000-000000000102',
     'b3000000-0000-0000-0000-000000000002',
     CURRENT_DATE - 2, 'Demo submitted expense', 'Approval demonstration', 2400),
    ('b3000000-0000-0000-0000-000000000203',
     'b3000000-0000-0000-0000-000000000103',
     'b3000000-0000-0000-0000-000000000002',
     CURRENT_DATE - 1, 'Demo approved expense', 'Accounting demonstration', 3600);

COMMIT;

SELECT 'DEMO_READY'
    || '|users=' || (SELECT count(*) FROM koiki_user)
    || '|permissions=' || (SELECT count(*) FROM koiki_permission)
    || '|departments=' || (SELECT count(*) FROM kkref_department)
    || '|categories=' || (SELECT count(*) FROM kkref_expense_category)
    || '|expenses=' || (SELECT count(*) FROM kkref_expense_request);
'@

$sql = $sqlTemplate.Replace('__DEMO_PASSWORD__', $demoPassword)
$result = @(Invoke-Postgres -Sql $sql)
if ($result.Count -ne 1 -or $result[0] -ne 'DEMO_READY|users=1|permissions=5|departments=1|categories=1|expenses=3') {
    throw "Demo data verification returned an unexpected result: $($result -join ', ')"
}

Write-Host 'Disposable Reference demo data is ready.'
Write-Host 'Login URL: http://127.0.0.1:18080/login'
Write-Host 'Email: p3-demo-user@example.test'
Write-Host "Password: $demoPassword"
Write-Host 'The password exists only in this terminal and the disposable database.'
Write-Host 'Stop the --rm PostgreSQL container after the demonstration to discard all demo data.'

$demoPassword = $null
$sql = $null
