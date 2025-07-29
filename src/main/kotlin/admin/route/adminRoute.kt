package com.ducks.admin.route

import io.ktor.server.routing.*

fun Route.adminRoute() {
    adminCoffeeShopsRoute()
    adminShopsRoute()
}