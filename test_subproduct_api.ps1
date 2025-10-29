# Test script for SubProduct API endpoint
$body = @{
    productId = 9
    subProductCode = "TEST-001"
    subProductName = "Test Sub Product"
    inttCode = "TEST-INT"
    cumGLNum = "110101001"
    extGLNum = "001"
    subProductStatus = "Active"
    makerId = "ADMIN"
} | ConvertTo-Json

Write-Host "Testing SubProduct API endpoint..."
Write-Host "Request body: $body"

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8082/api/subproducts" -Method POST -Body $body -ContentType "application/json"
    Write-Host "Success! Response:"
    $response | ConvertTo-Json -Depth 3
} catch {
    Write-Host "Error occurred:"
    Write-Host "Status Code: $($_.Exception.Response.StatusCode)"
    Write-Host "Error Message: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody"
    }
}
