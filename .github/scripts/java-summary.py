#!/usr/bin/env python3
"""Prints a Markdown summary of the Surefire and JaCoCo results (run from apps/api-gateway).

Used for the GitHub job summary, so the numbers shown for a run are measured by that run.
"""
import csv
import glob
import xml.etree.ElementTree as ET

tests = failures = errors = skipped = 0
for report in glob.glob("target/surefire-reports/TEST-*.xml"):
    suite = ET.parse(report).getroot()
    tests += int(suite.get("tests", 0))
    failures += int(suite.get("failures", 0))
    errors += int(suite.get("errors", 0))
    skipped += int(suite.get("skipped", 0))

print("### api-gateway")
print()
print("| Tests | Failures | Errors | Skipped |")
print("|------:|---------:|-------:|--------:|")
print(f"| {tests} | {failures} | {errors} | {skipped} |")

try:
    totals = {}
    with open("target/site/jacoco/jacoco.csv", newline="") as handle:
        for row in csv.DictReader(handle):
            for key in ("LINE_MISSED", "LINE_COVERED", "BRANCH_MISSED", "BRANCH_COVERED"):
                totals[key] = totals.get(key, 0) + int(row[key])
    lines = totals["LINE_COVERED"] + totals["LINE_MISSED"]
    branches = totals["BRANCH_COVERED"] + totals["BRANCH_MISSED"]
    print()
    print(f"Line coverage: **{100 * totals['LINE_COVERED'] / lines:.1f} %** "
          f"({totals['LINE_COVERED']}/{lines}), "
          f"branch coverage: **{100 * totals['BRANCH_COVERED'] / branches:.1f} %** "
          f"({totals['BRANCH_COVERED']}/{branches}). Gate: 80 % lines.")
except FileNotFoundError:
    print()
    print("No JaCoCo report (the build stopped before the verify phase).")
