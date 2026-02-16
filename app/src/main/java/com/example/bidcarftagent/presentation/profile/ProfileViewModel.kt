package com.example.bidcarftagent.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bidcarftagent.data.repository.BackendRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val companyName: String = "",
    val companyProfileJson: String? = null,
    val industries: List<String> = emptyList(),
    val companySize: String = "",
    val contactEmail: String? = null,
    val services: List<String> = emptyList(),
    val techFrontend: List<String> = emptyList(),
    val techBackend: List<String> = emptyList(),
    val techDatabase: List<String> = emptyList(),
    val techCloud: List<String> = emptyList(),
    val techDevops: List<String> = emptyList(),
    val deliveryModels: List<String> = emptyList(),
    val typicalDurationMonths: Int? = null,
    val pricingCurrency: String = "",
    val pricingSample: Map<String, Number> = emptyMap(),
    val securityPractices: List<String> = emptyList(),
    val caseStudies: List<Pair<String, String>> = emptyList()
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val backendRepository: BackendRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val fbUser = firebaseAuth.currentUser
                val displayName = fbUser?.displayName ?: ""
                val email = fbUser?.email ?: ""
                val photo = fbUser?.photoUrl?.toString()

                val resp = backendRepository.getProfile()
                var companyName = ""
                var companyProfileJson: String? = null
                var industries: List<String> = emptyList()
                var companySize = ""
                var contactEmail: String? = null
                var servicesList: MutableList<String> = mutableListOf()
                var techFrontend: List<String> = emptyList()
                var techBackend: List<String> = emptyList()
                var techDatabase: List<String> = emptyList()
                var techCloud: List<String> = emptyList()
                var techDevops: List<String> = emptyList()
                var deliveryModels: List<String> = emptyList()
                var typicalDurationMonths: Int? = null
                var pricingCurrency = ""
                var pricingSample: MutableMap<String, Number> = mutableMapOf()
                var securityPractices: List<String> = emptyList()
                var caseStudies: MutableList<Pair<String, String>> = mutableListOf()
                if (!resp.isNullOrBlank()) {
                    // parse using org.json to avoid generic gson issues
                    try {
                        val jo = org.json.JSONObject(resp)
                        var profileObj: org.json.JSONObject? = null
                        if (jo.has("profile") && !jo.isNull("profile")) {
                            profileObj = jo.getJSONObject("profile")
                        } else if (jo.has("user") && !jo.isNull("user")) {
                            val userObj = jo.getJSONObject("user")
                            if (userObj.has("companyProfile") && !userObj.isNull("companyProfile")) {
                                profileObj = userObj.getJSONObject("companyProfile")
                            }
                        }
                        if (profileObj != null) {
                            companyProfileJson = profileObj.toString()
                            if (profileObj.has("company_identity") && !profileObj.isNull("company_identity")) {
                                val ci = profileObj.getJSONObject("company_identity")
                                companyName = ci.optString("name", "")
                                industries = try {
                                    val arr = ci.optJSONArray("industries")
                                    if (arr != null) (0 until arr.length()).map { arr.optString(it) } else emptyList()
                                } catch (_: Exception) { emptyList() }
                                companySize = ci.optString("company_size", "")
                                contactEmail = try { ci.getJSONObject("contact").optString("email", null) } catch (_: Exception) { null }
                            }

                            if (profileObj.has("services") && !profileObj.isNull("services")) {
                                val servObj = profileObj.getJSONObject("services")
                                val keys = servObj.keys()
                                while (keys.hasNext()) {
                                    val k = keys.next()
                                    if (servObj.optBoolean(k, false)) servicesList.add(k.replace('_', ' ').capitalize())
                                }
                            }

                            if (profileObj.has("tech_stack") && !profileObj.isNull("tech_stack")) {
                                val tech = profileObj.getJSONObject("tech_stack")
                                techFrontend = jsonArrayToList(tech, "frontend")
                                techBackend = jsonArrayToList(tech, "backend")
                                techDatabase = jsonArrayToList(tech, "database")
                                techCloud = jsonArrayToList(tech, "cloud")
                                techDevops = jsonArrayToList(tech, "devops")
                            }

                            if (profileObj.has("delivery_capability") && !profileObj.isNull("delivery_capability")) {
                                val del = profileObj.getJSONObject("delivery_capability")
                                deliveryModels = jsonArrayToList(del, "delivery_models")
                                typicalDurationMonths = if (del.has("typical_project_duration_months")) del.optInt("typical_project_duration_months", 0) else null
                            }

                            if (profileObj.has("pricing_rules") && !profileObj.isNull("pricing_rules")) {
                                val pr = profileObj.getJSONObject("pricing_rules")
                                pricingCurrency = pr.optString("currency", "")
                                if (pr.has("monthly_cost_per_role") && !pr.isNull("monthly_cost_per_role")) {
                                    val pc = pr.getJSONObject("monthly_cost_per_role")
                                    val keys = pc.keys()
                                    while (keys.hasNext()) {
                                        val k = keys.next()
                                        pricingSample[k] = pc.optDouble(k, 0.0)
                                    }
                                }
                            }

                            if (profileObj.has("security_and_compliance") && !profileObj.isNull("security_and_compliance")) {
                                val sc = profileObj.getJSONObject("security_and_compliance")
                                securityPractices = jsonArrayOptToList(sc, "security_practices")
                            }

                            if (profileObj.has("experience_and_case_studies") && !profileObj.isNull("experience_and_case_studies")) {
                                val ecs = profileObj.getJSONObject("experience_and_case_studies")
                                caseStudies = mutableListOf()
                                val arr = ecs.optJSONArray("case_studies")
                                if (arr != null) {
                                  for (i in 0 until arr.length()) {
                                    val it = arr.optJSONObject(i)
                                    if (it != null) {
                                      caseStudies.add(Pair(it.optString("industry",""), it.optString("solution","")))
                                    }
                                  }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore parse errors
                    }
                }

                _uiState.value = ProfileUiState(
                    isLoading = false,
                    displayName = displayName,
                    email = email,
                    photoUrl = photo,
                    companyName = companyName,
                    companyProfileJson = companyProfileJson,
                    industries = industries,
                    companySize = companySize,
                    contactEmail = contactEmail,
                    services = servicesList.toList(),
                    techFrontend = techFrontend,
                    techBackend = techBackend,
                    techDatabase = techDatabase,
                    techCloud = techCloud,
                    techDevops = techDevops,
                    deliveryModels = deliveryModels,
                    typicalDurationMonths = typicalDurationMonths,
                    pricingCurrency = pricingCurrency,
                    pricingSample = pricingSample,
                    securityPractices = securityPractices,
                    caseStudies = caseStudies.toList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun jsonArrayToList(parent: org.json.JSONObject, key: String): List<String> {
        return try {
            val arr = parent.optJSONArray(key) ?: return emptyList()
            (0 until arr.length()).mapNotNull { arr.optString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun jsonArrayOptToList(parent: org.json.JSONObject, key: String): List<String> {
        return jsonArrayToList(parent, key)
    }
}

