package com.swparks.ui.screens.parks.map

private const val CLUSTER_TEXT_SIZE_SHORT = 38f
private const val CLUSTER_TEXT_SIZE_MEDIUM = 34f
private const val CLUSTER_TEXT_SIZE_LONG = 28f
private const val CLUSTER_TEXT_SIZE_OVERFLOW = 22f
private const val CLUSTER_LABEL_SHORT_MAX_LENGTH = 2
private const val CLUSTER_LABEL_MEDIUM_LENGTH = 3
private const val CLUSTER_LABEL_LONG_LENGTH = 4

internal fun clusterTextSize(label: String): Float {
    return when {
        label.length <= CLUSTER_LABEL_SHORT_MAX_LENGTH -> CLUSTER_TEXT_SIZE_SHORT
        label.length == CLUSTER_LABEL_MEDIUM_LENGTH -> CLUSTER_TEXT_SIZE_MEDIUM
        label.length == CLUSTER_LABEL_LONG_LENGTH -> CLUSTER_TEXT_SIZE_LONG
        else -> CLUSTER_TEXT_SIZE_OVERFLOW
    }
}
