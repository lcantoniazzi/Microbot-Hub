package net.runelite.client.plugins.microbot.moonsofperil;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.moonsofperil.enums.Widgets;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class Utilities {
    public static void changeAttackStyle(String style) {
        if (Rs2Tab.getCurrentTab() != InterfaceTab.COMBAT) {
            Rs2Tab.switchTo(InterfaceTab.COMBAT);
            sleepUntil(() -> Rs2Tab.getCurrentTab() == InterfaceTab.COMBAT, 2000);
        }
        if (style.equals("Slash")) {
            Microbot.log("Current attack style is " + Rs2Combat.getWeaponAttackStyle());
            Rs2Widget.clickWidget(Widgets.ZOMBIE_AXE_SLASH.ID);
        } else {
            Microbot.log("Current attack style is " + Rs2Combat.getWeaponAttackStyle());
            Rs2Widget.clickWidget(Widgets.ZOMBIE_AXE_CRUSH.ID);
        }
    }
}
