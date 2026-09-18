$ErrorActionPreference = "Stop"

function Wait-Http {
    param(
        [string]$Url,
        [int]$Attempts = 90
    )

    for ($i = 0; $i -lt $Attempts; $i++) {
        try {
            Invoke-WebRequest -Uri $Url -UseBasicParsing | Out-Null
            return
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }

    throw "Timed out waiting for $Url"
}

function Post-Json {
    param(
        [string]$Url,
        [object]$Body
    )

    Invoke-RestMethod `
        -Method Post `
        -Uri $Url `
        -ContentType "application/json" `
        -Body ($Body | ConvertTo-Json -Depth 10)
}

function Patch-Json {
    param(
        [string]$Url,
        [object]$Body = $null
    )

    if ($null -eq $Body) {
        return Invoke-RestMethod `
            -Method Patch `
            -Uri $Url
    }

    Invoke-RestMethod `
        -Method Patch `
        -Uri $Url `
        -ContentType "application/json" `
        -Body ($Body | ConvertTo-Json -Depth 10)
}

Write-Host "Waiting for business services and saga monitor..."

Wait-Http "http://localhost:8081/actuator/health"
Wait-Http "http://localhost:8082/actuator/health"
Wait-Http "http://localhost:8084/actuator/health"
Wait-Http "http://localhost:8085/actuator/health"
Wait-Http "http://localhost:8090/actuator/health"

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

Write-Host "Creating category..."
$category = Post-Json `
    "http://localhost:8081/api/categories" `
    @{
        name = "E2E-$suffix"
        description = "Final E2E category"
    }

Write-Host "Creating product..."
$product = Post-Json `
    "http://localhost:8081/api/products" `
    @{
        name = "E2E Product $suffix"
        description = "Final E2E product"
        price = 125.50
        sku = "E2E-$suffix"
        categoryId = $category.id
    }

Write-Host "Creating inventory..."
$inventory = Post-Json `
    "http://localhost:8082/api/inventory" `
    @{
        productId = $product.id
        quantity = 10
    }

Write-Host "Creating order..."
$order = Post-Json `
    "http://localhost:8084/api/orders" `
    @{
        userId = 999
        items = @(
            @{
                productId = $product.id
                quantity = 2
            }
        )
    }

if ($order.status -ne "PENDING") {
    throw "Expected order PENDING, got $($order.status)"
}

Write-Host "Creating payment..."
$payment = Post-Json `
    "http://localhost:8085/api/payments" `
    @{
        orderId = $order.id
        currency = "TRY"
    }

Write-Host "Processing payment..."
$payment = Patch-Json `
    "http://localhost:8085/api/payments/$($payment.id)/process"

if ($payment.status -ne "PROCESSING") {
    throw "Expected payment PROCESSING, got $($payment.status)"
}

Write-Host "Succeeding payment..."
$payment = Patch-Json `
    "http://localhost:8085/api/payments/$($payment.id)/succeed"

if ($payment.status -ne "SUCCEEDED") {
    throw "Expected payment SUCCEEDED, got $($payment.status)"
}

$orderAfter =
    Invoke-RestMethod `
        -Method Get `
        -Uri "http://localhost:8084/api/orders/$($order.id)"

if ($orderAfter.status -ne "CONFIRMED") {
    throw "Expected order CONFIRMED, got $($orderAfter.status)"
}

$inventoryPage =
    Invoke-RestMethod `
        -Method Get `
        -Uri "http://localhost:8082/api/inventory?productId=$($product.id)"

$inventoryAfter =
    $inventoryPage.content | Select-Object -First 1

if ($inventoryAfter.quantity -ne 8) {
    throw "Expected quantity 8, got $($inventoryAfter.quantity)"
}

if ($inventoryAfter.reservedQuantity -ne 0) {
    throw "Expected reservedQuantity 0, got $($inventoryAfter.reservedQuantity)"
}

Write-Host "Waiting for transactional outbox -> Kafka -> Saga projection..."

$saga = $null

for ($i = 0; $i -lt 30; $i++) {
    try {
        $saga =
            Invoke-RestMethod `
                -Method Get `
                -Uri "http://localhost:8090/api/sagas/$($order.id)"

        if ($saga.terminalStatus -eq "COMPLETED") {
            break
        }
    }
    catch {
        # Event may not have reached the projection yet.
    }

    Start-Sleep -Seconds 1
}

if ($null -eq $saga) {
    throw "Saga projection was not created"
}

if ($saga.terminalStatus -ne "COMPLETED") {
    throw "Expected saga COMPLETED, got $($saga.terminalStatus)"
}

Write-Host ""
Write-Host "FINAL E2E PASSED" -ForegroundColor Green
Write-Host "Product ID: $($product.id)"
Write-Host "Order ID:   $($order.id)"
Write-Host "Payment ID: $($payment.id)"
Write-Host "Saga:       $($saga.terminalStatus)"
