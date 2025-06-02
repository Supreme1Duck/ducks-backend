package com.ducks.model

enum class SeasonModel(val id: Int, val valueName: String) {
    DEMI_SEASON(0, "Деми"),
    SUMMER(1, "Лето"),
    WINTER(2, "Зима"),
    ALL_YEAR(3, "Круглогодичный");
}