package com.swparks.ui.model

import com.swparks.R
import com.swparks.domain.model.Gender

/**
 * Пол в виде ресурса строки (подпись в списках выбора).
 * Domain-модель [Gender] не зависит от Android-ресурсов — маппинг выполняется в UI-слое
 */
val Gender.descriptionRes: Int
    get() =
        when (this) {
            Gender.MALE -> R.string.man
            Gender.FEMALE -> R.string.woman
        }

/**
 * Обозначение пола в виде ресурса строки.
 * Domain-модель [Gender] не зависит от Android-ресурсов — маппинг выполняется в UI-слое
 */
val Gender.sexRes: Int
    get() =
        when (this) {
            Gender.MALE -> R.string.male
            Gender.FEMALE -> R.string.female
        }
