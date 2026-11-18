-- Deu's Tea Stall POS, UI and bill update v2.
-- Run once in the Supabase SQL editor BEFORE installing the updated app.
-- Additive only: nothing is dropped or recreated, existing rows are kept.
--
-- Adapted to the existing schema:
--   * The spec's bills.payment_mode already exists as bills.payment_method ('CASH' / 'UPI').
--   * The spec's bill_lines.name_snapshot / price_snapshot already exist as
--     bill_items.item_name / bill_items.unit_price.

-- bills: payment details
alter table bills add column if not exists cash_received int;
alter table bills add column if not exists change_returned int;
alter table bills alter column payment_method set default 'CASH';
do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'bills_payment_method_check') then
    -- NOT VALID: enforced for new rows without failing on any odd legacy row
    alter table bills add constraint bills_payment_method_check
      check (payment_method in ('CASH', 'UPI')) not valid;
  end if;
end $$;

-- bill_items: category snapshot so summaries survive menu edits and deletes
alter table bill_items add column if not exists category text;

-- day_closes: one row per trading day (the app upserts on date)
create table if not exists day_closes (
  date date primary key,
  opening_float int not null default 0,
  closed_at timestamptz
);

-- If Row Level Security is enabled on your tables, day_closes needs insert AND update
-- policies (the app upserts). Mirror whatever the bills table uses, for example:
-- alter table day_closes enable row level security;
-- create policy "pos insert day_closes" on day_closes for insert to anon with check (true);
-- create policy "pos update day_closes" on day_closes for update to anon using (true) with check (true);
