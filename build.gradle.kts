import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  base
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.ktlint)
  alias(libs.plugins.spotless)
  alias(libs.plugins.detekt) apply false
}

ktlint {
  filter {
    exclude { it.file.path.contains("/build/") }
  }
}

spotless {
  format("xml") {
    target("**/src/**/*.xml")
    targetExclude("**/build/**")
    eclipseWtp(EclipseWtpFormatterStep.XML).configFile("config/spotless/xml.prefs")
    trimTrailingWhitespace()
    endWithNewline()
  }
}
