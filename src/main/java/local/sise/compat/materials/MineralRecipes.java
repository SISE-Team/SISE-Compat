package local.sise.compat.materials;

import java.util.*;

/** Only the seven reviewed mineral-processing failures, never unrelated pack recipes. */
public final class MineralRecipes {
    public static final Set<String> CRUSHING = Set.of(
        "neoecoae:crushing/aluminum_alloy_dust", "neoecoae:crushing/black_tungsten_alloy_dust",
        "neoecoae:crushing/energized_crystal_dust", "neoecoae:crushing/energized_flux_crystal_dust");
    public static final Set<String> CRYSTAL_FIXING = Set.of(
        "neoecoae:crystal_fixer/chipped_budding_energized_crystal",
        "neoecoae:crystal_fixer/damaged_budding_energized_crystal",
        "neoecoae:crystal_fixer/flawed_budding_energized_crystal");

    private MineralRecipes() {}
}
