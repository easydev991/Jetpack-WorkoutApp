package com.swparks.ui.state

/**
 * Элемент списка выбора с уникальным идентификатором.
 *
 * @property id Стабильный идентификатор (используется как LazyColumn key)
 * @property label Отображаемый текст
 */
data class SelectableItem(val id: String, val label: String)
