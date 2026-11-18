I can't create files, a zip or a PDF in this chat, so here's everything as an indexed set of files. Copy each block into a file with the name shown, keep the folder structure, and zip the folder. For the PDF, open `design/design.html` in Chrome and save it as a PDF (steps are in the README).

## Index

| # | File | What it is |
|---|---|---|
| 1 | `README.md` | Package index and how to make the PDF |
| 2 | `UPDATE_PROMPT.md` | The prompt for your Claude to update the existing app |
| 3 | `design/design.html` | All screens and slips, 7 printable pages, which becomes the design PDF |
| 4 | `bill/ReceiptFormatter.kt` | Bill, day summary and month summary print code (DantSu) |
| 5 | `bill/FooterCardRenderer.kt` | Draws the Functional footer card as a printable image |
| 6 | `bill/ChangeCalculator.kt` | Change, quick amounts and notes to return |
| 7 | `bill/receipt-templates.txt` | Raw printer markup for all three slips |
| 8 | `assets/functional-logo-traced.svg` | My traced copy of your logo, as a backup only |

Also add your **original logo file** to `assets/` as `functional-logo.svg` or `.png`.

---

### 1. `README.md`

````markdown
# Deu's Tea Stall: UI and bill update pack (v2)

Design and print code updates for the existing Deu's Tea Stall POS app.
Hand this whole folder to Claude together with the app's codebase, and start with UPDATE_PROMPT.md.

## Contents

deus-tea-stall-update/
├── README.md                        this file
├── UPDATE_PROMPT.md                 the prompt to give Claude
├── design/
│   └── design.html                  all screens and slips (print to PDF, see below)
├── bill/
│   ├── ReceiptFormatter.kt          customer bill, day and month summary (DantSu markup)
│   ├── FooterCardRenderer.kt        Functional footer card as a 384px image
│   ├── ChangeCalculator.kt          change, quick amounts, notes to return
│   └── receipt-templates.txt        raw printer markup with sample values
└── assets/
    ├── functional-logo.svg          ORIGINAL logo (add it yourself)
    └── functional-logo-traced.svg   traced backup, use only if the original isn't available

## Make the design PDF

1. Open design/design.html in Chrome (it needs internet once for fonts, icons and QR codes).
2. Press Ctrl+P (Cmd+P on Mac).
3. Destination: Save as PDF. Paper: A4. Margins: Default.
4. Under More settings, turn ON "Background graphics" (otherwise colors and dark cards disappear).
5. Save as design/deus-tea-stall-design-v2.pdf.

## Placeholders to replace

- +91 XXXXX XXXXX: Functional contact number (footer card, summary slips)
- deusteastall@upi: the stall's real UPI ID
- Goa: shop address line on slips
````

---

### 2. `UPDATE_PROMPT.md`

````markdown
# Update prompt: Deu's Tea Stall POS, UI and bill v2

You are updating an **existing, working** Android app (Kotlin, Jetpack Compose, MVVM, Hilt,
Room, Supabase, DantSu ESC/POS printing to a Posiflow KP206 58mm printer). Do not rebuild it.

## How to work

1. Read the codebase first. Before changing anything, write me a short map: screens, ViewModels,
   Room entities and DB version, Supabase tables, and how printing works today.
2. Compare it with `design/design.html` (or its PDF) and list what already matches and what changes.
3. Make the changes in small, separate steps, in the order below. Keep the existing architecture,
   naming and package structure. Reuse existing components where they fit.
4. Never use destructive migrations. Add a Room `Migration` for every schema change and give me
   `ALTER TABLE` SQL for Supabase instead of recreating tables.
5. Files in `/bill` are drop-in references: adjust the package name and `R.` references, and merge
   them with existing code rather than duplicating models that already exist.

## Changes

### 1. Theme
Align colors and type with page 1 of the design: background #FAF8F5, surface #F3EFE6, border #E6E0D3
(1dp, 0 elevation), espresso #2C221E, terracotta #C87D55, text #1C1917 / #78716C. Plus Jakarta Sans for
UI, JetBrains Mono for all prices, totals, bill numbers and quantities. Light mode only: no dynamic
color, `android:forceDarkAllowed=false`. Handle edge-to-edge insets (targetSdk 35).

### 2. Counter (design page 2)
- Shop name "Deu's Tea Stall".
- Category chips must wrap to a second line, never clip.
- Tap = +1, minus button = −1. Long-press (450ms) or tapping the quantity badge opens a **quantity
  keypad bottom sheet**: quick chips 5/10/20/50, keypad 1–9, C, 0, backspace, max 3 digits,
  "Set quantity" (shows "Remove item" when 0).
- Hint under the grid: "Tap to add one. Hold or tap the number to type a quantity."
- "Save and print" now opens the Payment screen instead of printing directly.

### 3. Payment and change (design page 3)
- New Payment screen: bill total, Cash | UPI segmented control (Cash default).
- Cash: "Cash received" field with keypad (1–9, 00, 0, backspace, max 5 digits), 4 quick-amount
  chips from `ChangeCalculator.suggestions()` (first one labelled "Exact"), and a live result card:
  nothing entered / "Still to collect ₹X" (warning) / "Exact amount" (success) /
  "Return to customer ₹X" (espresso card) with note chips from `ChangeCalculator.breakdown()`.
- UPI: QR of `upi://pay?pa={upiId}&pn=Deus%20Tea%20Stall&am={total}.00&cu=INR&tn=Bill%20{billNo}`.
  The cashier confirms manually with "Mark paid and print".
- Validate on tap with inline errors ("Enter the cash received first", "Collect ₹X more before
  printing"). Always save the bill first, then print; if the printer is offline, queue the print.
- Store on the bill: payment mode, cash received, change returned.

### 4. Bills: Today and This month (design page 4)
- Today: sales hero (total, bills, items sold, avg bill), unsynced-bills banner with "Sync now",
  sales by hour (6 AM–9 PM, busiest hour in terracotta), by category, item-wise sales,
  Cash sales / UPI sales / Cash in drawer (opening float + cash sales), recent bills,
  "Print summary" and "Close day" (asks for opening float if not set, saves a day-close record,
  prints the day summary).
- This month: total, days open, daily average, bills, "On pace for ₹X" (daily avg × days in month),
  daily bars (today terracotta, future days flat), best day, week by week, top 5 sellers,
  by category, cash vs UPI, "Print month summary".
- Summaries must use the item name and price stored on each bill line, so edited or deleted menu
  items still count correctly. If bill lines don't store these yet, add them.

### 5. Menu manager (design page 5, left)
- New "Menu" tab: add/edit form (name max 24, price digits max 4, category), list grouped by category
  with an availability switch (off = sold out, hidden from Counter), edit, and two-tap delete
  (soft delete). Errors: "Enter an item name", "Enter a price of at least ₹1",
  "{name} is already on the menu".
- Bottom nav becomes: Counter, Bills, Menu, Printer.

### 6. Printer screen (design page 5, right)
Bluetooth | USB segmented control, status card, paired devices, paper facts (58mm, 203 dpi,
32 chars/line), auto-reconnect switch, "Print test slip", Connect/Disconnect.

### 7. Bills on paper (design pages 6–7, files in /bill)
- Replace the current receipt text with `ReceiptFormatter.customerBill()`, `daySummary()` and
  `monthSummary()`. Every line must fit 32 characters; long item names wrap as the formatter does.
- Customer bills end with the Functional footer card from `FooterCardRenderer` (384px wide,
  < 256px tall, pure black and white), printed with `[C]<img>…</img>`. If image printing fails,
  fall back to `ReceiptFormatter.textFooter()`.
- Footer card text: logo + "Functional" (capital F), "Built for {shopDisplayName}.",
  "Let's build yours.", phone number. QR: `https://enterprises.functionalloop.in/?ref={slug}&src=bill-card`.
- Logo: use `assets/functional-logo.svg` (import as a Vector Asset, black fill). Print it at least
  56px (~7mm) tall so the gaps in the F stay open.
- Summary slips have no QR; they end with "Billing by Functional" and "Need help? {phone}".
- Add ZXing core (`com.google.zxing:core`) if it isn't already a dependency.
- Keep shop name, display name, slug, phone and UPI ID in one editable `ShopConfig` (DataStore).

### 8. Data
Room: add to bills `paymentMode` (CASH/UPI, default CASH), `cashReceived: Int?`,
`changeReturned: Int?`; to bill lines `nameSnapshot`, `priceSnapshot` if missing; to menu items
`isAvailable`, `isDeleted` if missing; new `DayClose(date, openingFloat, closedAt, syncState)`.
Supabase (adapt table names to the existing schema):

```sql
alter table bills add column if not exists payment_mode text not null default 'CASH'
  check (payment_mode in ('CASH','UPI'));
alter table bills add column if not exists cash_received int;
alter table bills add column if not exists change_returned int;
alter table bill_lines add column if not exists name_snapshot text;
alter table bill_lines add column if not exists price_snapshot int;
create table if not exists day_closes (
  date date primary key, opening_float int not null default 0, closed_at timestamptz
);
```
New rows follow the existing offline-first sync (write to Room as pending, sync when online).

## Done when
- [ ] Payment shows correct results for 230/300, 230/230, 230/200 and 95/500
- [ ] Bills save when the printer is offline and print after it reconnects
- [ ] Every printed line fits 32 characters; a real KP206 test print shows the footer card crisp
- [ ] Summaries match the database, including deleted menu items
- [ ] Existing data survives the migration
- [ ] UI stays light on a Samsung phone in dark mode; nothing hides behind system bars
````

---

### 3. `design/design.html`

```html
<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8">
<title>Deu's Tea Stall: UI and bill design v2</title>
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;700&display=swap" rel="stylesheet">
<link href="https://cdn.jsdelivr.net/npm/@tabler/icons-webfont@latest/dist/tabler-icons.min.css" rel="stylesheet">
<style>
*{box-sizing:border-box}
body{margin:0;background:#E9E4DA;font-family:'Plus Jakarta Sans',sans-serif;color:#1C1917;-webkit-print-color-adjust:exact;print-color-adjust:exact}
.page{width:794px;margin:24px auto;background:#fff;padding:36px 32px;page-break-after:always}
@page{size:A4;margin:8mm}
@media print{body{background:#fff}.page{margin:0;width:auto;padding:0;zoom:.74}}
h1{font-size:30px;font-weight:800;margin:0 0 6px;color:#2C221E;letter-spacing:-.5px}
h2{font-size:20px;font-weight:700;margin:0 0 4px;color:#2C221E}
.sub{color:#78716C;font-size:13px;margin:0 0 22px;line-height:1.5}
.row{display:flex;gap:28px;justify-content:center;align-items:flex-start}
.cap{font-size:12px;color:#57534E;margin:10px 0 0;width:320px;line-height:1.5}
.mo{font-family:'JetBrains Mono',monospace}
.ph{width:320px;background:#FAF8F5;border:1px solid #E6E0D3;border-radius:28px;overflow:hidden;position:relative;display:flex;flex-direction:column}
.sb{display:flex;justify-content:space-between;padding:10px 20px 0;font-size:11px;color:#78716C}
.hd{padding:12px 16px 4px;display:flex;justify-content:space-between;align-items:baseline}
.ttl{font-size:19px;font-weight:700;color:#2C221E}
.pd{padding:4px 16px 16px}
.fx{display:flex;align-items:center}.sbw{justify-content:space-between}
.pill{display:flex;align-items:center;gap:6px;border-radius:999px;padding:5px 10px;font-size:11px;font-weight:600;white-space:nowrap;background:#E3F0E6;color:#1F5C38}
.dot{width:7px;height:7px;border-radius:50%;background:#2F9E5B}
.chips{display:flex;gap:6px;flex-wrap:wrap;padding:6px 16px 10px}
.chip{padding:6px 11px;border-radius:999px;border:1px solid #E6E0D3;background:#F3EFE6;font-size:12px;color:#57534E;font-weight:500;white-space:nowrap}
.chip.on{background:#2C221E;color:#FAF8F5;border-color:#2C221E}
.grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;padding:0 16px 6px}
.card{position:relative;background:#F3EFE6;border:1px solid #E6E0D3;border-radius:14px;padding:12px;height:84px}
.card.has{border-color:#C87D55;background:#F5E8DC}
.card>i{font-size:18px;color:#C87D55}
.cn{font-size:13.5px;font-weight:600;margin-top:4px;line-height:1.2}
.cp{font-size:13px;color:#78716C;margin-top:2px;font-family:'JetBrains Mono',monospace}
.qb{position:absolute;top:9px;right:9px;min-width:28px;height:26px;padding:0 7px;border-radius:13px;background:#C87D55;color:#fff;font-size:13px;font-weight:700;display:flex;align-items:center;justify-content:center;font-family:'JetBrains Mono',monospace}
.mn{position:absolute;bottom:9px;right:9px;width:30px;height:30px;border-radius:9px;border:1px solid #C87D55;background:#FAF8F5;color:#C87D55;display:flex;align-items:center;justify-content:center;font-size:16px}
.cbar{background:#2C221E;color:#FAF8F5;margin:0 10px 10px;border-radius:20px;padding:12px 12px 12px 16px;display:flex;align-items:center;gap:8px}
.cd{background:#F3EFE6;border:1px solid #E6E0D3;border-radius:14px;padding:12px}
.lb{font-size:12px;color:#78716C;margin:16px 0 6px;font-weight:600}
.rw{display:flex;align-items:center;gap:10px;padding:9px 0;border-top:1px solid #E6E0D3}
.rw:first-child{border-top:none}
.sg{display:flex;background:#F3EFE6;border:1px solid #E6E0D3;border-radius:12px;padding:3px}
.sg>div{flex:1;text-align:center;padding:7px 4px;border-radius:9px;font-size:12px;color:#78716C;font-weight:500}
.sg>div.on{background:#2C221E;color:#FAF8F5;font-weight:600}
.bt{text-align:center;padding:11px 8px;border-radius:12px;font-size:13px;font-weight:600}
.pri{background:#C87D55;color:#fff}.gh{border:1px solid #E6E0D3;background:#FAF8F5}.dk{background:#2C221E;color:#FAF8F5}
.nv{display:flex;justify-content:space-around;border-top:1px solid #E6E0D3;padding:8px 0 12px;font-size:11px;color:#78716C;margin-top:auto;background:#FAF8F5}
.nv>div{text-align:center;min-width:56px}.nv i{font-size:20px;display:block}.nv .on{color:#2C221E;font-weight:700}
.hero{margin-top:12px;background:#2C221E;color:#FAF8F5;border-radius:16px;padding:16px}
.hero .l{font-size:12px;color:#C9BBAE}.hero .big{font-size:30px;font-weight:700;font-family:'JetBrains Mono',monospace}
.st{display:grid;grid-template-columns:repeat(3,1fr);gap:6px;margin-top:10px}
.st b{display:block;font-size:15px;font-family:'JetBrains Mono',monospace}.st span{font-size:11px;color:#C9BBAE}
.tr{height:6px;border-radius:3px;background:#E6E0D3;overflow:hidden;margin:4px 0 10px}.tr>div{height:6px;background:#C87D55}
.kv{display:flex;justify-content:space-between;font-size:13px}
.bars{display:flex;align-items:flex-end;gap:3px;height:74px}.bars>div{flex:1;border-radius:3px 3px 0 0;background:#2C221E}
.k{height:44px;border-radius:12px;background:#F3EFE6;border:1px solid #E6E0D3;display:flex;align-items:center;justify-content:center;font-size:19px;font-family:'JetBrains Mono',monospace}
.kg{display:grid;grid-template-columns:repeat(3,1fr);gap:8px}
.sw{width:36px;height:22px;border-radius:11px;background:#C87D55;position:relative;flex:none}.sw::after{content:'';position:absolute;top:3px;right:3px;width:16px;height:16px;border-radius:50%;background:#fff}
.sw.off{background:#D6CFC4}.sw.off::after{right:auto;left:3px}
.note{display:inline-flex;padding:5px 9px;border-radius:6px;background:#FAF8F5;border:1px solid #E6E0D3;font-size:12px;font-family:'JetBrains Mono',monospace;font-weight:700;color:#1C1917}
.slip{width:248px;background:#fff;padding:18px 12px 14px;font-family:'JetBrains Mono',monospace;font-size:11.5px;line-height:1.55;border:1px solid #E6E0D3;border-bottom:1px dashed #B9B0A3;flex:none}
.r{display:flex;justify-content:space-between;white-space:pre}.c{text-align:center;white-space:pre}
.sh{font-size:19px;font-weight:700;letter-spacing:1px;line-height:1.3}
.tb{display:flex;justify-content:space-between;background:#1C1917;color:#fff;font-size:15px;font-weight:700;padding:5px 6px;margin:6px 0}
.fc{border:1.5px solid #1C1917;border-radius:6px;padding:8px;display:flex;gap:9px;align-items:center;font-family:'Plus Jakarta Sans',sans-serif}
.swt{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}
.swt .s{height:56px;border-radius:12px;border:1px solid #E6E0D3}
.swt p{margin:6px 0 0;font-size:12px;line-height:1.4}
table.sp{border-collapse:collapse;width:100%;font-size:13px}table.sp td{border-top:1px solid #E6E0D3;padding:7px 6px;vertical-align:top}
ul.rules{font-size:13px;line-height:1.7;padding-left:18px;margin:8px 0 0}
</style></head><body>
<svg width="0" height="0" style="position:absolute" aria-hidden="true"><defs><symbol id="fmark" viewBox="405 335 505 560"><path d="M903,343 L598,455 Q548,475 548,525 L545,600 Q542,622 522,632 L458,658 Q416,676 416,718 L416,887 L510,848 Q543,832 543,797 L544,650 Q546,612 580,602 L818,510 Q850,497 860,478 L885,438 Q900,413 900,385 Z"/><path d="M563,787 L563,690 Q565,655 595,642 L730,588 L726,690 Q720,728 686,741 Z"/></symbol></defs></svg>

<!-- PAGE 1 -->
<section class="page">
<div class="fx" style="gap:12px;margin-bottom:18px"><svg width="34" height="38" fill="#2C221E"><use href="#fmark"/></svg><div class="mo" style="font-size:12px;color:#78716C">Functional, design pack v2, 13 Nov 2026</div></div>
<h1>Deu's Tea Stall</h1>
<p class="sub">UI and bill design update for the existing POS app: Android 15, Samsung phone, Posiflow KP206 58mm printer. Read with UPDATE_PROMPT.md and the code in /bill. Sales figures are sample data.</p>
<table class="sp">
<tr><td class="mo">1</td><td>Design tokens</td><td style="color:#78716C">this page</td></tr>
<tr><td class="mo">2</td><td>Counter and quantity keypad</td><td></td></tr>
<tr><td class="mo">3</td><td>Payment: cash with change, and UPI</td><td></td></tr>
<tr><td class="mo">4</td><td>Bills: today and this month</td><td></td></tr>
<tr><td class="mo">5</td><td>Menu manager and printer</td><td></td></tr>
<tr><td class="mo">6</td><td>Customer bill and Functional footer card</td><td></td></tr>
<tr><td class="mo">7</td><td>Day and month summary slips</td><td></td></tr>
</table>
<h2 style="margin-top:28px">1. Design tokens</h2>
<p class="sub">Light mode only. Flat: 1dp borders, no shadows, no gradients.</p>
<div class="swt">
<div><div class="s" style="background:#FAF8F5"></div><p><b>Background</b><br><span class="mo">#FAF8F5</span></p></div>
<div><div class="s" style="background:#F3EFE6"></div><p><b>Surface</b><br><span class="mo">#F3EFE6</span></p></div>
<div><div class="s" style="background:#E6E0D3"></div><p><b>Border</b><br><span class="mo">#E6E0D3</span></p></div>
<div><div class="s" style="background:#F5E8DC;border-color:#C87D55"></div><p><b>Selected card</b><br><span class="mo">#F5E8DC</span></p></div>
<div><div class="s" style="background:#2C221E"></div><p><b>Espresso</b> primary<br><span class="mo">#2C221E</span></p></div>
<div><div class="s" style="background:#C87D55"></div><p><b>Terracotta</b> actions<br><span class="mo">#C87D55</span></p></div>
<div><div class="s" style="background:#1C1917"></div><p><b>Text</b><br><span class="mo">#1C1917</span></p></div>
<div><div class="s" style="background:#78716C"></div><p><b>Text secondary</b><br><span class="mo">#78716C</span></p></div>
<div><div class="s" style="background:#E3F0E6"></div><p><b>Success</b><br><span class="mo">#E3F0E6 / #1F5C38</span></p></div>
<div><div class="s" style="background:#F8ECD4"></div><p><b>Warning</b><br><span class="mo">#F8ECD4 / #7A4E0E</span></p></div>
<div><div class="s" style="background:#EFEAE3"></div><p><b>Offline</b><br><span class="mo">#EFEAE3 / #57534E</span></p></div>
<div><div class="s" style="background:#B42318"></div><p><b>Danger text</b><br><span class="mo">#B42318</span></p></div>
</div>
<div class="row" style="justify-content:flex-start;gap:40px;margin-top:24px">
<div><div style="font-size:26px;font-weight:700;color:#2C221E">Plus Jakarta Sans</div><div style="font-size:13px;color:#78716C">Headings, buttons, labels. 700 titles, 600 labels, 500 body.</div></div>
<div><div class="mo" style="font-size:26px;font-weight:700">₹1,52,710</div><div style="font-size:13px;color:#78716C">JetBrains Mono: prices, totals, bill numbers, quantities.</div></div>
</div>
<ul class="rules">
<li>Radii: cards 14, hero cards 16, buttons 12, sheets 22 (top), checkout bar 20, chips fully round.</li>
<li>Sentence case. Buttons start with a verb: "Save and print", "Set quantity", "Add to menu".</li>
<li>₹ with Indian grouping on screen; "Rs." on paper (the printer has no ₹ glyph).</li>
</ul>
</section>

<!-- PAGE 2 -->
<section class="page">
<h2>2. Counter and quantity keypad</h2>
<p class="sub">Tap a card to add one; minus removes one. Hold a card or tap its number to type a quantity. Chips wrap, never clip.</p>
<div class="row">
<div>
<div class="ph">
<div class="sb mo"><span>21:15</span><span><i class="ti ti-antenna-bars-3"></i> <i class="ti ti-battery-3"></i></span></div>
<div style="padding:10px 16px 6px">
<div class="fx sbw"><div style="font-size:18px;font-weight:700;color:#2C221E">Deu's Tea Stall</div><div class="pill"><span class="dot"></span>KP206 connected</div></div>
<div class="fx sbw" style="margin-top:8px"><div style="font-size:12px;color:#57534E">Next bill <b class="mo" style="color:#1C1917">#1042</b></div><div class="sg" style="padding:2px;border-radius:10px"><div class="on" style="padding:4px 10px;font-size:11px"><i class="ti ti-bluetooth"></i> BT</div><div style="padding:4px 10px;font-size:11px"><i class="ti ti-usb"></i> USB</div></div></div>
</div>
<div class="chips"><div class="chip on">All</div><div class="chip">Hot tea</div><div class="chip">Snacks</div><div class="chip">Cold drinks</div></div>
<div class="grid">
<div class="card has"><i class="ti ti-cup"></i><div class="cn" style="padding-right:30px">Cutting chai</div><div class="cp">₹10</div><div class="qb">2</div><div class="mn"><i class="ti ti-minus"></i></div></div>
<div class="card has"><i class="ti ti-cup"></i><div class="cn" style="padding-right:30px">Special chai</div><div class="cp">₹15</div><div class="qb">2</div><div class="mn"><i class="ti ti-minus"></i></div></div>
<div class="card"><i class="ti ti-cup"></i><div class="cn">Masala chai</div><div class="cp">₹20</div></div>
<div class="card"><i class="ti ti-cup"></i><div class="cn">Ginger lemon tea</div><div class="cp">₹20</div></div>
<div class="card has"><i class="ti ti-bread"></i><div class="cn" style="padding-right:30px">Bhaji pav</div><div class="cp">₹40</div><div class="qb">3</div><div class="mn"><i class="ti ti-minus"></i></div></div>
<div class="card has"><i class="ti ti-bread"></i><div class="cn" style="padding-right:30px">Ros omelette</div><div class="cp">₹60</div><div class="qb">1</div><div class="mn"><i class="ti ti-minus"></i></div></div>
<div class="card"><i class="ti ti-bread"></i><div class="cn">Samosa</div><div class="cp">₹15</div></div>
<div class="card"><i class="ti ti-glass"></i><div class="cn">Kokum sherbet</div><div class="cp">₹25</div></div>
</div>
<div style="font-size:11px;color:#A8A29E;text-align:center;padding:4px 16px 10px">Tap to add one. Hold or tap the number to type a quantity.</div>
<div class="cbar"><div style="flex:1"><div style="font-size:11px;color:#C9BBAE">8 items</div><div class="mo" style="font-size:22px;font-weight:700">₹230</div></div><div style="border:1px solid #5A4A42;color:#E6DCD2;border-radius:12px;padding:10px 12px;font-size:13px">Clear</div><div style="background:#C87D55;color:#fff;border-radius:12px;padding:10px 12px;font-size:13px;font-weight:600"><i class="ti ti-printer"></i> Save and print</div></div>
<div class="nv"><div class="on"><i class="ti ti-layout-grid"></i>Counter</div><div><i class="ti ti-receipt"></i>Bills</div><div><i class="ti ti-tools-kitchen-2"></i>Menu</div><div><i class="ti ti-printer"></i>Printer</div></div>
</div>
<p class="cap">Counter. Selected cards get a terracotta border and quantity badge. "Save and print" opens Payment.</p>
</div>
<div>
<div class="ph">
<div class="sb mo"><span>21:15</span><span><i class="ti ti-antenna-bars-3"></i> <i class="ti ti-battery-3"></i></span></div>
<div style="padding:10px 16px 6px"><div class="fx sbw"><div style="font-size:18px;font-weight:700;color:#2C221E">Deu's Tea Stall</div><div class="pill"><span class="dot"></span>KP206 connected</div></div></div>
<div class="chips"><div class="chip on">All</div><div class="chip">Hot tea</div><div class="chip">Snacks</div><div class="chip">Cold drinks</div></div>
<div class="grid" style="padding-bottom:14px"><div class="card has"><div class="cn">Cutting chai</div><div class="cp">₹10</div></div><div class="card"><div class="cn">Special chai</div><div class="cp">₹15</div></div></div>
<div style="position:absolute;inset:0;background:rgba(28,25,23,.45)"></div>
<div style="position:relative;background:#FAF8F5;border-radius:22px 22px 0 0;padding:14px 16px 18px;margin-top:-40px">
<div style="width:36px;height:4px;border-radius:2px;background:#E6E0D3;margin:0 auto 12px"></div>
<div class="fx sbw" style="align-items:baseline"><div style="font-size:16px;font-weight:700">Cutting chai</div><div class="mo" style="font-size:12px;color:#78716C">₹10 each</div></div>
<div class="fx sbw" style="align-items:flex-end;margin:12px 0 10px;background:#F3EFE6;border:1px solid #E6E0D3;border-radius:14px;padding:10px 14px"><div><div style="font-size:11px;color:#78716C">Quantity</div><div class="mo" style="font-size:34px;font-weight:700;line-height:1.1">12<span style="color:#C87D55;font-weight:400">|</span></div></div><div class="mo" style="font-size:16px;color:#C87D55;font-weight:700">₹120</div></div>
<div class="fx" style="gap:6px;margin-bottom:10px"><div class="chip" style="flex:1;text-align:center">5</div><div class="chip" style="flex:1;text-align:center">10</div><div class="chip" style="flex:1;text-align:center">20</div><div class="chip" style="flex:1;text-align:center">50</div></div>
<div class="kg"><div class="k">1</div><div class="k">2</div><div class="k">3</div><div class="k">4</div><div class="k">5</div><div class="k">6</div><div class="k">7</div><div class="k">8</div><div class="k">9</div><div class="k" style="font-size:15px">C</div><div class="k">0</div><div class="k"><i class="ti ti-backspace"></i></div></div>
<div class="fx" style="gap:8px;margin-top:12px"><div class="bt gh" style="flex:1">Cancel</div><div class="bt pri" style="flex:1">Set quantity</div></div>
</div>
</div>
<p class="cap">Quantity keypad (bottom sheet). Max 3 digits. At 0 the button reads "Remove item".</p>
</div>
</div>
</section>

<!-- PAGE 3 -->
<section class="page">
<h2>3. Payment and change</h2>
<p class="sub">Opens from "Save and print". Cash shows change and which notes to return; UPI shows a QR with the amount filled in.</p>
<div class="row">
<div>
<div class="ph">
<div style="padding:16px 16px 8px" class="fx"><i class="ti ti-arrow-left" style="font-size:20px;color:#2C221E;margin-right:10px"></i><div><div style="font-size:17px;font-weight:700;color:#2C221E">Payment</div><div style="font-size:12px;color:#78716C">Bill <span class="mo">#1042</span>, 8 items</div></div></div>
<div class="pd">
<div class="fx sbw" style="align-items:baseline;padding:4px 0 12px"><span style="font-size:13px;color:#57534E">Bill total</span><span class="mo" style="font-size:28px;font-weight:700">₹230</span></div>
<div class="sg"><div class="on"><i class="ti ti-cash"></i> Cash</div><div><i class="ti ti-qrcode"></i> UPI</div></div>
<div style="font-size:12px;color:#78716C;margin:14px 0 6px">Cash received</div>
<div class="fx sbw" style="align-items:baseline;background:#F3EFE6;border:1.5px solid #C87D55;border-radius:14px;padding:8px 14px"><span class="mo" style="font-size:30px;font-weight:700">₹300<span style="color:#C87D55;font-weight:400">|</span></span><span style="font-size:12px;color:#78716C">Clear</span></div>
<div class="fx" style="gap:6px;margin-top:10px"><div class="chip mo" style="flex:1;text-align:center;font-weight:600;color:#1C1917">Exact</div><div class="chip mo" style="flex:1;text-align:center;font-weight:600;color:#1C1917">₹250</div><div class="chip on mo" style="flex:1;text-align:center;font-weight:600">₹300</div><div class="chip mo" style="flex:1;text-align:center;font-weight:600;color:#1C1917">₹500</div></div>
<div style="margin-top:12px;background:#2C221E;border-radius:14px;padding:12px 14px"><div class="fx sbw"><span style="font-size:13px;color:#E6DCD2;font-weight:600">Return to customer</span><span class="mo" style="font-size:30px;font-weight:700;color:#fff">₹70</span></div><div class="fx" style="gap:6px;margin-top:8px"><span class="note">₹50</span><span class="note">₹20</span></div></div>
<div class="kg" style="margin-top:12px"><div class="k">1</div><div class="k">2</div><div class="k">3</div><div class="k">4</div><div class="k">5</div><div class="k">6</div><div class="k">7</div><div class="k">8</div><div class="k">9</div><div class="k">00</div><div class="k">0</div><div class="k"><i class="ti ti-backspace"></i></div></div>
<div class="bt pri" style="margin-top:14px;padding:13px;font-size:14px"><i class="ti ti-printer"></i> Confirm and print</div>
</div>
</div>
<p class="cap">Cash. Other result states: "Enter the cash the customer gave you", "Still to collect ₹30" (warning colors), "Exact amount" (success colors).</p>
</div>
<div>
<div class="ph">
<div style="padding:16px 16px 8px" class="fx"><i class="ti ti-arrow-left" style="font-size:20px;color:#2C221E;margin-right:10px"></i><div><div style="font-size:17px;font-weight:700;color:#2C221E">Payment</div><div style="font-size:12px;color:#78716C">Bill <span class="mo">#1042</span>, 8 items</div></div></div>
<div class="pd">
<div class="fx sbw" style="align-items:baseline;padding:4px 0 12px"><span style="font-size:13px;color:#57534E">Bill total</span><span class="mo" style="font-size:28px;font-weight:700">₹230</span></div>
<div class="sg"><div><i class="ti ti-cash"></i> Cash</div><div class="on"><i class="ti ti-qrcode"></i> UPI</div></div>
<div style="text-align:center;padding-top:16px">
<div style="font-size:12px;color:#78716C">Customer scans to pay exactly</div>
<div class="mo" style="font-size:22px;font-weight:700;margin:2px 0 10px">₹230</div>
<div style="display:flex;justify-content:center"><div data-qr="upi://pay?pa=deusteastall@upi&pn=Deus%20Tea%20Stall&am=230.00&cu=INR&tn=Bill%201042" data-size="160" style="width:176px;height:176px;padding:8px;background:#fff;border:1px solid #E6E0D3;border-radius:14px"></div></div>
<div class="mo" style="font-size:12px;color:#57534E;margin-top:8px">deusteastall@upi</div>
<div style="font-size:11px;color:#78716C;margin-top:2px">No change needed for UPI</div>
</div>
<div class="bt pri" style="margin-top:22px;padding:13px;font-size:14px"><i class="ti ti-printer"></i> Mark paid and print</div>
</div>
</div>
<p class="cap">UPI. The app can't detect the payment; the cashier confirms after their phone or soundbox shows it arrived.</p>
</div>
</div>
</section>

<!-- PAGE 4 -->
<section class="page">
<h2>4. Bills: today and this month</h2>
<p class="sub">Segmented Today | This month. Numbers come from bill lines (name and price stored at sale time).</p>
<div class="row">
<div class="ph">
<div class="hd"><span class="ttl">Bills</span><span style="font-size:12px;color:#78716C">13 Nov 2026</span></div>
<div class="pd">
<div class="sg"><div class="on">Today</div><div>This month</div></div>
<div class="hero"><div class="l">Sales today</div><div class="big">₹12,290</div><div class="st"><div><b>318</b><span>Bills</span></div><div><b>646</b><span>Items sold</span></div><div><b>₹39</b><span>Avg bill</span></div></div></div>
<div class="cd fx" style="margin-top:10px;gap:10px;background:#F8ECD4;border-color:#EDD7AE"><i class="ti ti-cloud-off" style="font-size:18px;color:#7A4E0E"></i><div style="flex:1;font-size:12px;color:#7A4E0E"><div style="font-weight:600;font-size:13px">3 bills waiting to sync</div>Saved on this phone</div><div style="font-size:12px;font-weight:600;color:#7A4E0E;border:1px solid #D9B77A;border-radius:10px;padding:6px 10px">Sync now</div></div>
<div class="lb">Sales by hour</div>
<div class="cd"><div class="bars"><div style="height:21px"></div><div style="height:54px"></div><div style="height:70px;background:#C87D55"></div><div style="height:59px"></div><div style="height:43px"></div><div style="height:37px"></div><div style="height:48px"></div><div style="height:54px"></div><div style="height:43px"></div><div style="height:37px"></div><div style="height:48px"></div><div style="height:59px"></div><div style="height:65px"></div><div style="height:54px"></div><div style="height:32px"></div><div style="height:15px"></div></div>
<div class="mo fx sbw" style="font-size:11px;color:#78716C;margin-top:4px"><span>6a</span><span>10a</span><span>2p</span><span>6p</span><span>9p</span></div><div style="font-size:12px;color:#57534E;margin-top:6px">Busiest: 8 AM to 9 AM</div></div>
<div class="lb">Cash and UPI</div>
<div class="cd" style="padding:2px 12px">
<div class="rw"><span style="flex:1;font-size:13px">Cash sales</span><span class="mo" style="font-size:13px;font-weight:700">₹9,840</span></div>
<div class="rw"><span style="flex:1;font-size:13px">UPI sales</span><span class="mo" style="font-size:13px;font-weight:700">₹2,450</span></div>
<div class="rw"><span style="flex:1;font-size:13px">Cash in drawer <span style="color:#78716C;font-size:11px">incl. ₹500 float</span></span><span class="mo" style="font-size:13px;font-weight:700">₹10,340</span></div>
</div>
<div class="lb">By category</div>
<div class="cd">
<div class="kv"><span><i class="ti ti-cup" style="color:#C87D55"></i> Hot tea <span style="color:#78716C">47%</span></span><b class="mo">₹5,800</b></div><div class="tr"><div style="width:47%"></div></div>
<div class="kv"><span><i class="ti ti-bread" style="color:#C87D55"></i> Snacks <span style="color:#78716C">42%</span></span><b class="mo">₹5,125</b></div><div class="tr"><div style="width:42%"></div></div>
<div class="kv"><span><i class="ti ti-glass" style="color:#C87D55"></i> Cold drinks <span style="color:#78716C">11%</span></span><b class="mo">₹1,365</b></div><div class="tr" style="margin-bottom:0"><div style="width:11%"></div></div>
</div>
<div class="lb">Item-wise sales</div>
<div class="cd" style="padding:2px 12px">
<div class="rw"><div style="flex:1"><div style="font-size:13px;font-weight:600">Bhaji pav</div><div style="font-size:11px;color:#78716C"><span class="mo">64</span> sold at <span class="mo">₹40</span></div></div><b class="mo" style="font-size:13px">₹2,560</b></div>
<div class="rw"><div style="flex:1"><div style="font-size:13px;font-weight:600">Cutting chai</div><div style="font-size:11px;color:#78716C"><span class="mo">214</span> sold at <span class="mo">₹10</span></div></div><b class="mo" style="font-size:13px">₹2,140</b></div>
<div class="rw"><div style="flex:1"><div style="font-size:13px;font-weight:600">Special chai</div><div style="font-size:11px;color:#78716C"><span class="mo">96</span> sold at <span class="mo">₹15</span></div></div><b class="mo" style="font-size:13px">₹1,440</b></div>
<div class="rw" style="justify-content:center;color:#C87D55;font-size:12px;font-weight:600">Show all 12 items <i class="ti ti-chevron-down"></i></div>
</div>
<div class="lb">Recent bills</div>
<div class="cd" style="padding:2px 12px">
<div class="rw"><div style="flex:1"><div class="mo" style="font-size:13px;font-weight:700">#1041</div><div style="font-size:11px;color:#78716C">21:12, 2 items, Cash</div></div><span class="mo" style="font-size:13px">₹70</span><i class="ti ti-cloud-off" style="color:#D99A2B;font-size:16px"></i></div>
<div class="rw"><div style="flex:1"><div class="mo" style="font-size:13px;font-weight:700">#1040</div><div style="font-size:11px;color:#78716C">21:05, 4 items, UPI</div></div><span class="mo" style="font-size:13px">₹115</span><i class="ti ti-cloud-off" style="color:#D99A2B;font-size:16px"></i></div>
<div class="rw"><div style="flex:1"><div class="mo" style="font-size:13px;font-weight:700">#1038</div><div style="font-size:11px;color:#78716C">20:51, 3 items, Cash</div></div><span class="mo" style="font-size:13px">₹85</span><i class="ti ti-cloud-check" style="color:#2F9E5B;font-size:16px"></i></div>
</div>
<div class="fx" style="gap:8px;margin-top:14px"><div class="bt gh" style="flex:1"><i class="ti ti-printer"></i> Print summary</div><div class="bt pri" style="flex:1">Close day</div></div>
</div>
<div class="nv"><div><i class="ti ti-layout-grid"></i>Counter</div><div class="on"><i class="ti ti-receipt"></i>Bills</div><div><i class="ti ti-tools-kitchen-2"></i>Menu</div><div><i class="ti ti-printer"></i>Printer</div></div>
</div>
<div class="ph">
<div class="hd"><span class="ttl">Bills</span><span style="font-size:12px;color:#78716C">November 2026</span></div>
<div class="pd">
<div class="sg"><div>Today</div><div class="on">This month</div></div>
<div class="hero"><div class="l">November so far</div><div class="big">₹1,52,710</div><div class="st"><div><b>13</b><span>Days open</span></div><div><b>₹11,747</b><span>Daily avg</span></div><div><b>3,968</b><span>Bills</span></div></div><div style="font-size:12px;color:#E6DCD2;margin-top:10px;border-top:1px solid #4A3C35;padding-top:8px">On pace for <b class="mo" style="color:#fff">₹3,52,400</b> by month end</div></div>
<div class="lb">Daily sales</div>
<div class="cd"><div class="bars" style="gap:2px;height:80px"><div style="height:61px"></div><div style="height:65px"></div><div style="height:68px"></div><div style="height:55px"></div><div style="height:74px"></div><div style="height:62px"></div><div style="height:71px"></div><div style="height:78px"></div><div style="height:60px"></div><div style="height:66px"></div><div style="height:69px"></div><div style="height:54px"></div><div style="height:68px;background:#C87D55"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div><div style="height:4px;background:#E6E0D3"></div></div>
<div class="mo fx sbw" style="font-size:11px;color:#78716C;margin-top:4px"><span>1</span><span>10</span><span>20</span><span>30</span></div>
<div class="fx sbw" style="font-size:12px;color:#57534E;margin-top:8px;border-top:1px solid #E6E0D3;padding-top:8px"><span>Best day: 8 Nov</span><b class="mo" style="color:#1C1917">₹14,120</b></div></div>
<div class="lb">Week by week</div>
<div class="cd" style="padding:2px 12px">
<div class="rw"><div style="flex:1"><div style="font-size:13px;font-weight:600">Week 1</div><div style="font-size:11px;color:#78716C">1 to 7 Nov, 7 days</div></div><b class="mo" style="font-size:13px">₹81,800</b></div>
<div class="rw"><div style="flex:1"><div style="font-size:13px;font-weight:600">Week 2</div><div style="font-size:11px;color:#78716C">8 to 13 Nov, 6 days so far</div></div><b class="mo" style="font-size:13px">₹70,910</b></div>
</div>
<div class="lb">Top sellers this month</div>
<div class="cd">
<div class="kv"><span><b class="mo" style="color:#C87D55">1</b>&nbsp; Cutting chai</span><b class="mo">2,716</b></div><div class="tr"><div style="width:100%;background:#2C221E"></div></div>
<div class="kv"><span><b class="mo" style="color:#C87D55">2</b>&nbsp; Special chai</span><b class="mo">1,231</b></div><div class="tr"><div style="width:45%;background:#2C221E"></div></div>
<div class="kv"><span><b class="mo" style="color:#C87D55">3</b>&nbsp; Bhaji pav</span><b class="mo">826</b></div><div class="tr"><div style="width:30%;background:#2C221E"></div></div>
<div class="kv"><span><b class="mo" style="color:#C87D55">4</b>&nbsp; Masala chai</span><b class="mo">748</b></div><div class="tr"><div style="width:28%;background:#2C221E"></div></div>
<div class="kv"><span><b class="mo" style="color:#C87D55">5</b>&nbsp; Samosa</span><b class="mo">603</b></div><div class="tr" style="margin-bottom:0"><div style="width:22%;background:#2C221E"></div></div>
</div>
<div class="lb">By category, cash and UPI</div>
<div class="cd" style="padding:2px 12px">
<div class="rw"><span style="flex:1;font-size:13px">Hot tea <span style="color:#78716C">47%</span></span><b class="mo" style="font-size:13px">₹71,800</b></div>
<div class="rw"><span style="flex:1;font-size:13px">Snacks <span style="color:#78716C">42%</span></span><b class="mo" style="font-size:13px">₹64,140</b></div>
<div class="rw"><span style="flex:1;font-size:13px">Cold drinks <span style="color:#78716C">11%</span></span><b class="mo" style="font-size:13px">₹16,770</b></div>
<div class="rw"><span style="flex:1;font-size:13px">Cash / UPI</span><b class="mo" style="font-size:13px">₹1,22,170 / ₹30,540</b></div>
</div>
<div class="bt gh" style="margin-top:14px"><i class="ti ti-printer"></i> Print month summary</div>
</div>
<div class="nv"><div><i class="ti ti-layout-grid"></i>Counter</div><div class="on"><i class="ti ti-receipt"></i>Bills</div><div><i class="ti ti-tools-kitchen-2"></i>Menu</div><div><i class="ti ti-printer"></i>Printer</div></div>
</div>
</div>
</section>

<!-- PAGE 5 -->
<section class="page">
<h2>5. Menu manager and printer</h2>
<p class="sub">Sold-out items stay in the menu but disappear from the counter. Delete needs a second tap.</p>
<div class="row">
<div class="ph">
<div class="hd"><span class="ttl">Menu</span><span style="font-size:12px;color:#78716C">12 items</span></div>
<div class="pd">
<div class="cd">
<div style="font-size:14px;font-weight:700;margin-bottom:10px">Add an item</div>
<div style="font-size:11px;color:#78716C;margin-bottom:4px">Name</div><div style="height:40px;border-radius:10px;border:1px solid #C87D55;background:#FAF8F5;display:flex;align-items:center;padding:0 10px;font-size:14px">Masala milk<span style="color:#C87D55">|</span></div>
<div class="fx" style="gap:8px;margin-top:10px;align-items:flex-end"><div style="width:96px"><div style="font-size:11px;color:#78716C;margin-bottom:4px">Price in ₹</div><div class="mo" style="height:40px;border-radius:10px;border:1px solid #E6E0D3;background:#FAF8F5;display:flex;align-items:center;padding:0 10px;font-size:14px">25</div></div><div style="flex:1;font-size:11px;color:#78716C;padding-bottom:12px">Shows on the counter right away</div></div>
<div style="font-size:11px;color:#78716C;margin:10px 0 4px">Category</div>
<div class="sg" style="background:#FAF8F5"><div class="on">Hot tea</div><div>Snacks</div><div>Cold drinks</div></div>
<div class="bt pri" style="margin-top:12px"><i class="ti ti-plus"></i> Add to menu</div>
</div>
<div class="lb">Hot tea</div>
<div class="cd" style="padding:2px 12px">
<div class="rw"><div style="flex:1"><div style="font-size:13px;font-weight:600">Cutting chai</div><div class="mo" style="font-size:11px;color:#78716C">₹10</div></div><div class="sw"></div><i class="ti ti-pencil" style="font-size:18px;color:#57534E"></i><i class="ti ti-trash" style="font-size:18px;color:#A8A29E"></i></div>
<div class="rw"><div style="flex:1"><div style="font-size:13px;font-weight:600">Special chai</div><div class="mo" style="font-size:11px;color:#78716C">₹15</div></div><div class="sw"></div><i class="ti ti-pencil" style="font-size:18px;color:#57534E"></i><i class="ti ti-trash" style="font-size:18px;color:#A8A29E"></i></div>
<div class="rw"><div style="flex:1"><div style="font-size:13px;font-weight:600;color:#A8A29E">Filter coffee</div><div style="font-size:11px;color:#78716C"><span class="mo">₹20</span>, sold out</div></div><div class="sw off"></div><i class="ti ti-pencil" style="font-size:18px;color:#57534E"></i><i class="ti ti-trash" style="font-size:18px;color:#A8A29E"></i></div>
</div>
<div class="lb">Snacks</div>
<div class="cd" style="padding:2px 12px">
<div class="rw"><div style="flex:1"><div style="font-size:13px;font-weight:600">Bhaji pav</div><div class="mo" style="font-size:11px;color:#78716C">₹40</div></div><div class="sw"></div><i class="ti ti-pencil" style="font-size:18px;color:#57534E"></i><i class="ti ti-trash" style="font-size:18px;color:#A8A29E"></i></div>
<div class="rw"><div style="flex:1"><div style="font-size:13px;font-weight:600">Ros omelette</div><div class="mo" style="font-size:11px;color:#78716C">₹60</div></div><div class="sw"></div><i class="ti ti-pencil" style="font-size:18px;color:#57534E"></i><i class="ti ti-trash" style="font-size:18px;color:#B42318"></i></div>
<div style="font-size:11px;color:#B42318;padding:0 0 8px">Tap the bin again to delete Ros omelette</div>
</div>
</div>
<div class="nv"><div><i class="ti ti-layout-grid"></i>Counter</div><div><i class="ti ti-receipt"></i>Bills</div><div class="on"><i class="ti ti-tools-kitchen-2"></i>Menu</div><div><i class="ti ti-printer"></i>Printer</div></div>
</div>
<div class="ph">
<div class="hd"><span class="ttl">Printer</span></div>
<div class="pd">
<div class="sg"><div class="on"><i class="ti ti-bluetooth"></i> Bluetooth</div><div><i class="ti ti-usb"></i> USB cable</div></div>
<div class="cd fx" style="margin-top:12px;gap:12px;background:#E3F0E6;border-color:#C9DFCF"><div style="width:44px;height:44px;border-radius:12px;background:#2C221E;color:#FAF8F5;display:flex;align-items:center;justify-content:center"><i class="ti ti-printer" style="font-size:22px"></i></div><div style="flex:1"><div style="font-weight:600;font-size:14px">Posiflow KP206</div><div style="font-size:12px;color:#1F5C38">Connected, ready to print</div><div class="mo" style="font-size:11px;color:#78716C">DC:0D:30:4A:11:8F</div></div></div>
<div class="lb">Paired devices</div>
<div class="cd" style="padding:2px 12px">
<div class="rw"><i class="ti ti-printer" style="color:#C87D55;font-size:18px"></i><div style="flex:1;font-size:13px">Posiflow KP206</div><i class="ti ti-circle-check" style="color:#2F9E5B;font-size:18px"></i></div>
<div class="rw"><i class="ti ti-headphones" style="color:#A8A29E;font-size:18px"></i><div style="flex:1;font-size:13px;color:#78716C">Galaxy Buds</div><span style="font-size:11px;color:#A8A29E">Not a printer</span></div>
</div>
<div class="lb">Paper</div>
<div class="cd" style="display:grid;grid-template-columns:repeat(3,1fr);text-align:center;padding:10px 4px"><div><b class="mo" style="font-size:15px">58mm</b><div style="font-size:11px;color:#78716C">width</div></div><div><b class="mo" style="font-size:15px">203</b><div style="font-size:11px;color:#78716C">dpi</div></div><div><b class="mo" style="font-size:15px">32</b><div style="font-size:11px;color:#78716C">chars/line</div></div></div>
<div class="cd fx" style="margin-top:10px;gap:10px"><div style="flex:1"><div style="font-size:13px;font-weight:600">Reconnect automatically</div><div style="font-size:11px;color:#78716C">Retries every 5 seconds if the link drops</div></div><div class="sw"></div></div>
<div class="fx" style="gap:8px;margin-top:14px"><div class="bt dk" style="flex:1">Print test slip</div><div class="bt gh" style="flex:1">Disconnect</div></div>
</div>
<div class="nv"><div><i class="ti ti-layout-grid"></i>Counter</div><div><i class="ti ti-receipt"></i>Bills</div><div><i class="ti ti-tools-kitchen-2"></i>Menu</div><div class="on"><i class="ti ti-printer"></i>Printer</div></div>
</div>
</div>
</section>

<!-- PAGE 6 -->
<section class="page">
<h2>6. Customer bill and Functional footer card</h2>
<p class="sub">58mm, 32 characters per line. The footer card is drawn as one black-and-white image and printed with an img tag.</p>
<div class="row">
<div class="slip">
<div class="c sh">DEU'S TEA STALL</div><div class="c">Goa</div><div class="c">================================</div>
<div class="r"><b>Bill #1042</b><span>13-Nov-26 21:15</span></div>
<div class="c">--------------------------------</div>
<div class="r"><b>Item</b><b>Qty x Rate     Amt</b></div>
<div class="r"><span>Special chai</span><span>2 x 15       30.00</span></div>
<div class="r"><span>Bhaji pav</span><span>3 x 40      120.00</span></div>
<div class="r"><span>Ros omelette</span><span>1 x 60       60.00</span></div>
<div class="r"><span>Cutting chai</span><span>2 x 10       20.00</span></div>
<div class="c">--------------------------------</div>
<div class="r"><span>Items: 8</span><span></span></div>
<div class="tb"><span>TOTAL</span><span>Rs. 230.00</span></div>
<div class="r"><span>Paid by</span><span>Cash</span></div>
<div class="r"><span>Cash received</span><span>Rs. 300.00</span></div>
<div class="r" style="font-weight:700"><span>Change returned</span><span>Rs. 70.00</span></div>
<div class="c">--------------------------------</div>
<div class="c" style="font-weight:700">Dev borem korum!</div><div class="c">Thank you, visit again</div><div class="c"> </div>
<div class="fc"><div data-qr="https://enterprises.functionalloop.in/?ref=deus-tea-stall&src=bill-card" data-size="66" style="width:66px;height:66px;flex:none"></div><div style="flex:1;line-height:1.3"><div class="fx" style="gap:4px"><svg width="26" height="29" fill="#1C1917"><use href="#fmark"/></svg><span style="font-size:15px;font-weight:800;letter-spacing:-.3px">Functional</span></div><div style="font-size:11px;font-weight:500;margin-top:3px">Built for Deu's.</div><div style="font-size:12px;font-weight:800">Let's build yours.</div><div class="mo" style="font-size:11px;font-weight:700;margin-top:4px;border-top:1px solid #1C1917;padding-top:4px">+91 XXXXX XXXXX</div></div></div>
<div class="c"> </div>
</div>
<div style="width:400px">
<div style="font-size:13px;font-weight:700;margin-bottom:8px">Footer card at print size (384 × 176 px)</div>
<div style="width:384px;height:176px;border:3px solid #1C1917;border-radius:10px;padding:0 14px;display:flex;align-items:center;gap:12px;background:#fff">
<div data-qr="https://enterprises.functionalloop.in/?ref=deus-tea-stall&src=bill-card" data-size="112" style="width:112px;height:112px;flex:none"></div>
<div style="flex:1">
<div class="fx" style="gap:6px"><svg width="50" height="56" fill="#1C1917"><use href="#fmark"/></svg><span style="font-size:30px;font-weight:800;letter-spacing:-.5px">Functional</span></div>
<div style="font-size:21px;font-weight:500;margin-top:8px;line-height:1.2">Built for Deu's.</div>
<div style="font-size:22px;font-weight:800;line-height:1.25">Let's build yours.</div>
<div class="mo" style="font-size:20px;font-weight:700;border-top:2px solid #1C1917;margin-top:6px;padding-top:4px">+91 XXXXX XXXXX</div>
</div></div>
<table class="sp" style="margin-top:14px">
<tr><td>Canvas</td><td>384 px wide (48mm at 203 dpi), 176 px tall, must stay under 256 px</td></tr>
<tr><td>Colors</td><td>Pure black on white, hard threshold, no greys or dithering</td></tr>
<tr><td>Border</td><td>3 px, radius 10 px, inner padding 14 px</td></tr>
<tr><td>QR</td><td>112 px, error correction M, 1-module margin, whole-pixel modules</td></tr>
<tr><td>Logo</td><td>Original file, black fill, 56 px tall (about 7mm). Smaller and the gaps in the F fill in</td></tr>
<tr><td>Text</td><td>Functional: Jakarta ExtraBold 30. Built for…: Medium 21. Let's build yours.: ExtraBold 22. Phone: JetBrains Mono Bold 20. Shrink to fit if needed</td></tr>
<tr><td>QR link</td><td class="mo" style="font-size:11px">enterprises.functionalloop.in/?ref=deus-tea-stall&amp;src=bill-card</td></tr>
</table>
</div>
</div>
</section>

<!-- PAGE 7 -->
<section class="page">
<h2>7. Day and month summary slips</h2>
<p class="sub">Read only by the owner, so no QR: they end with a support line instead.</p>
<div class="row">
<div class="slip">
<div class="c sh">DEU'S TEA STALL</div><div class="c">Goa</div><div class="c">================================</div>
<div class="c"><b>DAY SUMMARY</b></div><div class="c"> </div>
<div class="r"><span>Date</span><span>13-Nov-26</span></div>
<div class="r"><span>Printed</span><span>21:15</span></div>
<div class="r"><span>Bills</span><span>318</span></div>
<div class="r"><span>Items sold</span><span>646</span></div>
<div class="r"><span>Avg bill</span><span>Rs. 38.65</span></div>
<div class="r"><span>Bill range</span><span>#0724-#1041</span></div>
<div class="c">--------------------------------</div>
<div class="r"><b>Item</b><b>  Qty        Amt</b></div>
<div class="r"><span>Bhaji pav</span><span>   64    2560.00</span></div>
<div class="r"><span>Cutting chai</span><span>  214    2140.00</span></div>
<div class="r"><span>Special chai</span><span>   96    1440.00</span></div>
<div class="r"><span>Masala chai</span><span>   58    1160.00</span></div>
<div class="r"><span>Ros omelette</span><span>   18    1080.00</span></div>
<div class="r"><span>Vada pav</span><span>   39     780.00</span></div>
<div class="r"><span>Samosa</span><span>   47     705.00</span></div>
<div class="r"><span>Kokum sherbet</span><span>   26     650.00</span></div>
<div class="r"><span>Ginger lemon tea</span><span></span></div>
<div class="r"><span></span><span>   31     620.00</span></div>
<div class="r"><span>Lime soda</span><span>   19     475.00</span></div>
<div class="r"><span>Filter coffee</span><span>   22     440.00</span></div>
<div class="r"><span>Water 1L</span><span>   12     240.00</span></div>
<div class="c">--------------------------------</div>
<div class="r"><span>Hot tea</span><span>Rs. 5800.00</span></div>
<div class="r"><span>Snacks</span><span>Rs. 5125.00</span></div>
<div class="r"><span>Cold drinks</span><span>Rs. 1365.00</span></div>
<div class="c">--------------------------------</div>
<div class="r"><span>Cash sales</span><span>Rs. 9840.00</span></div>
<div class="r"><span>UPI sales</span><span>Rs. 2450.00</span></div>
<div class="r"><span>Opening float</span><span>Rs. 500.00</span></div>
<div class="r" style="font-weight:700"><span>Cash in drawer</span><span>Rs. 10340.00</span></div>
<div class="tb"><span>TOTAL</span><span>Rs. 12290.00</span></div>
<div class="c">- - - - - - - - - - - - - - - - </div>
<div class="c">Billing by Functional</div>
<div class="c">Need help? +91 XXXXX XXXXX</div>
<div class="c"> </div>
</div>
<div class="slip">
<div class="c sh">DEU'S TEA STALL</div><div class="c">Goa</div><div class="c">================================</div>
<div class="c"><b>MONTH SUMMARY</b></div><div class="c"> </div>
<div class="r"><span>Month</span><span>Nov-26</span></div>
<div class="r"><span>Days open</span><span>13</span></div>
<div class="r"><span>Bills</span><span>3968</span></div>
<div class="r"><span>Daily avg</span><span>Rs. 11746.92</span></div>
<div class="r"><span>Best day</span><span>08-Nov</span></div>
<div class="r"><span>Best day sales</span><span>Rs. 14120.00</span></div>
<div class="c">--------------------------------</div>
<div class="r"><span>Week 1 (1-7)</span><span>Rs. 81800.00</span></div>
<div class="r"><span>Week 2 (8-13)</span><span>Rs. 70910.00</span></div>
<div class="c">--------------------------------</div>
<div class="r"><b>Top items</b><b>Qty</b></div>
<div class="r"><span>Cutting chai</span><span>2716</span></div>
<div class="r"><span>Special chai</span><span>1231</span></div>
<div class="r"><span>Bhaji pav</span><span>826</span></div>
<div class="r"><span>Masala chai</span><span>748</span></div>
<div class="r"><span>Samosa</span><span>603</span></div>
<div class="c">--------------------------------</div>
<div class="r"><span>Hot tea</span><span>Rs. 71800.00</span></div>
<div class="r"><span>Snacks</span><span>Rs. 64140.00</span></div>
<div class="r"><span>Cold drinks</span><span>Rs. 16770.00</span></div>
<div class="c">--------------------------------</div>
<div class="r"><span>Cash sales</span><span>Rs. 122170.00</span></div>
<div class="r"><span>UPI sales</span><span>Rs. 30540.00</span></div>
<div class="tb"><span>TOTAL</span><span>Rs. 152710.00</span></div>
<div class="c">- - - - - - - - - - - - - - - - </div>
<div class="c">Billing by Functional</div>
<div class="c">Need help? +91 XXXXX XXXXX</div>
<div class="c"> </div>
</div>
</div>
</section>

<script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
<script>
document.querySelectorAll('[data-qr]').forEach(function(el){
  var s=+el.dataset.size;
  try{new QRCode(el,{text:el.dataset.qr,width:s,height:s,colorDark:'#1C1917',colorLight:'#ffffff',correctLevel:QRCode.CorrectLevel.M});}catch(e){}
});
</script>
</body></html>
```

---

### 4. `bill/ReceiptFormatter.kt`

```kotlin
package com.yourapp.print // TODO: change to your package

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PaymentMode { CASH, UPI }

data class ShopConfig(
    val printName: String = "DEU'S TEA STALL",   // max 16 chars for the big font
    val displayName: String = "Deu's",           // used in "Built for Deu's."
    val address: String = "Goa",
    val slug: String = "deus-tea-stall",         // QR tracking ref
    val supportPhone: String = "+91 XXXXX XXXXX",
    val upiId: String = "deusteastall@upi",
)

data class BillLine(val name: String, val qty: Int, val rate: Int) {
    val amount: Int get() = qty * rate
}

data class PrintableBill(
    val billNo: Int,
    val createdAt: Long,
    val lines: List<BillLine>,
    val paymentMode: PaymentMode,
    val cashReceived: Int? = null,
    val changeReturned: Int? = null,
) {
    val total: Int get() = lines.sumOf { it.amount }
    val itemCount: Int get() = lines.sumOf { it.qty }
}

data class ItemTotal(val name: String, val qty: Int, val amount: Int)

data class DaySummary(
    val date: Long,
    val printedAt: Long,
    val bills: Int,
    val firstBillNo: Int,
    val lastBillNo: Int,
    val items: List<ItemTotal>,
    val categories: List<Pair<String, Int>>,
    val cashSales: Int,
    val upiSales: Int,
    val openingFloat: Int,
) {
    val total: Int get() = cashSales + upiSales
    val itemsSold: Int get() = items.sumOf { it.qty }
}

data class MonthSummary(
    val month: Long,
    val daysOpen: Int,
    val bills: Int,
    val bestDay: Long,
    val bestDaySales: Int,
    val weeks: List<Pair<String, Int>>,          // e.g. "Week 1 (1-7)" to 81800
    val topItems: List<ItemTotal>,
    val categories: List<Pair<String, Int>>,
    val cashSales: Int,
    val upiSales: Int,
) {
    val total: Int get() = cashSales + upiSales
}

/**
 * Builds DantSu ESC/POS markup for a 58mm printer:
 * EscPosPrinter(connection, 203, 48f, 32). Every line fits 32 characters.
 */
object ReceiptFormatter {
    const val COLS = 32
    private val en = Locale.ENGLISH

    private fun fmt(pattern: String, time: Long) = SimpleDateFormat(pattern, en).format(Date(time))
    private fun clean(s: String) = s.replace(Regex("[<>\\[\\]]"), "").take(COLS)
    private fun amt(r: Int) = "$r.00"
    private fun amt(d: Double) = String.format(Locale.US, "%.2f", d)
    private fun rs(r: Int) = "Rs. ${amt(r)}"
    private fun rule(c: Char) = "[C]${c.toString().repeat(COLS)}\n"
    private fun lr(left: String, right: String) = "[L]$left[R]$right\n"

    /** Long names get their own line; the numbers go on the next line. */
    private fun item(name: String, right: String, maxName: Int) =
        if (name.length > maxName) "[L]$name\n[R]$right\n" else lr(name, right)

    /** Inverted TOTAL bar, padded by hand to exactly 32 chars. */
    private fun totalBar(total: Int): String {
        val left = " TOTAL"
        val right = "${rs(total)} "
        val gap = (COLS - left.length - right.length).coerceAtLeast(1)
        return "[L]<font size='tall' color='bg-black'><b>$left${" ".repeat(gap)}$right</b></font>\n"
    }

    private fun header(shop: ShopConfig): String {
        val size = if (shop.printName.length <= 16) "big" else "tall"
        return "[C]<b><font size='$size'>${clean(shop.printName)}</font></b>\n" +
            "[C]${clean(shop.address)}\n" + rule('=')
    }

    fun footerUrl(shop: ShopConfig) =
        "https://enterprises.functionalloop.in/?ref=${shop.slug}&src=bill-card"

    /** Fallback when the printer can't print the footer image. */
    fun textFooter(shop: ShopConfig): String = buildString {
        append("[C]- - - - - - - - - - - - - - - -\n")
        append("[C]<b>Built for ${clean(shop.displayName)}.</b>\n")
        append("[C]Let's build yours.\n")
        append("[C]<qrcode size='18'>${footerUrl(shop)}</qrcode>\n")
        append("[C]<b>Functional  ${shop.supportPhone}</b>\n")
    }

    private fun summaryFooter(shop: ShopConfig) =
        "[C]- - - - - - - - - - - - - - - -\n[C]Billing by Functional\n[C]Need help? ${shop.supportPhone}\n[L]\n[L]\n"

    /** @param footerImgHex from FooterCardRenderer.hex(); pass null to use the text footer. */
    fun customerBill(bill: PrintableBill, shop: ShopConfig, footerImgHex: String?): String = buildString {
        append(header(shop))
        append(lr("<b>Bill #${bill.billNo}</b>", fmt("dd-MMM-yy HH:mm", bill.createdAt)))
        append(rule('-'))
        append(lr("<b>Item</b>", "<b>${"Qty x Rate".padEnd(10)}${"Amt".padStart(8)}</b>"))
        bill.lines.forEach { l ->
            append(item(clean(l.name), "${l.qty} x ${l.rate}".padEnd(10) + amt(l.amount).padStart(8), 13))
        }
        append(rule('-'))
        append("[L]Items: ${bill.itemCount}\n")
        append(totalBar(bill.total))
        when (bill.paymentMode) {
            PaymentMode.UPI -> append(lr("Paid by", "UPI"))
            PaymentMode.CASH -> {
                append(lr("Paid by", "Cash"))
                bill.cashReceived?.let { append(lr("Cash received", rs(it))) }
                bill.changeReturned?.let { append(lr("<b>Change returned</b>", "<b>${rs(it)}</b>")) }
            }
        }
        append(rule('-'))
        append("[C]<b>Dev borem korum!</b>\n[C]Thank you, visit again\n[L]\n")
        if (footerImgHex != null) append("[C]<img>$footerImgHex</img>\n") else append(textFooter(shop))
        append("[L]\n[L]\n")
    }

    fun daySummary(d: DaySummary, shop: ShopConfig): String = buildString {
        append(header(shop))
        append("[C]<b>DAY SUMMARY</b>\n[L]\n")
        append(lr("Date", fmt("dd-MMM-yy", d.date)))
        append(lr("Printed", fmt("HH:mm", d.printedAt)))
        append(lr("Bills", "${d.bills}"))
        append(lr("Items sold", "${d.itemsSold}"))
        append(lr("Avg bill", "Rs. " + amt(if (d.bills > 0) d.total.toDouble() / d.bills else 0.0)))
        append(lr("Bill range", String.format(en, "#%04d-#%04d", d.firstBillNo, d.lastBillNo)))
        append(rule('-'))
        append(lr("<b>Item</b>", "<b>${"Qty".padStart(5)}${"Amt".padStart(11)}</b>"))
        d.items.sortedByDescending { it.amount }.forEach {
            append(item(clean(it.name), "${it.qty}".padStart(5) + amt(it.amount).padStart(11), 15))
        }
        append(rule('-'))
        d.categories.forEach { (name, a) -> append(lr(clean(name), rs(a))) }
        append(rule('-'))
        append(lr("Cash sales", rs(d.cashSales)))
        append(lr("UPI sales", rs(d.upiSales)))
        append(lr("Opening float", rs(d.openingFloat)))
        append(lr("<b>Cash in drawer</b>", "<b>${rs(d.openingFloat + d.cashSales)}</b>"))
        append(totalBar(d.total))
        append(summaryFooter(shop))
    }

    fun monthSummary(m: MonthSummary, shop: ShopConfig): String = buildString {
        append(header(shop))
        append("[C]<b>MONTH SUMMARY</b>\n[L]\n")
        append(lr("Month", fmt("MMM-yy", m.month)))
        append(lr("Days open", "${m.daysOpen}"))
        append(lr("Bills", "${m.bills}"))
        append(lr("Daily avg", "Rs. " + amt(if (m.daysOpen > 0) m.total.toDouble() / m.daysOpen else 0.0)))
        append(lr("Best day", fmt("dd-MMM", m.bestDay)))
        append(lr("Best day sales", rs(m.bestDaySales)))
        append(rule('-'))
        m.weeks.forEach { (label, a) -> append(lr(clean(label), rs(a))) }
        append(rule('-'))
        append(lr("<b>Top items</b>", "<b>Qty</b>"))
        m.topItems.take(8).forEach { append(lr(clean(it.name).take(24), "${it.qty}")) }
        append(rule('-'))
        m.categories.forEach { (name, a) -> append(lr(clean(name), rs(a))) }
        append(rule('-'))
        append(lr("Cash sales", rs(m.cashSales)))
        append(lr("UPI sales", rs(m.upiSales)))
        append(totalBar(m.total))
        append(summaryFooter(shop))
    }
}
```

---

### 5. `bill/FooterCardRenderer.kt`

```kotlin
package com.yourapp.print // TODO: change to your package

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Draws the Functional footer card as a pure black-and-white bitmap for the KP206.
 *
 * Usage:
 *   val hex = footerRenderer.hex(printer, shop)
 *   printer.printFormattedText(ReceiptFormatter.customerBill(bill, shop, hex))
 * If image printing throws, print again with footerImgHex = null (text footer).
 */
class FooterCardRenderer(
    private val context: Context,
    @DrawableRes private val logoRes: Int,          // functional-logo.svg imported as Vector Asset
    @FontRes private val fontMedium: Int,           // plus_jakarta_sans_medium
    @FontRes private val fontExtraBold: Int,        // plus_jakarta_sans_extrabold
    @FontRes private val fontMonoBold: Int,         // jetbrains_mono_bold
) {
    companion object {
        const val WIDTH = 384     // 48mm at 203 dpi
        const val HEIGHT = 176    // DantSu images must stay under 256 px tall
        private const val PAD = 14
        private const val QR = 112
        private const val LOGO_H = 56 // about 7mm; smaller and the logo's gaps fill in
    }

    private var cacheKey: String? = null
    private var cacheHex: String? = null

    /** Hex string for [C]<img>…</img>, cached until shop details change. */
    fun hex(printer: EscPosPrinter, shop: ShopConfig): String {
        val key = "${shop.displayName}|${shop.slug}|${shop.supportPhone}"
        if (key == cacheKey) cacheHex?.let { return it }
        return PrinterTextParserImg.bitmapToHexadecimalString(printer, render(shop)).also {
            cacheKey = key
            cacheHex = it
        }
    }

    fun render(shop: ShopConfig): Bitmap {
        val bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp).apply { drawColor(Color.WHITE) }

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f
        }
        c.drawRoundRect(RectF(1.5f, 1.5f, WIDTH - 1.5f, HEIGHT - 1.5f), 10f, 10f, border)

        val qr = qrBitmap(ReceiptFormatter.footerUrl(shop), QR)
        c.drawBitmap(qr, PAD.toFloat(), ((HEIGHT - QR) / 2).toFloat(), null)

        val x = (PAD + QR + 12).toFloat()
        val maxW = WIDTH - x - PAD

        // Logo + wordmark
        val logo = ContextCompat.getDrawable(context, logoRes)!!.mutate()
        val logoW = LOGO_H * logo.intrinsicWidth / logo.intrinsicHeight
        val logoTop = 12
        logo.setBounds(x.toInt(), logoTop, x.toInt() + logoW, logoTop + LOGO_H)
        logo.setTint(Color.BLACK)
        logo.draw(c)

        val word = paint(fontExtraBold, 30f).also { fit(it, "Functional", maxW - logoW - 6) }
        val wordBaseline = logoTop + LOGO_H / 2 - (word.ascent() + word.descent()) / 2
        c.drawText("Functional", x + logoW + 6, wordBaseline, word)

        val line1 = "Built for ${shop.displayName}."
        val p1 = paint(fontMedium, 21f).also { fit(it, line1, maxW) }
        c.drawText(line1, x, 98f, p1)

        val line2 = "Let's build yours."
        val p2 = paint(fontExtraBold, 22f).also { fit(it, line2, maxW) }
        c.drawText(line2, x, 126f, p2)

        val rule = Paint().apply { color = Color.BLACK; strokeWidth = 2f }
        c.drawLine(x, 138f, WIDTH - PAD.toFloat(), 138f, rule)

        val p3 = paint(fontMonoBold, 20f).also { fit(it, shop.supportPhone, maxW) }
        c.drawText(shop.supportPhone, x, 162f, p3)

        return toMonochrome(bmp)
    }

    private fun paint(@FontRes font: Int, size: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = size
        typeface = ResourcesCompat.getFont(context, font) ?: Typeface.DEFAULT_BOLD
    }

    /** Shrinks the text size until it fits the available width. */
    private fun fit(p: Paint, text: String, maxW: Float) {
        while (p.measureText(text) > maxW && p.textSize > 12f) p.textSize -= 1f
    }

    /** QR with whole-pixel modules so it stays sharp and scannable on thermal paper. */
    private fun qrBitmap(text: String, size: Int): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
        val scale = (size / m.width).coerceAtLeast(1)
        val off = (size - m.width * scale) / 2
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val c = Canvas(out)
        val p = Paint().apply { color = Color.BLACK }
        for (y in 0 until m.height) for (x in 0 until m.width) {
            if (m.get(x, y)) {
                val l = (off + x * scale).toFloat()
                val t = (off + y * scale).toFloat()
                c.drawRect(l, t, l + scale, t + scale, p)
            }
        }
        return out
    }

    /** Hard black/white threshold: no greys, no dithering. */
    private fun toMonochrome(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        for (i in px.indices) {
            val c = px[i]
            val lum = (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000
            px[i] = if (lum < 140) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }
}
```

---

### 6. `bill/ChangeCalculator.kt`

```kotlin
package com.yourapp.domain.payment // TODO: change to your package

/** All amounts are whole rupees. */
object ChangeCalculator {
    private val DENOMS = intArrayOf(500, 200, 100, 50, 20, 10, 5, 2, 1)

    /** Positive = give back, negative = still to collect, 0 = exact. */
    fun change(total: Int, received: Int): Int = received - total

    /** Quick-amount chips. 230 -> [230, 250, 300, 500]; 300 -> [300, 350, 400, 500]. */
    fun suggestions(total: Int): List<Int> {
        val out = linkedSetOf(total)
        for (d in intArrayOf(50, 100, 500, 1000, 2000)) {
            out += (total / d + 1) * d          // next multiple strictly above total
            if (out.size >= 4) break
        }
        return out.sorted().take(4)
    }

    /** Fewest notes and coins. 70 -> [(50, 1), (20, 1)]. */
    fun breakdown(change: Int): List<Pair<Int, Int>> {
        var left = change
        return DENOMS.mapNotNull { d ->
            (left / d).takeIf { it > 0 }?.let { n -> left -= n * d; d to n }
        }
    }
}
```

---

### 7. `bill/receipt-templates.txt`

```text
DEU'S TEA STALL: RAW PRINTER MARKUP (DantSu, 58mm, 32 columns)
Generated in code by ReceiptFormatter.kt. Sample values below.

=== CUSTOMER BILL ===
[C]<b><font size='big'>DEU'S TEA STALL</font></b>
[C]Goa
[C]================================
[L]<b>Bill #1042</b>[R]13-Nov-26 21:15
[C]--------------------------------
[L]<b>Item</b>[R]<b>Qty x Rate     Amt</b>
[L]Special chai[R]2 x 15       30.00
[L]Bhaji pav[R]3 x 40      120.00
[L]Ros omelette[R]1 x 60       60.00
[L]Cutting chai[R]2 x 10       20.00
[C]--------------------------------
[L]Items: 8
[L]<font size='tall' color='bg-black'><b> TOTAL               Rs. 230.00 </b></font>
[L]Paid by[R]Cash
[L]Cash received[R]Rs. 300.00
[L]<b>Change returned</b>[R]<b>Rs. 70.00</b>
[C]--------------------------------
[C]<b>Dev borem korum!</b>
[C]Thank you, visit again
[L]
[C]<img>{FOOTER_CARD_HEX}</img>
[L]
[L]

(UPI bill: replace the three payment lines with  [L]Paid by[R]UPI )
(Item names over 13 chars: name on its own [L] line, numbers on the next [R] line)

=== FALLBACK FOOTER (if image printing fails) ===
[C]- - - - - - - - - - - - - - - -
[C]<b>Built for Deu's.</b>
[C]Let's build yours.
[C]<qrcode size='18'>https://enterprises.functionalloop.in/?ref=deus-tea-stall&src=bill-card</qrcode>
[C]<b>Functional  +91 XXXXX XXXXX</b>

=== DAY SUMMARY ===
[C]<b><font size='big'>DEU'S TEA STALL</font></b>
[C]Goa
[C]================================
[C]<b>DAY SUMMARY</b>
[L]
[L]Date[R]13-Nov-26
[L]Printed[R]21:15
[L]Bills[R]318
[L]Items sold[R]646
[L]Avg bill[R]Rs. 38.65
[L]Bill range[R]#0724-#1041
[C]--------------------------------
[L]<b>Item</b>[R]<b>  Qty        Amt</b>
[L]Bhaji pav[R]   64    2560.00
[L]Cutting chai[R]  214    2140.00
[L]Ginger lemon tea
[R]   31     620.00
(...one line per item, sorted by amount; names over 15 chars wrap)
[C]--------------------------------
[L]Hot tea[R]Rs. 5800.00
[L]Snacks[R]Rs. 5125.00
[L]Cold drinks[R]Rs. 1365.00
[C]--------------------------------
[L]Cash sales[R]Rs. 9840.00
[L]UPI sales[R]Rs. 2450.00
[L]Opening float[R]Rs. 500.00
[L]<b>Cash in drawer</b>[R]<b>Rs. 10340.00</b>
[L]<font size='tall' color='bg-black'><b> TOTAL             Rs. 12290.00 </b></font>
[C]- - - - - - - - - - - - - - - -
[C]Billing by Functional
[C]Need help? +91 XXXXX XXXXX
[L]
[L]

=== MONTH SUMMARY ===
[C]<b><font size='big'>DEU'S TEA STALL</font></b>
[C]Goa
[C]================================
[C]<b>MONTH SUMMARY</b>
[L]
[L]Month[R]Nov-26
[L]Days open[R]13
[L]Bills[R]3968
[L]Daily avg[R]Rs. 11746.92
[L]Best day[R]08-Nov
[L]Best day sales[R]Rs. 14120.00
[C]--------------------------------
[L]Week 1 (1-7)[R]Rs. 81800.00
[L]Week 2 (8-13)[R]Rs. 70910.00
[C]--------------------------------
[L]<b>Top items</b>[R]<b>Qty</b>
[L]Cutting chai[R]2716
[L]Special chai[R]1231
[L]Bhaji pav[R]826
[L]Masala chai[R]748
[L]Samosa[R]603
[C]--------------------------------
[L]Hot tea[R]Rs. 71800.00
[L]Snacks[R]Rs. 64140.00
[L]Cold drinks[R]Rs. 16770.00
[C]--------------------------------
[L]Cash sales[R]Rs. 122170.00
[L]UPI sales[R]Rs. 30540.00
[L]<font size='tall' color='bg-black'><b> TOTAL            Rs. 152710.00 </b></font>
[C]- - - - - - - - - - - - - - - -
[C]Billing by Functional
[C]Need help? +91 XXXXX XXXXX
[L]
[L]
```

---

### 8. `assets/functional-logo-traced.svg`

```xml
<svg xmlns="http://www.w3.org/2000/svg" viewBox="405 335 505 560">
  <!-- Traced backup of the Functional F mark. Use the original logo file when available. -->
  <path fill="#000000" d="M903,343 L598,455 Q548,475 548,525 L545,600 Q542,622 522,632 L458,658 Q416,676 416,718 L416,887 L510,848 Q543,832 543,797 L544,650 Q546,612 580,602 L818,510 Q850,497 860,478 L885,438 Q900,413 900,385 Z"/>
  <path fill="#000000" d="M563,787 L563,690 Q565,655 595,642 L730,588 L726,690 Q720,728 686,741 Z"/>
</svg>
```

Once the files are saved, do one test print on the KP206 to confirm the footer card and logo come out sharp before rolling it out.