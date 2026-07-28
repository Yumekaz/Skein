param(
    [string]$ReportDirectory = "app/build/reports/reliability"
)

$ErrorActionPreference = "Stop"

function Write-BarChart {
    param(
        [string]$Path,
        [string]$Title,
        [string[]]$Labels,
        [int[]]$Values,
        [string[]]$Colors
    )

    $width = 720; $height = 420; $left = 72; $bottom = 355; $chartHeight = 270
    $maxValue = [Math]::Max(1, ($Values | Measure-Object -Maximum).Maximum)
    $barWidth = 90; $gap = 38
    $bars = for ($i = 0; $i -lt $Values.Count; $i++) {
        $barHeight = [Math]::Round($Values[$i] / $maxValue * $chartHeight)
        $x = $left + $i * ($barWidth + $gap)
        $y = $bottom - $barHeight
        "<rect x='$x' y='$y' width='$barWidth' height='$barHeight' fill='$($Colors[$i])'/><text x='$($x + 45)' y='$($y - 8)' text-anchor='middle'>$($Values[$i])</text><text x='$($x + 45)' y='$($bottom + 24)' text-anchor='middle' font-size='12'>$($Labels[$i])</text>"
    }
    @"
<svg xmlns='http://www.w3.org/2000/svg' width='$width' height='$height' viewBox='0 0 $width $height'>
  <style>text { font-family: Arial, sans-serif; fill: #172033; } .title { font-size: 21px; font-weight: bold; }</style>
  <rect width='100%' height='100%' fill='#ffffff'/>
  <text class='title' x='36' y='42'>$Title</text>
  <line x1='$left' y1='$bottom' x2='680' y2='$bottom' stroke='#172033'/>
  <line x1='$left' y1='85' x2='$left' y2='$bottom' stroke='#172033'/>
  $($bars -join "`n  ")
</svg>
"@ | Set-Content -LiteralPath $Path -Encoding utf8
}

$ordering = Import-Csv (Join-Path $ReportDirectory 'ordering.csv')
Write-BarChart -Path (Join-Path $ReportDirectory 'ordering.svg') -Title 'Partition merge ordering violations' `
    -Labels @('arrival order', 'Lamport order') `
    -Values @([int]$ordering.arrival_order_violations, [int]$ordering.lamport_order_violations) `
    -Colors @('#dc4c64', '#1a9c67')

$fec = Import-Csv (Join-Path $ReportDirectory 'fec.csv')
$labels = @(); $values = @(); $colors = @()
foreach ($row in $fec) {
    $loss = [int]([double]$row.loss_rate * 100)
    $labels += "$loss% baseline"; $values += [int]$row.baseline_deliveries; $colors += '#dc4c64'
    $labels += "$loss% FEC"; $values += [int]$row.fec_deliveries; $colors += '#1a9c67'
}
Write-BarChart -Path (Join-Path $ReportDirectory 'fec.svg') -Title 'Controlled-loss delivery comparison' `
    -Labels $labels -Values $values -Colors $colors

Write-Host "Wrote ordering.svg and fec.svg to $ReportDirectory"
