package es.mixmat.listener.ui.share

import android.util.Log
import es.mixmat.listener.R
import es.mixmat.listener.data.api.RateLimitException
import es.mixmat.listener.data.repository.AuthRepository
import es.mixmat.listener.data.repository.RecognitionRepository
import es.mixmat.listener.data.session.UnseenTracks
import es.mixmat.listener.domain.model.Platforms
import es.mixmat.listener.domain.model.RecognitionResult
import es.mixmat.listener.domain.model.Track
import es.mixmat.listener.ui.text.UiText
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
    private lateinit var unseenTracks: UnseenTracks

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
        bpm = null,
        musicalKey = null,
        keyScale = null,
    )

    private val testResult = RecognitionResult(
        status = "saved",
        source = "link",
        historyId = "hist123",
        track = testTrack,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        recognitionRepository = mockk()
        authRepository = mockk()
        unseenTracks = UnseenTracks()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        ShareViewModel(recognitionRepository, authRepository, unseenTracks)

    @Test
    fun `resolve with valid URL sets result`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } returns testResult

        val viewModel = createViewModel()
        viewModel.resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isResolving)
        assertNotNull(state.result)
        assertEquals("saved", state.result!!.status)
        assertEquals("Midnight City", state.result!!.track!!.title)
        assertNull(state.error)
    }

    @Test
    fun `resolve with duplicate status shows result`() = runTest {
        val duplicateResult = testResult.copy(status = "duplicate")
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } returns duplicateResult

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
        assertEquals(UiText(R.string.share_sign_in_first), state.error)
    }

    @Test
    fun `resolve with non-music text sets error`() = runTest {
        every { authRepository.hasToken() } returns true

        val viewModel = createViewModel()
        viewModel.resolve("just some random text")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isResolving)
        assertEquals(UiText(R.string.share_no_supported_link), state.error)
    }

    @Test
    fun `resolve extracts URL from surrounding text`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } returns testResult

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
        assertEquals(UiText(R.string.share_rate_limited, 60), state.error)
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

        assertEquals(UiText(R.string.share_link_type_unsupported), viewModel.uiState.value.error)
    }

    @Test
    fun `resolve with network error sets generic error`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } throws IOException("No network")

        val viewModel = createViewModel()
        viewModel.resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiText(R.string.share_something_went_wrong), viewModel.uiState.value.error)
    }

    /**
     * A resolved link lands in history just like a mic recognition, so it earns
     * the History marker too. Missed on the first pass, which made the dot
     * untestable for anyone testing via the share path rather than the mic.
     */
    @Test
    fun `a resolved link marks history unseen`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } returns testResult

        createViewModel().resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(unseenTracks.hasUnseen.value)
    }

    @Test
    fun `a resolve that saves nothing leaves the marker alone`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } returns
            testResult.copy(status = "no_match", historyId = null, track = null)

        createViewModel().resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(unseenTracks.hasUnseen.value)
    }

    @Test
    fun `a failed resolve leaves the marker alone`() = runTest {
        every { authRepository.hasToken() } returns true
        coEvery { recognitionRepository.resolve(any(), any()) } throws IOException("No network")

        createViewModel().resolve("https://open.spotify.com/track/123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(unseenTracks.hasUnseen.value)
    }

    // Group selection and sharing moved to TrackShareViewModelTest when the
    // picker moved into the shared sheet.
}
