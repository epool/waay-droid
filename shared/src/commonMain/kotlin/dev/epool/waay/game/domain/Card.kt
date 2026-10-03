package dev.epool.waay.game.domain

/**
 * One presented card. [bitValue] (2^k) is the hidden mapping used for decoding and never leaves
 * the domain (FR-011); [numbers] is what the player sees.
 */
internal data class Card(
    val bitValue: Int,
    val numbers: List<Int>,
)
