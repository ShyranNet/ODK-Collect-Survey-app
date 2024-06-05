package org.odk.collect.maps.layers

import androidx.core.net.toUri
import androidx.fragment.app.testing.FragmentScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.odk.collect.androidshared.ui.FragmentFactoryBuilder
import org.odk.collect.fragmentstest.FragmentScenarioLauncherRule
import org.odk.collect.settings.InMemSettingsProvider
import org.odk.collect.shared.TempFiles
import org.odk.collect.strings.R
import org.odk.collect.testshared.EspressoHelpers
import org.odk.collect.testshared.FakeScheduler
import org.odk.collect.testshared.RecyclerViewMatcher
import org.odk.collect.testshared.RobolectricHelpers
import java.io.File

@RunWith(AndroidJUnit4::class)
class OfflineMapLayersImporterTest {
    private val scheduler = FakeScheduler()
    private val referenceLayerRepository = TestReferenceLayerRepository()
    private val settingsProvider = InMemSettingsProvider()

    @get:Rule
    val fragmentScenarioLauncherRule = FragmentScenarioLauncherRule(
        FragmentFactoryBuilder()
            .forClass(OfflineMapLayersImporter::class) {
                OfflineMapLayersImporter(referenceLayerRepository, scheduler, settingsProvider)
            }.build()
    )

    @Test
    fun `clicking the 'cancel' button dismisses the dialog`() {
        launchFragment().onFragment {
            scheduler.flush()
            assertThat(it.isVisible, equalTo(true))
            EspressoHelpers.clickOnText(R.string.cancel)
            assertThat(it.isVisible, equalTo(false))
        }
    }

    @Test
    fun `clicking the 'add layer' button dismisses the dialog`() {
        launchFragment().onFragment {
            scheduler.flush()
            assertThat(it.isVisible, equalTo(true))
            it.viewModel.loadLayersToImport(emptyList())
            onView(withId(org.odk.collect.maps.R.id.add_layer_button)).perform(click())
            scheduler.flush()
            RobolectricHelpers.runLooper()
            assertThat(it.isVisible, equalTo(false))
        }
    }

    @Test
    fun `progress indicator is displayed during loading layers`() {
        val file1 = TempFiles.createTempFile("layer1", MbtilesFile.FILE_EXTENSION)
        val file2 = TempFiles.createTempFile("layer2", MbtilesFile.FILE_EXTENSION)

        launchFragment().onFragment {
            it.viewModel.loadLayersToImport(listOf(file1.toUri(), file2.toUri()))
        }

        onView(withId(org.odk.collect.maps.R.id.progress_indicator)).check(matches(isDisplayed()))
        onView(withId(org.odk.collect.maps.R.id.layers)).check(matches(not(isDisplayed())))

        scheduler.flush()

        onView(withId(org.odk.collect.maps.R.id.progress_indicator)).check(matches(not(isDisplayed())))
        onView(withId(org.odk.collect.maps.R.id.layers)).check(matches(isDisplayed()))
    }

    @Test
    fun `the 'cancel' button is enabled during loading layers`() {
        launchFragment()

        onView(withId(org.odk.collect.maps.R.id.cancel_button)).check(matches(isEnabled()))
        scheduler.flush()
        onView(withId(org.odk.collect.maps.R.id.cancel_button)).check(matches(isEnabled()))
    }

    @Test
    fun `the 'add layer' button is disabled during loading layers`() {
        launchFragment()

        onView(withId(org.odk.collect.maps.R.id.add_layer_button)).check(matches(not(isEnabled())))
        scheduler.flush()
        onView(withId(org.odk.collect.maps.R.id.add_layer_button)).check(matches(isEnabled()))
    }

    @Test
    fun `'All projects' location should be selected by default`() {
        launchFragment()

        onView(withId(org.odk.collect.maps.R.id.all_projects_option)).check(matches(isChecked()))
        onView(withId(org.odk.collect.maps.R.id.current_project_option)).check(matches(not(isChecked())))
    }

    @Test
    fun `checking location sets selection correctly`() {
        launchFragment()
        scheduler.flush()

        onView(withId(org.odk.collect.maps.R.id.current_project_option)).perform(click())

        onView(withId(org.odk.collect.maps.R.id.all_projects_option)).check(matches(not(isChecked())))
        onView(withId(org.odk.collect.maps.R.id.current_project_option)).check(matches(isChecked()))

        onView(withId(org.odk.collect.maps.R.id.all_projects_option)).perform(click())

        onView(withId(org.odk.collect.maps.R.id.all_projects_option)).check(matches(isChecked()))
        onView(withId(org.odk.collect.maps.R.id.current_project_option)).check(matches(not(isChecked())))
    }

    @Test
    fun `recreating maintains the selected layers location`() {
        val scenario = launchFragment()
        scheduler.flush()

        onView(withId(org.odk.collect.maps.R.id.current_project_option)).perform(click())

        scenario.recreate()

        onView(withId(org.odk.collect.maps.R.id.all_projects_option)).check(matches(not(isChecked())))
        onView(withId(org.odk.collect.maps.R.id.current_project_option)).check(matches(isChecked()))
    }

    @Test
    fun `the list of selected layers should be displayed`() {
        val file1 = TempFiles.createTempFile("layer1", MbtilesFile.FILE_EXTENSION)
        val file2 = TempFiles.createTempFile("layer2", MbtilesFile.FILE_EXTENSION)

        launchFragment().onFragment {
            it.viewModel.loadLayersToImport(listOf(file1.toUri(), file2.toUri()))
        }

        scheduler.flush()

        onView(withId(org.odk.collect.maps.R.id.layers)).check(matches(RecyclerViewMatcher.withListSize(2)))
        onView(withText(file1.name)).check(matches(isDisplayed()))
        onView(withText(file2.name)).check(matches(isDisplayed()))
    }

    @Test
    fun `recreating maintains the list of selected layers`() {
        val file1 = TempFiles.createTempFile("layer1", MbtilesFile.FILE_EXTENSION)
        val file2 = TempFiles.createTempFile("layer2", MbtilesFile.FILE_EXTENSION)

        val scenario = launchFragment().onFragment {
            it.viewModel.loadLayersToImport(listOf(file1.toUri(), file2.toUri()))
        }

        scheduler.flush()

        scenario.recreate()

        onView(withId(org.odk.collect.maps.R.id.layers)).check(matches(RecyclerViewMatcher.withListSize(2)))
        onView(withText(file1.name)).check(matches(isDisplayed()))
        onView(withText(file2.name)).check(matches(isDisplayed()))
    }

    @Test
    fun `only mbtiles files are taken into account`() {
        val file1 = TempFiles.createTempFile("layer1", MbtilesFile.FILE_EXTENSION)
        val file2 = TempFiles.createTempFile("layer2", ".txt")

        launchFragment().onFragment {
            it.viewModel.loadLayersToImport(listOf(file1.toUri(), file2.toUri()))
        }

        scheduler.flush()

        onView(withId(org.odk.collect.maps.R.id.layers)).check(matches(RecyclerViewMatcher.withListSize(1)))
        onView(withText(file1.name)).check(matches(isDisplayed()))
        onView(withText(file2.name)).check(doesNotExist())
    }

    @Test
    fun `clicking the 'add layer' button moves the files to the shared layers dir if it is selected`() {
        val file1 = TempFiles.createTempFile("layer1", MbtilesFile.FILE_EXTENSION).also {
            it.writeText("blah1")
        }
        val file2 = TempFiles.createTempFile("layer2", MbtilesFile.FILE_EXTENSION).also {
            it.writeText("blah2")
        }

        launchFragment().onFragment {
            it.viewModel.loadLayersToImport(listOf(file1.toUri(), file2.toUri()))
        }

        scheduler.flush()

        onView(withId(org.odk.collect.maps.R.id.add_layer_button)).perform(click())
        scheduler.flush()

        assertThat(File(referenceLayerRepository.getSharedLayersDirPath()).listFiles().size, equalTo(2))
        assertThat(File(referenceLayerRepository.getProjectLayersDirPath()).listFiles().size, equalTo(0))

        val copiedFile1 = File(referenceLayerRepository.getSharedLayersDirPath(), file1.name)
        assertThat(copiedFile1.exists(), equalTo(true))
        assertThat(copiedFile1.readText(), equalTo("blah1"))

        val copiedFile2 = File(referenceLayerRepository.getSharedLayersDirPath(), file2.name)
        assertThat(copiedFile2.exists(), equalTo(true))
        assertThat(copiedFile2.readText(), equalTo("blah2"))
    }

    @Test
    fun `clicking the 'add layer' button moves the files to the project layers dir if it is selected`() {
        val file1 = TempFiles.createTempFile("layer1", MbtilesFile.FILE_EXTENSION).also {
            it.writeText("blah1")
        }
        val file2 = TempFiles.createTempFile("layer2", MbtilesFile.FILE_EXTENSION).also {
            it.writeText("blah2")
        }

        launchFragment().onFragment {
            it.viewModel.loadLayersToImport(listOf(file1.toUri(), file2.toUri()))
        }

        scheduler.flush()

        onView(withId(org.odk.collect.maps.R.id.current_project_option)).perform(scrollTo(), click())

        onView(withId(org.odk.collect.maps.R.id.add_layer_button)).perform(click())
        scheduler.flush()

        assertThat(File(referenceLayerRepository.getSharedLayersDirPath()).listFiles().size, equalTo(0))
        assertThat(File(referenceLayerRepository.getProjectLayersDirPath()).listFiles().size, equalTo(2))

        val copiedFile1 = File(referenceLayerRepository.getProjectLayersDirPath(), file1.name)
        assertThat(copiedFile1.exists(), equalTo(true))
        assertThat(copiedFile1.readText(), equalTo("blah1"))

        val copiedFile2 = File(referenceLayerRepository.getProjectLayersDirPath(), file2.name)
        assertThat(copiedFile2.exists(), equalTo(true))
        assertThat(copiedFile2.readText(), equalTo("blah2"))
    }

    private fun launchFragment(): FragmentScenario<OfflineMapLayersImporter> {
        return fragmentScenarioLauncherRule.launchInContainer(OfflineMapLayersImporter::class.java)
    }
}
