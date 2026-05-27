package com.example.workouttracker.feature.nutrition.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.core.auth.AuthSessionStore
import com.example.workouttracker.data.local.LegacyDataMigrator
import com.example.workouttracker.data.local.NutritionRepository
import com.example.workouttracker.data.local.FridgeItemEntity
import com.example.workouttracker.data.local.NutritionEntryEntity
import com.example.workouttracker.data.local.ProductRepository
import com.example.workouttracker.data.local.UserEntity
import com.example.workouttracker.data.local.UserRepository
import com.example.workouttracker.data.local.WeightSyncRepository
import com.example.workouttracker.data.local.WorkoutTrackerDao
import com.example.workouttracker.feature.nutrition.presentation.Dish
import com.example.workouttracker.feature.nutrition.presentation.DailyNutritionSummary
import com.example.workouttracker.feature.nutrition.presentation.DishIngredient
import com.example.workouttracker.feature.nutrition.presentation.Ingredient
import com.example.workouttracker.feature.nutrition.presentation.MealPlan
import com.example.workouttracker.feature.nutrition.presentation.MealType
import com.example.workouttracker.feature.nutrition.presentation.NutritionEntry
import com.example.workouttracker.feature.nutrition.presentation.NutritionCalculator
import com.example.workouttracker.feature.nutrition.presentation.NutritionProfile
import com.example.workouttracker.feature.nutrition.presentation.Norm
import com.example.workouttracker.feature.nutrition.presentation.ProfilePrefillData
import com.example.workouttracker.feature.nutrition.presentation.Sex
import com.example.workouttracker.llm.NutritionAiProvider
import com.example.workouttracker.feature.nutrition.presentation.FridgeProduct
import com.example.workouttracker.feature.nutrition.presentation.QuantityUnit
import com.example.workouttracker.feature.nutrition.presentation.ProductLookupResult
import com.example.workouttracker.feature.nutrition.presentation.FridgeItemUiModel
import com.example.workouttracker.feature.nutrition.presentation.NutritionUiEffect
import com.example.workouttracker.feature.nutrition.presentation.NutritionUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.stateIn
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    authSessionStore: AuthSessionStore,
    private val dao: WorkoutTrackerDao,
    private val userRepository: UserRepository,
    private val nutritionRepository: NutritionRepository,
    private val productRepository: ProductRepository,
    private val weightSyncRepository: WeightSyncRepository,
    private val migrator: LegacyDataMigrator,
    nutritionAiProvider: NutritionAiProvider
) : ViewModel() {

    private val userId = authSessionStore.currentUserIdOrGuest()
    private val nutritionAiRepository = nutritionAiProvider.forUser(userId)

    private val _entries = MutableStateFlow<List<NutritionEntry>>(emptyList())
    val entries: StateFlow<List<NutritionEntry>> = _entries

    // ---- состояние плана питания ----
    private val _mealPlan = MutableStateFlow<MealPlan?>(null)
    val mealPlan: StateFlow<MealPlan?> = _mealPlan

    private val _isPlanLoading = MutableStateFlow(false)
    val isPlanLoading: StateFlow<Boolean> = _isPlanLoading

    private val _planError = MutableStateFlow<String?>(null)
    val planError: StateFlow<String?> = _planError

    private val _planMessage = MutableStateFlow<String?>(null)
    val planMessage: StateFlow<String?> = _planMessage

    private val _profile = MutableStateFlow<NutritionProfile?>(null)
    val profile: StateFlow<NutritionProfile?> = _profile

    private val _profilePrefill = MutableStateFlow(ProfilePrefillData())
    val profilePrefill: StateFlow<ProfilePrefillData> = _profilePrefill

    private val _recommendedNorm = MutableStateFlow<Norm?>(null)
    val recommendedNorm: StateFlow<Norm?> = _recommendedNorm

    private val _fridgeExtraPrompt = MutableStateFlow<FridgeExtraPrompt?>(null)
    val fridgeExtraPrompt: StateFlow<FridgeExtraPrompt?> = _fridgeExtraPrompt


    private val _fridgeItems = MutableStateFlow<List<FridgeItemUiModel>>(emptyList())
    val fridgeItems = _fridgeItems.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    private val _lookupProduct = MutableStateFlow<ProductLookupResult?>(null)
    val lookupProduct = _lookupProduct.asStateFlow()

    private val _lookupError = MutableStateFlow<String?>(null)
    val lookupError = _lookupError.asStateFlow()

    private val _lookupBarcode = MutableStateFlow<String?>(null)
    val lookupBarcode = _lookupBarcode.asStateFlow()

    private val _savedProducts = MutableStateFlow<List<ProductLookupResult>>(emptyList())
    val savedProducts = _savedProducts.asStateFlow()

    private val _dailyNorm = MutableStateFlow<Map<String, Int>>(emptyMap())
    val dailyNormState: StateFlow<Map<String, Int>> = _dailyNorm.asStateFlow()
    val dailyNorm: Map<String, Int>
        get() = _dailyNorm.value

    private val _effects = MutableSharedFlow<NutritionUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<NutritionUiEffect> = _effects

    data class AdjustedGoal(
        val calories: Int,
        val protein: Int,
        val fats: Int,
        val carbs: Int
    )

    data class FridgeExtraPrompt(
        val fridge: List<FridgeProduct>
    )

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<NutritionUiState> =
        combine(
            entries,
            dailyNormState,
            recommendedNorm,
            profile,
            profilePrefill,
            mealPlan,
            isPlanLoading,
            planError,
            fridgeExtraPrompt,
            fridgeItems
        ) { values ->
            val entriesValue = values[0] as List<NutritionEntry>
            val normValue = values[1] as Map<String, Int>
            val recommendedNormValue = values[2] as Norm?
            val profileValue = values[3] as NutritionProfile?
            val profilePrefillValue = values[4] as ProfilePrefillData
            val mealPlanValue = values[5] as MealPlan?
            val isPlanLoadingValue = values[6] as Boolean
            val planErrorValue = values[7] as String?
            val fridgePromptValue = values[8] as FridgeExtraPrompt?
            val fridgeItemsValue = values[9] as List<FridgeItemUiModel>
            val grouped = entriesValue.groupBy { it.date }.toSortedMap(compareByDescending { it })
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val todayEntries = grouped[today].orEmpty()
            val todaySummary = DailyNutritionSummary(
                date = today,
                calories = todayEntries.sumOf { it.calories },
                protein = todayEntries.sumOf { it.protein },
                fats = todayEntries.sumOf { it.fats },
                carbs = todayEntries.sumOf { it.carbs }
            )

            NutritionUiState(
                entries = entriesValue,
                groupedEntries = grouped,
                todayTotal = NutritionEntry(
                    date = todaySummary.date,
                    name = if (todayEntries.isEmpty()) "" else "Итого",
                    calories = todaySummary.calories,
                    protein = todaySummary.protein,
                    carbs = todaySummary.carbs,
                    fats = todaySummary.fats,
                    weight = todayEntries.sumOf { it.weight }
                ),
                dailyNorm = normValue,
                recommendedNorm = recommendedNormValue,
                profile = profileValue,
                profilePrefill = profilePrefillValue,
                mealPlan = mealPlanValue,
                isPlanLoading = isPlanLoadingValue,
                planError = planErrorValue,
                fridgePrompt = fridgePromptValue?.fridge,
                fridgeItems = fridgeItemsValue
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            NutritionUiState()
        )

    init {
        viewModelScope.launch {
            migrator.migrateAllKnownUsers()
            if (userRepository.getUserById(userId) == null) {
                dao.upsertUser(UserEntity(id = userId, name = userId, email = ""))
            }
            weightSyncRepository.harmonizeCurrentWeight(userId)
            _dailyNorm.value = nutritionRepository.getCustomNorm(userId) ?: defaultNorm()
            loadProfilePrefillFromMainProfile()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            _mealPlan.value = nutritionRepository.getMealPlan(userId, today)
            nutritionRepository.observeEntries(userId).collect { rows ->
                _entries.value = rows
            }
        }

        viewModelScope.launch {
            nutritionRepository.observeFridgeItems(userId).collect { rows ->
                _fridgeItems.value = rows.map {
                    FridgeItemUiModel(
                        id = it.id,
                        name = it.name,
                        unitType = if (it.unitType == QuantityUnit.PIECES.name) QuantityUnit.PIECES else QuantityUnit.GRAMS,
                        amount = it.amount,
                        calories100 = it.calories100,
                        protein100 = it.protein100,
                        fats100 = it.fats100,
                        carbs100 = it.carbs100,
                        barcode = it.barcode,
                        updatedAt = it.updatedAt
                    )
                }
            }
        }

        viewModelScope.launch {
            nutritionRepository.observeProfile(userId).collect { profile ->
                _profile.value = profile
                _recommendedNorm.value = profile?.let(NutritionCalculator::calculateRecommendedNorm)
                _dailyNorm.value = nutritionRepository.getCustomNorm(userId) ?: defaultNorm()
                val profileNorm = profile?.let {
                    mapOf(
                        "calories" to (_recommendedNorm.value?.calories ?: 2500),
                        "protein" to (_recommendedNorm.value?.protein ?: 120),
                        "fats" to (_recommendedNorm.value?.fats ?: 80),
                        "carbs" to (_recommendedNorm.value?.carbs ?: 300)
                    )
                }
                if (dailyNorm == defaultNorm() && profileNorm != null) {
                    _dailyNorm.value = profileNorm
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    private fun parseNutritionEntity(entity: NutritionEntryEntity): NutritionEntry {
        val root = JSONObject(entity.dishJson)
        val dish = parseDish(root)
        return NutritionEntry(
            id = UUID.fromString(entity.id),
            date = entity.dateIso,
            mealType = MealType.values().firstOrNull { it.name == entity.mealType } ?: MealType.OTHER,
            dish = dish,
            portionWeight = entity.portionWeight
        )
    }

    fun getEntriesByDate(date: String): Map<MealType, List<NutritionEntry>> {
        return entries.value
            .filter { it.date == date }
            .groupBy { it.mealType }
    }

    fun getDailySummary(date: String): DailyNutritionSummary {
        val list = entries.value.filter { it.date == date }
        return DailyNutritionSummary(
            date = date,
            calories = list.sumOf { it.calories },
            protein = list.sumOf { it.protein },
            fats = list.sumOf { it.fats },
            carbs = list.sumOf { it.carbs }
        )
    }

    fun getDailySummaries(): List<DailyNutritionSummary> {
        val currentEntries = entries.value

        return currentEntries
            .groupBy { it.date }
            .map { (date, list) ->
                DailyNutritionSummary(
                    date = date,
                    calories = list.sumOf { it.calories },
                    protein = list.sumOf { it.protein },
                    fats = list.sumOf { it.fats },
                    carbs = list.sumOf { it.carbs }
                )
            }
            .sortedByDescending { it.date }
    }

    private fun calculateGoal(
        recommendedNorm: Norm?,
        userNorm: Map<String, Int>?
    ): AdjustedGoal {
        val defaultNorm = mapOf(
            "calories" to 2000,
            "protein" to 120,
            "fats" to 70,
            "carbs" to 250
        )

        val targetCaloriesBase = userNorm?.get("calories")
            ?: recommendedNorm?.calories
            ?: defaultNorm.getValue("calories")

        val targetProteinBase = userNorm?.get("protein")
            ?: recommendedNorm?.protein
            ?: defaultNorm.getValue("protein")

        val targetFatsBase = userNorm?.get("fats")
            ?: recommendedNorm?.fats
            ?: defaultNorm.getValue("fats")

        val targetCarbsBase = userNorm?.get("carbs")
            ?: recommendedNorm?.carbs
            ?: defaultNorm.getValue("carbs")

        return AdjustedGoal(
            calories = targetCaloriesBase.coerceAtLeast(0),
            protein = targetProteinBase.coerceAtLeast(0),
            fats = targetFatsBase.coerceAtLeast(0),
            carbs = targetCarbsBase.coerceAtLeast(0)
        )
    }


    private fun parseDish(obj: JSONObject): Dish {
        val ingredientsArray = obj.optJSONArray("ingredients") ?: JSONArray()
        val ingredients = buildList {
            for (i in 0 until ingredientsArray.length()) {
                add(parseDishIngredient(ingredientsArray.getJSONObject(i)))
            }
        }
        return Dish(
            id = obj.optUuidOrRandom("id"),
            name = obj.optString("name", ""),
            ingredients = ingredients
        )
    }

    private fun parseDishIngredient(obj: JSONObject): DishIngredient {
        val ingredientObj = obj.getJSONObject("ingredient")
        return DishIngredient(
            id = obj.optUuidOrRandom("id"),
            ingredient = parseIngredient(ingredientObj),
            weightInDish = obj.optInt("weightInDish", 0)
        )
    }

    private fun parseIngredient(obj: JSONObject): Ingredient {
        return Ingredient(
            id = obj.optUuidOrRandom("id"),
            name = obj.optString("name", ""),
            caloriesPer100g = obj.optDouble("caloriesPer100g", 0.0).toFloat(),
            proteinPer100g = obj.optDouble("proteinPer100g", 0.0).toFloat(),
            fatsPer100g = obj.optDouble("fatsPer100g", 0.0).toFloat(),
            carbsPer100g = obj.optDouble("carbsPer100g", 0.0).toFloat()
        )
    }


    private fun JSONObject.optUuidOrRandom(key: String): UUID {
        val raw = optString(key, "").trim()
        if (raw.isEmpty()) return UUID.randomUUID()
        return runCatching { UUID.fromString(raw) }.getOrElse { UUID.randomUUID() }
    }


    fun addEntry(entry: NutritionEntry) {
        viewModelScope.launch {
            nutritionRepository.upsertEntry(userId, entry)
        }
    }

    fun addEntry(date: String, mealType: MealType, dish: Dish, portionWeight: Int) {
        addEntry(
            NutritionEntry(
                date = date,
                mealType = mealType,
                dish = dish,
                portionWeight = portionWeight
            )
        )
    }

    fun removeEntry(id: UUID) {
        viewModelScope.launch {
            nutritionRepository.deleteEntry(userId, id)
        }
    }

    fun updateEntry(updated: NutritionEntry) {
        viewModelScope.launch {
            nutritionRepository.upsertEntry(userId, updated)
        }
    }

    fun lookupBarcode(code: String) {
        viewModelScope.launch {
            _lookupBarcode.value = code
            _lookupError.value = null
            _lookupProduct.value = null
            val response = productRepository.lookup(code)
            if (response.product != null) {
                _lookupProduct.value = response.product
            } else {
                _lookupError.value = response.errorMessage
                    ?: "Продукт не найден в локальной базе. Заполните данные вручную."
            }
        }
    }

    fun loadSavedProducts(query: String = "") {
        viewModelScope.launch {
            _savedProducts.value = productRepository.getCachedProducts(query)
        }
    }

    fun selectSavedProduct(product: ProductLookupResult) {
        _lookupBarcode.value = product.barcode
        _lookupError.value = null
        _lookupProduct.value = product
    }

    fun clearLookupProduct() {
        _lookupProduct.value = null
        _lookupError.value = null
        _lookupBarcode.value = null
    }

    fun saveManualBarcodeProduct(
        barcode: String,
        name: String,
        calories100: Float,
        protein100: Float,
        fats100: Float,
        carbs100: Float
    ) {
        val normalizedName = name.trim()
        if (barcode.isBlank() || normalizedName.isBlank()) {
            _lookupError.value = "Штрихкод и название продукта обязательны."
            return
        }

        viewModelScope.launch {
            val manualProduct = ProductLookupResult(
                barcode = barcode,
                name = normalizedName,
                calories100 = calories100.coerceAtLeast(0f),
                protein100 = protein100.coerceAtLeast(0f),
                fats100 = fats100.coerceAtLeast(0f),
                carbs100 = carbs100.coerceAtLeast(0f),
                source = "manual_local",
                isPartial = false,
                isSuspicious = false
            )
            productRepository.saveManualProduct(manualProduct)
            _lookupBarcode.value = barcode
            _lookupError.value = null
            _lookupProduct.value = manualProduct
        }
    }

    fun addScannedProductToDiary(
        date: String,
        mealType: MealType,
        product: ProductLookupResult,
        grams: Int
    ) {
        addEntry(
            date = date,
            mealType = mealType,
            dish = Dish(
                name = product.name,
                ingredients = listOf(
                    DishIngredient(
                        ingredient = Ingredient(
                            name = product.name,
                            caloriesPer100g = product.calories100 ?: 0f,
                            proteinPer100g = product.protein100 ?: 0f,
                            fatsPer100g = product.fats100 ?: 0f,
                            carbsPer100g = product.carbs100 ?: 0f
                        ),
                        weightInDish = grams
                    )
                )
            ),
            portionWeight = grams
        )
    }


    fun addManualProductToFridge(
        name: String,
        calories100: Float,
        protein100: Float,
        fats100: Float,
        carbs100: Float,
        amount: Int,
        unit: QuantityUnit
    ) {
        viewModelScope.launch {
            nutritionRepository.upsertFridgeItem(
                FridgeItemEntity(
                    userId = userId,
                    name = name,
                    unitType = unit.name,
                    amount = amount,
                    calories100 = calories100,
                    protein100 = protein100,
                    fats100 = fats100,
                    carbs100 = carbs100
                )
            )
        }
    }

    fun removeFridgeItem(itemId: Long) {
        viewModelScope.launch {
            nutritionRepository.deleteFridgeItem(userId, itemId)
        }
    }

    fun updateFridgeItem(
        itemId: Long,
        name: String,
        calories100: Float,
        protein100: Float,
        fats100: Float,
        carbs100: Float,
        amount: Int,
        unit: QuantityUnit
    ) {
        val existing = _fridgeItems.value.firstOrNull { it.id == itemId } ?: return
        viewModelScope.launch {
            nutritionRepository.updateFridgeItem(
                FridgeItemEntity(
                    id = itemId,
                    userId = userId,
                    name = name,
                    unitType = unit.name,
                    amount = amount,
                    calories100 = calories100,
                    protein100 = protein100,
                    fats100 = fats100,
                    carbs100 = carbs100,
                    barcode = existing.barcode,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun addScannedProductToFridge(product: ProductLookupResult, amount: Int, unit: QuantityUnit) {
        viewModelScope.launch {
            nutritionRepository.upsertFridgeItem(
                FridgeItemEntity(
                    userId = userId,
                    name = product.name,
                    unitType = unit.name,
                    amount = amount,
                    calories100 = product.calories100 ?: 0f,
                    protein100 = product.protein100 ?: 0f,
                    fats100 = product.fats100 ?: 0f,
                    carbs100 = product.carbs100 ?: 0f,
                    barcode = product.barcode
                )
            )
        }
    }

    fun deductFridgeItem(item: FridgeItemUiModel, amount: Int) {
        viewModelScope.launch {
            val left = (item.amount - amount).coerceAtLeast(0)
            if (left == 0) {
                nutritionRepository.deleteFridgeItem(userId, item.id)
            } else {
                nutritionRepository.updateFridgeItem(
                    FridgeItemEntity(
                        id = item.id,
                        userId = userId,
                        name = item.name,
                        unitType = item.unitType.name,
                        amount = left,
                        calories100 = item.calories100,
                        protein100 = item.protein100,
                        fats100 = item.fats100,
                        carbs100 = item.carbs100,
                        barcode = item.barcode
                    )
                )
            }
        }
    }

    fun updateProfile(profile: NutritionProfile) {
        _profile.value = profile
        _recommendedNorm.value = NutritionCalculator.calculateRecommendedNorm(profile)
        viewModelScope.launch {
            nutritionRepository.upsertProfile(userId, profile, dailyNorm)
            weightSyncRepository.saveWeightMeasurement(userId, profile.weightKg)
            loadProfilePrefillFromMainProfile()
            _effects.emit(NutritionUiEffect.Snackbar("Профиль питания сохранён"))
        }
    }

    private fun loadProfilePrefillFromMainProfile() {
        viewModelScope.launch {
            val user = userRepository.getUserById(userId)
            val sex = user?.gender
                ?.trim()
                ?.lowercase(Locale.getDefault())
                ?.let { raw ->
                    when {
                        raw.contains("жен") || raw.contains("female") -> Sex.FEMALE
                        raw.contains("муж") || raw.contains("male") -> Sex.MALE
                        else -> null
                    }
                }

            _profilePrefill.value = ProfilePrefillData(
                sex = sex,
                age = user?.age?.takeIf { it in 5..120 },
                heightCm = user?.height?.takeIf { it in 100f..250f }?.roundToInt(),
                weightKg = user?.weight?.takeIf { it in 20f..300f }
            )
        }

    }

    fun updateNorm(norm: Map<String, Int>) {
        _dailyNorm.value = norm
        viewModelScope.launch {
            nutritionRepository.updateCustomNorm(userId, _profile.value, norm)
            _effects.emit(NutritionUiEffect.Snackbar("Нормы сохранены"))
        }
    }

    fun generatePlanFromFridge(fridge: List<FridgeProduct>, allowExtraProducts: Boolean) {
        _fridgeExtraPrompt.value = null
        generatePlanFromFridgeInternal(fridge, allowExtraProducts, forceProceed = false)
    }

    private fun generatePlanFromFridgeInternal(
        fridge: List<FridgeProduct>,
        allowExtraProducts: Boolean,
        forceProceed: Boolean
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (hasCachedPlanForDate(today)) {
            _planError.value = "План на сегодня уже создан"
            return
        }

        val adjustedGoal = calculateGoal(recommendedNorm.value, dailyNorm)

        viewModelScope.launch {
            _isPlanLoading.value = true
            _planError.value = null
            try {
                if (!allowExtraProducts && !forceProceed) {
                    val maxCaloriesFromFridge = fridge.sumOf { product ->
                        estimateFridgeCalories(product)
                    }
                    if (maxCaloriesFromFridge < adjustedGoal.calories * 0.9) {
                        _fridgeExtraPrompt.value = FridgeExtraPrompt(fridge)
                        return@launch
                    }
                }

                val plan = nutritionAiRepository.generatePlanFromFridge(
                    date = today,
                    fridge = fridge,
                    profile = profile.value,
                    recommendedNorm = recommendedNorm.value,
                    userNorm = dailyNorm,
                    allowExtraProducts = allowExtraProducts,
                    goalCalories = adjustedGoal.calories,
                    goalProtein = adjustedGoal.protein,
                    goalFats = adjustedGoal.fats,
                    goalCarbs = adjustedGoal.carbs
                )
                _mealPlan.value = plan
                nutritionRepository.saveMealPlan(userId, plan)
            } catch (e: Exception) {
                e.printStackTrace()
                _planError.value = e.message ?: "Ошибка при генерации плана"
            } finally {
                _isPlanLoading.value = false
            }
        }
    }

    private fun estimateFridgeCalories(product: FridgeProduct): Double {
        val amount = (product.availableGrams ?: 0).coerceAtLeast(0)
        if (amount == 0) return 0.0

        // Для "шт" используем приближение: 1 шт ≈ 100 г, чтобы корректно оценить покрытие цели.
        val estimatedGrams = if (product.unitType == QuantityUnit.PIECES) amount * 100 else amount
        return estimatedGrams.toDouble() * product.calories100.toDouble() / 100.0
    }

    fun allowExtraProductsForFridgePlan() {
        val fridge = _fridgeExtraPrompt.value?.fridge ?: return
        _fridgeExtraPrompt.value = null
        generatePlanFromFridgeInternal(fridge, allowExtraProducts = true, forceProceed = true)
    }

    fun continueWithoutExtraProducts() {
        val fridge = _fridgeExtraPrompt.value?.fridge ?: return
        _fridgeExtraPrompt.value = null
        generatePlanFromFridgeInternal(fridge, allowExtraProducts = false, forceProceed = true)
    }

    fun generateTodayPlan() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            _isPlanLoading.value = true
            _planError.value = null
            try {
                val plan = nutritionAiRepository.generatePersonalizedPlan(
                    date = today,
                    profile = profile.value,
                    recommendedNorm = recommendedNorm.value,
                    userNorm = dailyNorm
                )
                _mealPlan.value = plan
                nutritionRepository.saveMealPlan(userId, plan)
            } catch (e: Exception) {
                e.printStackTrace()
                _planError.value = e.message ?: "Ошибка при генерации плана"
            } finally {
                _isPlanLoading.value = false
            }
        }
    }

    fun replaceMeal(mealType: MealType, comment: String?) {
        val currentPlan = _mealPlan.value
        if (currentPlan == null) {
            _planError.value = "План на сегодня не создан"
            return
        }

        viewModelScope.launch {
            _isPlanLoading.value = true
            _planError.value = null
            try {
                val newMeal = nutritionAiRepository.replaceMeal(
                    date = currentPlan.date,
                    mealType = mealType,
                    currentPlan = currentPlan,
                    profile = profile.value,
                    recommendedNorm = recommendedNorm.value,
                    userNorm = dailyNorm,
                    comment = comment
                )
                val updatedMeals = currentPlan.meals.map { existing ->
                    if (existing.type == mealType) newMeal else existing
                }
                val updatedPlan = currentPlan.copy(meals = updatedMeals)
                _mealPlan.value = updatedPlan
                nutritionRepository.saveMealPlan(userId, updatedPlan)
            } catch (e: Exception) {
                e.printStackTrace()
                _planError.value = e.message ?: "Ошибка при замене приёма"
            } finally {
                _isPlanLoading.value = false
            }
        }
    }

    fun reuseYesterdayPlan() {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = formatter.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = formatter.format(calendar.time)

        viewModelScope.launch {
            _isPlanLoading.value = true
            _planError.value = null
            try {
                val yesterdayPlan = nutritionRepository.getMealPlan(userId, yesterday)
                if (yesterdayPlan == null) {
                    _planError.value = "План за вчера отсутствует"
                    return@launch
                }
                nutritionRepository.saveMealPlan(userId, yesterdayPlan.copy(date = today))
                val todayPlan = nutritionRepository.getMealPlan(userId, today)
                _mealPlan.value = todayPlan
                _planMessage.value = "План перенесён с вчерашнего дня"
            } catch (e: Exception) {
                e.printStackTrace()
                _planError.value = e.message ?: "Не удалось перенести план"
            } finally {
                _isPlanLoading.value = false
            }
        }
    }

    fun resetTodayPlan() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _mealPlan.value = null
        viewModelScope.launch { nutritionRepository.deleteMealPlan(userId, today) }
        _planError.value = null
    }

    fun hasCachedPlanForDate(date: String): Boolean {
        return runCatching { runBlocking { nutritionRepository.getMealPlan(userId, date) != null } }.getOrDefault(false)
    }

    fun setPlanError(message: String) {
        _planError.value = message
    }

    fun consumePlanMessage() {
        _planMessage.value = null
    }

    fun clearPlanError() {
        _planError.value = null
    }

    fun emitMessage(message: String) {
        viewModelScope.launch {
            _effects.emit(NutritionUiEffect.Snackbar(message))
        }
    }

    private fun defaultNorm(): Map<String, Int> = mapOf(
        "calories" to 2500,
        "protein" to 120,
        "carbs" to 300,
        "fats" to 80
    )
}

