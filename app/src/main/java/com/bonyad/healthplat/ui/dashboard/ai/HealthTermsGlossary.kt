package com.bonyad.healthplat.ui.dashboard.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonyad.healthplat.R

/**
 * Health terms glossary — maps Persian medical/health terms to simple definitions.
 * Terms are sorted by length descending so longer phrases match first
 * (e.g. "خواب با حرکات سریع چشم" before "خواب").
 */
object HealthTermsGlossary {

    private val terms: Map<String, String> = mapOf(
        "ضربان قلب در حالت استراحت" to
                "تعداد دفعاتی که قلب شما در هر دقیقه، زمانی که آرام هستید و فعالیت بدنی خاصی ندارید (مثل نشستن یا دراز کشیدن روی مبل)، می\u200Cتپد.",
        "خواب با حرکات سریع چشم" to
                "یا خواب REM، مرحله\u200Cای از خواب که در آن مغز بسیار فعال است و چشم\u200Cها زیر پلک به\u200Cسرعت حرکت می\u200Cکنند. بیشتر خواب\u200Cدیدن\u200Cها (رؤیاها) در این مرحله رخ می\u200Cدهد و برای تقویت حافظه، یادگیری و تنظیم احساسات روزمره بسیار مهم است.",
        "شاخص توده بدنی" to
                "یا BMI، معیاری استاندارد که از تناسب وزن و قد شما به دست می\u200Cآید. این شاخص به\u200Cسادگی نشان می\u200Cدهد که آیا وزن شما در محدوده سالم قرار دارد، یا دچار کمبود وزن و اضافه\u200Cوزن هستید.",
        "بیشینه ضربان قلب" to
                "بالاترین تعداد تپش قلب شما در یک دقیقه که معمولاً در حین ورزش، هیجان، استرس یا فعالیت\u200Cهای تند روزانه ثبت می\u200Cشود.",
        "فعالیت هوازی" to
                "هر نوع فعالیت بدنی و ورزشی (مثل پیاده\u200Cروی تند، دویدن سبک، شنا یا دوچرخه\u200Cسواری) که باعث افزایش ضربان قلب و تندتر شدن تنفس شما می\u200Cشود و برای تقویت قلب بسیار مفید است.",
        "اکسیژن خون" to
                "میزان اکسیژنی که در خون شما جریان دارد. سطح طبیعی آن نشان می\u200Cدهد ریه\u200Cها و قلب شما به\u200Cخوبی کار می\u200Cکنند تا اکسیژن لازم را به تمام سلول\u200Cهای بدن برسانند.",
        "نمره آمادگی" to
                "یا Readiness Score، نمره\u200Cای کلی که نشان می\u200Cدهد بدن شما در این روز چقدر برای انجام فعالیت\u200Cها و چالش\u200Cهای روزمره انرژی و آمادگی دارد.",
        "غلات کامل" to
                "دانه\u200Cها و محصولاتی (مانند نان\u200Cهای سبوس\u200Cدار، جو دوسر یا برنج قهوه\u200Cای) که لایه\u200Cهای مغذی آن\u200Cها (سبوس) در کارخانه جدا نشده است. این مواد فیبر بالایی دارند و برای هضم و سلامت قلب عالی هستند.",
        "خواب عمیق" to
                "یا Deep Sleep، باکیفیت\u200Cترین و نیروبخش\u200Cترین مرحله خواب است. در این زمان، ضربان قلب و تنفس به کندترین حالت خود می\u200Cرسند و بیدار کردن شما سخت\u200Cتر است. این خواب برای ترمیم بافت\u200Cهای آسیب\u200Cدیده، بازیابی انرژی فیزیکی و تقویت سیستم ایمنی حیاتی است.",
        "خواب سبک" to
                "یا Light Sleep، مرحله\u200Cای که به عنوان پلی بین بیداری و خواب عمیق عمل می\u200Cکند. در این مرحله، بدن شروع به آرام شدن می\u200Cکند اما هنوز ممکن است با صداهای محیط به\u200Cراحتی بیدار شوید. به طور طبیعی، بیشترین زمان خواب شبانه شما در این مرحله سپری می\u200Cشود."
    )

    /** Terms sorted by key length descending — longest match first */
    val sortedTerms: List<Pair<String, String>> = terms.entries
        .sortedByDescending { it.key.length }
        .map { it.key to it.value }
}

// ── Colors (reused from AiAnalysisScreen) ───────────────────────────────────

private val CardBackground = Color(0xFF131B2E)
private val AccentCyan = Color(0xFF4ECDC4)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF8A9199)
private val TextLightGray = Color(0xFFB0B8C1)

// ── ClickableHealthTermText ─────────────────────────────────────────────────

/**
 * Renders [text] with glossary health terms underlined and colored as clickable links.
 * When a term is tapped, [onTermClick] fires with (term, definition).
 */
@Composable
fun ClickableHealthTermText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = TextWhite,
    textAlign: TextAlign? = null,
    onTermClick: (term: String, definition: String) -> Unit
) {
    val annotated = remember(text, onTermClick) {
        buildGlossaryAnnotatedString(text, color, onTermClick)
    }

    Text(
        text = annotated,
        modifier = modifier,
        style = style.copy(textAlign = textAlign ?: style.textAlign)
    )
}

/**
 * Builds an [AnnotatedString] where every occurrence of a glossary term is styled
 * with underline + accent color and uses [LinkAnnotation.Clickable] for tap handling.
 */
private fun buildGlossaryAnnotatedString(
    text: String,
    baseColor: Color,
    onTermClick: (term: String, definition: String) -> Unit
): AnnotatedString {
    // Find all term matches: (startIndex, endIndex, termKey)
    data class Match(val start: Int, val end: Int, val term: String)

    val matches = mutableListOf<Match>()
    val claimed = BooleanArray(text.length) // prevent overlapping matches

    for ((term, _) in HealthTermsGlossary.sortedTerms) {
        var searchFrom = 0
        while (true) {
            val idx = text.indexOf(term, searchFrom)
            if (idx == -1) break
            val end = idx + term.length
            // Only add if no character in this range is already claimed
            val overlaps = (idx until end).any { claimed[it] }
            if (!overlaps) {
                matches.add(Match(idx, end, term))
                for (i in idx until end) claimed[i] = true
            }
            searchFrom = idx + 1
        }
    }

    // Sort by start position so we can build the string left-to-right
    matches.sortBy { it.start }

    return buildAnnotatedString {
        var cursor = 0
        for (match in matches) {
            // Append plain text before this match
            if (cursor < match.start) {
                pushStyle(SpanStyle(color = baseColor))
                append(text.substring(cursor, match.start))
                pop()
            }
            // Append the matched term with LinkAnnotation for click handling
            val definition = HealthTermsGlossary.sortedTerms
                .first { it.first == match.term }.second
            val termKey = match.term
            withLink(
                LinkAnnotation.Clickable(
                    tag = termKey,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = AccentCyan,
                            textDecoration = TextDecoration.Underline
                        )
                    ),
                    linkInteractionListener = {
                        onTermClick(termKey, definition)
                    }
                )
            ) {
                append(text.substring(match.start, match.end))
            }
            cursor = match.end
        }
        // Append remaining text
        if (cursor < text.length) {
            pushStyle(SpanStyle(color = baseColor))
            append(text.substring(cursor))
            pop()
        }
    }
}

// ── TermDefinitionBottomSheet ───────────────────────────────────────────────

/**
 * Dark-themed bottom sheet that shows a health term and its simple definition.
 *
 * @param term  The term title (e.g. "اکسیژن خون")
 * @param definition  The plain-language definition
 * @param onDismiss  Called when the sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermDefinitionBottomSheet(
    term: String,
    definition: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header: term title + close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Term title
                    Text(
                        text = term,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = AccentCyan,
                        modifier = Modifier.weight(1f)
                    )

                    // Close button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close_square),
                            contentDescription = "بستن",
                            tint = TextGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Label
                Text(
                    text = "تعریف ساده و کاربردی برای شما",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Definition
                Text(
                    text = definition,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 26.sp
                    ),
                    color = TextLightGray,
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
