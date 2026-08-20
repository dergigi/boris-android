package org.dergigi.boris.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeOnboardingStoreTest {
    @Test
    fun loggedOutFirstLaunchShowsThePrompt() {
        assertTrue(
            HomeOnboardingStore.shouldShowFirstTime(
                localDismissed = false,
                settingsDismissed = false,
                loggedIn = false,
                settingsReady = true,
                hasRemoteSettings = false,
            ),
        )
    }

    @Test
    fun localOrSettingsDismissHidesThePrompt() {
        assertFalse(
            HomeOnboardingStore.shouldShowFirstTime(
                localDismissed = true,
                settingsDismissed = false,
                loggedIn = false,
                settingsReady = true,
                hasRemoteSettings = false,
            ),
        )
        assertFalse(
            HomeOnboardingStore.shouldShowFirstTime(
                localDismissed = false,
                settingsDismissed = true,
                loggedIn = true,
                settingsReady = true,
                hasRemoteSettings = false,
            ),
        )
    }

    @Test
    fun loggedInWaitsForSettingsThenHidesReturningUsers() {
        assertFalse(
            HomeOnboardingStore.shouldShowFirstTime(
                localDismissed = false,
                settingsDismissed = false,
                loggedIn = true,
                settingsReady = false,
                hasRemoteSettings = false,
            ),
        )
        assertFalse(
            HomeOnboardingStore.shouldShowFirstTime(
                localDismissed = false,
                settingsDismissed = false,
                loggedIn = true,
                settingsReady = true,
                hasRemoteSettings = true,
            ),
        )
        assertTrue(
            HomeOnboardingStore.shouldShowFirstTime(
                localDismissed = false,
                settingsDismissed = false,
                loggedIn = true,
                settingsReady = true,
                hasRemoteSettings = false,
            ),
        )
    }
}
