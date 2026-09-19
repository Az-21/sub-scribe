import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
import org.gradle.api.attributes.Bundling

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  base
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.spotless)
  alias(libs.plugins.detekt) apply false
}

// KtLint 2.x moved to the `io.github.ktlint.core` coordinates and no Gradle plugin published on the
// plugin portal supports them yet, so use the official custom Gradle integration:
// https://ktlint.github.io/ktlint/latest/install/integrations/
val ktlint = configurations.create("ktlint")

dependencies {
  ktlint(libs.ktlint.cli) {
    attributes {
      attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
  }
}

val ktlintCheck =
  tasks.register<JavaExec>("ktlintCheck") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("io.github.ktlint.core.Main")
    args("**/src/**/*.kt", "**.kts", "!**/build/**")
  }

tasks.check {
  dependsOn(ktlintCheck)
}

tasks.register<JavaExec>("ktlintFormat") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Check Kotlin code style and format"
  classpath = ktlint
  mainClass.set("io.github.ktlint.core.Main")
  args("-F", "**/src/**/*.kt", "**.kts", "!**/build/**")
}

spotless {
  format("xml") {
    target("*/src/**/*.xml")
    targetExclude("**/build/**")
    eclipseWtp(EclipseWtpFormatterStep.XML).configFile("config/spotless/xml.prefs")
    trimTrailingWhitespace()
    endWithNewline()
  }
}
