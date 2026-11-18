package studio.modroll.checks.critfall;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import studio.modroll.critfall.api.ArmorClassBreakdown;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.combat.Derivation;

public final class CritfallArmorClass {

    private CritfallArmorClass() {}

    public static int worn(LivingEntity entity) {
        return RollService.armorClassBreakdown(entity).total();
    }

    /**
     * Critfall's API only breaks down the AC of what is worn, so this goes through its public but non-API {@link
     * Derivation}: switch to an API method once Critfall has one.
     */
    public static int withoutBodyArmor(LivingEntity entity) {
        ArmorClassBreakdown breakdown = RollService.armorClassBreakdown(entity);
        if (!breakdown.derived()) {
            return breakdown.total();
        }
        ItemStack bodyArmor = entity.getItemBySlot(EquipmentSlot.CHEST);
        return Derivation.armorClass(
                without(entity, Attributes.ARMOR, bodyArmor), without(entity, Attributes.ARMOR_TOUGHNESS, bodyArmor));
    }

    private static double without(LivingEntity entity, Holder<Attribute> attribute, ItemStack piece) {
        AttributeInstance worn = entity.getAttribute(attribute);
        if (worn == null) {
            return 0.0;
        }
        AttributeInstance copy = new AttributeInstance(attribute, changed -> {});
        copy.replaceFrom(worn);
        piece.forEachModifier(EquipmentSlot.CHEST, (modified, modifier) -> {
            if (modified.equals(attribute)) {
                copy.removeModifier(modifier.id());
            }
        });
        return copy.getValue();
    }
}
