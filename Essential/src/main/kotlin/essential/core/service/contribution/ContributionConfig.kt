package essential.core.service.contribution

import kotlinx.serialization.Serializable

/**
 * Contribution scoring configuration.
 *
 * All weights are configurable so server owners can tune the scoring without a rebuild.
 * Block/item keys use Mindustry content names (e.g. "graphite-press", "silicon").
 */
@Serializable
data class ContributionConfig(
    // The feature is gated by `module.contribution` in the core config. This flag is a
    // secondary in-feature switch and defaults to on, so enabling the module is enough.
    val enabled: Boolean = true,

    // --- Mining (drill output polled every second) ---
    /** Score per mined ore unit while the team is below the core threshold. */
    val miningPerOre: Double = 1.0,
    /** Multiplier applied to mining score once the team core reaches [coreThreshold] of that item. */
    val postThresholdMultiplier: Double = 0.1,
    /** Core stored amount (copper/lead) at which mining score drops to [postThresholdMultiplier]. */
    val coreThreshold: Int = 30_000,
    /** Core stored titanium amount that switches titanium production score from high to low. */
    val titaniumThreshold: Int = 5_000,

    // --- Factory first-build score (block name -> points) ---
    val factoryBuildScore: Map<String, Int> = mapOf(
        "graphite-press" to 80,
        "multi-press" to 80,
        "silicon-smelter" to 100,
        "silicon-crucible" to 100,
        "kiln" to 150,
        "pulverizer" to 150,
        "melter" to 190,            // titanium-adjacent processing
        "separator" to 190,
        "disassembler" to 250,
        "plastanium-compressor" to 450,
        "phase-weaver" to 600,      // meta(phase fabric)
        "surge-smelter" to 800,
    ),

    // --- Produced item score (item name -> points each) ---
    // 석탄 -> 흑연/실리콘 등. 석탄 자체는 점수 없음(목록에 없으면 0점).
    val itemProduceScore: Map<String, Int> = mapOf(
        "graphite" to 7,
        "silicon" to 15,
        "metaglass" to 10,
        // titanium handled specially via titaniumThreshold (15 before, 8 after)
        "thorium" to 17,
        "plastanium" to 30,
        "phase-fabric" to 60,       // meta
        "surge-alloy" to 90,
    ),
    /** Titanium production score before the core reaches [titaniumThreshold]. */
    val titaniumScoreBeforeThreshold: Int = 15,
    /** Titanium production score after the core reaches [titaniumThreshold]. */
    val titaniumScoreAfterThreshold: Int = 8,

    // --- Power ---
    /** Fraction of net power production converted to score per second. */
    val powerScoreRatio: Double = 0.10,

    // --- Resource penalty on build/break ---
    /**
     * Block name substrings exempt from the resource (-score) cost on build and from the
     * per-second loss when self-deconstructed (conveyors, walls, turrets).
     */
    val resourcePenaltyExempt: List<String> = listOf("conveyor", "duct", "wall", "turret"),
    /** Multiplier applied to a block's resource cost when subtracting build penalty. */
    val buildPenaltyMultiplier: Double = 1.0,
)
