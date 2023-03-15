package com.lambda

import com.lambda.client.plugin.api.Plugin
import com.lambda.modules.ExampleModule

internal object ExamplePlugin : Plugin() {

    override fun onLoad() {
        modules.add(ExampleModule)
    }
}