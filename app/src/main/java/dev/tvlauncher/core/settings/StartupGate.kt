package dev.tvlauncher.core.settings

/**
 * Whether the launcher may open itself when the TV finishes starting up. Installing the app must
 * never take over Home, so start-up also needs one of the user's explicit opt-ins: the Home button
 * service switched on in Accessibility, or "Use as home screen" (the HOME alias) in Setup.
 */
object StartupGate {
    fun shouldOpenAtBoot(openOnStartup: Boolean, serviceEnabled: Boolean, homeAliasEnabled: Boolean): Boolean =
        openOnStartup && (serviceEnabled || homeAliasEnabled)
}
