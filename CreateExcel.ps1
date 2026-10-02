$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false

$csvPath = Join-Path (Get-Location) "StudentResults.csv"
$xlsxPath = Join-Path (Get-Location) "StudentResults.xlsx"

# Open CSV
$csvBook = $excel.Workbooks.Open($csvPath)

# Save as real XLSX
$csvBook.SaveAs($xlsxPath, 51)

$csvBook.Close($false)
$excel.Quit()

# Open the new Excel file
Start-Process "C:\Program Files\Microsoft Office\root\Office16\EXCEL.EXE" $xlsxPath

Write-Host "StudentResults.xlsx created successfully!"http://localhost:8080