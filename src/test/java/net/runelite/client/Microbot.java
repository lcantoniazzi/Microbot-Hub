package net.runelite.client;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.runelite.client.plugins.microbot.agility.MicroAgilityPlugin;
import net.runelite.client.plugins.devtools.DevToolsPlugin;
import net.runelite.client.plugins.loginscreen.LoginScreenPlugin;
import net.runelite.client.plugins.microbot.accountselector.AutoLoginPlugin;
import net.runelite.client.plugins.microbot.barrowsGandolf.BarrowsPlugin;
import net.runelite.client.plugins.microbot.birdhouseruns.FornBirdhouseRunsPlugin;
import net.runelite.client.plugins.microbot.moonsofperil.MoonsOfPerilPlugin;
import net.runelite.client.plugins.microbot.qualityoflife.QoLPlugin;
import net.runelite.client.plugins.microbot.thieving.ThievingPlugin;

public class Microbot
{

	private static final Class<?>[] debugPlugins = {
		ThievingPlugin.class,
            FornBirdhouseRunsPlugin.class,
            LoginScreenPlugin.class,
            AutoLoginPlugin.class,
            QoLPlugin.class,
            DevToolsPlugin.class,
            MoonsOfPerilPlugin.class,
            MicroAgilityPlugin.class,
            BarrowsPlugin.class,
	};

    public static void main(String[] args) throws Exception
    {
		List<Class<?>> _debugPlugins = Arrays.stream(debugPlugins).collect(Collectors.toList());
        RuneLiteDebug.pluginsToDebug.addAll(_debugPlugins);
        RuneLiteDebug.main(args);
    }
}
