package com.github.hackertube.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.hackertube.api.RetrofitInstance
import com.github.hackertube.api.obj.PipedInstance
import com.github.hackertube.db.DatabaseHolder.Database
import com.github.hackertube.db.obj.CustomInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.MalformedURLException

class InstancesModel : ViewModel() {
    val customInstances = Database.customInstanceDao().getAllFlow()
        .flowOn(Dispatchers.IO)

    val instances = customInstances.map { instances ->
        val instances = instances.map { PipedInstance(it.name, it.apiUrl) }.toMutableList()

        // add the currently used instances to the list if they're currently down / not part
        // of the public instances list
        val apiUrl = RetrofitInstance.pipedAuthUrl
        if (instances.none { it.apiUrl == apiUrl }) {
            val origin = apiUrl.toHttpUrl().host
            instances.add(PipedInstance(origin, apiUrl, isCurrentlyDown = true))
        }
        instances.sortBy { it.name }

        instances
    }

    fun addCustomInstance(
        apiUrlInput: String,
        instanceNameInput: String?,
        frontendUrlInput: String?
    ) {
        if (apiUrlInput.isEmpty()) throw IllegalArgumentException()

        val apiUrl = apiUrlInput.toHttpUrlOrNull() ?: throw MalformedURLException()
        val frontendUrl = if (!frontendUrlInput.isNullOrBlank()) {
            frontendUrlInput.toHttpUrlOrNull() ?: throw MalformedURLException()
        } else {
            null
        }

        viewModelScope.launch(Dispatchers.IO) {
            val instanceName = instanceNameInput ?: apiUrl.host

            Database.customInstanceDao()
                .insert(
                    CustomInstance(
                        instanceName,
                        apiUrl.toString(),
                        frontendUrl?.toString().orEmpty()
                    )
                )
        }
    }

    fun deleteCustomInstance(customInstance: CustomInstance) =
        viewModelScope.launch(Dispatchers.IO) {
            Database.customInstanceDao().deleteCustomInstance(customInstance)
        }
}