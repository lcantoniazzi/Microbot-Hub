package net.runelite.client.plugins.microbot.barrowsGandolf;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.inventorysetups.InventorySetup;
import net.runelite.client.plugins.microbot.mmcaves.enums.MagicSpell;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import net.runelite.client.plugins.microbot.util.magic.Spell;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

@ConfigGroup("barrows")
@ConfigInformation("1. Have an inventory setup for magic & range (if using a separate setup for Ahrim) <br><br> 2. Required items: house teleport runes, food, Catalyic runes, and a spade.<br /><br /> 3. Spells: Wind: Blast, Wave, and Surge.")
public interface BarrowsConfig extends Config {
    @ConfigItem(
            keyName = "inventorySetupMagic",
            name = "Inventory Setup Magic",
            description = "Magic Inventory Setup to use for Barrows",
            position = 0
    )
    default InventorySetup inventorySetupMagic() { return null; }
    @ConfigItem(
            keyName = "inventorySetupRange",
            name = "Inventory Setup Range",
            description = "Range Inventory Setup to use for Barrows",
            position = 1
    )
    default InventorySetup inventorySetupRange() { return null; }
    @ConfigItem(
            keyName = "Food",
            name = "Food",
            description = "type of food",
            position = 2
    )
    default Rs2Food food()
    {
        return Rs2Food.KARAMBWAN;
    }

    @ConfigItem(
            keyName = "minFood",
            name = "Min Food",
            description = "Minimum amount of food to have for a run.",
            position = 3
    )
    @Range(min = 1, max = 28)
    default int minFood() {
        return 5;
    }

    @ConfigItem(
            keyName = "spellToUse",
            name = "Spell To Use",
            description = "What spell should be used when fighting the brothers?",
            position = 5
    )
    default SpellOption spellToUse() {
        return SpellOption.WIND_WAVE; // Default selection
    }

    enum SpellOption {
        WIND_SURGE(MagicAction.WIND_SURGE, Runes.WRATH),
        WIND_WAVE(MagicAction.WIND_WAVE, Runes.BLOOD),
        WIND_BLAST(MagicAction.WIND_BLAST, Runes.DEATH);

        @Getter
        private final MagicAction spell;
        @Getter
        private final Runes rune;

        SpellOption(MagicAction spell, Runes rune) {
            this.spell = spell;
            this.rune = rune;
        }
    }

    @ConfigItem(
            keyName = "minRuneAmount",
            name = "Min Runes",
            description = "Minimum amount of runes before banking",
            position = 5
    )
    @Range(min = 50, max = 10000)
    default int minRuneAmount() {
        return 180;
    }

    @ConfigItem(
            keyName = "shouldGainRP",
            name = "Aim for 86+% rewards potential",
            description = "Should we gain additional RP other than the barrows brothers?",
            position = 6
    )
    default boolean shouldGainRP() {
        return false;
    }

    @ConfigItem(
            keyName = "shouldPrayAgainstWeakerBrothers",
            name = "Pray against Torag, Verac, and Guthans?",
            description = "Should we Pray against Torag, Verac, and Guthans?",
            position = 7
    )
    default boolean shouldPrayAgainstWeakerBrothers() {
        return true;
    }

}
