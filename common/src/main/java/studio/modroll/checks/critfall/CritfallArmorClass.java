package studio.modroll.checks.critfall;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.critfall.api.RollService;

public final class CritfallArmorClass {

    private CritfallArmorClass() {}

    public static int worn(LivingEntity entity) {
        return RollService.armorClassBreakdown(entity).total();
    }

    public static int withoutBodyArmor(LivingEntity entity) {
        return RollService.armorClassBreakdown(entity, EquipmentSlot.CHEST).total();
    }
}
