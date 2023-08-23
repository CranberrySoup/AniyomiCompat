package com.lagradost.aniyomicompat

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.core.preference.AndroidPreferenceStore
import eu.kanade.tachiyomi.core.preference.PreferenceStore
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get
import java.lang.ref.WeakReference

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AniyomiPlugin().load(this)
    }
}

class AppModule(val app: Application) : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<PreferenceStore> {
            AndroidPreferenceStore(app)
        }
        addSingleton(app)
        addSingletonFactory { app }
        addSingletonFactory {
            NetworkPreferences(
                preferenceStore = get(),
                verboseLogging = false,
            )
        }
        addSingletonFactory { NetworkHelper(app) }
        addSingletonFactory {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        }
        addSingletonFactory {
            XML {
                unknownChildHandler = UnknownChildHandler { _, _, _, _, _ -> emptyList() }
                autoPolymorphic = true
                xmlDeclMode = XmlDeclMode.Charset
                indent = 4
                xmlVersion = XmlVersion.XML10
            }
        }
        addSingletonFactory { JavaScriptEngine(app) }
        addSingletonFactory {
            BasePreferences(app, get())
        }
        ContextCompat.getMainExecutor(app).execute {
            get<NetworkHelper>()
        }
    }
}