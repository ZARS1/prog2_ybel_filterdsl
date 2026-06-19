---
title: Student Support Code for 'Filter-DSL' Task
---

# Blatt 06 – Filter-DSL und AST-Verarbeitung

## Projektübersicht

In diesem Projekt wird eine kleine Filtersprache für eine Songliste verarbeitet. Benutzer können Suchausdrücke eingeben, zum Beispiel:

```text
artist == "Beatles"
year <= 1990
genre in ("rock", "jazz")
not artist == "Beatles"
```

ANTLR zerlegt den eingegebenen Filter zunächst in einen Parse-Tree. Anschließend wird dieser Parse-Tree in einen kompakteren abstrakten Syntaxbaum, den sogenannten AST, übersetzt.

Für die Übersetzung wurden zwei unterschiedliche Ansätze implementiert:

1. Visitor-Pattern
2. Manuelle Traversierung mit typbasierter Verarbeitung

Die beiden Varianten erzeugen denselben AST und können dadurch miteinander verglichen werden.

## Projektablauf

Der Weg von der Eingabe bis zum Suchergebnis ist:

```text
Filtertext
→ ANTLR Lexer und Parser
→ Parse-Tree
→ AST-Builder
→ abstrakter Syntaxbaum
→ Evaluator
→ passende Songs
```

Die grafische Anwendung lädt eine Songliste aus einer CSV-Datei. Nach Eingabe eines gültigen Filters werden die passenden Songs in der Oberfläche angezeigt.

## AST-Modell

Der AST wird mit Records und einer versiegelten Schnittstelle modelliert.

Unterstützte Ausdruckstypen sind:

* `Comparison` für Vergleiche
* `And` für logische UND-Verknüpfungen
* `Or` für logische ODER-Verknüpfungen
* `Not` für Negationen
* `InList` für Vergleiche mit mehreren möglichen Werten

Beispielsweise wird dieser Ausdruck:

```text
artist == "Beatles" and year == 1965
```

als `And`-Knoten mit zwei untergeordneten `Comparison`-Knoten dargestellt.

## Visitor-Implementierung

Die Klasse `AstBuilderVisitor` verwendet den von ANTLR bereitgestellten `FilterBaseVisitor`.

Während der Traversierung werden Ausdrücke und Werte auf Stacks zwischengespeichert. Sobald die Kindknoten eines Ausdrucks verarbeitet wurden, werden die benötigten Elemente vom Stack genommen und zu einem neuen AST-Knoten zusammengesetzt.

Der Visitor verarbeitet unter anderem:

* Query und Expression
* AND- und OR-Verknüpfungen
* NOT-Ausdrücke
* geklammerte Ausdrücke
* Vergleiche
* Wertelisten
* String- und Zahlenwerte

## Manuelle AST-Erzeugung

Die Klasse `AstBuilderPattern` traversiert den Parse-Tree direkt über eigene Methoden.

Jede Methode verarbeitet einen bestimmten Kontext und gibt unmittelbar einen AST-Knoten zurück. Dadurch ist der Datenfluss leichter sichtbar und es wird kein interner Stack benötigt.

Beispiele für diese Methoden sind:

* `buildExpr`
* `buildOrExpr`
* `buildAndExpr`
* `buildNotExpr`
* `buildComparison`
* `buildLiteral`

## Vergleich der beiden Ansätze

### Visitor-Pattern

Vorteile:

* passt gut zu den von ANTLR generierten Klassen
* klare Zuordnung zwischen Grammatikregeln und Visitor-Methoden
* neue Verarbeitungsschritte können als weitere Visitor implementiert werden

Nachteile:

* der Ablauf ist indirekter
* bei einem zustandsbehafteten Visitor werden zusätzliche Stacks benötigt
* Fehler beim Pushen und Poppen sind schwieriger zu erkennen

### Manuelle Traversierung

Vorteile:

* direkter Rückgabewert pro Methode
* leichter nachvollziehbarer Datenfluss
* kein zusätzlicher Zustand über Stacks notwendig

Nachteile:

* Traversierung muss vollständig selbst gesteuert werden
* Änderungen an der Grammatik müssen manuell berücksichtigt werden
* bei vielen Grammatikregeln kann die Klasse schnell größer werden

Für diese Aufgabe fand ich die manuelle Variante leichter lesbar. Der Visitor zeigt dafür gut, wie das klassische Visitor-Pattern mit einem ANTLR-Parse-Tree eingesetzt wird.

## AST-Vereinfachung

Die Methode `AstBuilders.simplify` normalisiert den erzeugten AST.

Dabei wird insbesondere eine doppelte Negation entfernt:

```text
not not artist == "Beatles"
```

wird zu:

```text
artist == "Beatles"
```

Auch verschachtelte `And`-, `Or`- und `Not`-Ausdrücke werden rekursiv verarbeitet.

## Tests

Es wurden JUnit-Tests für wichtige Filterausdrücke erstellt.

Getestet werden unter anderem:

* String-Vergleiche
* Zahlenvergleiche
* AND-Verknüpfungen
* NOT-Ausdrücke
* IN-Listen
* identische Ergebnisse beider AST-Builder
* Vereinfachung einer doppelten Negation

Die Tests helfen dabei, die Struktur des erzeugten AST unabhängig von der Benutzeroberfläche zu prüfen.

## Anpassung an Java 21

Die Projektvorlage verwendete einzelne Funktionen, die eine neuere Java-Version oder Vorschaufeatures voraussetzten.

Für die Ausführung mit Java 21 wurden unter anderem folgende Stellen angepasst:

```java
_ -> action()
```

wurde ersetzt durch:

```java
event -> action()
```

Außerdem wurden Ausgaben mit `IO.println` durch `System.out.println` ersetzt.

## Ausführung

Projekt formatieren und testen:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat clean build
```

Anwendung starten:

```powershell
.\gradlew.bat run
```

Danach kann über das Menü `File` die Datei geladen werden:

```text
src/main/resources/songlist.txt
```

Beispiel für einen Filter:

```text
genre in ("rock", "jazz") and year <= 1990
```

## Persönliche Reflexion

Bei dieser Aufgabe habe ich verstanden, dass der Parse-Tree von ANTLR noch sehr stark von der Grammatik abhängt. Der AST enthält dagegen nur die Informationen, die für die spätere Auswertung tatsächlich benötigt werden.

Besonders interessant war der Vergleich zwischen Visitor und manueller Traversierung. Beide Varianten lösen dasselbe Problem, unterscheiden sich aber deutlich im Aufbau und im Umgang mit Zwischenzuständen.

Durch die Tests konnte ich außerdem erkennen, wie wichtig die Operatorreihenfolge bei Ausdrücken mit `and`, `or` und `not` ist.


## About

This represents the student support code for the [Filter-DSL task].

## License

This [work] by [Carsten Gips] and [contributors] is licensed under [MIT].

  [Filter-DSL task]: https://github.com/Programmiermethoden-CampusMinden/Prog2-Lecture/tree/master/homework
  [work]: https://github.com/Programmiermethoden-CampusMinden/prog2_ybel_filterdsl
  [Carsten Gips]: https://github.com/cagix
  [contributors]: https://github.com/Programmiermethoden-CampusMinden/prog2_ybel_filterdsl/graphs/contributors
  [MIT]: LICENSE.md
