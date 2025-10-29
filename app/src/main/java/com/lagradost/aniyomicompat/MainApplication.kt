package com.lagradost.aniyomicompat

import android.app.Application
import dev.mihon.injekt.patchInjekt
import eu.kanade.domain.DomainModule
import eu.kanade.domain.SYDomainModule
import eu.kanade.tachiyomi.di.AppModule
import eu.kanade.tachiyomi.di.PreferenceModule
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AniyomiPlugin().load(this)
    }
}

class CustomAppModule(val app: Application) : InjektModule {
    override fun InjektRegistrar.registerInjectables() {

        patchInjekt()
        Injekt.importModule(PreferenceModule(app))
        Injekt.importModule(AppModule(app))
        Injekt.importModule(DomainModule())
        Injekt.importModule(SYDomainModule())

        addSingleton(app)

    }
}