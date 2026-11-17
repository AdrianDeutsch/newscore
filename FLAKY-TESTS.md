# Flaky Tests

Ein Test, der ohne Codeänderung mal grün, mal rot ist, untergräbt das Vertrauen in die Pipeline und
wird behandelt, nicht ignoriert.

## Aktuell bekannt

| Test | Ursache | Umgang | Status |
|------|---------|--------|--------|
| _keine_ | | | |

## Regeln

- **Keine automatischen Wiederholungen.** Die End-to-End-Suite läuft mit `retries: 0`
  (`e2e/playwright.config.ts`). Ein Test, der nur beim zweiten Versuch besteht, soll als Fehler
  sichtbar werden.
- **Warten auf Zustände, nicht auf Zeit.** Asynchrone Abläufe (Kafka, Suchindex, Cache) werden mit
  `expect.poll` bzw. Awaitility bis zu einer Obergrenze abgefragt, nicht mit festen Pausen.
- **Eindeutige Testdaten.** Tests, die in geteilten Zustand schreiben (PostgreSQL, Suchindex, Cache),
  verwenden eindeutige Marker oder stellen den Ausgangszustand wieder her.

## Vorgehen bei einem Verdacht

1. **Erkennen:** fehlgeschlagene Läufe in der CI-Historie mit dem Code vergleichen.
2. **Einordnen:** Testfehler, Produktfehler oder Umgebung.
3. **Eintragen:** hier in die Tabelle, mit Ursache, sobald bekannt.
4. **Beheben oder isolieren:** Ursache beheben; nur wenn das nicht sofort geht, den Test mit
   Begründung deaktivieren (`@Disabled("flaky: …")` bzw. `test.fixme`).
