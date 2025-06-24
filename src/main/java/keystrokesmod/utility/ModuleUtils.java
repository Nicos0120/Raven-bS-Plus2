package keystrokesmod.utility;

import keystrokesmod.event.*;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.impl.movement.Bhop;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.utility.command.CommandManager;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import keystrokesmod.module.ModuleManager;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class ModuleUtils {
    private final Minecraft mc;

    public ModuleUtils(Minecraft mc) {
        this.mc = mc;
    }

    public static boolean isBreaking;
    public static boolean threwFireball, threwFireballLow;
    public static long MAX_EXPLOSION_DIST_SQ = 10;
    private long FIREBALL_TIMEOUT = 500L, fireballTime = 0;
    public static int inAirTicks, groundTicks, stillTicks, rcTick;
    public static int fadeEdge;
    public static double offsetValue = 0.00100012;
    public static boolean isAttacking;
    private int attackingTicks;
    public static int unTargetTicks;
    public static int profileTicks = -1, swapTick;
    public static int lastY, thisY;
    public static boolean lastTickOnGround, lastTickPos1, lastYDif;
    private boolean thisTickOnGround, thisTickPos1;
    public static boolean firstDamage;

    public static boolean isBlocked;

    public static boolean damage;
    private int damageTicks;
    private boolean lowhopAir;

    private int edgeTick;

    private boolean dontCheckFD;

    public static boolean canSlow, didSlow, setSlow, hasSlowed;
    private static boolean allowFriction;

    private float yaw;

    private boolean ldmg;

    private int placeFrequency, removeFrequency, heldDelay, rcDelay;

    public static boolean worldChange;


    //-0.0784000015258789 = ground value

    //§

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.entity == mc.thePlayer) {
            ModuleManager.disabler.disablerLoaded = false;
            inAirTicks = 0;
            worldChange = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSendPacketsAll(SendAllPacketsEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        Packet packet = e.getPacket();
        if (packet instanceof C07PacketPlayerDigging && isBlocked) {
            C07PacketPlayerDigging c07 = (C07PacketPlayerDigging) packet;
            if (Objects.equals(String.valueOf(c07.getStatus()), "RELEASE_USE_ITEM")) {
                isBlocked = false;
            }
        }
        if (packet instanceof C09PacketHeldItemChange && isBlocked) {
            isBlocked = false;
        }
        if (packet instanceof C08PacketPlayerBlockPlacement && Utils.holdingSword() && !BlockUtils.isInteractable(mc.objectMouseOver) && !isBlocked) {
            isBlocked = true;
        }
        if (packet instanceof C02PacketUseEntity) {
            isAttacking = true;
            attackingTicks = 5;
        }


        if (e.getPacket() instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging c07 = (C07PacketPlayerDigging) packet;
            if (Objects.equals(String.valueOf(c07.getStatus()), "START_DESTROY_BLOCK")) {
                isBreaking = true;
            }
            if (Objects.equals(String.valueOf(c07.getStatus()), "ABORT_DESTROY_BLOCK") || Objects.equals(String.valueOf(c07.getStatus()), "STOP_DESTROY_BLOCK")) {
                isBreaking = false;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }

        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            placeFrequency++;
        }

        if (e.getPacket() instanceof C09PacketHeldItemChange) {
            swapTick = 2;
        }

        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && Utils.holdingFireball()) {
            if (Utils.keybinds.isMouseDown(1)) {
                fireballTime = System.currentTimeMillis();
                threwFireball = true;
                if (mc.thePlayer.rotationPitch > 50F) {
                    threwFireballLow = true;
                }
            }
        }

    }

    public static boolean hasTeleported;
    private int htpt;

    @SubscribeEvent
    public void onReceivePacketsAll(ReceiveAllPacketsEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.isCanceled()) {
            return;
        }
        if (e.getPacket() instanceof S27PacketExplosion) {
            firstDamage = false;

            dontCheckFD = true;
        }
        if (e.getPacket() instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {

                damage = true;
                damageTicks = 0;

                if (!dontCheckFD) {
                    firstDamage = true;
                }
                dontCheckFD = false;

                ldmg = true;

            }
        }
        if (e.getPacket() instanceof S08PacketPlayerPosLook) {
            hasTeleported = true;
            htpt = 2;
        }
    }

    private int ft = 0;

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        if (mc.thePlayer.hurtTime == 9 || mc.thePlayer.hurtTime == 0) {
            ft = 0;
        }
        if (bhopBoostConditions()) {
            if (mc.thePlayer.hurtTime > 0 && ft == 0 && firstDamage) {
                double base = Utils.getHorizontalSpeed();
                if (base <= 0) {
                    base = 0.01;
                }
                Utils.setSpeed(base);
                ft = -1;
            }
        }
        if (veloBoostConditions()) {
            if (mc.thePlayer.hurtTime > 0 && ft == 0 && firstDamage) {
                double added = 0;
                if (Utils.getHorizontalSpeed() <= Velocity.minExtraSpeed.getInput()) {
                    added = Velocity.extraSpeedBoost.getInput() / 100;
                    if (Velocity.reverseDebug.isToggled()) {
                        Utils.modulePrint("&7[&dR&7] Applied extra boost | Original speed: " + Utils.getHorizontalSpeed());
                    }
                }
                double base = Utils.getHorizontalSpeed();
                if (base <= 0) {
                    base = 0.01;
                }
                Utils.setSpeed((base * (Velocity.reverseHorizontal.getInput() / 100)) * (1 + added));
                ft = -1;
            }
        }
        firstDamage = false;
        worldChange = false;
    }

    private boolean bhopBoostConditions() {
        if (ModuleManager.bhop.isEnabled() && ModuleManager.bhop.damageBoost.isToggled() && (!ModuleManager.bhop.damageBoostRequireKey.isToggled() || ModuleManager.bhop.damageBoostKey.isPressed())) {
            return true;
        }
        return false;
    }

    private boolean veloBoostConditions() {
        if (ModuleManager.velocity.isEnabled() && ModuleManager.velocity.mode.getInput() == 2) {
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {

        if (hasTeleported && htpt > 0) {
            htpt--;
        }
        else {
            htpt = 0;
            hasTeleported = false;
        }

        rcTick = Utils.keybinds.isMouseDown(1) ? ++rcTick : 0;

        //Autoswap option "Legit"

        if (placeFrequency > 0) {
            if (++removeFrequency > 2) {
                removeFrequency = 0;
                placeFrequency--;
            }
        }
        else {
            removeFrequency = 0;
        }
        if (!Utils.keybinds.isMouseDown(1)) {
            if (++rcDelay > 3) {
                placeFrequency = 0;
            }
            heldDelay = 0;
        }
        else {
            rcDelay = 0;
        }
        if (holdingBlocks() && rcDelay == 0) {
            heldDelay++;
        } else {
            if (heldDelay > 0) {
                heldDelay--;
            }
            if (rcDelay == 0) {
                if (heldDelay > 0 && (placeFrequency > 1 || heldDelay > 4)) {
                    if (getSlot() != -1 && ModuleManager.autoSwap.legit.isToggled()) {
                        mc.thePlayer.inventory.currentItem = getSlot();
                    }
                }
            }
        }

        if (swapTick > 0) {
            --swapTick;
        }

        if (ModuleManager.killAura.stoppedTargeting && ++unTargetTicks >= 2) {
            ModuleManager.killAura.stoppedTargeting = false;
        }

        if (canSlow || ModuleManager.scaffold.isEnabled) {
            double motionVal = 0.9507832 - ((double) inAirTicks / 10000) - Utils.randomizeDouble(0.00001, 0.00006);
            if (!hasSlowed) motionVal = motionVal - 0.15;
            if (mc.thePlayer.hurtTime == 0 && !setSlow && !mc.thePlayer.onGround) {
                setSlow = hasSlowed = true;
            }
            didSlow = true;
        }
        if (didSlow && mc.thePlayer.onGround) {
            canSlow = didSlow = false;
        }
        if (groundTicks > 1) {
            hasSlowed = false;
        }
        if (mc.thePlayer.onGround || mc.thePlayer.hurtTime != 0) {
            setSlow = false;
        }

        if (!ModuleManager.bhop.running && !ModuleManager.scaffold.fastScaffoldKeepY) {
            allowFriction = false;
        }
        else if (!mc.thePlayer.onGround) {
            allowFriction = true;
        }

        if (damage && ++damageTicks >= 8) {
            damage = firstDamage = false;
            damageTicks = 0;
        }

        profileTicks++;

        if (isAttacking) {
            if (attackingTicks <= 0) {
                isAttacking = false;
            }
            else {
                --attackingTicks;
            }
        }

        if (LongJump.slotReset && ++LongJump.slotResetTicks >= 2) {
            LongJump.stopModules = false;
            LongJump.slotResetTicks = 0;
            LongJump.slotReset = false;
        }

        if (fireballTime > 0 && (System.currentTimeMillis() - fireballTime) > FIREBALL_TIMEOUT / 3) {
            threwFireballLow = false;
            ModuleManager.velocity.disableVelo = false;
        }

        if (fireballTime > 0 && (System.currentTimeMillis() - fireballTime) > FIREBALL_TIMEOUT) {
            threwFireball = threwFireballLow = false;
            fireballTime = 0;
            ModuleManager.velocity.disableVelo = false;
        }

        if (CommandManager.status.cooldown != 0) {
            if (mc.thePlayer.ticksExisted % 20 == 0) {
                CommandManager.status.cooldown--;
            }
        }
    }

    private int getSlot() {
        int slot = -1;
        int highestStack = -1;
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) itemStack.getItem()) && itemStack.stackSize > 0) {
                if (Utils.getBedwarsStatus() == 2 && ((ItemBlock) itemStack.getItem()).getBlock() instanceof BlockTNT) {
                    continue;
                }
                if (heldItem != null && heldItem.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) heldItem.getItem()) && !itemStack.getItem().getClass().equals(heldItem.getItem().getClass())) {
                    continue;
                }
                if (itemStack.stackSize > highestStack) {
                    highestStack = itemStack.stackSize;
                    slot = i;
                }
            }
        }
        return slot;
    }

    private boolean holdingBlocks() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            return false;
        }
        return true;
    }

    private boolean tower() {
        return ModuleManager.tower.canTower() && ModuleManager.tower.towerMove.getInput() != 8;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        int simpleY = (int) Math.round((e.posY % 1) * 10000);

        if (ModuleManager.scaffold.offsetDelay > 0) {
            --ModuleManager.scaffold.offsetDelay;
        }

        lastTickOnGround = thisTickOnGround;
        thisTickOnGround = mc.thePlayer.onGround;

        lastTickPos1 = thisTickPos1;
        thisTickPos1 = mc.thePlayer.posY % 1 == 0;

        lastY = thisY;
        thisY = (int) mc.thePlayer.posY;

        if (thisY >= lastY + 2 || thisY <= lastY - 2) {
            lastYDif = true;
        }
        else {
            lastYDif = false;
        }

        inAirTicks = mc.thePlayer.onGround ? 0 : ++inAirTicks;
        groundTicks = !mc.thePlayer.onGround ? 0 : ++groundTicks;
        stillTicks = Utils.isMoving() ? 0 : ++stillTicks;

        handleLowhop();

        if (ModuleManager.bhop.setRotation) {
            if (!ModuleManager.killAura.rotating && !ModuleManager.scaffold.isEnabled) {
                yaw = ModuleManager.scaffold.getMotionYaw() - 130.625F * Math.signum(
                        MathHelper.wrapAngleTo180_float(ModuleManager.scaffold.getMotionYaw() - yaw)
                );
                e.setYaw(yaw);
                RotationUtils.setFakeRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            }
            if (mc.thePlayer.onGround) {
                ModuleManager.bhop.setRotation = false;
            }
        }

        if (ModuleManager.scaffold.canBlockFade && !ModuleManager.scaffold.isEnabled && ++fadeEdge >= 45) {
            ModuleManager.scaffold.canBlockFade = false;
            fadeEdge = 0;
            ModuleManager.scaffold.highlight.clear();
        }

        ldmg = false;
    }

    private void resetLowhop() {
        ModuleManager.bhop.lowhop = ModuleManager.scaffold.lowhop = false;
        ModuleManager.bhop.didMove = false;
        lowhopAir = false;
        edgeTick = 0;
    }

    public static void handleSlow() {
        didSlow = false;
        canSlow = true;
    }

    public static double applyFrictionMulti() {
        final int speedAmplifier = Utils.getSpeedAmplifier();
        if (speedAmplifier > 1 && allowFriction) {
            return Bhop.friction.getInput();
        }
        return 1;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!ModuleManager.scaffold.canBlockFade) {
            return;
        }
        if (!Utils.nullCheck() || !ModuleManager.scaffold.highlightBlocks.isToggled() || ModuleManager.scaffold.highlight.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Timer>> iterator = ModuleManager.scaffold.highlight.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Timer> entry = iterator.next();
            if (entry.getValue() == null) {
                entry.setValue(new Timer(750));
                entry.getValue().start();
            }
            int alpha = entry.getValue() == null ? 210 : 210 - entry.getValue().getValueInt(0, 210, 1);
            if (alpha == 0) {
                iterator.remove();
                continue;
            }
            RenderUtils.renderBlock(entry.getKey(), Utils.mergeAlpha(Theme.getGradient((int) HUD.theme.getInput(), 0), alpha), true, false);
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        String stripped = Utils.stripColor(e.message.getUnformattedText());

        //online
        if (stripped.contains("You tipped ") && stripped.contains(" in") && stripped.contains("!") && CommandManager.status.start) {
            CommandManager.status.start = false;
            Utils.modulePrint("§a " + CommandManager.status.ign + " is online");
            e.setCanceled(true);
        }
        if ((stripped.contains("You've already tipped someone in the past hour in") && stripped.contains("! Wait a bit and try again!") || stripped.contains("You've already tipped that person today in ")) && CommandManager.status.start) {
            CommandManager.status.start = false;
            Utils.modulePrint("§a " + CommandManager.status.ign + " is online");
            //client.print(util.colorSymbol + "7^ if player recently left the server this may be innacurate (rate limited)");
            e.setCanceled(true);
        }
        //offline
        if (stripped.contains("That player is not online, try another user!") && CommandManager.status.start) {
            CommandManager.status.start = false;
            Utils.modulePrint("§7 " + CommandManager.status.ign + " is offline");
            e.setCanceled(true);
        }
        //invalid name
        if (stripped.contains("Can't find a player by the name of '") && CommandManager.status.start) {
            CommandManager.status.cooldown = 0;
            CommandManager.status.start = false;
            CommandManager.status.currentMode = CommandManager.status.lastMode;
            Utils.modulePrint("§7 " + CommandManager.status.ign + " doesn't exist");
            e.setCanceled(true);
        }
        if (stripped.contains("That's not a valid username!") && CommandManager.status.start) {
            CommandManager.status.cooldown = 0;
            CommandManager.status.start = false;
            CommandManager.status.currentMode = CommandManager.status.lastMode;
            Utils.modulePrint("§binvalid username");
            e.setCanceled(true);
        }
        //checking urself
        if (stripped.contains("You cannot give yourself tips!") && CommandManager.status.start) {
            CommandManager.status.cooldown = 0;
            CommandManager.status.start = false;
            CommandManager.status.currentMode = CommandManager.status.lastMode;
            Utils.modulePrint("§a " + CommandManager.status.ign + " is online");
            e.setCanceled(true);
        }
    }

    private void handleLowhop() {
        Block blockAbove = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ));
        Block blockBelow = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ));
        Block blockBelow2 = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 2, mc.thePlayer.posZ));
        Block block = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
        int simpleY = (int) Math.round((mc.thePlayer.posY % 1) * 10000);

        if ((ModuleManager.bhop.didMove || ModuleManager.scaffold.lowhop) && (!ModuleManager.bhop.disablerOnly.isToggled() || ModuleManager.bhop.disablerOnly.isToggled() && ModuleManager.disabler.disablerLoaded)) {
            if (ModuleManager.scaffold.lowhop) {
                switch (simpleY) {
                    case 4200:
                        mc.thePlayer.motionY = 0.39;
                        break;
                    case 1138:
                        mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                        break;
                    case 2031:
                        mc.thePlayer.motionY = mc.thePlayer.motionY - 0.2;
                        resetLowhop();
                        break;
                }
            }
            else if (ModuleManager.bhop.didMove) {
                if (mc.thePlayer.isCollidedVertically || ldmg && Velocity.vertical.getInput() != 0 && !ModuleManager.velocity.dontEditMotion() || block instanceof BlockSlab) {// || !ModuleManager.bhop.lowhop && (!(block instanceof BlockAir) || !(blockAbove instanceof BlockAir) || blockBelow instanceof BlockSlab || (blockBelow instanceof BlockAir && blockBelow2 instanceof BlockAir))) {
                    resetLowhop();
                    return;
                }
                switch ((int) ModuleManager.bhop.mode.getInput()) {
                    case 2: // 9 tick
                        switch (simpleY) {
                            case 13:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.02483;
                                ModuleManager.bhop.lowhop = true;
                                break;
                            case 2000:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.1913;
                                break;
                            case 7016:
                                mc.thePlayer.motionY = mc.thePlayer.motionY + 0.08;
                                break;
                        }
                        if (ModuleUtils.inAirTicks >= 7 && Utils.isMoving()) {
                            Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer));
                        }
                        if (ModuleUtils.inAirTicks >= 9) {
                            resetLowhop();
                        }
                        break;
                    case 3: // 8 tick
                        if (!ModuleManager.bhop.isNormalPos || (block instanceof BlockStairs)) {
                            resetLowhop();
                            break;
                        }
                        boolean g1 = Utils.distanceToGround(mc.thePlayer) <= 1.2;
                        //disable
                        if (inAirTicks >= 9 || inAirTicks >= 5 && !g1) {
                            resetLowhop();
                            break;
                        }
                        if (inAirTicks == 1) {
                            mc.thePlayer.motionY = 0.38999998569488;
                            ModuleManager.bhop.lowhop = true;
                        }
                        if (inAirTicks == 2) {
                            mc.thePlayer.motionY = 0.30379999189377;
                        }
                        if (inAirTicks == 3) {
                            mc.thePlayer.motionY = 0.08842400075912;
                        }
                        if (inAirTicks == 4) {
                            mc.thePlayer.motionY = -0.19174457909538;
                        }
                        if (inAirTicks == 5 && g1) {
                            mc.thePlayer.motionY = -0.26630949469659;
                        }
                        if (inAirTicks == 6 && g1) {
                            mc.thePlayer.motionY = -0.26438340940798;
                        }
                        if (inAirTicks == 7 && g1) {
                            mc.thePlayer.motionY = -0.33749574778843;
                        }
                        //strafe
                        if (inAirTicks >= 6 && Utils.isMoving()) {
                            Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer));
                        }
                        break;
                    case 4: // 7 tick
                        switch (simpleY) {
                            case 4200:
                                mc.thePlayer.motionY = 0.39;
                                ModuleManager.bhop.lowhop = true;
                                break;
                            case 1138:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                                break;
                            case 2031:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.2;
                                resetLowhop();
                                break;
                        }
                        break;
                }
            }
        }
        if (!mc.thePlayer.onGround) {
            lowhopAir = true;
        }
        else if (lowhopAir) {
            resetLowhop();
            if (!ModuleManager.bhop.isEnabled()) {
                ModuleManager.bhop.isNormalPos = false;
            }
        }
    }

}