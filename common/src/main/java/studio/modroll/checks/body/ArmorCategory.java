package studio.modroll.checks.body;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import studio.modroll.checks.Checks;

/**
 * The 5e armor weight a piece belongs to, from the item tags {@code checks:light_armor},
 * {@code checks:medium_armor} and {@code checks:heavy_armor}; lightest first. An untagged piece counts
 * as no armor. Only body armor, the piece worn on the chest, sets the weight.
 */
public enum ArmorCategory {
    NONE("none", Optional.empty()),
    LIGHT("light", Optional.of(tag("light_armor"))),
    MEDIUM("medium", Optional.of(tag("medium_armor"))),
    HEAVY("heavy", Optional.of(tag("heavy_armor")));

    public static final StreamCodec<ByteBuf, ArmorCategory> STREAM_CODEC =
            ByteBufCodecs.idMapper(index -> values()[index], ArmorCategory::ordinal);

    private final String id;
    private final Optional<TagKey<Item>> tag;

    ArmorCategory(String id, Optional<TagKey<Item>> tag) {
        this.id = id;
        this.tag = tag;
    }

    public String id() {
        return id;
    }

    /**
     * The body armor's category, as in 5e, where armor is what covers the body: a helmet, leggings or
     * boots never limit DEX, whatever their tags.
     */
    public static ArmorCategory worn(LivingEntity entity) {
        return of(entity.getItemBySlot(EquipmentSlot.CHEST));
    }

    /** A piece in more than one tag counts as the heaviest of them. */
    public static ArmorCategory of(ItemStack piece) {
        for (int i = values().length - 1; i > 0; i--) {
            ArmorCategory category = values()[i];
            if (category.tag.filter(piece::is).isPresent()) {
                return category;
            }
        }
        return NONE;
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Checks.id(path));
    }
}
