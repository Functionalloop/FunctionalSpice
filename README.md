# FunctionalSpice: Deu's Tea Stall POS & 58mm Billing App (Android 15)

Offline-first Point of Sale and 58mm thermal receipt app for Deu's Tea Stall, Goa. Kotlin 2.0,
Jetpack Compose (Material 3), Clean Architecture + MVVM, Hilt, Room, Supabase sync, and the DantSu
ESC/POS SDK driving a Posiflow KP206 58mm printer over Bluetooth or USB.

Version 2 (13 Nov 2026) applies the "UI and bill update pack v2" (`bigData.md`): new design tokens,
a Payment screen with change calculation and UPI QR, Bills analytics, a Menu manager, a Printer
screen, and new customer bill / day summary / month summary slips with the Functional footer (logo strip since v2.1).

---

## 1. Specs

- `compileSdk 35`, `targetSdk 35` (Android 15 / One UI 7), `minSdk 28`
- Light mode only: no dynamic color, `android:forceDarkAllowed=false`, edge-to-edge with light system bars
- Offline-first: every bill is written to Room first (`syncStatus = PENDING`) and pushed to Supabase when online
- Printer: `EscPosPrinter(connection, 203, 48f, 32)` = 203 dpi, 48mm printable on 58mm paper, 32 chars/line

## 2. Design system (v3, "neo" look)

| Token | Color | Use |
|---|---|---|
| Background | `#F2F2F5` | screens, system bars |
| Surface | `#FFFFFF`, 20dp radius, no border or elevation | cards |
| Selected card | `#F0FAD2` with 1.5dp ink border | counter items in the bill |
| Ink | `#1B1B1D` | primary pill buttons, selected chips, hero cards, checkout bar |
| Lime | `#C3EF3C` | accent: "Save and print", switches, chart highlight, success |
| Text | `#151517` / `#8A8A92` | primary / secondary |
| Warning / Offline / Danger | `#FFF3DD` / `#EDEDF1` / `#D92D20` | status |

Pill-shaped buttons, chips and segmented controls; 44dp outlined square icon buttons for back.
Space Grotesk for all UI text, with tabular figures for prices, totals, bill numbers and quantities
(bundled in `res/font`, OFL licensed). Color names in `core/theme/Color.kt` are kept from v2 and mean
the roles above. ₹ with Indian grouping on screen (₹1,52,710), "Rs." on paper.

## 3. Screens

Bottom nav: **Counter, Bills, Menu, Printer**.

- **Counter**: category chips wrap (never clip). Tap a card = +1, minus = −1. Long-press (450ms) or tap the
  quantity badge opens the quantity keypad (chips 5/10/20/50, max 3 digits, "Remove item" at 0).
  "Save and print" opens Payment. Sold-out items are hidden.
- **Payment**: Cash | UPI. Cash keypad (max 5 digits), quick amounts from `ChangeCalculator.suggestions()`,
  live result (still to collect / exact / return ₹X with the notes to hand back). UPI shows a QR for
  `upi://pay?pa=…&am=…`; the cashier confirms with "Mark paid and print". The bill is always saved first,
  then printed. If the printer is offline the bill waits in the print queue.
- **Bills**: Today (sales, sync banner, sales by hour, cash/UPI/cash in drawer, categories, item-wise,
  recent bills with reprint, Print summary, Close day) and This month (total, days open, daily average,
  on-pace estimate, daily bars, best day, weeks, top 5, categories, cash vs UPI, Print month summary).
  All numbers come from the name, price and category stored on each bill line.
- **Menu**: add/edit (name max 24, price max 4 digits, category), availability switch (off = sold out),
  two-tap soft delete.
- **Printer**: Bluetooth | USB, status, paired devices, paper facts, auto-reconnect (every 5 s),
  print queue, test slip, Connect/Disconnect, and the editable **shop details** (`ShopConfig` in DataStore:
  shop name, name on bill, "Built for …" name, address, UPI ID, Functional phone).

## 4. Bills on paper (design pages 6–7)

`data/printer/ReceiptFormatter.kt` builds `customerBill()`, `daySummary()` and `monthSummary()`; every line
fits 32 characters (unit-tested).

Customer bill (v2.2 layout): the chai cup logo (`res/drawable/cup_logo.xml`, drawn by `CupLogoRenderer`,
384 × 72 px), **LOCAL SPICE**, `T E A   S T A L L` (the editable "line under name"), `Canacona, Goa`, a tagline
that changes with the time of day, `Bill #` with date and time, one line per item (`Special chai x2 ... 30`;
a long name goes on its own line), a double-width TOTAL (tall when the amount is too long for wide), then
`Paid via UPI` or Cash / Change, "Dev borem korum!", and the footer: the `[logo] Functional` strip drawn by
`FooterStripRenderer` (384 × 72 px, logo 56 dots tall, black and white), "Built for Deu", "Yours next? functionalloop.in" and
`93220 53720`. If an image can't be printed, the bill prints again without the cup and with the word
**Functional** in place of the strip.
Summary slips end with "Billing by Functional" and the same phone number.

## 5. Data

Room is at **version 2**. `MIGRATION_1_2` (`data/local/Migrations.kt`) is purely additive, so existing bills survive:

- `bills`: `cashReceived`, `changeReturned`, `printPending` (the existing `paymentMethod` column is the payment mode)
- `bill_items`: `category` snapshot, backfilled from the menu (`itemName` / `unitPrice` already were the name and price snapshots)
- `menu_items`: `isDeleted` (soft delete; `isAvailable` already existed)
- new `day_closes(date, openingFloat, closedAt, syncState)`

Schemas are exported to `app/schemas/` so future migrations can be checked. Never add
`fallbackToDestructiveMigration()` back: write a `Migration` for every schema change.

Supabase: run `supabase/migrations/2026-11-13_v2_payment_and_day_close.sql` once **before** rolling out the
update (adds `cash_received`, `change_returned`, `bill_items.category` and the `day_closes` table).

## 6. Before rollout

1. Replace the placeholders in the app: Printer tab → Shop details → Edit:
   UPI ID (`deusteastall@upi`) and address line (`Goa`). The Functional phone defaults to `93220 53720`.
2. Replace the logo: `res/drawable/functional_logo.xml` is built from the traced backup. Import the original
   `functional-logo.svg` (File > New > Vector Asset > Local file, name `functional_logo`, black fill).
3. Run the Supabase SQL above.
4. Do one test print on the KP206 (Printer tab → Print test slip) and check the logo strip is sharp and the TOTAL line looks right.

## 7. Structure

```
app/src/main/java/com/functional/spice/
├── core/theme/            Color tokens, fonts, light-only theme
├── domain/
│   ├── model/             Bill, BillItem, MenuItem, ShopConfig, DayClose, summaries, Money (₹ formatting)
│   ├── payment/           ChangeCalculator, UpiLink
│   ├── analytics/         SalesAnalytics (Bills screen + printed summaries)
│   └── repository/        Bill, Menu, DayClose, Printer, Settings interfaces
├── data/
│   ├── local/             Room DB v2, DAOs, entities, Migrations.kt, starter menu
│   ├── remote/            Supabase client + DTOs
│   ├── printer/           Kp206PrinterManager, ReceiptFormatter, FooterStripRenderer, PrintQueue
│   ├── settings/          DataStore ShopConfig + printer preferences
│   └── repository/        Implementations
├── di/                    Hilt modules
└── presentation/
    ├── SpiceApp.kt        Bottom nav + Payment overlay
    ├── home/              Counter + quantity keypad
    ├── payment/           Payment screen + PaymentState (pure, unit-tested)
    ├── bills/             Today / This month
    ├── menu/              Menu manager
    ├── printer/           Printer + shop details
    └── components/        Shared design components, QR image
```

## 8. Build and test

```bash
gradlew.bat test            # unit tests: payment cases, 32-column slips, analytics, menu validation
gradlew.bat assembleDebug
```

On Windows use `gradlew.bat` (the checked-in `gradlew` shell script is incomplete) with `JAVA_HOME`
pointing at a JDK 17+ such as Android Studio's `jbr`.

## 9. Connecting the KP206

- **Bluetooth**: pair the printer in Android settings (PIN `0000` or `1234`), open the Printer tab, tap
  Connect and allow Bluetooth. If several printers are paired, tap the one to use.
- **USB OTG**: plug the printer in, choose USB cable, tap Connect and allow USB access.
#   F u n c t i o n a l S p i c e  
 