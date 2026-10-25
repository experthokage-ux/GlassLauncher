package com.fares.glass.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.TimeZone

/** The verse changes every this many hours. */
private const val HOURS_PER_VERSE = 4

/** King James Version (public domain). Add or remove verses here: text to reference. */
private val verses = listOf(
    "The LORD is my shepherd; I shall not want." to "Psalm 23:1",
    "God is our refuge and strength, a very present help in trouble." to "Psalm 46:1",
    "Trust in the LORD with all thine heart; and lean not unto thine own understanding." to "Proverbs 3:5",
    "In all thy ways acknowledge him, and he shall direct thy paths." to "Proverbs 3:6",
    "I can do all things through Christ which strengtheneth me." to "Philippians 4:13",
    "Be careful for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known unto God." to "Philippians 4:6",
    "Fear thou not; for I am with thee: be not dismayed; for I am thy God: I will strengthen thee; yea, I will help thee." to "Isaiah 41:10",
    "Be strong and of a good courage; be not afraid, neither be thou dismayed: for the LORD thy God is with thee whithersoever thou goest." to "Joshua 1:9",
    "For I know the thoughts that I think toward you, saith the LORD, thoughts of peace, and not of evil, to give you an expected end." to "Jeremiah 29:11",
    "And we know that all things work together for good to them that love God, to them who are the called according to his purpose." to "Romans 8:28",
    "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life." to "John 3:16",
    "Thy word is a lamp unto my feet, and a light unto my path." to "Psalm 119:105",
    "Come unto me, all ye that labour and are heavy laden, and I will give you rest." to "Matthew 11:28",
    "I will lift up mine eyes unto the hills, from whence cometh my help. My help cometh from the LORD, which made heaven and earth." to "Psalm 121:1-2",
    "They that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary." to "Isaiah 40:31",
    "The LORD is my light and my salvation; whom shall I fear? the LORD is the strength of my life; of whom shall I be afraid?" to "Psalm 27:1",
    "Rejoicing in hope; patient in tribulation; continuing instant in prayer." to "Romans 12:12",
    "And now abideth faith, hope, charity, these three; but the greatest of these is charity." to "1 Corinthians 13:13",
    "Delight thyself also in the LORD; and he shall give thee the desires of thine heart." to "Psalm 37:4",
    "But seek ye first the kingdom of God, and his righteousness; and all these things shall be added unto you." to "Matthew 6:33",
    "This is the day which the LORD hath made; we will rejoice and be glad in it." to "Psalm 118:24",
    "Now faith is the substance of things hoped for, the evidence of things not seen." to "Hebrews 11:1",
    "For God hath not given us the spirit of fear; but of power, and of love, and of a sound mind." to "2 Timothy 1:7",
    "He that dwelleth in the secret place of the most High shall abide under the shadow of the Almighty." to "Psalm 91:1",
    "Peace I leave with you, my peace I give unto you: not as the world giveth, give I unto you. Let not your heart be troubled, neither let it be afraid." to "John 14:27",
    "Commit thy works unto the LORD, and thy thoughts shall be established." to "Proverbs 16:3",
    "They are new every morning: great is thy faithfulness." to "Lamentations 3:23",
    "O taste and see that the LORD is good: blessed is the man that trusteth in him." to "Psalm 34:8",
    "And let us not be weary in well doing: for in due season we shall reap, if we faint not." to "Galatians 6:9",
    "He hath shewed thee, O man, what is good; and what doth the LORD require of thee, but to do justly, and to love mercy, and to walk humbly with thy God?" to "Micah 6:8",
)

/** Which verse to show right now. Changes every HOURS_PER_VERSE hours, in local time. */
private fun currentIndex(): Int {
    val now = System.currentTimeMillis()
    val local = now + TimeZone.getDefault().getOffset(now)
    val slot = local / (HOURS_PER_VERSE * 60L * 60_000L)
    return (slot % verses.size).toInt()
}

/** A verse in a classic serif font, inside a glass box like the top buttons. */
@Composable
fun VerseText(backdrop: Backdrop, modifier: Modifier = Modifier) {
    var index by remember { mutableStateOf(currentIndex()) }

    // Check once a minute whether it is time for the next verse.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            index = currentIndex()
        }
    }

    val (text, reference) = verses[index]
    val shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 6f)

    Box(modifier.widthIn(max = 420.dp)) {
        // The glass layer sits behind the text and matches its size.
        Glass(
            backdrop,
            Modifier.matchParentSize(),
            shape = RoundedCornerShape(20.dp),
            tint = Color.White.copy(alpha = 0.10f),
        )
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            BasicText(
                "\u201C$text\u201D",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 20.sp,
                    shadow = shadow,
                ),
            )
            BasicText(
                "\u2014 $reference",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    shadow = shadow,
                ),
            )
        }
    }
}
