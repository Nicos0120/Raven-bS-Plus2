package keystrokesmod.module.impl.combat;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.minigames.SkyWars;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityGiantZombie;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.util.EnumFacing.DOWN;


public class KillAura extends Module {
    public boolean blocked;
    private SliderSetting aps;
    public SliderSetting autoBlockMode;
    private SliderSetting fov;
    private SliderSetting attackRange;
    private SliderSetting swingRange;
    private SliderSetting blockRange;
    public SliderSetting rotationMode;
    public SliderSetting rotateMode;
    private SliderSetting rotationSmoothing;
    private SliderSetting sortMode;
    private SliderSetting switchDelay;
    private SliderSetting targets;
    private ButtonSetting attackMobs;
    private ButtonSetting targetInvis;
    private ButtonSetting disableInInventory;
    private ButtonSetting disableWhileBlocking;
    private ButtonSetting disableWhileMining;
    private ButtonSetting hitThroughBlocks;
    private ButtonSetting ignoreTeammates;
    public ButtonSetting manualBlock;
    private ButtonSetting prioritizeEnemies;
    private ButtonSetting requireMouseDown;
    private ButtonSetting silentSwing;
    private ButtonSetting weaponOnly;

    private String[] autoBlockModes = new String[] { "Manual", "Vanilla", "Partial", "Blink", "Swap" };
    private String[] rotationModes = new String[] { "Silent", "Lock view", "None" };
    private String[] rotateModes = new String[] { "Attack", "Swing" };
    private String[] sortModes = new String[] { "Distance", "Health", "Hurttime", "Yaw" };

    // autoblock related
    private String[] swapBlacklist = { "compass", "snowball", "spawn", "skull" };

    // target variables
    public static EntityLivingBase target;
    public static EntityLivingBase attackingEntity;
    private HashMap<Integer, Integer> hitMap = new HashMap<>(); // entity id, ticks existed client
    private List<Entity> hostileMobs = new ArrayList<>();
    private Map<Integer, Boolean> golems = new HashMap<>(); // entity id, is teammate
    private double attackv = 0.006;

    // blocking related
    public boolean blockingClient;
    public boolean blockingServer;
    private int interactTicks;
    private boolean partialDown;
    private int partialTicks;
    private boolean wasUsing;
    private boolean hasAutoblocked;
    private boolean hasBlocked;

    // blink related
    private boolean swapped;
    public boolean blink;

    // other
    private long lastTime = 0L;
    private long delay;
    private boolean shouldAttack;
    private int previousAutoBlockMode;
    private boolean reset;
    private boolean rotated;
    private boolean sendUnBlock;
    private int delayTicks = 0;
    private boolean lastPressedLeft;
    private boolean lastPressedRight;
    public boolean stoppedTargeting; // can only be true for 1 tick
    public boolean targeting, rotating;
    private int cycle;
    public int sAttacked;
    private int lastSet;
    private boolean hasTargeted;
    public boolean t;
    private int getTicks;

    public KillAura() {
        super("KillAura", category.combat);
        this.registerSetting(aps = new SliderSetting("APS", 16.0, 1.0, 20.0, 0.5));
        this.registerSetting(autoBlockMode = new SliderSetting("Autoblock", 0, autoBlockModes));
        this.registerSetting(fov = new SliderSetting("FOV", 360.0, 30.0, 360.0, 4.0));
        this.registerSetting(attackRange = new SliderSetting("Range (attack)", 3.0, 3.0, 6.0, 0.05));
        this.registerSetting(swingRange = new SliderSetting("Range (swing)", 3.3, 3.0, 8.0, 0.05));
        this.registerSetting(blockRange = new SliderSetting("Range (block)", 6.0, 3.0, 12.0, 0.05));
        this.registerSetting(rotationMode = new SliderSetting("Rotation mode", 0, rotationModes));
        this.registerSetting(rotateMode = new SliderSetting("Rotate on", 0, rotateModes));
        this.registerSetting(rotationSmoothing = new SliderSetting("Rotation smoothing", 0, 0, 10, 1));
        this.registerSetting(sortMode = new SliderSetting("Sort mode", 0, sortModes));
        this.registerSetting(switchDelay = new SliderSetting("Switch delay", "ms", 200.0, 50.0, 1000.0, 25.0));
        this.registerSetting(targets = new SliderSetting("Targets", 3.0, 1.0, 10.0, 1.0));
        this.registerSetting(targetInvis = new ButtonSetting("Target invis", true));
        this.registerSetting(attackMobs = new ButtonSetting("Attack mobs", false));
        this.registerSetting(disableInInventory = new ButtonSetting("Disable in inventory", true));
        this.registerSetting(disableWhileBlocking = new ButtonSetting("Disable while blocking", false));
        this.registerSetting(disableWhileMining = new ButtonSetting("Disable while mining", false));
        this.registerSetting(hitThroughBlocks = new ButtonSetting("Hit through blocks", true));
        this.registerSetting(ignoreTeammates = new ButtonSetting("Ignore teammates", true));
        this.registerSetting(manualBlock = new ButtonSetting("Manual block", false)); // does absolutely nothing
        this.registerSetting(prioritizeEnemies = new ButtonSetting("Prioritize enemies", false));
        this.registerSetting(requireMouseDown = new ButtonSetting("Require mouse down", false));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing while blocking", false));
        this.registerSetting(weaponOnly = new ButtonSetting("Weapon only", false));
    }

    @Override
    public String getInfo() {
        if (rotationMode.getInput() == 2) { // None, return FOV if rotation mode is none
            return String.valueOf((int) this.fov.getInput());
        }
        return rotationModes[(int) rotationMode.getInput()];
    }

    @Override
    public void onEnable() {
        if (rotationMode.getInput() == 0 && autoBlockMode.getInput() <= 1) {
            delayTicks = 1;
        }
    }

    @Override
    public void onDisable() {
        hitMap.clear();
        lastSet = 0;
        if (autoBlockOverride()) { // interact autoblock
            resetAutoblock(true);
        }
        blink = false;
        interactTicks = 0;
        setTarget(null);
        if (rotated || reset) {
            resetYaw();
        }
        rotated = false;
        swapped = false;
        partialTicks = 0;
        delayTicks = 0;
        sAttacked = 0;
        t = false;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        targeting = false;
        wasUsing = mc.gameSettings.keyBindUseItem.isKeyDown();

        if (!Utils.holdingSword()) {
            sendUnBlock = false;
            if (hasTargeted) {
                if (target == null && Utils.tabbedIn()) {
                    if (Mouse.isButtonDown(1)) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                    }
                }
            }
        }
        hasTargeted = false;

        if (autoBlockMode.getInput() == 3) {
            if (target == null || !manualBlock() && manualBlock.isToggled()) {
                if (ModuleUtils.swapTick == 0 && !ModuleUtils.isBlocked) {
                    interactTicks = 1;
                    getTicks = getAPSToTicks(10);
                } else {
                    interactTicks = 0;
                }
            }
        }
        if (mc.currentScreen == null || mc.currentScreen.allowUserInput) {
            boolean pressedLeft = Mouse.isButtonDown(0);
            if (pressedLeft && !lastPressedLeft) {
                onCustomMouse(0, true);
            }
            if (!pressedLeft && lastPressedLeft) {
                onCustomMouse(0, false);
            }
            boolean pressedRight = Mouse.isButtonDown(1);
            if (pressedRight && !lastPressedRight) {
                onCustomMouse(1, true);
            }
            if (!pressedRight && lastPressedRight) {
                onCustomMouse(1, false);
            }
            lastPressedRight = pressedRight;
            lastPressedLeft = pressedLeft;
        }

        if (!basicCondition() || !settingCondition()) {
            setTarget(null);
        }
        if (reset) {
            resetYaw();
            reset = false;
        }
        delayTicks--;
        if (delayTicks >= 0) {
            return;
        }
        if (sendUnBlock) {
            sendUnBlock = false;
            if (Raven.packetsHandler.C07.sentCurrentTick.get()) {
                return;
            }
            sendDigPacket();
            return;
        }
        if (target == null) {
            resetAutoblock(true);
            return;
        }
        if (ModuleManager.bedAura.stopAutoblock) {
            resetAutoblock(false);
            return;
        }
        double distanceToBB = getDistanceToBoundingBox(target);
        boolean inBlockRange = distanceToBB <= blockRange.getInput();
        if (!autoBlockOverride() || !inBlockRange || (!manualBlock() && manualBlock.isToggled())) { // regular swing & attack if autoblock isnt overriding or isnt in autoblock range
            handleSwingAndAttack(distanceToBB, false);
            resetAutoblock(true);
            interactTicks = 0;
        }
        else if (inBlockRange && autoBlockOverride() && manualBlock()) {
            handleAutoBlock(distanceToBB);
        }
        else if ((autoBlockOverride() && !Utils.holdingSword()) || !inBlockRange || !manualBlock()) { // for autoblocks
            resetAutoblock(true);
            interactTicks = 0;
        }
        if (inBlockRange) {
            handleBlocking();
        }
        if (mc.currentScreen == null || mc.currentScreen.allowUserInput) {
            boolean pressedRight = Mouse.isButtonDown(1);
            if (pressedRight && !lastPressedRight) {
                onCustomMouse(1, true);
            }
            if (!pressedRight && lastPressedRight) {
                onCustomMouse(1, false);
            }
            lastPressedRight = pressedRight;
        }
        targeting = true;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onClientRotation(ClientRotationEvent e) {
        rotating = false;
        handleTarget();
        if (delayTicks >= 0) {
            if (rotated) {
                resetYaw(e);
            }
            return;
        }
        if (!basicCondition() || !settingCondition()) {
            setTarget(null);
            if (rotated) {
                resetYaw(e);
            }
            return;
        }
        if (target == null) {
            if (rotated) {
                resetYaw(e);
            }
            return;
        }
        if (ModuleManager.bedAura.stopAutoblock) {
            if (rotated) {
                resetYaw(e);
            }
            return;
        }
        if (rotationMode.getInput() != 2) {
            if (rotateMode.getInput() == 0 && inRange(target, attackRange.getInput() - attackv) || rotateMode.getInput() == 1 && inRange(target, swingRange.getInput())) {
                float[] rotations = RotationUtils.getRotations(target, RotationUtils.prevRenderYaw, RotationUtils.prevRenderPitch);
                float[] smoothedRotations = getRotationsSmoothed(rotations);
                if (rotationMode.getInput() == 0) { // silent
                    e.yaw = smoothedRotations[0];
                    e.pitch = smoothedRotations[1];
                    rotated = true;
                    rotating = true;
                }
                else {
                    mc.thePlayer.rotationYaw = smoothedRotations[0];
                    mc.thePlayer.rotationPitch = smoothedRotations[1];
                    rotating = true;
                }
            }
            else if (rotationMode.getInput() == 0) {
                if (rotated) {
                    reset = true;
                    e.yaw = RotationUtils.serverRotations[0];
                    e.pitch = RotationUtils.serverRotations[1];
                    rotated = false;
                    rotating = true;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        Packet packet = e.getPacket();
        if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement) e.getPacket();
            if (delayTicks >= 0) {
                if (p.getStack() != null && p.getStack().getItem() instanceof ItemSword && p.getPlacedBlockDirection() != 255) {
                    e.setCanceled(true);
                }
            }
        }
    }

    @Override
    public void onUpdate() {
        if (rotationMode.getInput() == 1 && target != null) {
            if (inRange(target, attackRange.getInput() - attackv)) {
                float[] rotations = RotationUtils.getRotations(target, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                float[] smoothedRotations = getRotationsSmoothed(rotations);
                mc.thePlayer.rotationYaw = smoothedRotations[0];
                mc.thePlayer.rotationPitch = smoothedRotations[1];
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            if (System.currentTimeMillis() - this.lastTime >= delay && target != null) {
                this.lastTime = System.currentTimeMillis();
                updateAttackDelay();
                if (target != null) {
                    shouldAttack = true;
                }
                if (rotationMode.getInput() == 0) {
                    //mc.thePlayer.renderArmYaw = mc.thePlayer.rotationYaw;
                    //mc.thePlayer.prevRenderArmYaw = mc.thePlayer.rotationYaw;
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.button == 0 || e.button == 1) {
            if (!Utils.holdingWeapon() || target == null || !settingCondition()) {
                return;
            }
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onScrollSlot(PreSlotScrollEvent e) {
        int slot = e.slot;
        slot = (slot > 0) ? 1 : ((slot < 0) ? -1 : 0);
        slot = Math.floorMod(mc.thePlayer.inventory.currentItem - slot, 9);
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        if (stack != null && stack.getItem() instanceof ItemSword && wasUsing && Utils.lookingAtBlock()) {
            onSwapSlot();
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7Scroll swap detected, setting delay to &b" + delayTicks + "&7. (&d" + mc.thePlayer.ticksExisted + "&7)");
            }
        }
    }

    @SubscribeEvent
    public void onSlotUpdate(SlotUpdateEvent e) {
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(e.slot);
        if (stack != null && stack.getItem() instanceof ItemSword && wasUsing && Utils.lookingAtBlock()) {
            onSwapSlot();
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7Swap detected, setting delay to &b" + delayTicks + "&7. (&d" + mc.thePlayer.ticksExisted + "&7)");
            }
        }
    }

    @SubscribeEvent
    public void onSetAttackTarget(LivingSetAttackTargetEvent e) {
        if (e.entity != null && !hostileMobs.contains(e.entity)) {
            if (!(e.target instanceof EntityPlayer) || !e.target.getName().equals(mc.thePlayer.getName())) {
                return;
            }
            hostileMobs.add(e.entity);
        }
        if (e.target == null && hostileMobs.contains(e.entity)) {
            hostileMobs.remove(e.entity);
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7mob stopped attack player");
            }
        }
    }

    public void onSwapSlot() {
        delayTicks = 1;
        if (autoBlockMode.getInput() > 0 && !manualBlock()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        }
    }

    public void onCustomMouse(int button, boolean state) {
        if (autoBlockOverride()) {
            return;
        }
        if (button == 1) {
            if (state) {
                if (target != null) {
                    if (basicCondition() && settingCondition()) {
                        if (!ModuleManager.bedAura.breakTick) {
                            if (isLookingAtEntity()) {
                                if (!mc.thePlayer.isBlocking() || !disableWhileBlocking.isToggled()) {
                                    interactAt(true, true, false, true);
                                }
                            }
                        }
                    }
                    ReflectionUtils.setItemInUse(blockingClient = true);
                    sendBlockPacket();
                    // cancel
                }
                else {
                    delayTicks = 1;
                }
            }
            else {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                if (blockingClient) {
                    srb();
                    sendUnBlock = true;
                }
            }
        }
        else if (button == 0) {
            if (!state) {
                delayTicks = 1;
            }
            if (mc.currentScreen == null && state && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && !Mouse.isButtonDown(1)) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
                KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
            }
            else if (!state) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            }
        }
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            hitMap.clear();
            lastSet = 0;
            hostileMobs.clear();
            golems.clear();
        }
    }

    private void setTarget(Entity entity) {
        if (entity == null || !(entity instanceof EntityLivingBase)) {
            srb();
            if (autoBlockOverride()) {
                resetAutoblock(true);
            }
            swapped = false;
            partialTicks = 0;
            interactTicks = 0;
            if (target != null) {
                stoppedTargeting = true;
                ModuleUtils.unTargetTicks = 0;
            }
            target = null;
            attackingEntity = null;
            sAttacked = 0;
            t = false;
        }
        else {
            target = (EntityLivingBase) entity;
            sAttacked++;
            hasTargeted = true;
            t = true;
        }
    }

    private void handleTarget() {
        // Narrow down the targets available
        List<EntityLivingBase> availableTargets = new ArrayList<>();
        double maxRange = getMaxRange();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == null || entity == mc.thePlayer || entity.isDead) {
                continue;
            }
            if (entity instanceof EntityPlayer) {
                if (Utils.isFriended((EntityPlayer) entity)) {
                    continue;
                }
                if (((EntityPlayer) entity).deathTime != 0) {
                    continue;
                }
                if (AntiBot.isBot(entity) || (Utils.isTeammate(entity) && ignoreTeammates.isToggled())) {
                    continue;
                }
            }
            else if (entity instanceof EntityCreature && attackMobs.isToggled()) {
                if (((EntityCreature) entity).tasks == null || ((EntityCreature) entity).isAIDisabled() || ((EntityCreature) entity).deathTime != 0) { // no ai
                    continue;
                }
                if (!entity.getClass().getCanonicalName().startsWith("net.minecraft.entity.monster.")) {
                    continue;
                }
                if (Utils.getBedwarsStatus() == 2 && entity instanceof EntityPigZombie) {
                    continue;
                }
            }
            else {
                continue;
            }
            if (entity.isInvisible() && !targetInvis.isToggled()) {
                continue;
            }
            float fovInput = (float) fov.getInput();
            if (fovInput != 360.0f && !Utils.inFov(fovInput, entity)) {
                continue;
            }
            if (mc.thePlayer.getDistanceToEntity(entity) < maxRange + maxRange / 3) { // simple distance check
                availableTargets.add((EntityLivingBase) entity);
            }
        }
        // Init as a new class and adding to list
        List<KillAuraTarget> toClassTargets = new ArrayList<>();
        for (EntityLivingBase target : availableTargets) {
            double distanceRayCasted = getDistanceToBoundingBox(target);
            if (distanceRayCasted > maxRange) {
                continue;
            }
            if (!(target instanceof EntityPlayer) && attackMobs.isToggled() && !isHostile((EntityCreature) target)) {
                continue;
            }
            if (!hitThroughBlocks.isToggled() && (!Utils.canPlayerBeSeen(target) || !inRange(target, attackRange.getInput() - attackv))) {
                continue;
            }
            toClassTargets.add(new KillAuraTarget(distanceRayCasted, target.getHealth(), target.hurtTime, RotationUtils.distanceFromYaw(target, false), target.getEntityId(), (target instanceof EntityPlayer) ? Utils.isEnemy((EntityPlayer) target) : false));
        }
        // Sorting targets
        Comparator<KillAuraTarget> comparator = null;
        switch ((int) sortMode.getInput()) {
            case 0:
                comparator = Comparator.comparingDouble(entity -> entity.distance);
                break;
            case 1:
                comparator = Comparator.comparingDouble(entityPlayer -> (double)entityPlayer.health);
                break;
            case 2:
                comparator = Comparator.comparingDouble(entityPlayer2 -> (double)entityPlayer2.hurttime);
                break;
            case 3:
                comparator = Comparator.comparingDouble(entity2 -> entity2.yawDelta);
                break;
        }
        if (prioritizeEnemies.isToggled()) {
            List<KillAuraTarget> enemies = new ArrayList<>();
            for (KillAuraTarget entity : toClassTargets) {
                if (entity.isEnemy) {
                    enemies.add(entity);
                }
            }
            if (!enemies.isEmpty()) {
                toClassTargets = new ArrayList<>(enemies);
            }
        }
        if (sortMode.getInput() != 0) {
            Collections.sort(toClassTargets, Comparator.comparingDouble(entity -> entity.distance));
        }
        Collections.sort(toClassTargets, comparator); // then sort by selected sorting mode

        List<KillAuraTarget> attackTargets = new ArrayList<>();
        for (KillAuraTarget killAuraTarget : toClassTargets) {
            if (killAuraTarget.distance <= attackRange.getInput() - attackv) {
                attackTargets.add(killAuraTarget);
            }
        }

        if (!attackTargets.isEmpty()) {

            if (sAttacked == 0) {
                ++lastSet;
            }

            // Switch aura
            int ticksExisted = lastSet;
            int switchDelayTicks = (int) (switchDelay.getInput() / 50);
            long noHitTicks = (long) Math.min(attackTargets.size(), targets.getInput()) * switchDelayTicks;
            for (KillAuraTarget auraTarget : attackTargets) {
                Integer firstHit = hitMap.get(auraTarget.entityId);
                if (firstHit == null || ticksExisted - firstHit >= switchDelayTicks) {
                    continue;
                }
                if (auraTarget.distance < attackRange.getInput() - attackv) {
                    setTarget(mc.theWorld.getEntityByID(auraTarget.entityId));
                    return;
                }
            }

            for (KillAuraTarget auraTarget : attackTargets) {
                Integer firstHit = hitMap.get(auraTarget.entityId);
                if (firstHit == null || ticksExisted >= firstHit + noHitTicks) {
                    hitMap.put(auraTarget.entityId, lastSet);
                    setTarget(mc.theWorld.getEntityByID(auraTarget.entityId));
                    return;
                }
            }
        }
        else if (!toClassTargets.isEmpty()) {
            KillAuraTarget killAuraTarget = toClassTargets.get(0);
            setTarget(mc.theWorld.getEntityByID(killAuraTarget.entityId));
        }
        else {
            setTarget(null);
        }
    }

    private void handleSwingAndAttack(double distance, boolean swung) {
        boolean inAttackDistance = inRange(target, attackRange.getInput() - attackv);
        if ((distance <= swingRange.getInput() || inAttackDistance) && shouldAttack && !swung) { // swing if in swing range or needs to attack
            if (!mc.thePlayer.isBlocking() || !disableWhileBlocking.isToggled()) {
                swingItem();
            }
        }
        if (inAttackDistance) {
            attackingEntity = target;
            if (shouldAttack) {
                shouldAttack = false;
                if (ModuleManager.bedAura.breakTick) {
                    return;
                }
                if (!isLookingAtEntity()) {
                    return;
                }
                if (!mc.thePlayer.isBlocking() || !disableWhileBlocking.isToggled()) {
                    mc.playerController.attackEntity(mc.thePlayer, target);
                    sAttacked = 0;
                }
            }
        }
        else {
            attackingEntity = null;
        }
    }

    private boolean isHostile(EntityCreature entityCreature) {
        if (SkyWars.onlyAuraHostiles()) {
            if (entityCreature instanceof EntityGiantZombie) {
                return false;
            }
            return !ModuleManager.skyWars.spawnedMobs.contains(entityCreature.getEntityId());
        }
        else if (entityCreature instanceof EntitySilverfish) {
            String teamColor = Utils.getFirstColorCode(entityCreature.getCustomNameTag());
            String teamColorSelf = Utils.getFirstColorCode(mc.thePlayer.getDisplayName().getFormattedText());
            if (!teamColor.isEmpty() && (teamColorSelf.equals(teamColor) || Utils.isTeammate(entityCreature))) { // same team
                return false;
            }
            return true;
        }
        else if (entityCreature instanceof EntityIronGolem) {
            if (Utils.getBedwarsStatus() != 2) {
                return true;
            }
            if (!golems.containsKey(entityCreature.getEntityId())) {
                double nearestDistance = -1;
                EntityArmorStand nearestArmorStand = null;
                for (Entity entity : mc.theWorld.loadedEntityList) {
                    if (!(entity instanceof EntityArmorStand)) {
                        continue;
                    }
                    String stripped = Utils.stripString(entity.getDisplayName().getFormattedText());
                    if (stripped.contains("[") && stripped.endsWith("]")) {
                        double distanceSq = entity.getDistanceSq(entityCreature.posX, entityCreature.posY, entityCreature.posZ);
                        if (distanceSq < nearestDistance || nearestDistance == -1) {
                            nearestDistance = distanceSq;
                            nearestArmorStand = (EntityArmorStand) entity;
                        }
                    }
                }
                if (nearestArmorStand != null) {
                    String teamColor = Utils.getFirstColorCode(nearestArmorStand.getDisplayName().getFormattedText());
                    String teamColorSelf = Utils.getFirstColorCode(mc.thePlayer.getDisplayName().getFormattedText());
                    boolean isTeam = false;
                    if (!teamColor.isEmpty() && (teamColorSelf.equals(teamColor) || Utils.isTeammate(nearestArmorStand))) { // same team
                        isTeam = true;
                    }
                    golems.put(entityCreature.getEntityId(), isTeam);
                    return !isTeam;
                }
                if (ModuleManager.bedwars.spawnedMobs.contains(entityCreature.getEntityId())) {
                    return false;
                }
                return true;
            }
            else {
                return !golems.getOrDefault(entityCreature.getEntityId(), false);
            }
        }
        else if (entityCreature instanceof EntityPigZombie && Utils.getBedwarsStatus() != 2) {
            return false;
        }
        return hostileMobs.contains(entityCreature);
    }

    private int getAPSToTicks(double cap) {
        double apsv = aps.getInput();
        if (apsv > cap) {
            apsv = cap;
        }
        if (apsv >= 20) {
            return 0;
        }
        if (apsv >= 16) {
            return (int) Utils.randomizeDouble(0.0D, 1.0D);
        }
        if (apsv >= 15) {
            return 1;
        }
        if (apsv >= 11) {
            return (int) Utils.randomizeDouble(1.0D, 2.0D);
        }
        if (apsv >= 10) {
            return 2;
        }
        if (apsv >= 7) {
            return (int) Utils.randomizeDouble(2.0D, 3.0D);
        }
        if (apsv >= 6) {
            return (int) Utils.randomizeDouble(3.0D, 4.0D);
        }
        if (apsv >= 5) {
            return 4;
        }
        if (apsv >= 4) {
            return 5;
        }
        if (apsv >= 3) {
            return (int) Utils.randomizeDouble(6.0D, 7.0D);
        }
        if (apsv >= 2) {
            return 10;
        }
        if (apsv >= 1) {
            return 20;
        }
        if (apsv >= 0) {
            return -1;
        }
        return -1;
    }

    private void handleBlocking() {
        if (!Utils.holdingSword()) {
            return;
        }
        /*if (this.blockingClient == blockState && autoBlockMode.getInput() != 3) {
            return;
        }*/
        if (autoBlockMode.getInput() != previousAutoBlockMode) {
            if (previousAutoBlockMode > 0) {
                resetAutoblock(true);
            }
        }
        previousAutoBlockMode = (int) autoBlockMode.getInput();
        if (!t || !hasAutoblocked) {
            return;
        }
        ReflectionUtils.setItemInUse(blockingClient);
    }

    private double getMaxRange() {
        return Math.max(Math.max(swingRange.getInput(), attackRange.getInput() - attackv), blockRange.getInput());
    }

    public boolean autoBlockOverride() {
        return autoBlockMode.getInput() > 0 && Utils.holdingSword() && manualBlock();
    }

    private float unwrapYaw(float yaw, float prevYaw) {
        return prevYaw + ((((yaw - prevYaw + 180f) % 360f) + 360f) % 360f - 180f);
    }

    private boolean isLookingAtEntity() { //
        if (rotationMode.getInput() == 0 && rotationSmoothing.getInput() > 0) { // silent
            return RotationUtils.isPossibleToHit(attackingEntity, attackRange.getInput() - attackv, RotationUtils.serverRotations);
        }
        return true;
    }

    private void handleAutoBlock(double distance) {
        boolean inAttackDistance = inRange(target, attackRange.getInput() - attackv);
        if (inAttackDistance) {
            attackingEntity = target;
        }
        boolean swung = false;
        hasAutoblocked = true;
        switch ((int) autoBlockMode.getInput()) {
            case 1:
                blockingClient = true;
                interactTicks++;
                if (!hasBlocked) {
                    handleInteractAndAttack(distance, true, true, swung);
                    sendBlockPacket();
                    interactTicks = 0;
                }
                else {
                    if (interactTicks >= getAPSToTicks(20)) {
                        handleInteractAndAttack(distance, true, true, swung);
                        interactTicks = 0;
                    }
                }
                break;
            case 2: // partial
                if (interactTicks == 0) {
                    getTicks = getAPSToTicks(10);
                }
                interactTicks++;
                if (interactTicks == 1) {
                    if (ModuleUtils.isBlocked) {
                        sendDigPacket();
                        blockingClient = false;
                    }
                }
                if (interactTicks == 2) {
                    handleInteractAndAttack(distance, true, true, swung);
                    sendBlockPacket();
                    blockingClient = true;
                }
                if (interactTicks >= getTicks) {
                    interactTicks = 0;
                }
                break;
            case 3: // blink
                blockingClient = true;
                if (interactTicks == 0) {
                    getTicks = getAPSToTicks(10);
                }
                interactTicks++;
                if (interactTicks == 1) {
                    if (ModuleUtils.isBlocked) {
                        blink = true;
                        setSwapSlot();
                    }
                }
                if (interactTicks == 2) {
                    setCurrentSlot();
                    handleInteractAndAttack(distance, true, true, swung);
                    sendBlockPacket();
                    blink = false;
                }
                if (interactTicks >= getTicks) {
                    interactTicks = 0;
                }
                break;
            case 4: // swap
                blockingClient = true;
                interactTicks++;
                if (interactTicks == 1) {
                    if (ModuleUtils.isBlocked) {
                        setSwapSlot();
                        setCurrentSlot();
                    }
                    handleInteractAndAttack(distance, true, true, swung);
                    sendBlockPacket();
                }
                if (interactTicks >= getAPSToTicks(20)) {
                    interactTicks = 0;
                }
                break;
        }
    }

    private void resetYaw(ClientRotationEvent event) {
        reset = true;
        event.yaw = RotationUtils.serverRotations[0];
        event.pitch = RotationUtils.serverRotations[1];
        rotated = false;
    }

    private void srb() {
        if (blockingClient) {
            ReflectionUtils.setItemInUse(blockingClient = false);
        }
    }

    private boolean basicCondition() {
        if (!Utils.nullCheck()) {
            return false;
        }
        if (mc.thePlayer.isDead) {
            return false;
        }
        return true;
    }

    private void setCurrentSlot() {
        if (!swapped) {
            return;
        }
        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
        Raven.packetsHandler.playerSlot.set(mc.thePlayer.inventory.currentItem);
        swapped = false;
    }

    private void setSwapSlot() {
        int bestSwapSlot = getBestSwapSlot();
        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(bestSwapSlot));
        Raven.packetsHandler.playerSlot.set(bestSwapSlot);
        swapped = true;
        blockingServer = false;
        hasBlocked = false;
    }

    private void sendDigPacket() {
        if (!Utils.holdingSword()) {
            return;
        }
        mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, DOWN));
        blockingServer = false;
        hasBlocked = false;
    }

    private void sendBlockPacket() {
        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        blockingServer = true;
        hasBlocked = true;
    }

    private boolean settingCondition() {
        if (requireMouseDown.isToggled() && !Mouse.isButtonDown(0)) {
            return false;
        }
        else if (weaponOnly.isToggled() && !Utils.holdingWeapon()) {
            return false;
        }
        else if (disableWhileMining.isToggled() && Utils.isMining()) {
            return false;
        }
        else if (disableInInventory.isToggled() && mc.currentScreen != null) {
            return false;
        }
        else if (ModuleManager.bedAura != null && ModuleManager.bedAura.isEnabled() && !ModuleManager.bedAura.allowAura.isToggled() && ModuleManager.bedAura.currentBlock != null) {
            return false;
        }
        return true;
    }

    private void setKeyBindState(int keycode, boolean state, boolean invokeTick) {
        KeyBinding.setKeyBindState(keycode, state);
        if (invokeTick) {
            KeyBinding.onTick(keycode);
        }
    }

    private void updateAttackDelay() {
        delay = (long)(1000.0 / aps.getInput() + Utils.randomizeInt(-4, 4));
    }

    private void swingItem() {
        if (silentSwing.isToggled() && mc.thePlayer.isBlocking()) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
        }
        else {
            mc.thePlayer.swingItem();
        }
    }

    public static double getDistanceToBoundingBox(Entity target) {
        if (mc.thePlayer == null) {
            return 0;
        }
        Vec3 playerEyePos = mc.thePlayer.getPositionEyes(((IAccessorMinecraft) mc).getTimer().renderPartialTicks);
        AxisAlignedBB boundingBox = target.getEntityBoundingBox();
        double nearestX = MathHelper.clamp_double(playerEyePos.xCoord, boundingBox.minX, boundingBox.maxX);
        double nearestY = MathHelper.clamp_double(playerEyePos.yCoord, boundingBox.minY, boundingBox.maxY);
        double nearestZ = MathHelper.clamp_double(playerEyePos.zCoord, boundingBox.minZ, boundingBox.maxZ);
        Vec3 nearestPoint = new Vec3(nearestX, nearestY, nearestZ);
        return playerEyePos.distanceTo(nearestPoint);
    }

    private int getBestSwapSlot() {
        int currentSlot = mc.thePlayer.inventory.currentItem;
        int bestSlot = -1;
        double bestDamage = -1;
        for (int i = 0; i < 9; ++i) {
            if (i == currentSlot) {
                continue;
            }
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            double damage = Utils.getDamageLevel(stack);
            if (damage != 0) {
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestSlot = i;
                }
            }
        }
        if (bestSlot == -1) {
            for (int i = 0; i < 9; ++i) {
                if (i == currentSlot) {
                    continue;
                }
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack == null || Arrays.stream(swapBlacklist).noneMatch(stack.getUnlocalizedName().toLowerCase()::contains)) {
                    bestSlot = i;
                    break;
                }
            }
        }

        return bestSlot;
    }

    private int getNextSlot() {
        int currentSlot = mc.thePlayer.inventory.currentItem;
        int next = -1;

        if (currentSlot < 8) {
            next = currentSlot + 1;
        }
        else {
            next = currentSlot - 1;
        }

        return next;
    }

    public void resetYaw() {
        float serverYaw = RotationUtils.serverRotations[0];
        float unwrappedYaw = unwrapYaw(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw), serverYaw);
        mc.thePlayer.rotationYaw = unwrappedYaw;
        mc.thePlayer.prevRotationYaw = unwrappedYaw;
    }

    private void interactAt(boolean interactAt, boolean interact, boolean noEvent, boolean requireInteractAt) {
        if (attackingEntity == null) {
            return;
        }
        if (ModuleManager.bedAura.breakTick) {
            return;
        }
        boolean sent = false;
        if (interactAt) {
            boolean canHit = RotationUtils.isPossibleToHit(attackingEntity, attackRange.getInput() - attackv, RotationUtils.serverRotations);
            if (!canHit) {
                return;
            }
            MovingObjectPosition mov = RotationUtils.rayTrace(10, ((IAccessorMinecraft) mc).getTimer().renderPartialTicks, RotationUtils.serverRotations, hitThroughBlocks.isToggled() ? attackingEntity : null);
            if (mov != null && mov.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mov.entityHit == attackingEntity) {
                Vec3 hitVec = mov.hitVec;
                hitVec = new Vec3(hitVec.xCoord - attackingEntity.posX, hitVec.yCoord - attackingEntity.posY, hitVec.zCoord - attackingEntity.posZ);
                if (!noEvent) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(attackingEntity, hitVec));
                }
                else {
                    PacketUtils.sendPacketNoEvent(new C02PacketUseEntity(attackingEntity, hitVec));
                }
                sent = true;
            }
        }
        if (requireInteractAt && !sent) {
            return;
        }
        if (interact) {
            if (!noEvent) {
                mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(attackingEntity, C02PacketUseEntity.Action.INTERACT));
            }
            else {
                PacketUtils.sendPacketNoEvent(new C02PacketUseEntity(attackingEntity, C02PacketUseEntity.Action.INTERACT));
            }
        }
    }

    private float[] getRotationsSmoothed(float rotations[]) {
        float serverYaw = RotationUtils.serverRotations[0];
        float serverPitch = RotationUtils.serverRotations[1];
        float unwrappedYaw = unwrapYaw(rotations[0], serverYaw);

        float deltaYaw = unwrappedYaw - serverYaw;
        float deltaPitch = rotations[1] - serverPitch;

        float yawSmoothing = (float) rotationSmoothing.getInput();
        float pitchSmoothing = yawSmoothing;

        float strafe = mc.thePlayer.moveStrafing;
        if (strafe < 0 && deltaYaw < 0 || strafe > 0 && deltaYaw > 0) {
            yawSmoothing = Math.max(1f, yawSmoothing / 2f);
        }

        float motionY = (float) mc.thePlayer.motionY;
        if (motionY > 0 && deltaPitch > 0 || motionY < 0 && deltaPitch < 0) {
            pitchSmoothing = Math.max(1f, pitchSmoothing / 2f);
        }

        serverYaw += deltaYaw / Math.max(1f, yawSmoothing);
        serverPitch += deltaPitch / Math.max(1f, pitchSmoothing);

        return new float[] { serverYaw, serverPitch };
    }

    private void handleInteractAndAttack(double distance, boolean interactAt, boolean interact, boolean swung) {
        if (ModuleManager.antiFireball != null && ModuleManager.antiFireball.isEnabled() && ModuleManager.antiFireball.fireball != null && ModuleManager.antiFireball.attack) {
            if (ModuleManager.bedAura.breakTick) {
                return;
            }
            if (!ModuleManager.antiFireball.silentSwing.isToggled()) {
                mc.thePlayer.swingItem();
            }
            else {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            }
            mc.playerController.attackEntity(mc.thePlayer, ModuleManager.antiFireball.fireball);
            if (interact) {
                mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(ModuleManager.antiFireball.fireball, C02PacketUseEntity.Action.INTERACT));
            }
        }
        else {
            handleSwingAndAttack(distance, swung);
            interactAt(interactAt, interact, false, false);
        }
    }

    public void resetAutoblock(boolean unblock) {
        if (!hasAutoblocked) {
            return;
        }
        blink = false;
        if (Raven.packetsHandler.playerSlot.get() != mc.thePlayer.inventory.currentItem && swapped) {
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            Raven.packetsHandler.playerSlot.set(mc.thePlayer.inventory.currentItem);
        }
        else if (unblock && !ModuleManager.scaffold.isEnabled && ModuleUtils.isBlocked) {
            sendUnBlock = true;
        }
        swapped = false;
        interactTicks = 0;
        hasAutoblocked = false;
        hasBlocked = false;
    }

    private boolean inRange(final Entity target, final double distance) {
        return RotationUtils.isPossibleToHit(target, distance, RotationUtils.getRotations(target));
    }

    private boolean manualBlock() {
        return (!manualBlock.isToggled() || Mouse.isButtonDown(1)) && Utils.holdingSword();
    }

    static class KillAuraTarget {
        double distance;
        float health;
        int hurttime;
        double yawDelta;
        int entityId;
        boolean isEnemy;

        public KillAuraTarget(double distance, float health, int hurttime, double yawDelta, int entityId, boolean isEnemy) {
            this.distance = distance;
            this.health = health;
            this.hurttime = hurttime;
            this.yawDelta = yawDelta;
            this.entityId = entityId;
            this.isEnemy = isEnemy;
        }
    }
}