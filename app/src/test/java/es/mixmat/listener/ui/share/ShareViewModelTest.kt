package es.mixmat.listener.ui.share

import android.util.Log
import es.mixmat.listener.data.api.RateLimitException
import es.mixmat.listener.data.repository.AuthRepository
import es.mixmat.listener.data.repository.GroupRepository
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.data.repository.RecognitionRepository
import es.mixmat.listener.domain.model.Group
import es.mixmat.listener.domain.model.Platforms
import es.mixmat.listener.domain.model.RecognitionResult
import es.mixmat.listener.domain.model.Track
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ShareViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var recognitionRepository: RecognitionRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var groupRepository: GroupRepository

    private val testTrack = Track(
        title = "Midnight City",
        artist = "M83",
        thumbnail = "https://example.com/thumb.jpg",
        shortcode = "aBcDeF12",
        shareUrl = "https://mixmat.es/aBcDeF12",
        platforms = Platforms(
            spotify = "https://open.spotify.com/track/123",
            tidal = "https://tidal.com/track/456",
            appleMusic = "https://music.apple.com/track/789",
        ),
    )

    private val testResult = RecognitionResult(
        status = "saved",
        source = "link",
        historyId = "hist123",
        track = testTrack,
    )

    private val testGroups = listOf(
        Group(id = "g1", name = "Listen", description = null),
        Group(id = "g2", name = "Friday Jams", description = null),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        recognitionRepository = mockk()
        authRepository = mockk()
        historyRepository = mockk()
        groupRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ShareViewModel(
        recognitionRepository, authRepository, historyRepository, groupRepository,
    )

    @Test
    fun `resolve with valid URL sets result and loads groups`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } returns testResult
        coEvery { groupRepository.getGroups() } returns testGroups

        val viewModel = createViewModel()
        viewModel.resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isResolving)
        assertNotNull(state.result)
        assertEquals("saved", state.result!!.status)
        assertEquals("Midnight City", state.result!!.track!!.title)
        assertEquals(2, state.groups.size)
        assertNull(state.error)
    }

    @Test
    fun `resolve with duplicate status shows result`() = runTest {
        val duplicateResult = testResult.copy(status = "duplicate")
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } returns duplicateResult
        coEvery { groupRepository.getGroups() } returns testGroups

        val viewModel = createViewModel()
        viewModel.resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("duplicate", viewModel.uiState.value.result!!.status)
    }

    @Test
    fun `resolve with no auth token sets error`() = runTest {
        every { authRepository.hasToken() } returns false

        val viewModel = createViewModel()
        viewModel.resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isResolving)
        assertNull(state.result)
        assertEquals("Sign in to MixMates Listener first.", state.error)
    }

    @Test
    fun `resolve with non-music text sets error`() = runTest {
        every { authRepository.hasToken() } returns true

        val viewModel = createViewModel()
        viewModel.resolve("just some random text")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isResolving)
        assertTrue(state.error!!.contains("No supported music link"))
    }

    @Test
    fun `resolve extracts URL from surrounding text`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } returns testResult
        coEvery { groupRepository.getGroups() } returns emptyList()

        val viewModel = createViewModel()
        viewModel.resolve("Check this out! https://open.spotify.com/track/123 so good")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { recognitionRepository.resolve("https://open.spotify.com/track/123", null) }
        assertNotNull(viewModel.uiState.value.result)
    }

    @Test
    fun `resolve with rate limit sets error`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } throws RateLimitException(60, null)

        val viewModel = createViewModel()
        viewModel.resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isResolving)
        assertTrue(state.error!!.contains("Rate limit"))
        assertTrue(state.error!!.contains("60"))
    }

    @Test
    fun `resolve with 400 sets unsupported URL error`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } throws HttpException(
            Response.error<Any>(400, "".toResponseBody(null)),
        )

        val viewModel = createViewModel()
        viewModel.resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error!!.contains("isn't supported"))
    }

    @Test
    fun `resolve with network error sets generic error`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } throws IOException("No network")

        val viewModel = createViewModel()
        viewModel.resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.error!!.contains("Something went wrong"))
    }

    @Test
    fun `toggleGroup adds and removes group`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } returns testResult
        coEvery { groupRepository.getGroups() } returns testGroups

        val viewModel = createViewModel()
        viewModel.resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleGroup("g2")
        assertTrue("g2" in viewModel.uiState.value.selectedGroupIds)

        viewModel.toggleGroup("g2")
        assertFalse("g2" in viewModel.uiState.value.selectedGroupIds)
    }

    @Test
    fun `share calls repository and sets result`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } returns testResult
        coEvery { groupRepository.getGroups() } returns testGroups
        coEvery { historyRepository.share(any(), any()) } returns mapOf("g2" to "shared")

        val viewModel = createViewModel()
        viewModel.resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleGroup("g2")
        viewModel.share()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSharing)
        assertEquals("shared", state.shareResult!!["g2"])
        coVerify { historyRepository.share("hist123", listOf("g2")) }
    }

    @Test
    fun `share failure sets error`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } returns testResult
        coEvery { groupRepository.getGroups() } returns testGroups
        coEvery { historyRepository.share(any(), any()) } throws RuntimeException("Failed")

        val viewModel = createViewModel()
        viewModel.resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleGroup("g2")
        viewModel.share()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Couldn't share — try again", viewModel.uiState.value.error)
    }
}
