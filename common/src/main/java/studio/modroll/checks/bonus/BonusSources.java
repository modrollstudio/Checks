package studio.modroll.checks.bonus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.api.Stat;
import studio.modroll.checks.data.MatchEntry;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoreService;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.trait.TraitHooks;

/**
 * The loaded bonus sources, swapped on every reload, and what they add to an entity's rolls and passive
 * scores. The stat screen and {@code /checks get} show the modifiers and passive scores from here.
 */
public final class BonusSources {

    private static volatile List<BonusSource> sources = List.of();

    private BonusSources() {}

    public static void set(Map<ResourceLocation, BonusSource> loaded) {
        sources = loaded.values().stream()
                .sorted(Comparator.comparing(BonusSource::id))
                .toList();
    }

    /** In id order. */
    public static List<BonusSource> all() {
        return sources;
    }

    public static void clear() {
        sources = List.of();
    }

    /** The datapack sources while they are enabled, then the entity's trait roll bonuses. */
    public static SituationalBonus forCheck(LivingEntity entity, CheckKind kind, Stat stat) {
        List<BonusPart> parts = new ArrayList<>();
        if (settings().enabled()) {
            parts.addAll(
                    SituationalBonus.of(sources, bearer(entity), kind, stat).parts());
        }
        parts.addAll(TraitHooks.rollBonuses(entity, kind, stat));
        return new SituationalBonus(parts);
    }

    /** The skill modifier a check adds once its flat bonus sources are counted. */
    public static int skillModifier(LivingEntity entity, Skill skill) {
        return ScoreService.skillModifier(entity, skill)
                + forCheck(entity, CheckKind.CHECK, skill).bonus();
    }

    /** The save modifier a saving throw adds once its flat bonus sources are counted. */
    public static int saveModifier(LivingEntity entity, Ability ability) {
        return ScoreService.saveModifier(entity, ability)
                + forCheck(entity, CheckKind.SAVE, ability).bonus();
    }

    /** {@code 10 +} the skill modifier, plus the skill check's bonus sources counted the passive way. */
    public static int passiveScore(LivingEntity entity, Skill skill) {
        return ScoreService.passiveScore(entity, skill)
                + forCheck(entity, CheckKind.CHECK, skill)
                        .passiveBonus(settings().passiveAdvantage());
    }

    private static ScoresConfig.BonusSourceSettings settings() {
        return ScoresRuntime.config().extensions().bonusSources();
    }

    private static BonusSource.Bearer bearer(LivingEntity entity) {
        return new BonusSource.Bearer() {
            @Override
            public boolean wears(List<EquipmentSlot> slots, MatchEntry item) {
                return slots.stream().map(entity::getItemBySlot).anyMatch(stack -> matches(stack, item));
            }

            @Override
            public boolean hasEffect(ResourceLocation effect) {
                return BuiltInRegistries.MOB_EFFECT
                        .getHolder(effect)
                        .map(entity::hasEffect)
                        .orElse(false);
            }
        };
    }

    private static boolean matches(ItemStack stack, MatchEntry item) {
        return !stack.isEmpty()
                && item.matches(
                        BuiltInRegistries.ITEM.getKey(stack.getItem()),
                        tag -> stack.is(TagKey.create(Registries.ITEM, tag)));
    }
}
