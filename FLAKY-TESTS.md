# Flaky Tests

## Was ist ein Flaky Test?

Ein Test, der ohne Codeänderung manchmal grün, manchmal rot ist.
Flaky Tests untergraben das Vertrauen in die Pipeline und müssen behandelt werden.

## Aktuelle Flaky Tests

| Test | Ursache | Workaround | Status |
|------|---------|------------|--------|
| _– aktuell keine –_ | | | |

> Sobald ein flaky Test erkannt wird, hier eintragen und ein Ticket anlegen.
> Beispiel-Kandidat für später: `KafkaConsumerIT#processEvent_withHighLoad`
> (Race condition bei EmbeddedKafka-Startup → `@RetryingTest(3)`).

## Strategie

1. **Erkennen** — CI-History auf nicht-deterministische Fehlschläge prüfen.
2. **Klassifizieren** — `test-bug` / `product-bug` / `environment`.
3. **Taggen** — mit `@Disabled("flaky: NC-XX")` + Ticket anlegen.
4. **Fixen oder quarantänieren** — Ursache beheben oder dauerhaft isolieren.
