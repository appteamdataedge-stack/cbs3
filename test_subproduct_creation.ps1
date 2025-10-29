# Test script for SubProduct creation API
Write-Host "Testing SubProduct Creation API..." -ForegroundColor Green

# Wait for backend to start
Write-Host "Waiting for backend to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Test 1: Check if products exist
Write-Host "`n1. Testing GET /api/products..." -ForegroundColor Cyan
try {
    $productsResponse = Invoke-RestMethod -Uri "http://localhost:8082/api/products?page=0&size=10" -Method GET -ContentType "application/json"
    Write-Host "✅ Products API working" -ForegroundColor Green
    Write-Host "Products found: $($productsResponse.content.Count)"
    
    if ($productsResponse.content.Count -gt 0) {
        $firstProduct = $productsResponse.content[0]
        Write-Host "First product: ID=$($firstProduct.productId), Code=$($firstProduct.productCode), Name=$($firstProduct.productName)"
        $testProductId = $firstProduct.productId
    } else {
        Write-Host "❌ No products found in database" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Products API failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: Check GL options
Write-Host "`n2. Testing GET /api/subproducts/gl-options..." -ForegroundColor Cyan
try {
    $glOptionsResponse = Invoke-RestMethod -Uri "http://localhost:8082/api/subproducts/gl-options" -Method GET -ContentType "application/json"
    Write-Host "✅ GL Options API working" -ForegroundColor Green
    Write-Host "GL Options found: $($glOptionsResponse.Count)"
    
    if ($glOptionsResponse.Count -gt 0) {
        $firstGL = $glOptionsResponse[0]
        Write-Host "First GL: Num=$($firstGL.glNum), Name=$($firstGL.glName)"
        $testGLNum = $firstGL.glNum
    } else {
        Write-Host "❌ No GL options found" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ GL Options API failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 3: Create SubProduct
Write-Host "`n3. Testing POST /api/subproducts..." -ForegroundColor Cyan
$subProductData = @{
    productId = $testProductId
    subProductCode = "TEST-$(Get-Date -Format 'yyyyMMddHHmmss')"
    subProductName = "Test Sub Product $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    inttCode = "TEST-INT"
    cumGLNum = $testGLNum
    extGLNum = "001"
    subProductStatus = "ACTIVE"
    makerId = "ADMIN"
} | ConvertTo-Json

Write-Host "SubProduct data to be sent:"
Write-Host $subProductData

try {
    $createResponse = Invoke-RestMethod -Uri "http://localhost:8082/api/subproducts" -Method POST -Body $subProductData -ContentType "application/json"
    Write-Host "✅ SubProduct created successfully!" -ForegroundColor Green
    Write-Host "Created SubProduct ID: $($createResponse.subProductId)"
    Write-Host "Created SubProduct Code: $($createResponse.subProductCode)"
} catch {
    Write-Host "❌ SubProduct creation failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $responseStream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($responseStream)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response body: $responseBody" -ForegroundColor Red
    }
}

Write-Host "`nTest completed!" -ForegroundColor Green
