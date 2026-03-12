package br.com.leogsouza.escalav.ui.restrictions

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leogsouza.escalav.data.local.TokenStore
import br.com.leogsouza.escalav.data.remote.api.ApiService
import br.com.leogsouza.escalav.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class RestrictionsUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val restrictions: List<RestrictionDto> = emptyList(),
    val scheduleId: Int? = null,
    val volunteerId: Int? = null
)

data class RestrictionFormState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val restriction: RestrictionDto? = null,
    val restrictionTypes: List<RestrictionTypeDto> = emptyList(),
    val saved: Boolean = false
)

@HiltViewModel
class RestrictionsViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _listState = MutableStateFlow(RestrictionsUiState())
    val listState: StateFlow<RestrictionsUiState> = _listState

    private val _formState = MutableStateFlow(RestrictionFormState())
    val formState: StateFlow<RestrictionFormState> = _formState

    private fun getVolunteerIdFromToken(): Int? {
        val token = tokenStore.accessToken ?: return null
        return try {
            val payload = token.split(".")[1]
            val decoded = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP))
            JSONObject(decoded).getInt("id")
        } catch (e: Exception) { null }
    }

    fun loadRestrictions() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(loading = true, error = null)
            try {
                val volunteerId = getVolunteerIdFromToken()
                val schedules = api.getActiveSchedules()
                val scheduleId = schedules.firstOrNull()?.id
                val result = api.getRestrictions(
                    page = 1,
                    pageSize = 50,
                    scheduleId = scheduleId,
                    volunteerId = volunteerId
                )
                _listState.value = RestrictionsUiState(
                    restrictions = result.data,
                    scheduleId = scheduleId,
                    volunteerId = volunteerId
                )
            } catch (e: Exception) {
                _listState.value = _listState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun loadFormData(restrictionId: Int?) {
        viewModelScope.launch {
            _formState.value = RestrictionFormState(loading = true)
            try {
                val types = api.getRestrictionTypes()
                val restriction = restrictionId?.let { api.getRestrictionById(it) }
                _formState.value = RestrictionFormState(restrictionTypes = types, restriction = restriction)
            } catch (e: Exception) {
                _formState.value = RestrictionFormState(error = e.message)
            }
        }
    }

    fun saveRestriction(data: RestrictionDto, id: Int?) {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(saving = true, error = null)
            try {
                if (id == null) api.createRestriction(data)
                else api.updateRestriction(id, data)
                _formState.value = _formState.value.copy(saving = false, saved = true)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun deleteRestriction(id: Int) {
        viewModelScope.launch {
            try {
                api.deleteRestriction(id)
                loadRestrictions()
            } catch (e: Exception) {
                _listState.value = _listState.value.copy(error = e.message)
            }
        }
    }
}
