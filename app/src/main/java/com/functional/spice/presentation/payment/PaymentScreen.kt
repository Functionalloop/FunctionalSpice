package com.functional.spice.presentation.payment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.functional.spice.core.theme.Danger
import com.functional.spice.core.theme.DeepEspresso
import com.functional.spice.core.theme.MonoStyles
import com.functional.spice.core.theme.MutedStone
import com.functional.spice.core.theme.OnEspressoSoft
import com.functional.spice.core.theme.SoftCream
import com.functional.spice.core.theme.SoftCreamBorder
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.core.theme.StoneBody
import com.functional.spice.core.theme.SuccessBg
import com.functional.spice.core.theme.SuccessBorder
import com.functional.spice.core.theme.SuccessFg
import com.functional.spice.core.theme.WarmAlabaster
import com.functional.spice.core.theme.WarmCardamom
import com.functional.spice.core.theme.WarningBg
import com.functional.spice.core.theme.WarningBorder
import com.functional.spice.core.theme.WarningFg
import com.functional.spice.domain.model.Money
import com.functional.spice.domain.model.PaymentMethod
import com.functional.spice.domain.model.ShopConfig
import com.functional.spice.domain.payment.UpiLink
import com.functional.spice.presentation.components.CashKeys
import com.functional.spice.presentation.components.Keypad
import com.functional.spice.presentation.components.NoteChip
import com.functional.spice.presentation.components.QrImage
import com.functional.spice.presentation.components.Radius
import com.functional.spice.presentation.components.SegmentOption
import com.functional.spice.presentation.components.SpiceCard
import com.functional.spice.presentation.components.SquareIconButton
import com.functional.spice.presentation.components.SegmentedControl
import com.functional.spice.presentation.components.SpiceChip

/** Payment (design page 3): cash with change, or UPI QR. Opens from "Save and print". */
@Composable
fun PaymentScreen(
    state: PaymentState,
    shop: ShopConfig,
    onBack: () -> Unit,
    onMethod: (PaymentMethod) -> Unit,
    onKey: (String) -> Unit,
    onQuickAmount: (Int) -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val itemCount = state.draft.totalItemCount

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmAlabaster)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        // Design header: square back button, centred title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            SquareIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back to counter",
                onClick = onBack,
                enabled = !state.isSaving,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Payment", style = SpiceTypography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bill ", style = SpiceTypography.bodySmall)
                    Text("#${state.draft.billNumber}", style = MonoStyles.Caption.copy(fontSize = 12.sp))
                    Text(", $itemCount ${if (itemCount == 1) "item" else "items"}", style = SpiceTypography.bodySmall)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // "Your balance"-style card
            SpiceCard(modifier = Modifier.padding(top = 4.dp, bottom = 12.dp), contentPadding = PaddingValues(18.dp)) {
                Text("Bill total", style = SpiceTypography.bodyMedium, color = MutedStone)
                Text(Money.inr(state.total), style = MonoStyles.Hero.copy(fontSize = 36.sp, lineHeight = 42.sp))
            }
            SegmentedControl(
                options = listOf(
                    SegmentOption(PaymentMethod.CASH, "Cash", Icons.Outlined.Payments),
                    SegmentOption(PaymentMethod.UPI, "UPI", Icons.Outlined.QrCode2)
                ),
                selected = state.method,
                onSelect = onMethod,
                modifier = Modifier.fillMaxWidth()
            )
            when (state.method) {
                PaymentMethod.CASH -> CashSection(state, onKey, onQuickAmount, onClear)
                PaymentMethod.UPI -> UpiSection(state, shop)
            }
            Spacer(Modifier.height(12.dp))
        }

        // Bottom action
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)) {
            if (!state.willPrint && state.error == null) {
                Text(
                    text = "Under ${Money.inr(state.minPrintAmount)}: bill is saved, not printed. Reprint from Bills if needed.",
                    style = SpiceTypography.bodySmall,
                    color = StoneBody,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            state.error?.let {
                Text(
                    text = it,
                    style = SpiceTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Danger,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.Button))
                    .background(WarmCardamom)
                    .clickable(enabled = !state.isSaving, onClick = onConfirm)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Saving bill…", style = SpiceTypography.labelLarge.copy(fontSize = 14.sp), color = Color.White)
                } else {
                    Icon(
                        if (state.willPrint) Icons.Outlined.Print else Icons.Outlined.Check,
                        contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.confirmLabel,
                        style = SpiceTypography.labelLarge.copy(fontSize = 14.sp),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CashSection(
    state: PaymentState,
    onKey: (String) -> Unit,
    onQuickAmount: (Int) -> Unit,
    onClear: () -> Unit
) {
    Text("Cash received", style = SpiceTypography.bodySmall, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
    val inputShape = RoundedCornerShape(Radius.Card)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(inputShape)
            .background(SoftCream)
            .border(BorderStroke(1.5.dp, if (state.error != null) Danger else WarmCardamom), inputShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            buildAnnotatedString {
                append("₹" + (state.received?.let { Money.group(it) } ?: ""))
                withStyle(SpanStyle(color = WarmCardamom, fontWeight = FontWeight.Normal)) { append("|") }
            },
            style = MonoStyles.Hero,
            modifier = Modifier.weight(1f)
        )
        if (state.cashInput.isNotEmpty()) {
            Text(
                "Clear",
                style = SpiceTypography.bodySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClear)
                    .padding(8.dp)
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        state.suggestions.forEachIndexed { i, amount ->
            SpiceChip(
                text = if (i == 0) "Exact" else Money.inr(amount),
                selected = state.received == amount,
                onClick = { onQuickAmount(amount) },
                mono = true,
                center = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    CashResultCard(state.cashResult)
    Spacer(Modifier.height(12.dp))
    Keypad(keys = CashKeys, onKey = onKey)
}

@Composable
private fun CashResultCard(result: CashResult) {
    val shape = RoundedCornerShape(Radius.Card)
    when (result) {
        CashResult.Empty -> ResultBox(SoftCream, SoftCreamBorder) {
            Text("Enter the cash the customer gave you", style = SpiceTypography.bodyMedium, color = MutedStone)
        }
        is CashResult.StillToCollect -> ResultBox(WarningBg, WarningBorder) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Still to collect", style = SpiceTypography.labelLarge, color = WarningFg, modifier = Modifier.weight(1f))
                Text(Money.inr(result.amount), style = MonoStyles.PriceLarge, color = WarningFg)
            }
        }
        CashResult.Exact -> ResultBox(SuccessBg, SuccessBorder) {
            Text("Exact amount. No change to return.", style = SpiceTypography.labelLarge, color = SuccessFg)
        }
        is CashResult.ReturnChange -> Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(DeepEspresso)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Return to customer", style = SpiceTypography.labelLarge, color = OnEspressoSoft, modifier = Modifier.weight(1f))
                Text(Money.inr(result.amount), style = MonoStyles.Hero, color = Color.White)
            }
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                result.notes.forEach { (note, count) ->
                    NoteChip(if (count > 1) "$count × ${Money.inr(note)}" else Money.inr(note))
                }
            }
        }
    }
}

@Composable
private fun ResultBox(bg: Color, border: Color, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(Radius.Card)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) { content() }
}

@Composable
private fun UpiSection(state: PaymentState, shop: ShopConfig) {
    val link = UpiLink.build(shop.upiId, shop.shopName, state.total, state.draft.billNumber)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Customer scans to pay exactly", style = SpiceTypography.bodySmall)
        Text(Money.inr(state.total), style = MonoStyles.PriceLarge, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.Card))
                .background(Color.White)
                .border(1.dp, SoftCreamBorder, RoundedCornerShape(Radius.Card))
                .padding(10.dp)
        ) {
            QrImage(text = link, size = 200.dp, contentDescription = "UPI QR code for ${Money.inr(state.total)}")
        }
        Text(shop.upiId, style = MonoStyles.Caption.copy(fontSize = 12.sp, color = StoneBody), modifier = Modifier.padding(top = 8.dp))
        Text("No change needed for UPI", style = SpiceTypography.labelSmall, modifier = Modifier.padding(top = 2.dp))
        Text(
            "Tap \"Mark paid and print\" after your phone or soundbox shows the payment arrived.",
            style = SpiceTypography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp, start = 12.dp, end = 12.dp)
        )
    }
}
