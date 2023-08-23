package com.lagradost.aniyomicompat

import android.app.Application
import android.content.Context
import com.lagradost.AnimeExtensionLoader
import com.lagradost.cloudstream3.plugins.Plugin
import eu.kanade.tachiyomi.extension.anime.model.AnimeLoadResult
import uy.kohesive.injekt.Injekt

class AniyomiPlugin : Plugin() {
    override fun load(context: Context) {
        Injekt.importModule(AppModule(context.applicationContext as Application))
        val extensions = AnimeExtensionLoader().loadExtensions(context)
        extensions.forEach {
            if (it is AnimeLoadResult.Success) {
                val mainApis = it.extension.getMainApis()
                println("Loaded Aniyomi apis: $mainApis")
                mainApis.forEach(::registerMainAPI)
            }
        }
        println("Loaded Aniyomi extensions: $extensions")
    }
}