package dev.jacobandersen.sigil.service

import dev.jacobandersen.sigil.config.IndieAuthConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProfileClaimServiceTest {
    private fun service(
        name: String = "Example User",
        url: String = "https://sigil.test/",
        photo: String = "https://sigil.test/photo.jpg",
        email: String = "user@sigil.test",
    ) = ProfileClaimService(
        IndieAuthConfig(
            me = "https://sigil.test",
            profile = IndieAuthConfig.IndieAuthProfile(name = name, url = url, photo = photo, email = email),
        ),
    )

    @Test
    fun `no profile scope means no profile`() {
        assertNull(service().forScopes(listOf("create")))
    }

    @Test
    fun `profile scope returns claims without email`() {
        val profile = service().forScopes(listOf("profile", "create"))!!

        assertEquals("Example User", profile.name)
        assertEquals("https://sigil.test/", profile.url)
        assertEquals("https://sigil.test/photo.jpg", profile.photo)
        assertNull(profile.email)
    }

    @Test
    fun `email scope with profile returns the address`() {
        val profile = service().forScopes(listOf("profile", "email"))!!

        assertEquals("user@sigil.test", profile.email)
    }

    @Test
    fun `empty config means no profile`() {
        val empty =
            ProfileClaimService(
                IndieAuthConfig(me = "https://sigil.test", profile = IndieAuthConfig.IndieAuthProfile()),
            )

        assertNull(empty.forScopes(listOf("profile")))
    }
}
