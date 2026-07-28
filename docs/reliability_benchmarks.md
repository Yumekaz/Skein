# Deterministic reliability benchmarks

The headless benchmark uses fixed random seeds and does not require Android hardware:

```powershell
./gradlew testDebugUnitTest --tests com.skein.android.benchmark.ReliabilityBenchmarkTest
```

It writes reproducible data to `app/build/reports/reliability/`:

- `ordering.csv` measures adjacent ordering violations in a shuffled partition/merge sequence versus Lamport reconciliation.
- `fec.csv` compares complete-fragment baseline delivery with 8+4 FEC recovery at 30% and 40% simulated loss.
- `reliability.json` contains the same data for graphing or CI collection.

Create the two SVG graphs without downloading a charting library:

```powershell
.\tools\render_reliability_graphs.ps1
```

The script creates `ordering.svg` and `fec.svg` beside the CSV files.

The FEC benchmark is a codec-level controlled-loss measurement, not a claim about real-world BLE throughput. Physical-device testing remains required for radio behavior, latency, and interoperability. The 1 MiB multi-block ceiling applies to v2 packet frames; the legacy v1 packet-length field remains limited to 65,535 bytes.
