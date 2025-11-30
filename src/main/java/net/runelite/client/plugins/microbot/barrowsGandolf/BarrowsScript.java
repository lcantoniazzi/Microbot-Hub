package net.runelite.client.plugins.microbot.barrowsGandolf;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.cache.Rs2GroundItemCache;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.coords.Rs2WorldArea;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItemModel;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.inventory.Rs2RunePouch;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.magic.Rs2CombatSpells;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spellbook;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class BarrowsScript extends Script {
    
    public static boolean inTunnels = false;
    public static boolean firstRun = false;

    private boolean shouldBank = false;
    private boolean shouldAttackSkeleton = false;
    private boolean varbitCheckEnabled = true;

    public static String WhoisTun = "Unknown";
    public Runes neededRune = null;

    private int tunnelLoopCount = 0;
    int scriptDelay = Rs2Random.between(300,600);
    public static int ChestsOpened = 0;
    private int minRuneAmt;

    long walkerDelay = Rs2Random.between(1000,2000);

    private WorldPoint FirstLoopTile;
    private final WorldPoint Chest = new WorldPoint(3552,9694,0);

    private Rs2PrayerEnum NeededPrayer;
    public static List<String> barrowsPieces = new ArrayList<>();
    private ScheduledFuture<?> WalkToTheChestFuture;

    private Rs2InventorySetup inventorySetupMagic;
    private Rs2InventorySetup inventorySetupRange;
    private final boolean debugLogging = true;
    private final

    public boolean run(BarrowsConfig config, BarrowsPlugin plugin) {
        if (debugLogging) Microbot.log("At the beginning of this script running");
        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();

                inventorySetupMagic = new Rs2InventorySetup(config.inventorySetupMagic().getName(), mainScheduledFuture);
                inventorySetupRange = new Rs2InventorySetup(config.inventorySetupRange().getName(), mainScheduledFuture);

                if(firstRun) {
                    if (debugLogging) Microbot.log("Is the first run");
                    inventorySetupMagic.wearEquipment();
                    if (!inventorySetupMagic.doesEquipmentMatch()) {
                        while(!inventorySetupMagic.doesEquipmentMatch() && !inventorySetupMagic.doesInventoryMatch()) {
                            if(!super.isRunning()){ break; }
                            if (Rs2Bank.getNearestBank().getWorldPoint().distanceTo(Rs2Player.getWorldLocation()) > 6) {
                                Rs2Bank.walkToBank();
                            }
                            if (Rs2Bank.getNearestBank().getWorldPoint().distanceTo(Rs2Player.getWorldLocation()) <= 6) {
                                inventorySetupMagic.loadEquipment();
                                inventorySetupMagic.loadInventory();
                            }
                        }
                    }
                    firstRun = false;
                    if (debugLogging) Microbot.log("Done first  setup");
                }

                if(barrowsPieces.isEmpty()) barrowsPieces.add("Nothing yet.");

                if(Rs2Player.getWorldLocation().getY() > 9600 && Rs2Player.getWorldLocation().getY() < 9730) {
                    if (debugLogging) Microbot.log("We are in the tunnels.");
                    inTunnels = true;
                } else {
                    if(tunnelLoopCount != 0){
                        //reset the tunnels loop counter
                        tunnelLoopCount = 0;
                    }
                    if (debugLogging) Microbot.log("We are not in the tunnels.");
                    inTunnels = false;
                }

                minRuneAmt = config.minRuneAmount();
                if(!Rs2Magic.getSpellbook().equals(Rs2Spellbook.MODERN)){
                    Microbot.log("You are not on the normal spellbook, shutting down.");
                    super.shutdown();
                }

                shouldAttackSkeleton = config.shouldGainRP();

                outOfSupplies(config);

                if (!inTunnels && !shouldBank && Rs2Player.getWorldLocation().distanceTo(new WorldPoint(3573, 3296, 0)) > 60) {
                    //needed to intercept the walker
                    if(Rs2Bank.isOpen()) Rs2Bank.closeBank();
                    if (Rs2GameObject.getGameObject(4525) == null) {
                        Rs2Magic.cast(MagicAction.TELEPORT_TO_HOUSE);
                        sleepUntil(() -> Rs2Player.getAnimation() == net.runelite.api.gameval.AnimationID.TELEPORT, Rs2Random.between(2000, 4000));
                        sleepUntil(() -> !Rs2Player.isAnimating(), Rs2Random.between(6000, 10000));
                        sleepUntil(() -> Rs2GameObject.getGameObject(4525) != null, Rs2Random.between(6000, 10000));
                    }
                    handlePOH(config);
                    return;
                }

                if(!inTunnels && !shouldBank) {
                    for (BarrowsBrothers brother : BarrowsBrothers.values()) {
                        Rs2WorldArea mound = brother.getHumpWP();
                        NeededPrayer = brother.whatToPray;
                        outOfSupplies(config);
                        if(shouldBank){
                            return;
                        }

                        stopFutureWalker();
                        closeBank();

                        setAutoCast(config);

                        Microbot.log("Checking mound for: " + brother.getName());

                        if(everyBrotherWasKilled()){
                            if(WhoisTun.equals("Unknown")){
                                Microbot.log("We're not sure who tunnel is, and every brother is dead. Checking all mounds manually");
                                varbitCheckEnabled = false;
                            }
                        } else {
                            if(!varbitCheckEnabled){
                                varbitCheckEnabled = true;
                            }
                        }

                        if(!WhoisTun.equals("Unknown")){
                            if(!varbitCheckEnabled){
                                varbitCheckEnabled = true;
                            }
                        }

                        //resume progress from varbits
                        if(varbitCheckEnabled) {
                            if (brother.name.contains("Dharok")) {
                                if (Microbot.getVarbitValue(Varbits.BARROWS_KILLED_DHAROK) == 1 || Objects.equals(WhoisTun, BarrowsBrothers.DHAROK.name)) {
                                    Microbot.log("We all ready killed Dharok or he is the tunnel.");
                                    continue;
                                }
                                inventorySetupMagic.wearEquipment();
                            }
                            if (brother.name.contains("Guthan") || Objects.equals(WhoisTun, BarrowsBrothers.GUTHAN.name)) {
                                if (Microbot.getVarbitValue(Varbits.BARROWS_KILLED_GUTHAN) == 1) {
                                    Microbot.log("We all ready killed Guthan.");
                                    continue;
                                }
                                inventorySetupMagic.wearEquipment();
                            }
                            if (brother.name.contains("Karil") || Objects.equals(WhoisTun, BarrowsBrothers.KARIL.name)) {
                                if (Microbot.getVarbitValue(Varbits.BARROWS_KILLED_KARIL) == 1) {
                                    Microbot.log("We all ready killed Karil.");
                                    continue;
                                }
                                inventorySetupMagic.wearEquipment();
                            }
                            if (brother.name.contains("Torag") || Objects.equals(WhoisTun, BarrowsBrothers.TORAG.name)) {
                                if (Microbot.getVarbitValue(Varbits.BARROWS_KILLED_TORAG) == 1) {
                                    Microbot.log("We all ready killed Torag.");
                                    continue;
                                }
                                inventorySetupMagic.wearEquipment();
                            }
                            if (brother.name.contains("Verac") || Objects.equals(WhoisTun, BarrowsBrothers.VERAC.name)) {
                                if (Microbot.getVarbitValue(Varbits.BARROWS_KILLED_VERAC) == 1) {
                                    Microbot.log("We all ready killed Verac.");
                                    continue;
                                }
                                inventorySetupMagic.wearEquipment();
                            }
                            if (brother.name.contains("Ahrim") || Objects.equals(WhoisTun, BarrowsBrothers.AHRIM.name)) {
                                if (Microbot.getVarbitValue(Varbits.BARROWS_KILLED_AHRIM) == 1) {
                                    Microbot.log("We all ready killed Ahrim.");
                                    continue;
                                }
                                inventorySetupRange.wearEquipment();
                            }
                        }

                        plugin.getLockCondition().lock();

                        //Enter mound
                        if (Rs2Player.getWorldLocation().getPlane() != 3) {
                            Microbot.log("Entering the mound");

                            handlePOH(config);

                            goToTheMound(mound);

                            digIntoTheMound(mound);

                        }

                        if (Rs2Player.getWorldLocation().getPlane() == 3) {
                            Microbot.log("We're in the mound");

                            if(config.shouldPrayAgainstWeakerBrothers()){
                                activatePrayer(brother.getWhatToPray());
                            } else {
                                if(!brother.getName().contains("Torag") && !brother.getName().contains("Guthan") && !brother.getName().contains("Verac")){
                                    activatePrayer(brother.getWhatToPray());
                                }
                            }

                            // we're in the mound, prayer is active
                            GameObject sarc = Rs2GameObject.getGameObject("Sarcophagus");
                            Rs2NpcModel currentBrother = null;
                            Microbot.log("Found the Sarcophagus");
                            while(currentBrother == null) {
                                Microbot.log("Searching the Sarcophagus");
                                if (!super.isRunning()) break;


                                if (Rs2GameObject.interact(sarc, "Search")) {
                                    sleepUntil(() -> Rs2Player.isMoving(), Rs2Random.between(1000, 3000));
                                    sleepUntil(() -> !Rs2Player.isMoving() || Rs2Player.isInCombat(), Rs2Random.between(3000, 6000));
                                    // the brother could take a second to spawn in.
                                    sleepUntil(() -> hintNpcModel() != null || Rs2Dialogue.isInDialogue(), Rs2Random.between(750, 1500));
                                }

                                if(Rs2Dialogue.isInDialogue() && Rs2Dialogue.hasDialogueText("You've found a hidden")){
                                    WhoisTun = brother.name;
                                    Microbot.log(brother.name+" is our tunnel");
                                    break;
                                }

                                if(hintNpcModel() != null) {
                                    currentBrother = hintNpcModel();
                                } else {
                                    break;
                                }

                                if (currentBrother != null) break;
                            }

                            checkForAndFightBrother(config);

                            if(brother.name.equals(WhoisTun) && brother.name.contains("Ahrim")) {
                                if (Rs2Dialogue.isInDialogue()) {
                                    dialogueEnterTunnels();
                                    return;
                                }
                            }

                            leaveTheMound();
                        }
                    }
                }

                if(!WhoisTun.equals("Unknown") && !shouldBank && !inTunnels){
                    int howManyBrothersWereKilled = Microbot.getVarbitValue(Varbits.BARROWS_KILLED_DHAROK) + Microbot.getVarbitValue(Varbits.BARROWS_KILLED_GUTHAN) + Microbot.getVarbitValue(Varbits.BARROWS_KILLED_KARIL) + Microbot.getVarbitValue(Varbits.BARROWS_KILLED_TORAG) + Microbot.getVarbitValue(Varbits.BARROWS_KILLED_VERAC) + Microbot.getVarbitValue(Varbits.BARROWS_KILLED_AHRIM);
                    if(howManyBrothersWereKilled <= 4){
                        Microbot.log("We seem to have missed someone, checking all mounds again.");
                        return;
                    } else {
                        Microbot.log("Going to the tunnels.");
                    }

                    stopFutureWalker();
                    for (BarrowsBrothers brother : BarrowsBrothers.values()) {
                        if (brother.name.equals(WhoisTun)) {
                            NeededPrayer = brother.getWhatToPray();

                            Rs2WorldArea tunnelMound = brother.getHumpWP();

                            handlePOH(config);

                            goToTheMound(tunnelMound);

                            digIntoTheMound(tunnelMound);

                            while(!Rs2Dialogue.isInDialogue()) {
                                GameObject sarc = Rs2GameObject.getGameObject("Sarcophagus");

                                if (!super.isRunning()) break;

                                if (Rs2GameObject.interact(sarc, "Search")) {
                                    sleepUntil(() -> Rs2Player.isMoving(), Rs2Random.between(1000, 3000));
                                    sleepUntil(() -> !Rs2Player.isMoving() || Rs2Player.isInCombat(), Rs2Random.between(3000, 6000));
                                    sleepUntil(() -> Rs2Dialogue.isInDialogue(), Rs2Random.between(3000, 6000));
                                }

                                if(Rs2Dialogue.isInDialogue()) break;

                                if (inTunnels) break;

                                if (Rs2Player.getWorldLocation().getPlane() != 3) break;

                                if(!Rs2Dialogue.isInDialogue()){
                                    //Somehow we got tun wrong.
                                    Microbot.log("We're in the wrong tunnel mound. Leaving...");
                                    this.leaveTheMound();
                                    WhoisTun = "Unknown";
                                    return;
                                }

                            }

                            dialogueEnterTunnels();

                            break;
                        }
                    }
                }


                if(inTunnels && !shouldBank) {
                    Microbot.log("In the tunnels");

                    if (Rs2Player.getQuestState(Quest.HIS_FAITHFUL_SERVANTS) != QuestState.FINISHED) {
                        Microbot.showMessage("Complete the 'His Faithful Servants' quest for the webwalker to function correctly");
                        shutdown();
                        return;
                    }

                    if(!varbitCheckEnabled) varbitCheckEnabled=true;


                    leaveTheMound();
                    stuckInTunsCheck();
                    solvePuzzle();
                    checkForAndFightBrother(config);
                    eatFood();
                    outOfSupplies(config);
                    gainRP(config);
                    lootChampionScroll();

                    if(!Rs2Player.isMoving()) startWalkingToTheChest();

                    solvePuzzle();
                    checkForAndFightBrother(config);

                    if(Rs2GameObject.findObjectById(20973) != null
                            && (Rs2GameObject.hasLineOfSight(Rs2GameObject.findObjectById(20973))
                            || Rs2Player.distanceTo(Rs2GameObject.findObjectById(20973).getWorldLocation()) < 4)){
                        //chest ID: 20973
                        stopFutureWalker();

                        TileObject chest = Rs2GameObject.findObjectById(20973);

                        if(Rs2GameObject.interact(chest, "Open")){
                            if (everyBrotherWasKilled()) {
                                sleep(1000);
                            } else {
                                sleepUntil(()-> (hintNpcModel()!=null && hintNpcModel().getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) <= 5), Rs2Random.between(4000,6000));
                            }
                        } else {
                            return;
                        }

                        checkForAndFightBrother(config);

                        if(hintNpcModel()==null) {
                            int io = 0;
                            while (io < 2) {

                                if (!super.isRunning()) {
                                    break;
                                }

                                if (Rs2GameObject.interact(chest, "Search")) {
                                    sleep(500, 1500);
                                }

                                if (Rs2Widget.hasWidget("Barrows chest")) {
                                    break;
                                }

                                io++;
                            }
                            //we looted the chest time to reset

                            suppliesCheck(config);

                            if(shouldBank){
                                Microbot.log("We should bank.");
                                ChestsOpened++;
                                WhoisTun = "Unknown";
                                inTunnels = false;
                            } else {
                                Rs2Magic.cast(MagicAction.TELEPORT_TO_HOUSE);
                                sleepUntil(() -> Rs2Player.getWorldLocation().getY() < 9600 || Rs2Player.getWorldLocation().getY() > 9730, Rs2Random.between(6000, 10000));
                                ChestsOpened++;
                                WhoisTun = "Unknown";
                                inTunnels = false;
                            }
                        }
                    }
                    tunnelLoopCount++;
                }

                if(shouldBank){
                    if(!Rs2Bank.isOpen()){
                        //stop the walker
                        stopFutureWalker();
                        //tele out
                        outOfSupplies(config);
                        //walk to and open the bank
                        Rs2Bank.walkToBankAndUseBank(BankLocation.FEROX_ENCLAVE);
                        //unlock
                        plugin.getLockCondition().unlock();
                        inventorySetupMagic.loadEquipment();
                        inventorySetupMagic.loadInventory();
                    } else {
                        plugin.getLockCondition().unlock();
                        inventorySetupMagic.loadEquipment();
                        inventorySetupMagic.loadInventory();

                        suppliesCheck(config);
                        reJfount();
                    }
                }

                scriptDelay = Rs2Random.between(200,750);
                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                System.out.println("Total time for loop " + totalTime);

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, scriptDelay, TimeUnit.MILLISECONDS);
        return true;
    }

    public void checkForWorldMap(){
        if(Rs2Widget.getWidget(38993938) != null){
            if(Rs2Widget.getWidget(38993938).getText().contains("Key")){
                Rs2Keyboard.keyPress(KeyEvent.VK_ESCAPE);
            }
        }
    }

    public void closeBank(){
        if(Rs2Bank.isOpen()){
            while(Rs2Bank.isOpen()) {
                if(!super.isRunning()){break;}
                if (Rs2Bank.closeBank()) sleepUntil(() -> !Rs2Bank.isOpen(), Rs2Random.between(2000, 4000));
            }
        }
    }

    public void handlePOH(BarrowsConfig config){
       if(Rs2GameObject.getGameObject(4525) != null){
            Microbot.log("We're in our POH");
            GameObject altar = Rs2GameObject.getGameObject("Altar", true);
            eatUp();
            if(altar != null && Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER) != Microbot.getClient().getRealSkillLevel(Skill.PRAYER)){
                if(Rs2GameObject.interact(altar)){
                    sleepUntil(()-> Rs2Player.isMoving(), Rs2Random.between(2000,4000));
                    sleepUntil(()-> Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER) == Microbot.getClient().getRealSkillLevel(Skill.PRAYER), Rs2Random.between(10000,15000));
                }
            }
            GameObject regularPortal = Rs2GameObject.getGameObject("Barrows Portal");
            if(regularPortal != null){
                while(Rs2GameObject.getGameObject(4525) != null){
                    if(!super.isRunning()){break;}
                    if(!Rs2Player.isMoving()){
                        if(Rs2GameObject.interact(regularPortal, "Enter")){
                            sleepUntil(()-> Rs2Player.isMoving(), Rs2Random.between(2000,4000));
                            sleepUntil(()-> !Rs2Player.isMoving(), Rs2Random.between(10000,15000));
                            sleepUntil(()-> Rs2GameObject.getGameObject("Barrows Portal") == null, Rs2Random.between(10000,15000));
                        }
                    }
                }

            } else {
                // we have a nexus 33410
                Microbot.log("No nexus support yet, shutting down");
                super.shutdown();
            }
        }
    }

    public boolean everyBrotherWasKilled(){
        if(Microbot.getVarbitValue(Varbits.BARROWS_KILLED_DHAROK) == 1&&Microbot.getVarbitValue(Varbits.BARROWS_KILLED_GUTHAN) == 1&&Microbot.getVarbitValue(Varbits.BARROWS_KILLED_KARIL) == 1&&
                Microbot.getVarbitValue(Varbits.BARROWS_KILLED_TORAG) == 1&&Microbot.getVarbitValue(Varbits.BARROWS_KILLED_VERAC) == 1&&Microbot.getVarbitValue(Varbits.BARROWS_KILLED_AHRIM) == 1){
            return true;
        }

        return false;
    }

    public void dialogueEnterTunnels(){
        if (Rs2Dialogue.isInDialogue()) {
            while(Rs2Dialogue.isInDialogue()) {
                if (!super.isRunning()) break;

                if (Rs2Dialogue.hasContinue()) {
                    Rs2Dialogue.clickContinue();
                    sleepUntil(() -> Rs2Dialogue.hasDialogueOption("Yeah I'm fearless!"), Rs2Random.between(2000, 5000));
                    sleep(300, 600);
                }
                if (Rs2Dialogue.hasDialogueOption("Yeah I'm fearless!")) {
                    if (Rs2Dialogue.clickOption("Yeah I'm fearless!")) {
                        sleepUntil(() -> Rs2Player.getWorldLocation().getY() > 9600 && Rs2Player.getWorldLocation().getY() < 9730, Rs2Random.between(2500, 6000));
                        //allow some time for the tunnel to load.
                        sleep(1000, 2000);
                        inTunnels = true;
                    }
                }
                if (!Rs2Dialogue.isInDialogue()) break;

                if (inTunnels) break;

                if (Rs2Player.getWorldLocation().getPlane() != 3) break;
            }
        }
    }

    public void digIntoTheMound(Rs2WorldArea moundArea){
        while (moundArea.contains(Rs2Player.getWorldLocation()) && Rs2Player.getWorldLocation().getPlane() != 3) {
            checkForWorldMap();

            if (!super.isRunning()) break;

            //antipattern turn on prayer early
            antiPatternEnableWrongPrayer();

            antiPatternActivatePrayer();
            //antipattern

            if (Rs2Inventory.contains("Spade")) {
                if (Rs2Inventory.interact("Spade", "Dig")) {
                    sleepUntil(() -> Rs2Player.getWorldLocation().getPlane() == 3, Rs2Random.between(3000, 5000));
                }
            }

            if (Rs2Player.getWorldLocation().getPlane() == 3) break;
        }
    }

    public void goToTheMound(Rs2WorldArea moundArea){
        while (!moundArea.contains(Rs2Player.getWorldLocation())) {
            checkForWorldMap();
            int totalTiles = moundArea.toWorldPointList().size();
            WorldPoint randomMoundTile;
            if (!super.isRunning()) break;

            //antipattern turn on prayer early
            antiPatternEnableWrongPrayer();

            antiPatternActivatePrayer();

            antiPatternDropVials();
            //antipattern

            // We're not in the mound yet.
            randomMoundTile = moundArea.toWorldPointList().get(Rs2Random.between(0,(totalTiles-1)));
            if(Rs2Walker.walkTo(randomMoundTile))
                sleepUntil(()-> !Rs2Player.isMoving(), Rs2Random.between(2000,4000));

            if (moundArea.contains(Rs2Player.getWorldLocation())) {
                if(!Rs2Player.isMoving()) break;

            } else {
                Microbot.log("At the mound, but we can't dig yet.");
                randomMoundTile = moundArea.toWorldPointList().get(Rs2Random.between(0,(totalTiles-1)));

                //strange old man body blocking us
                if(Rs2Npc.getNpc("Strange Old Man")!=null){
                    if(Rs2Npc.getNpc("Strange Old Man").getWorldLocation() != null){
                        if(Rs2Npc.getNpc("Strange Old Man").getWorldLocation() == randomMoundTile){
                            while(Rs2Npc.getNpc("Strange Old Man").getWorldLocation() == randomMoundTile){
                                if(!super.isRunning()){break;}
                                randomMoundTile = moundArea.toWorldPointList().get(Rs2Random.between(0,(totalTiles-1)));
                                sleep(250,500);
                            }
                        }
                    }
                }

                Rs2Walker.walkCanvas(randomMoundTile);
                sleepUntil(()-> !Rs2Player.isMoving(), Rs2Random.between(2000,4000));
            }
        }
    }

    public void leaveTheMound(){
        if(Rs2GameObject.getGameObject("Staircase", true) != null) {
            if (Rs2GameObject.hasLineOfSight(Rs2GameObject.getGameObject("Staircase", true))) {
                if (Rs2Player.getWorldLocation().getPlane() == 3) {
                    while (Rs2Player.getWorldLocation().getPlane() == 3) {
                        Microbot.log("Leaving the mound");
                        if (!super.isRunning()) break;

                        if (Rs2GameObject.interact("Staircase", "Climb-up")) {
                            sleepUntil(() -> Rs2Player.getWorldLocation().getPlane() != 3, Rs2Random.between(3000, 6000));
                        }

                        if (Rs2Player.getWorldLocation().getPlane() != 3) {
                            //anti pattern turn off prayer
                            disablePrayer();
                            //anti pattern turn off prayer
                            break;
                        }
                    }
                }
                if (inTunnels) inTunnels = false;
            }
        }
    }

    public void lootChampionScroll(){
        Rs2GroundItemModel championScroll = Rs2GroundItemCache.getClosestItemByGameId(ItemID.SKELETON_CHAMPION_SCROLL).stream().findFirst().orElse(null);
        if(championScroll != null){
            Tile scrollsTile = championScroll.getTile();
            if(championScroll.isClickable() && Rs2GroundItem.hasLineOfSight(scrollsTile)){
                while(Rs2GroundItemCache.getClosestItemByGameId(ItemID.SKELETON_CHAMPION_SCROLL).stream().findFirst().orElse(null) != null && !Rs2Inventory.contains(championScroll.getId())){
                    if(!super.isRunning()) break;

                    Rs2GroundItem.interact(championScroll.getName(), "Take");
                    sleepUntil(()-> !Rs2Player.isMoving() && Rs2Inventory.contains(championScroll.getId()), Rs2Random.between(4000,12000));
                }
            }
        }
    }

    public void gainRP(BarrowsConfig config){
        if(shouldAttackSkeleton){
            int RP = Microbot.getVarbitValue(Varbits.BARROWS_REWARD_POTENTIAL);
            if(RP>650) return;
            if(everyBrotherWasKilled()) return;

            Rs2NpcModel skele = Rs2Npc.getNpc("Skeleton");

            if(skele == null || skele.isDead()) return;

            if(Rs2Npc.hasLineOfSight(skele)){
                stopFutureWalker();

                if(!Rs2Player.isInCombat()){
                    if(Rs2Npc.attack(skele)){
                        sleepUntil(()-> Rs2Player.isInCombat()&&!Rs2Player.isMoving(), Rs2Random.between(4000,8000));
                    }
                }

                if(Rs2Player.isInCombat()){
                    Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true, true);
                    while(Rs2Player.isInCombat()){
                        Microbot.log("Fighting the Skeleton.");
                        if (!super.isRunning()) break;


                        stopFutureWalker();
                        sleep(750,1500);
                        eatFood();
                        outOfSupplies(config);
                        antiPatternDropVials();

                        if(shouldBank){
                            Microbot.log("Breaking out we're out of supplies.");
                            break;
                        }

                        if(!Rs2Player.isInCombat()){
                            Microbot.log("Breaking out we're no longer in combat.");
                            break;
                        }

                        if (skele != null && skele.isDead()) {
                            Microbot.log("Breaking out the skeleton is dead.");
                            break;
                        }

                        if(Microbot.getVarbitValue(Varbits.BARROWS_REWARD_POTENTIAL)>870){
                            Microbot.log("Breaking out we have enough RP.");
                            break;
                        }

                        if(hintNpcModel()!=null) {
                            Rs2NpcModel barrowsbrother = hintNpcModel();
                            if(Rs2Npc.hasLineOfSight(barrowsbrother)) {
                                Microbot.log("The brother is here.");
                                break;
                            }
                        }

                    }
                    Rs2Prayer.disableAllPrayers(true);
                }
            }
        }
    }

    public void suppliesCheck(BarrowsConfig config){
        if (debugLogging) Microbot.log("Checking supplies.");
        Rs2RunePouch.fullUpdate();
        var runePouch = Rs2RunePouch.getRunes();
        if (debugLogging) Microbot.log("Got rune pouch.");
        var neededRune = config.spellToUse().getRune();
        if (debugLogging) Microbot.log("Needed rune is " +  neededRune.name());
        var inventoryMissingRunes = Rs2Inventory.get(neededRune.name()) == null || Rs2Inventory.get(neededRune.name()).getQuantity() <= minRuneAmt;
        var pouchMissingRunes = !runePouch.containsKey(neededRune) || runePouch.get(neededRune) <= minRuneAmt;
        if (pouchMissingRunes && inventoryMissingRunes) {
            Microbot.log("We have less than " + minRuneAmt + " " + neededRune.name());
            shouldBank = true;
            if (debugLogging) Microbot.log("Need to bank for runes.");
            return;
        }

        if (!Rs2Inventory.contains("Spade")) {
            Microbot.log("We don't have a spade.");
            shouldBank = true;
            if (debugLogging) Microbot.log("Need to bank for spade.");
            return;
        }
        if (Rs2Inventory.count(config.food().getName()) < config.minFood()) {
            Microbot.log("We have less than "  + config.minFood() + " food.");
            shouldBank = true;
            if (debugLogging) Microbot.log("Need to bank for food.");
            return;
        }

        if(Rs2Player.getRunEnergy() <= 5){
            Microbot.log("We need more run energy ");
            shouldBank = true;
            if (debugLogging) Microbot.log("Need to bank for run energy.");
            return;
        }
        if (debugLogging) Microbot.log("No need to bank.");
        shouldBank = false;
    }

    public void stuckInTunsCheck(){
        //needed for rare occasions where the walker messes up
        if(tunnelLoopCount < 1){
            FirstLoopTile = Rs2Player.getWorldLocation();
        }
        if(tunnelLoopCount >= 15){
            WorldPoint currentTile = Rs2Player.getWorldLocation();
            if(currentTile!=null&&FirstLoopTile!=null){
                if(currentTile.equals(FirstLoopTile)){
                    Microbot.log("We seem to be stuck. Resetting the walker");
                    stopFutureWalker();
                    tunnelLoopCount = 0;
                }
            }
        }
        if(tunnelLoopCount >= 30) tunnelLoopCount = 0;
    }

    public void setAutoCast(BarrowsConfig config) {
        MagicAction spell = config.spellToUse().getSpell();
        if (Objects.equals(spell.getName(), "Wind Surge")){
            if (Rs2Magic.getCurrentAutoCastSpell() != Rs2CombatSpells.WIND_SURGE) {
                Rs2Combat.setAutoCastSpell(Rs2CombatSpells.WIND_SURGE, false);
            }
        } else if (Objects.equals(spell.getName(), "Wind Wave")){
            if (Rs2Magic.getCurrentAutoCastSpell() != Rs2CombatSpells.WIND_WAVE) {
                Rs2Combat.setAutoCastSpell(Rs2CombatSpells.WIND_WAVE, false);
            }
        } else if (Objects.equals(spell.getName(), "Wind Blast")) {
            if (Rs2Magic.getCurrentAutoCastSpell() != Rs2CombatSpells.WIND_BLAST) {
                Rs2Combat.setAutoCastSpell(Rs2CombatSpells.WIND_BLAST, false);
            }
        }
    }

    public void activatePrayer(Rs2PrayerEnum prayer){
        if(!Rs2Prayer.isPrayerActive(prayer)){
            Microbot.log("Turning on Prayer.");
            drinkPrayerPot();
            Rs2Prayer.toggle(prayer, true, true);
        }
    }
    public void antiPatternEnableWrongPrayer(){
        if(!Rs2Prayer.isPrayerActive(NeededPrayer)){
            if(Rs2Random.between(0,100) <= Rs2Random.between(1,4)) {
                Rs2PrayerEnum wrongPrayer = null;
                int random = Rs2Random.between(0,100);
                if(random <= 50) wrongPrayer = Rs2PrayerEnum.PROTECT_MELEE;

                if(random > 50 && random < 75) wrongPrayer = Rs2PrayerEnum.PROTECT_RANGE;

                if(random >= 75) wrongPrayer = Rs2PrayerEnum.PROTECT_MAGIC;

                drinkPrayerPot();
                Rs2Prayer.toggle(wrongPrayer, true, true);
                sleep(0, 750);
            }
        }
    }
    public void antiPatternActivatePrayer(){
        if(!Rs2Prayer.isPrayerActive(NeededPrayer)){
            if(Rs2Random.between(0,100) <= Rs2Random.between(1,8)) {
                drinkPrayerPot();
                Rs2Prayer.toggle(NeededPrayer, true, true);
                sleep(0, 750);
            }
        }
    }
    public void antiPatternDropVials(){
        if(Rs2Random.between(0,100) <= Rs2Random.between(1,25)) {
            Rs2ItemModel whatToDrop = Rs2Inventory.get(it->it!=null&&it.getName().contains("Vial")||it.getName().contains("Butterfly jar"));
            if(whatToDrop!=null) {
                if (Rs2Inventory.contains(whatToDrop.getName())) {
                    if (Rs2Inventory.drop(whatToDrop.getName())) sleep(0, 750);
                }
            }
        }
    }
    public void outOfSupplies(BarrowsConfig config){
        suppliesCheck(config);
        // Needed because the walker won't teleport to the enclave while in the tunnels or in a barrow
        if(shouldBank && (inTunnels || Rs2Player.getWorldLocation().getPlane() == 3)){
            if(Rs2Magic.cast(MagicAction.TELEPORT_TO_HOUSE)){
                Microbot.log("We're out of supplies. Teleporting.");
                if(inTunnels) inTunnels=false;
                sleepUntil(() -> Rs2Player.isAnimating(), Rs2Random.between(2000, 4000));
                sleepUntil(() -> !Rs2Player.isAnimating(), Rs2Random.between(6000, 10000));
            }
        }
    }
    public void disablePrayer(){
        if(Rs2Random.between(0,100) >= Rs2Random.between(0,5)) {
            Rs2Prayer.disableAllPrayers(true);
            sleep(0,750);
        }
    }
    public void reJfount(){
        int rejat = Rs2Random.between(10,30);
        int runener = Rs2Random.between(50,65);
        while(Rs2Player.getBoostedSkillLevel(Skill.PRAYER) < rejat || Rs2Player.getRunEnergy() <= runener){
            if (!super.isRunning()) break;

            if(Rs2Bank.isOpen()){
                if(Rs2Bank.closeBank()) sleepUntil(()-> !Rs2Bank.isOpen(), Rs2Random.between(2000,4000));

            } else {
                GameObject rej = Rs2GameObject.getGameObject("Pool of Refreshment", true);
                if(rej == null) break;
                Microbot.log("Drinking");
                if(Rs2GameObject.interact(rej, "Drink")){
                    sleepUntil(()-> Rs2Player.isMoving(), Rs2Random.between(1000,3000));
                    sleepUntil(()-> !Rs2Player.isMoving(), Rs2Random.between(5000,10000));
                    sleepUntil(()-> Rs2Player.isAnimating(), Rs2Random.between(1000,4000));
                    sleepUntil(()-> !Rs2Player.isAnimating(), Rs2Random.between(1000,4000));
                }
            }

            if(Rs2Player.getBoostedSkillLevel(Skill.PRAYER) >= rejat && Rs2Player.getRunEnergy() >= runener) break;
        }
    }
    public void drinkPrayerPot(){
        boolean skipThePot = true;
        if(hintNpcModel() != null && !hintNpcModel().getName().contains("Dharok") && hintNpcModel().getHealthPercentage() < Rs2Random.between(40,50)) skipThePot = true;

        if(!skipThePot) {
            if (Rs2Player.getBoostedSkillLevel(Skill.PRAYER) <= Rs2Random.between(8, 15)) {
                if (Rs2Inventory.contains(it -> it != null && it.getName().contains("Prayer potion") || it.getName().contains("Moonlight moth"))) {
                    Rs2ItemModel prayerpotion = Rs2Inventory.get(it -> it != null && it.getName().contains("Prayer potion") || it.getName().contains("Moonlight moth"));
                    String action = "Drink";
                    if (prayerpotion.getName().equals("Moonlight moth")) action = "Release";

                    if (Rs2Inventory.interact(prayerpotion, action)) sleep(0, 750);
                }
            }
        }
    }

    public Rs2NpcModel hintNpcModel(){
        Optional<NPC> hintNpc = Microbot.getClientThread().runOnClientThreadOptional(
                () -> Microbot.getClient().getHintArrowNpc()
        );

        if(hintNpc.isPresent()) return new Rs2NpcModel(hintNpc.get());

        return null;
    }

    public void checkForAndFightBrother(BarrowsConfig config){
        if (hintNpcModel() != null) {
            stopFutureWalker();
            Rs2PrayerEnum neededprayer = Rs2PrayerEnum.PROTECT_MELEE;
            if (hintNpcModel() != null && Rs2Npc.hasLineOfSight(hintNpcModel())) {
                if(Objects.requireNonNull(hintNpcModel().getName()).contains("Ahrim")) {
                    neededprayer = Rs2PrayerEnum.PROTECT_MAGIC;
                    inventorySetupRange.wearEquipment();
                }

                if(Objects.requireNonNull(hintNpcModel().getName()).contains("Karil")) neededprayer = Rs2PrayerEnum.PROTECT_RANGE;

                while(hintNpcModel() != null){
                    Microbot.log("Fighting the brother.");

                    if (!super.isRunning()) break;


                    if(inTunnels) {
                        if (!Rs2Npc.hasLineOfSight(hintNpcModel())) {
                            Microbot.log("No LOS!");
                            break;
                        }
                    }

                    if(config.shouldPrayAgainstWeakerBrothers()){
                        activatePrayer(neededprayer);
                    } else {
                        if(!Objects.requireNonNull(hintNpcModel().getName()).contains("Torag") && !Objects.requireNonNull(hintNpcModel().getName()).contains("Guthan") && !Objects.requireNonNull(hintNpcModel().getName()).contains("Verac")){
                            activatePrayer(neededprayer);
                        }
                    }

                    if(hintNpcModel() != null && Rs2Player.getInteracting() != null && !Objects.equals(Rs2Player.getInteracting().getName(), hintNpcModel().getName())){
                        if(Rs2Npc.interact(hintNpcModel(), "Attack")){
                            sleepUntil(Rs2Player::isInCombat, Rs2Random.between(3000,6000));
                        }
                    } else {
                        if(!Rs2Player.isInCombat()){
                            if(Rs2Npc.interact(hintNpcModel(), "Attack")){
                                sleepUntil(Rs2Player::isInCombat, Rs2Random.between(3000,6000));
                            }
                        }
                    }

                    sleep(750,1500);
                    eatFood();
                    outOfSupplies(config);

                    if(hintNpcModel() == null) {
                        Microbot.log("Breaking out the brother is null.");
                        disablePrayer();
                        inventorySetupMagic.wearEquipment();
                        break;
                    }

                    if(hintNpcModel().isDead()){
                        Microbot.log("Breaking out the brother is dead.");
                        disablePrayer();
                        inventorySetupMagic.wearEquipment();
                        sleepUntil(()-> hintNpcModel() == null, Rs2Random.between(3000,6000));
                        break;
                    }
                }
                eatUp();
            }
        }
    }

    public void stopFutureWalker(){
        if(WalkToTheChestFuture!=null) {
            Rs2Walker.setTarget(null);
            WalkToTheChestFuture.cancel(true);
            //stop the walker and future
        }
    }

    private void walkToChest(){
        try {
            if (!inTunnels) {
                WalkToTheChestFuture.cancel(true);
                return;
            }

            Rs2Walker.walkTo(Chest);
        } catch (Exception e) {
            Microbot.log("walkToChest failed: " + e.getMessage());
        }
    }

    private void startWalkingToTheChest() {
        if(WalkToTheChestFuture != null && !WalkToTheChestFuture.isCancelled() && !WalkToTheChestFuture.isDone()) {
            return;
        }

        if(inTunnels) {
            WalkToTheChestFuture = scheduledExecutorService.scheduleWithFixedDelay(
                    this::walkToChest,
                    0,
                    walkerDelay,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public void eatFood(){
        if(Rs2Player.getHealthPercentage() <= 40){
            if(Rs2Inventory.contains(it->it!=null&&it.isFood())){
                Rs2ItemModel food = Rs2Inventory.get(it->it!=null&&it.isFood());
                if(Rs2Inventory.interact(food, "Eat")){
                    sleep(0,750);
                }
            }
        }
    }

    public void eatUp(){
        while(Rs2Player.getBoostedSkillLevel(Skill.HITPOINTS) < Rs2Player.getRealSkillLevel(Skill.HITPOINTS)){
            if(Rs2Inventory.contains(it->it!=null&&it.isFood())){
                Rs2ItemModel food = Rs2Inventory.get(it->it!=null&&it.isFood());
                if ((15 + Rs2Player.getBoostedSkillLevel(Skill.HITPOINTS)) <= Rs2Player.getRealSkillLevel(Skill.HITPOINTS)) {
                    Rs2Inventory.interact(food, "Eat");
                    sleep(0, 750);
                } else {
                    break;
                }
            }
        }
    }

    public void solvePuzzle(){
        //correct model ids are  6725, 6731, 6713, 6719
        //widget ids are 1638413, 1638415,1638417
        boolean stoppedTheWalker = false;

        int widgets[] = {1638413, 1638415, 1638417};
        int modelIDs[] = {6725, 6731, 6713, 6719};
        int random = Rs2Random.between(0,1000);
        int secondRandom = Rs2Random.between(1,10);

        sleepUntil(()-> Rs2Widget.getWidget(widgets[0]) != null ||
                Rs2Widget.getWidget(widgets[1]) != null ||
                Rs2Widget.getWidget(widgets[2]) != null, Rs2Random.between(300,800));

        for (int widget : widgets) {
            if(!super.isRunning()) break;

            if(Rs2Widget.getWidget(widget)!=null){
                if(!stoppedTheWalker){
                    stopFutureWalker();
                    stoppedTheWalker = true;
                }
                for (int modelID : modelIDs) {
                    if(!super.isRunning()) break;

                    if(Rs2Widget.getWidget(widget).getModelId() == modelID || random <= secondRandom){
                        Microbot.log("Solution found");
                        Rs2Widget.clickWidget(widget);
                        break;
                    }
                }
            } else {
                break;
            }
        }

    }


    public enum BarrowsBrothers {
        DHAROK ("Dharok the Wretched", new Rs2WorldArea(3573,3296,3,3,0), Rs2PrayerEnum.PROTECT_MELEE),
        GUTHAN ("Guthan the Infested", new Rs2WorldArea(3575,3280,3,3,0), Rs2PrayerEnum.PROTECT_MELEE),
        KARIL  ("Karil the Tainted", new Rs2WorldArea(3564,3274,3,3,0), Rs2PrayerEnum.PROTECT_RANGE),
        TORAG  ("Torag the Corrupted", new Rs2WorldArea(3552,3282,2,2,0), Rs2PrayerEnum.PROTECT_MELEE),
        VERAC  ("Verac the Defiled", new Rs2WorldArea(3556,3297,3,3,0), Rs2PrayerEnum.PROTECT_MELEE),
        AHRIM  ("Ahrim the Blighted", new Rs2WorldArea(3563,3288,3,3,0), Rs2PrayerEnum.PROTECT_MAGIC);

        private String name;

        private Rs2WorldArea humpWP;

        private Rs2PrayerEnum whatToPray;


        BarrowsBrothers(String name, Rs2WorldArea humpWP, Rs2PrayerEnum whatToPray) {
            this.name = name;
            this.humpWP = humpWP;
            this.whatToPray = whatToPray;
        }

        public String getName() { return name; }
        public Rs2WorldArea getHumpWP() { return humpWP; }
        public Rs2PrayerEnum getWhatToPray() { return whatToPray; }

    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
