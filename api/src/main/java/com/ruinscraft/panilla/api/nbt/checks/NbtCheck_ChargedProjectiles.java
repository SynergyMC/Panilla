package com.ruinscraft.panilla.api.nbt.checks;

import com.ruinscraft.panilla.api.IPanilla;
import com.ruinscraft.panilla.api.config.PStrictness;
import com.ruinscraft.panilla.api.nbt.INbtTagCompound;
import com.ruinscraft.panilla.api.nbt.INbtTagList;
import com.ruinscraft.panilla.api.nbt.NbtDataType;

public class NbtCheck_ChargedProjectiles extends NbtCheck {

    public NbtCheck_ChargedProjectiles() {
        super("ChargedProjectiles", PStrictness.AVERAGE);
    }

    @Override
    public NbtCheckResult check(INbtTagCompound tag, String itemName, IPanilla panilla) {
        NbtCheckResult result = NbtCheckResult.PASS;
        INbtTagList chargedProjectiles = tag.getList("ChargedProjectiles", NbtDataType.COMPOUND);

        for (int i = 0; i < chargedProjectiles.size(); i++) {
            INbtTagCompound chargedProjectile = NbtChecks.extractItemNbt(chargedProjectiles.getCompound(i));

            if (chargedProjectile != null && chargedProjectile.hasKey("Potion")) {
                String potion = chargedProjectile.getString("Potion");

                if (potion.endsWith("empty")) {
                    result = NbtCheckResult.FAIL;
                    break;
                }
            }
        }

        return result;
    }

}
