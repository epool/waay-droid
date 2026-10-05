# Contract: CardGridFit (shared → both UIs)

`commonMain`, package `dev.epool.waay.game.presentation`, explicit API mode. It is Swift-friendly:
an `object`, primitive parameters, a `data class` result and no generics. Swift calls it as
`CardGridFit.shared.fit(...)`.

```kotlin
public data class CardGrid(
    val columns: Int,
    val rows: Int,
    val cellWidth: Double,
    val cellHeight: Double,
    val fontSize: Double,
    val scrolls: Boolean,
)

public object CardGridFit {
    public fun fit(
        count: Int,
        maxDigits: Int,
        width: Double,
        height: Double,
        spacing: Double,
        minFontSize: Double,
        maxFontSize: Double,
    ): CardGrid
}
```

## Guarantees (each is a `CardGridFitTest` case)

| ID | Given | Then |
|---|---|---|
| F1 | Any input with a feasible fit at `minFontSize` | `scrolls == false`, and the grid (columns × cells + gaps) lies within `width × height` |
| F2 | Any input | `rows == ceil(count / columns)`, and `columns` is in `1..count` |
| F3 | Any non-scrolling result | No other column count yields a larger `fontSize`. A brute-force check over 1..count, with ties going to fewer empty cells and then the squarer grid. |
| F4 | Any result | `fontSize ≤ maxFontSize`, and `fontSize ≥ minFontSize` unless `scrolls` |
| F5 | No column count fits at `minFontSize` | `scrolls == true` and `fontSize == minFontSize`. Columns are the most that hold that size across `width`, and the total height exceeds `height`. |
| F6 | The configurations in [research ADR-013](../research.md#adr-013--grid-fit-as-shared-pure-kotlin-fr-001-to-fr-005) at the default minimum (14 and 15) | `scrolls` matches the feasibility table, and so the narrowed SC-001. Only small landscape phones with 64 numbers and phone split-screen halves with 32 or 64 numbers scroll. 64 numbers still fit phones in portrait, tablets and foldables at 18.2. |
| F7 | `width` or `height` ≤ 0, or `count` ≤ 0 | A defined degenerate result (1 column, `scrolls = true`), never an exception |
| F8 | The same inputs | The same output (pure, deterministic) |

## Font model (shared constants)

| Constant | Value | Meaning |
|---|---|---|
| Digit advance | 0.6 em | Tabular digits (Roboto Flex, SF Pro ≈ 0.55–0.6) |
| Horizontal padding | 0.5 em | Total inside a cell |
| Line height | 1.2 em | |
| Vertical padding | 0.4 em | Total inside a cell |

UIs MUST render numbers with tabular (monospaced) digits at `fontSize`, centred in their cells.
