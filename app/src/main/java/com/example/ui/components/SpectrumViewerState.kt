package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.example.domain.model.SpectrumPoint

@Stable
class SpectrumViewerState(
    initialDescending: Boolean = true,
    initialShowGrid: Boolean = true
) {
    var isDescendingAxis by mutableStateOf(initialDescending)
    var zoomLevel by mutableFloatStateOf(1f)
    var panOffsetX by mutableFloatStateOf(0f)
    var panOffsetY by mutableFloatStateOf(0f)
    var showGrid by mutableStateOf(initialShowGrid)
    var showPeakLabels by mutableStateOf(true)
    var isFullscreen by mutableStateOf(false)
    var activeCursorPoint by mutableStateOf<SpectrumPoint?>(null)
    var baselinePoints by mutableStateOf<List<SpectrumPoint>?>(null)
    var showBaseline by mutableStateOf(true)
    var selectedPeak by mutableStateOf<com.example.domain.model.Peak?>(null)

    // Comparison overlay states
    var showUnknownSpectrum by mutableStateOf(true)
    var showReferenceSpectrum by mutableStateOf(true)
    var showDifferenceSpectrum by mutableStateOf(false)
    var differenceSpectrumPoints by mutableStateOf<List<SpectrumPoint>?>(null)
    var normalizeComparison by mutableStateOf(false)

    fun resetZoom() {
        zoomLevel = 1f
        panOffsetX = 0f
        panOffsetY = 0f
    }

    fun zoomIn() {
        zoomLevel = (zoomLevel * 1.35f).coerceAtMost(10f)
    }

    fun zoomOut() {
        zoomLevel = (zoomLevel / 1.35f).coerceAtLeast(1f)
        if (zoomLevel == 1f) {
            panOffsetX = 0f
            panOffsetY = 0f
        }
    }

    fun toggleAxisDirection() {
        isDescendingAxis = !isDescendingAxis
    }

    fun toggleGrid() {
        showGrid = !showGrid
    }

    fun togglePeakLabels() {
        showPeakLabels = !showPeakLabels
    }

    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
    }

    fun toggleNormalizeComparison() {
        normalizeComparison = !normalizeComparison
    }
}

@Composable
fun rememberSpectrumViewerState(
    initialDescending: Boolean = true,
    initialShowGrid: Boolean = true
): SpectrumViewerState {
    return remember { SpectrumViewerState(initialDescending, initialShowGrid) }
}
