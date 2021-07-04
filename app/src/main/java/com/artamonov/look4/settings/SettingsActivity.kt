package com.artamonov.look4.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintSet
import com.artamonov.look4.BuildConfig
import com.artamonov.look4.R
import com.artamonov.look4.utils.WebViewType.DEFAULT_FAQ
import com.artamonov.look4.utils.WebViewType.PRIVACY_POLICY
import com.artamonov.look4.utils.startAboutUsActivity
import com.artamonov.look4.utils.startUserProfileEditActivity
import com.artamonov.look4.utils.startWebViewActivity
import java.util.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsItems()
        }
    }

    @Preview(showBackground = true)
    @Composable
    private fun SettingsItems() {
        MaterialTheme {
            Toolbar()
            LazyColumn(
                modifier = Modifier
                    .padding(15.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                item { SettingsButton(itemName = SettingsItem.Profile) }
                item { SettingsButton(itemName = SettingsItem.Faq) }
                item { SettingsButton(itemName = SettingsItem.AboutUs) }
                item { SettingsButton(itemName = SettingsItem.PrivacyPolicy) }
            }

            DebugView()
        }
    }

    @Composable
    private fun Toolbar() {
        val style = TextStyle(
            fontFamily = robotoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        ConstraintSet {
            val text1 = createRefFor("text1")
            val text2 = createRefFor("text2")
            val text3 = createRefFor("text3")

            constrain(text1) {
                start.linkTo(text2.end)
            }
            constrain(text2) {
                top.linkTo(text1.bottom)
            }

            constrain(text3) {
                start.linkTo(text2.end)
                top.linkTo(text2.bottom)
            }

        }
//        ConstraintLayout {
//            Text("Text1", Modifier.layoutId("text1"))
//            Text("Text2", Modifier.layoutId("text2"))
//            Text("This is a very long text", Modifier.layoutId("text3"))
//        }

//        WithConstraints {
//            val constraints = if (minWidth < 600.dp) {
//                decoupledConstraints(margin = 16.dp) // Portrait constraints
//            } else {
//                decoupledConstraints(margin = 32.dp) // Landscape constraints
//            }
//
//            ConstraintLayout(constraints) {
//                Button(
//                    onClick = { /* Do something */ },
//                    modifier = Modifier.layoutId("button")
//                ) {
//                    Text("Button")
//                }
//
//                Text("Text", Modifier.layoutId("text"))
//            }
//            }


//        Row(
//            modifier = Modifier
//                .wrapContentWidth()
//                .padding(20.dp),
//            horizontalArrangement = Arrangement.End
//        ) {
//            Text(
//                text = stringResource(id = R.string.settings_title),
//                style = style, modifier = Modifier.align(Alignment.CenterVertically)
//            )
//        }
//        Row(
//            modifier = Modifier
//                .padding(20.dp)
//                .background(color = MaterialTheme.colors.background)
//        ) {
//            Image(painter = painterResource(R.drawable.ic_arrow_back_black_24dp),
//                contentDescription = "Back button",
//                modifier = Modifier
//                    .size(30.dp)
//                    .clickable { onBackPressed() })
//        }


    }

    @Composable
    private fun DebugView() {
        val style = TextStyle(
            fontFamily = robotoFamily,
            fontWeight = FontWeight.Light,
            fontSize = 15.sp
        )
        Column(
            modifier = Modifier
                .padding(15.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                AnnotatedString(
                    if (BuildConfig.DEBUG) stringResource
                        (id = R.string.settings_debug_version, BuildConfig.VERSION_NAME) else
                        stringResource(id = R.string.settings_version, BuildConfig.VERSION_NAME)
                ),
                style = style,
                modifier = Modifier.padding(15.dp)
            )
        }
    }

    @Composable
    private fun SettingsButton(itemName: SettingsItem) {
        val style = TextStyle(
            fontFamily = robotoFamily,
            fontWeight = FontWeight.Light,
            fontSize = 20.sp
        )
        ClickableText(
            AnnotatedString(
                stringResource(id = itemName.id)
                    .toUpperCase(Locale.getDefault())
            ),
            style = style,
            onClick = {
                clickHandler(itemName)
            },
            modifier = Modifier.padding(15.dp)
        )
    }

    private fun clickHandler(itemName: SettingsItem) {
        when (itemName) {
            is SettingsItem.Profile -> startUserProfileEditActivity()
            is SettingsItem.Faq -> startWebViewActivity(DEFAULT_FAQ)
            is SettingsItem.AboutUs -> startAboutUsActivity()
            is SettingsItem.PrivacyPolicy -> startWebViewActivity(PRIVACY_POLICY)
        }
    }

    private val robotoFamily = FontFamily(
        Font(R.font.roboto_light, FontWeight.Light)
    )

}

