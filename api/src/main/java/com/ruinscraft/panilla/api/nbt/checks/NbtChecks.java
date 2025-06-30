package com.ruinscraft.panilla.api.nbt.checks;

import com.ruinscraft.panilla.api.IPanilla;
import com.ruinscraft.panilla.api.exception.FailedNbt;
import com.ruinscraft.panilla.api.exception.FailedNbtList;
import com.ruinscraft.panilla.api.exception.NbtNotPermittedException;
import com.ruinscraft.panilla.api.nbt.INbtTagCompound;
import com.ruinscraft.panilla.api.nbt.INbtTagList;
import com.ruinscraft.panilla.api.nbt.NbtDataType;
import com.ruinscraft.panilla.api.nbt.checks.paper1_20_6.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NbtChecks {

    private static final Map<String, NbtCheck> checks = new HashMap<>();

    static {
        // 1.20.6
        register(new NbtCheck_BlockEntityData());
        register(new NbtCheck_Container());
        register(new NbtCheck_ContainerLoot());
        register(new NbtCheck_CustomModelData_1_20_6());
        register(new NbtCheck_CustomName());
        register(new NbtCheck_CustomPotionEffects_1_20_6());
        register(new NbtCheck_Fireworks());
        register(new NbtCheck_Lock());
        register(new NbtCheck_Lore());
        register(new NbtCheck_PaperRange());
        register(new NbtCheck_SkullOwner1_20_6());
        register(new NbtCheck_WritableBookContent());
        register(new NbtCheck_WrittenBookContent());
        register(new NbtCheck_EntityData());
        register(new NbtCheck_ChargedProjectiles_1_20_6());
        register(new NbtCheck_Enchantments_1_20_6());
        // non-vanilla
        register(new NbtCheck_weBrushJson1_20_6());

        // other
        // vanilla
        register(new NbtCheck_Unbreakable());
        register(new NbtCheck_CanDestroy());
        register(new NbtCheck_CanPlaceOn());
        register(new NbtCheck_BlockEntityTag());
        register(new NbtCheck_BlockStateTag());
        register(new NbtCheck_ench());
        register(new NbtCheck_RepairCost());
        register(new NbtCheck_AttributeModifiers());
        register(new NbtCheck_CustomPotionEffects());
        register(new NbtCheck_Potion());
        register(new NbtCheck_CustomPotionColor());
        register(new NbtCheck_display());
        register(new NbtCheck_HideFlags());
        register(new NbtCheck_resolved());
        register(new NbtCheck_generation());
        register(new NbtCheck_author());
        register(new NbtCheck_title());
        register(new NbtCheck_pages());
        register(new NbtCheck_SkullOwner());
        register(new NbtCheck_SkullProfile());
        register(new NbtCheck_Explosion());
        register(new NbtCheck_Fireworks());
        register(new NbtCheck_EntityTag());
        register(new NbtCheck_BucketVariantTag());
        register(new NbtCheck_map());
        register(new NbtCheck_map_scale_direction());
        register(new NbtCheck_Decorations());
        register(new NbtCheck_Effects());
        register(new NbtCheck_CustomModelData());
        register(new NbtCheck_HasVisualFire()); // 1.17
        register(new NbtCheck_ChargedProjectiles());
        register(new NbtCheck_Items());
        // non-vanilla
        register(new NbtCheck_weBrushJson());
    }

    private static void register(NbtCheck check) {
        checks.put(check.getName(), check);
        for (String alias : check.getAliases()) checks.put(alias, check);
    }

    public static Map<String, NbtCheck> getChecks() {
        return checks;
    }

    public static void checkPacketPlayIn(int slot, INbtTagCompound tag, String nmsItemClassName, String nmsPacketClassName,
                                         IPanilla panilla) throws NbtNotPermittedException {
        List<FailedNbt> failedNbtList = checkAll(tag, nmsItemClassName, panilla);

        FailedNbt lastNonCritical = null;

        for (FailedNbt failedNbt : failedNbtList) {
            if (failedNbt.result == NbtCheck.NbtCheckResult.CRITICAL) {
                throw new NbtNotPermittedException(nmsPacketClassName, false, failedNbt, slot);
            } else if (FailedNbt.fails(failedNbt)) {
                lastNonCritical = failedNbt;
            }
        }

        if (lastNonCritical != null) {
            throw new NbtNotPermittedException(nmsPacketClassName, true, lastNonCritical, slot);
        }
    }

    public static void checkPacketPlayOut(int slot, INbtTagCompound tag, String nmsItemClassName, String nmsPacketClassName,
                                          IPanilla panilla) throws NbtNotPermittedException {
        FailedNbtList failedNbtList = checkAll(tag, nmsItemClassName, panilla);

        if (failedNbtList.containsCritical()) {
            throw new NbtNotPermittedException(nmsPacketClassName, false, failedNbtList.getCritical(), slot);
        }

        FailedNbt failedNbt = failedNbtList.findFirstNonCritical();

        if (failedNbt != null) {
            throw new NbtNotPermittedException(nmsPacketClassName, false, failedNbt, slot);
        }
    }

    private static boolean tagMeetsKeyThreshold(INbtTagCompound tag, IPanilla panilla) {
        int maxNonMinecraftKeys = panilla.getPConfig().maxNonMinecraftNbtKeys;

        if (tag.getNonMinecraftKeys().size() > maxNonMinecraftKeys) {
            return false;
        }

        for (String key : tag.getKeys()) {
            if (tag.hasKeyOfType(key, NbtDataType.COMPOUND)) {
                INbtTagCompound subTag = tag.getCompound(key);

                if (!tagMeetsKeyThreshold(subTag, panilla)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static FailedNbtList checkAll(INbtTagCompound tag, String nmsItemClassName, IPanilla panilla) {
        FailedNbtList failedNbtList = new FailedNbtList();
        if (!tagMeetsKeyThreshold(tag, panilla)) {
            failedNbtList.add(FailedNbt.FAIL_KEY_THRESHOLD);
        }

        for (String key : tag.getKeys()) {
            if (panilla.getPConfig().nbtWhitelist.contains(key)) {
                continue;
            }

            if (tag.hasKeyOfType(key, NbtDataType.LIST)) {
                INbtTagList list = tag.getList(key);

                if (list.size() > 128) {
                    failedNbtList.add(new FailedNbt((key), NbtCheck.NbtCheckResult.CRITICAL));
                }
            }

            NbtCheck check = checks.get(key);

            if (check == null) {
                // a non-minecraft NBT tag
                continue;
            }

            if (check.getTolerance().ordinal() > panilla.getPConfig().strictness.ordinal()) {
                continue;
            }

            NbtCheck.NbtCheckResult result = check.check(tag, nmsItemClassName, panilla);

            if (result != NbtCheck.NbtCheckResult.PASS) {
                failedNbtList.add(new FailedNbt(key, result));
            }
        }

        return failedNbtList;
    }

    public static INbtTagCompound extractItemNbt(INbtTagCompound rootTag) {
        INbtTagCompound itemTag = null;
        if (rootTag.hasKey("tag")) {
            itemTag = rootTag.getCompound("tag");
        } else if (rootTag.hasKey("item")) {
            itemTag = rootTag.getCompound("item");
            if (itemTag.hasKey("components")) {
                itemTag = itemTag.getCompound("components");
            }
        }
        return itemTag;
    }
}
