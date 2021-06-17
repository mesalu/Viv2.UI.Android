package com.mesalu.viv2.android_ui.ui.charting;

/**
 * Represents zones from which samples can be taken that the chart may display
 *
 * Acronym description:
 *
 * H -> Hot
 * C -> Cold
 * S -> Side
 * I -> Inner/Inside
 * U -> Under
 * E-> Enclosure
 *
 * e.g. HSIE is the sensor that is located inside the tank on the hot end of the enclosure
 */
public enum SampleZone {
    HSIE, HSUE, MIE, CSIE, CSUE
}
