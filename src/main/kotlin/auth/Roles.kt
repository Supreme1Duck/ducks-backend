package com.ducks.auth

enum class Roles(val role: String) {
    Admin("admin"), Client("client"), Seller("seller"), CoffeeSeller("coffee-seller")
}