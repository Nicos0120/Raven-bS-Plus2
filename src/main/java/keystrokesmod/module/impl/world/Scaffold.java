package keystrokesmod.module.impl.world;

import keystrokesmod.event.*;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayerSP;
import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import keystrokesmod.utility.Timer;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Scaffold extends Module {
    private final SliderSetting motion;
    public SliderSetting rotation, fakeRotation;
    private SliderSetting sprint;
    private SliderSetting floatFirstJump;
    private SliderSetting fastScaffold;
    private SliderSetting multiPlace;

    public ButtonSetting autoSwap;
    private ButtonSetting fastOnRMB;
    public ButtonSetting highlightBlocks;
    private ButtonSetting jumpFacingForward;
    public ButtonSetting safeWalk;
    public ButtonSetting showBlockCount;
    private ButtonSetting silentSwing;
    private ButtonSetting prioritizeSprintWithSpeed;

    private String[] rotationModes = new String[] { "§cDisabled", "Simple", "Offset", "Precise" };
    private String[] fakeRotationModes = new String[] { "§cDisabled", "None", "Strict", "Smooth", "Spin", "Precise" };
    private String[] sprintModes = new String[] { "§cDisabled", "Vanilla", "Float" };
    private String[] fastScaffoldModes = new String[] { "§cDisabled", "Jump-A", "Jump-B", "Jump-C", "Adjust", "Hyp-y", "Hyp-tick" };
    private String[] multiPlaceModes = new String[] { "§cDisabled", "1 extra", "2 extra", "3 extra", "4 extra" };

    //Highlight blocks
    public Map<BlockPos, Timer> highlight = new HashMap<>();
    public boolean canBlockFade;

    //Block count
    private ScaffoldBlockCount scaffoldBlockCount;

    //swapping related
    public AtomicInteger lastSlot = new AtomicInteger(-1);
    private int spoofSlot;
    public boolean hasSwapped;
    private int blockSlot = -1;

    //placements related
    private boolean hasPlaced, finishProcedure, stopUpdate, stopUpdate2;
    private PlaceData lastPlacement;
    private EnumFacing[] facings = { EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP };
    private BlockPos[] offsets = { new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(0, -1, 0) };
    private Vec3 targetBlock;
    private PlaceData blockInfo;
    private Vec3 blockPos, hitVec, lookVec;

    //bypass related
    private boolean rotateForward;
    private double startYPos = -1;
    public boolean fastScaffoldKeepY;
    private boolean firstKeepYPlace;
    private boolean rotatingForward;
    private int keepYTicks;
    public boolean lowhop;
    private int rotationDelay;
    private boolean floatJumped;
    private boolean floatStarted;
    private boolean floatWasEnabled;
    private boolean floatKeepY;
    public int offsetDelay;
    public boolean placedVP;
    private boolean jump;
    private int floatTicks;
    public boolean blink;

    //disable checks
    public boolean moduleEnabled;
    public boolean isEnabled;
    private boolean disabledModule;
    private boolean dontDisable, towerEdge;
    private int disableTicks;
    private int scaffoldTicks;

    //rotation related
    private boolean was451, was452;
    private float minPitch, minOffset, pOffset;
    private float edge;
    private long firstStroke, yawEdge, vlS, swDelay;
    private float lastEdge2, yawAngle, theYaw;
    private boolean enabledOffGround = false;
    private float[] blockRotations;
    public float yaw, pitch, blockYaw, yawOffset, lastOffset;
    private boolean set2;
    private float maxOffset;
    private int sameMouse;
    private int randomF, yawChanges, dynamic;
    private boolean getVTR, resetm;
    private float VTRY;
    private float normalYaw, normalPitch;
    private int switchvl;
    private int dt;
    private float getSmooth, lastYawS, smoothedYaw;
    private boolean neg;
    private float yawWithOffset;
    private int rt;
    //fake rotations
    private float fakeYaw, fakePitch;
    private float fakeYaw1, fakeYaw2;


    public Scaffold() {
        super("Scaffold", category.world);
        this.registerSetting(motion = new SliderSetting("Motion", "%", 100, 50, 150, 1));
        this.registerSetting(rotation = new SliderSetting("Rotation", 1, rotationModes));
        this.registerSetting(fakeRotation = new SliderSetting("Rotation (fake)", 0, fakeRotationModes));
        this.registerSetting(sprint = new SliderSetting("Sprint mode", 0, sprintModes));
        this.registerSetting(prioritizeSprintWithSpeed = new ButtonSetting("Prioritize sprint with speed", false));
        this.registerSetting(floatFirstJump = new SliderSetting("§eFloat §rfirst jump speed", "%", 100, 50, 100, 1));
        this.registerSetting(fastScaffold = new SliderSetting("Fast scaffold", 0, fastScaffoldModes));

        this.registerSetting(multiPlace = new SliderSetting("Multi-place", 0, multiPlaceModes));

        this.registerSetting(autoSwap = new ButtonSetting("Auto swap", true));
        this.registerSetting(fastOnRMB = new ButtonSetting("Fast on RMB", true));
        this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", true));
        this.registerSetting(jumpFacingForward = new ButtonSetting("Jump facing forward", false));
        this.registerSetting(safeWalk = new ButtonSetting("Safewalk", true));
        this.registerSetting(showBlockCount = new ButtonSetting("Show block count", true));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
        //this.registerSetting(pitchOffset = new SliderSetting("Pitch offset", "", 9, 0, 30, 1));

        //this.registerSetting(offsetAmount = new SliderSetting("Offset amount", "%", 100, 0, 100, 0.25));

        this.alwaysOn = true;
    }

    public void guiUpdate() {
        this.prioritizeSprintWithSpeed.setVisible(sprint.getInput() > 0, this);
        this.floatFirstJump.setVisible(sprint.getInput() == 2, this);
    }

    public void onDisable() {
        if (ModuleManager.tower.canTower() && (ModuleManager.tower.dCount == 0 || !Utils.isMoving())) {
            towerEdge = true;
        }
        disabledModule = true;
        moduleEnabled = false;

        if (!isEnabled) {
            scaffoldBlockCount.beginFade();
        }
    }

    public void onEnable() {
        dt = 0;
        isEnabled = true;
        moduleEnabled = true;
        ModuleUtils.fadeEdge = 0;
        edge = -999999929;
        minPitch = 80F;
        if (!mc.thePlayer.onGround) {
            rotationDelay = 2;
            enabledOffGround = true;
        }
        lastEdge2 = mc.thePlayer.rotationYaw;

        FMLCommonHandler.instance().bus().register(scaffoldBlockCount = new ScaffoldBlockCount(mc));
        lastSlot.set(-1);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent e) {
        if (!isEnabled) {
            return;
        }
        if (e.button == 0 || e.button == 1) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        normalYaw = mc.thePlayer.rotationYaw;
        normalPitch = mc.thePlayer.rotationPitch;
        if (dt > 0) {
            return;
        }
        //Fake rotations
        if (fakeRotation.getInput() > 0) {
            if (fakeRotation.getInput() == 1) {
                fakeYaw = normalYaw;
                fakePitch = normalPitch;
            } else if (fakeRotation.getInput() == 2) {
                fakeYaw = fakeYaw1;
                if (blockRotations != null) {
                    fakePitch = blockRotations[1] + 5;
                } else {
                    fakePitch = 80f;
                }
            } else if (fakeRotation.getInput() == 3) {
                fakeYaw2 = mc.thePlayer.rotationYaw - hardcodedYaw();
                float yawDifference = Utils.getAngleDifference(lastEdge2, fakeYaw2);
                float smoothingFactor = 0.35f;
                fakeYaw2 = (lastEdge2 + yawDifference * smoothingFactor);
                lastEdge2 = fakeYaw2;

                fakeYaw = fakeYaw2;
                if (blockRotations != null) {
                    fakePitch = blockRotations[1] + 5;
                } else {
                    fakePitch = 80f;
                }
            } else if (fakeRotation.getInput() == 4) {
                fakeYaw += 25.71428571428571F;
                fakePitch = 90F;
            } else if (fakeRotation.getInput() == 5) {
                if (blockRotations != null) {
                    fakeYaw2 = blockRotations[0];
                    fakePitch = blockRotations[1];
                } else {
                    fakeYaw2 = mc.thePlayer.rotationYaw - hardcodedYaw() - 180;
                    fakePitch = 88F;
                }
                float yawDifference = Utils.getAngleDifference(lastEdge2, fakeYaw2);
                float smoothingFactor = 0.35f;
                fakeYaw2 = (lastEdge2 + yawDifference * smoothingFactor);
                lastEdge2 = fakeYaw2;

                fakeYaw = fakeYaw2;
            }
            RotationUtils.setFakeRotations(fakeYaw, fakePitch);
        }
        if (!isEnabled) {
            dt++;
            return;
        }
        if (Utils.isMoving()) {
            scaffoldTicks++;
        }
        else {
            scaffoldTicks = 0;
        }
        canBlockFade = true;
        if (Utils.keysDown() && usingFastScaffold() && !ModuleManager.invmove.active() && fastScaffold.getInput() >= 1 && !ModuleManager.tower.canTower() && !LongJump.function) { // jump mode
            if (mc.thePlayer.onGround && Utils.isMoving()) {
                if (scaffoldTicks > 1) {
                    jump = true;
                    rotateForward(true);
                    if (startYPos == -1 || Math.abs(startYPos - mc.thePlayer.posY) > 2) {
                        startYPos = mc.thePlayer.posY;
                        fastScaffoldKeepY = true;
                    }
                }
            }
        }
        else if (fastScaffoldKeepY) {
            fastScaffoldKeepY = firstKeepYPlace = false;
            startYPos = -1;
            keepYTicks = 0;
        }

        //Float
        if (sprint.getInput() == 2 && !usingFastScaffold() && !fastScaffoldKeepY && !ModuleManager.tower.canTower() && !LongJump.function) {
            floatWasEnabled = true;
            if (!floatStarted && offsetDelay == 0) {
                if (ModuleUtils.groundTicks > 8 && mc.thePlayer.onGround) {
                    floatKeepY = true;
                    startYPos = e.posY;
                    rotateForward(true);
                    mc.thePlayer.jump();
                    if (Utils.isMoving()) {
                        double fvl = (getSpeed(getSpeedLevel()) - Utils.randomizeDouble(0.0003, 0.0001)) * (floatFirstJump.getInput() / 100);
                        Utils.setSpeed(fvl);
                    }
                    floatJumped = true;
                } else if (ModuleUtils.groundTicks <= 8 && mc.thePlayer.onGround) {
                    floatStarted = true;
                }
                if (floatJumped && !mc.thePlayer.onGround) {
                    floatStarted = true;
                }
            }

            if (floatStarted && mc.thePlayer.onGround) {
                floatKeepY = false;
                startYPos = -1;
                if (moduleEnabled && mc.thePlayer.posY % 1 == 0) {
                    ++floatTicks;
                    rotateForward = false;
                    rotationDelay = 0;
                    /*if (rt == 0) {
                        blink = true;
                        rotateForward(false);
                    }
                    ++rt;
                    if (rt == 3) {
                        rt = 0;
                        blink = false;
                    }*/
                    if (floatTicks > 1) {
                        e.setPosY(e.getPosY() + 1e-3);
                        floatTicks = 0;
                    }
                    else {
                        e.setPosY(e.getPosY() + 1e-4);
                    }
                    if (sprint.getInput() == 2 && Utils.isMoving() && !ModuleManager.invmove.active()) Utils.setSpeed(getFloatSpeed(getSpeedLevel()));
                    ModuleUtils.groundTicks = 0;
                    offsetDelay = 2;
                    ModuleManager.tower.delay = true;
                    ModuleManager.tower.delayTicks = 0;
                }
            }
        } else if (floatWasEnabled && moduleEnabled) {
            if (floatKeepY) {
                startYPos = -1;
            }
            if (mc.thePlayer.onGround) {
                Utils.setSpeed(Utils.getHorizontalSpeed() / 2);
            }
            floatStarted = floatJumped = floatKeepY = floatWasEnabled = blink = false;
            floatTicks = rt = 0;
        }

        if (blockRotations != null) {
            if (mc.thePlayer.rotationYaw == lastOffset) {
                sameMouse++;
            }
            else {
                sameMouse = 0;
                yawChanges++;
            }
            if (sameMouse > 2) {
                yawChanges = 0;
            }
            lastOffset = mc.thePlayer.rotationYaw;
            if (yawChanges > 15) {
                randomF = 1;
                yawEdge = Utils.time();
            }
            if (yawEdge > 0 && (Utils.time() - yawEdge) > 500) {
                yawEdge = 0;
            }
        }
        else {
            fakeYaw1 = mc.thePlayer.rotationYaw - hardcodedYaw();
        }

        dynamic = 0;
        if (targetBlock != null) {
            Vec3 lookAt = new Vec3(targetBlock.xCoord - lookVec.xCoord, targetBlock.yCoord - lookVec.yCoord, targetBlock.zCoord - lookVec.zCoord);
            blockRotations = RotationUtils.getRotations(lookAt);
            targetBlock = null;
            fakeYaw1 = mc.thePlayer.rotationYaw - hardcodedYaw();
            if (yawEdge == 0) {
                randomF = Utils.randomizeInt(0, 9);
                yawEdge = Utils.time();
            }
            dynamic++;
        }
        randomF = 0;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onClientRotation(ClientRotationEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!isEnabled) {
            lastMY = getMotionYaw();
            return;
        }

        if (jump) {
            mc.thePlayer.setSprinting(true);
            mc.thePlayer.jump();
            jump = false;
            if (!Settings.movementFix.isToggled()) {
                Utils.setSpeed((getSpeed(getSpeedLevel()) * ModuleUtils.applyFrictionMulti()));
            }
            if (fastScaffold.getInput() == 6 || fastScaffold.getInput() == 3 && firstKeepYPlace) {
                lowhop = true;
            }
        }


        switch ((int) rotation.getInput()) {
            case 1:
                yaw = mc.thePlayer.rotationYaw - hardcodedYaw();
                pitch = 76F;
                if (!Utils.isDiagonal(true)) {
                    //yaw += 45;
                }
                e.setRotations(yaw, pitch);
                theYaw = yaw;
                break;
            case 2:
                offsetRots(e);
                break;
            case 3:
                preciseRots(e);
                break;
        }
        if (edge != 1) {
            switchvl++;
            edge = 1;
        }
        if (mc.thePlayer.onGround) {
            enabledOffGround = false;
        }

        //jump facing forward
        if (rotationDelay > 0) --rotationDelay;
        if (!mc.thePlayer.onGround) {
            rotateForward = false;
        }
        if (rotateForward && jumpFacingForward.isToggled()) {
            if (rotation.getInput() > 0) {
                float forwardYaw = (mc.thePlayer.rotationYaw - hardcodedYaw() - 180);
                e.setYaw(forwardYaw);
                e.setPitch(90F);
            }
        }
        else {
            rotatingForward = false;
        }

        if (!Settings.movementFix.isToggled() && mc.thePlayer.motionX == 0.0D && mc.thePlayer.motionZ == 0.0D) {
            if (blockRotations != null) {
                e.setYaw(blockRotations[0]);
            }
        }

        if (ModuleManager.tower.isVerticalTowering()) {
            if (blockRotations != null && (!getVTR || ModuleManager.tower.ebDelay <= 1 || !ModuleManager.tower.firstVTP)) {
                VTRY = blockRotations[0];
                getVTR = true;
            }
            if (getVTR) {
                e.setYaw(VTRY);
            }
            if (ModuleManager.tower.yaw != 0) {
                e.setYaw(ModuleManager.tower.yaw);
            }
            if (ModuleManager.tower.pitch != 0) {
                e.setPitch(ModuleManager.tower.pitch);
            }
        }
        else {
            getVTR = false;
        }
    }

    private float lastMY;

    private void handleV() {
        float yawBackwards2 = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - hardcodedYaw();
        double dif = (lastMY - getMotionYaw());
        double v = 2.5;
        float offset = (yawWithOffset - yawBackwards2);
        if (offset > yawAngle || offset < -yawAngle) {
            lastYawS = getSmooth = smoothedYaw = yaw;
            return;
        }
        if (dif >= 0 && dif < v || dif <= -0 && dif > -v || mc.thePlayer.onGround) {
            lastYawS = getSmooth = smoothedYaw = yaw;
            return;
        }

        getSmooth = yaw;
        float yawDifference = Utils.getAngleDifference(lastYawS, getSmooth);
        float smoothingFactor = 0.1f;
        getSmooth = (lastYawS + yawDifference * smoothingFactor);
        lastYawS = getSmooth;
        smoothedYaw = getSmooth;
        yaw = smoothedYaw;
    }

    private void handleSmoothing() {
        handleV();

        lastMY = getMotionYaw();
    }

    private void offsetRots(ClientRotationEvent e) {
        float moveAngle = (float) getMovementAngle();
        float relativeYaw = mc.thePlayer.rotationYaw + moveAngle;
        float normalizedYaw = (relativeYaw % 360 + 360) % 360;
        float quad = normalizedYaw % 90;

        float side = MathHelper.wrapAngleTo180_float(getMotionYaw() - yaw);
        float yawBackwards = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - hardcodedYaw();
        float blockYawOffset = MathHelper.wrapAngleTo180_float(yawBackwards - blockYaw);

        long strokeDelay = 250;

        float first = 76F;
        float sec = 78F;

        if (quad <= 5 || quad >= 85) {
            yawAngle = 126.425F;
            minOffset = 11;
            minPitch = first;
        }
        if (quad > 5 && quad <= 15 || quad >= 75 && quad < 85) {
            yawAngle = 127.825F;
            minOffset = 9;
            minPitch = first;
        }
        if (quad > 15 && quad <= 25 || quad >= 65 && quad < 75) {
            yawAngle = 129.625F;
            minOffset = 8;
            minPitch = first;
        }
        if (quad > 25 && quad <= 32 || quad >= 58 && quad < 65) {
            yawAngle = 130.485F;
            minOffset = 7;
            minPitch = sec;
        }
        if (quad > 32 && quad <= 38 || quad >= 52 && quad < 58) {
            yawAngle = 133.485F;
            minOffset = 6;
            minPitch = sec;
        }
        if (quad > 38 && quad <= 42 || quad >= 48 && quad < 52) {
            yawAngle = 135.625F;
            minOffset = 4;
            minPitch = sec;
        }
        if (quad > 42 && quad <= 45 || quad >= 45 && quad < 48) {
            yawAngle = 137.625F;
            minOffset = 3;
            minPitch = sec;
        }
        //float offsetAmountD = ((((float) offsetAmount.getInput() / 10) - 10) * -2) - (((float) offsetAmount.getInput() / 10) - 10);
        //yawAngle += offsetAmountD;

        float offset = yawAngle;//(!Utils.scaffoldDiagonal(false)) ? 125.500F : 143.500F;


        float nigger = 0;

        if (quad > 45) {
            nigger = 10;
        }
        else {
            nigger = -10;
        }
        if (switchvl > 0) {
                    /*if (vlS > 0 && (System.currentTimeMillis() - vlS) > strokeDelay && firstStroke == 0) {
                        switchvl = 0;
                        vlS = 0;
                    }*/
            //if (switchvl > 0) {
            firstStroke = Utils.time();
            switchvl = 0;
            vlS = 0;
            resetm = true;
            //}
        }
        else {
            vlS = Utils.time();
        }
        if (firstStroke > 0 && (Utils.time() - firstStroke) > strokeDelay) {
            firstStroke = 0;
        }
        if (Utils.fallDist() <= 2 && Utils.getHorizontalSpeed() > 0.1) {
            enabledOffGround = false;
        }
        if (enabledOffGround) {
            if (blockRotations != null) {
                yaw = blockRotations[0];
                pitch = blockRotations[1];
            } else {
                yaw = mc.thePlayer.rotationYaw - hardcodedYaw() - nigger;
                pitch = minPitch;
            }
            e.setRotations(yaw, pitch);
            return;
        }

        if (blockRotations != null) {
            blockYaw = blockRotations[0];
            pitch = blockRotations[1];
            yawOffset = blockYawOffset;
            if (pitch < minPitch) {
                pitch = minPitch;
            }
        } else {
            pitch = minPitch;
            if (edge == 1 && ((quad <= 3 || quad >= 87) && !Utils.scaffoldDiagonal(false))) {
                firstStroke = Utils.time();
            }
            yawOffset = 5;
            dynamic = 2;
        }

        if (!Utils.isMoving() || Utils.getHorizontalSpeed() == 0.0D) {
            e.setRotations(theYaw, pitch);
            return;
        }

        float motionYaw = getMotionYaw();

        float newYaw = motionYaw - offset * Math.signum(
                MathHelper.wrapAngleTo180_float(motionYaw - yaw)
        );
        yaw = MathHelper.wrapAngleTo180_float(newYaw);

        if (quad > 3 && quad < 87 && dynamic > 0) {
            if (quad < 45F) {
                if (firstStroke == 0) {
                    if (side >= 0) {
                        set2 = false;
                    } else {
                        set2 = true;
                    }
                }
                if (was452) {
                    switchvl++;
                }
                was451 = true;
                was452 = false;
            } else {
                if (firstStroke == 0) {
                    if (side >= 0) {
                        set2 = true;
                    } else {
                        set2 = false;
                    }
                }
                if (was451) {
                    switchvl++;
                }
                was452 = true;
                was451 = false;
            }
        }
        double minSwitch = (!Utils.scaffoldDiagonal(false)) ? 9 : 15;
        if (side >= 0) {
            if (yawOffset <= -minSwitch && firstStroke == 0 && dynamic > 0) {
                if (quad <= 3 || quad >= 87) {
                    if (set2) {
                        switchvl++;
                    }
                    set2 = false;
                }
            } else if (yawOffset >= 0 && firstStroke == 0 && dynamic > 0) {
                if (quad <= 3 || quad >= 87) {
                    if (yawOffset >= minSwitch) {
                        if (!set2) {
                            switchvl++;
                        }
                        set2 = true;
                    }
                }
            }
            if (set2) {
                if (yawOffset <= -0) yawOffset = -0;
                if (yawOffset >= minOffset) yawOffset = minOffset;
                theYaw = (yaw + offset * 2) - yawOffset;
                e.setRotations(theYaw, pitch);
                return;
            }
        } else if (side <= -0) {
            if (yawOffset >= minSwitch && firstStroke == 0 && dynamic > 0) {
                if (quad <= 3 || quad >= 87) {
                    if (set2) {
                        switchvl++;
                    }
                    set2 = false;
                }
            } else if (yawOffset <= 0 && firstStroke == 0 && dynamic > 0) {
                if (quad <= 3 || quad >= 87) {
                    if (yawOffset <= -minSwitch) {
                        if (!set2) {
                            switchvl++;
                        }
                        set2 = true;
                    }
                }
            }
            if (set2) {
                if (yawOffset >= 0) yawOffset = 0;
                if (yawOffset <= -minOffset) yawOffset = -minOffset;
                theYaw = (yaw - offset * 2) - yawOffset;
                e.setRotations(theYaw, pitch);
                return;
            }
        }

        if (side >= 0) {
            if (yawOffset >= 0) yawOffset = 0;
            if (yawOffset <= -minOffset) yawOffset = -minOffset;
        } else if (side <= -0) {
            if (yawOffset <= -0) yawOffset = -0;
            if (yawOffset >= minOffset) yawOffset = minOffset;
        }
        theYaw = yaw - yawOffset;
        e.setRotations(theYaw, pitch);
    }

    private void offsetRots2(ClientRotationEvent e) {
        float moveAngle2 = (float) getMovementAngle();
        float relativeYaw2 = mc.thePlayer.rotationYaw + moveAngle2;
        float normalizedYaw2 = (relativeYaw2 % 360 + 360) % 360;
        float quad2 = normalizedYaw2 % 90;

        float yawBackwards2 = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - hardcodedYaw();
        float blockYawOffset2 = MathHelper.wrapAngleTo180_float(yawBackwards2 - blockYaw);

        long sDelay = 250;

        if (switchvl > 0) {
            /*if (vlS > 0 && (System.currentTimeMillis() - vlS) > strokeDelay && firstStroke == 0) {
                switchvl = 0;
                vlS = 0;
            }*/
            //if (switchvl > 0) {
            firstStroke = Utils.time();
            switchvl = 0;
            vlS = 0;
            resetm = true;
            //}
        }
        else {
            vlS = Utils.time();
        }
        if (firstStroke > 0 && (System.currentTimeMillis() - firstStroke) > sDelay) {
            firstStroke = 0;
        }

        float firstv = 77.5F;
        float secv = 77.5F;

        if (quad2 <= 5 || quad2 >= 85) {
            yawAngle = 58.685F;
            minOffset = 13;
            minPitch = firstv;
        }
        if (quad2 > 5 && quad2 <= 15 || quad2 >= 75 && quad2 < 85) {
            yawAngle = 56.185F;
            minOffset = 11;
            minPitch = firstv;
        }
        if (quad2 > 15 && quad2 <= 25 || quad2 >= 65 && quad2 < 75) {
            yawAngle = 53.385F;
            minOffset = 9;
            minPitch = firstv;
        }
        if (quad2 > 25 && quad2 <= 32 || quad2 >= 58 && quad2 < 65) {
            yawAngle = 50.385F;
            minOffset = 7;
            minPitch = secv;
        }
        if (quad2 > 32 && quad2 <= 38 || quad2 >= 52 && quad2 < 58) {
            yawAngle = 48.185F;
            minOffset = 6;
            minPitch = secv;
        }
        if (quad2 > 38 && quad2 <= 42 || quad2 >= 48 && quad2 < 52) {
            yawAngle = 46.185F;
            minOffset = 4;
            minPitch = secv;
        }
        if (quad2 > 42 && quad2 <= 45 || quad2 >= 45 && quad2 < 48) {
            yawAngle = 42.885F;
            minOffset = 3;
            minPitch = secv;
        }

        if (blockRotations != null) {
            blockYaw = blockRotations[0];
            pitch = blockRotations[1];
            yawOffset = blockYawOffset2;
            if (pitch < minPitch) {
                pitch = minPitch;
            }
        } else {
            pitch = minPitch;
            if (edge == 1 && ((quad2 <= 3 || quad2 >= 87) && !Utils.scaffoldDiagonal(false))) {
                switchvl++;
            }
            yawOffset = 5;
            dynamic = 2;
        }

        if (quad2 > 3 && quad2 < 87 && dynamic > 0) {
            if (quad2 < 45F) {
                if (firstStroke == 0) {
                    set2 = true;
                }
                if (was452) {
                    switchvl++;
                }
                was451 = true;
                was452 = false;
            } else {
                if (firstStroke == 0) {
                    set2 = false;
                }
                if (was451) {
                    switchvl++;
                }
                was452 = true;
                was451 = false;
            }
        }
        double minSwitch2 = (!Utils.scaffoldDiagonal(false)) ? 9 : 15;

        if (yawOffset <= -minSwitch2 && firstStroke == 0 && dynamic > 0) {
            if (quad2 <= 3 || quad2 >= 87) {
                if (set2) {
                    switchvl++;
                }
                set2 = true;
            }
        } else if (yawOffset >= 0 && firstStroke == 0 && dynamic > 0) {
            if (quad2 <= 3 || quad2 >= 87) {
                if (yawOffset >= minSwitch2) {
                    if (!set2) {
                        switchvl++;
                    }
                    set2 = false;
                }
            }
        }

        if (set2) {
            yaw = ((yawBackwards2));
            if (yawOffset <= -0) yawOffset = 0;
            if (yawOffset >= minOffset) yawOffset = minOffset;
            yawWithOffset = yaw + yawAngle - yawOffset;
            neg = false;
            handleSmoothing();
            e.setRotations(yaw + yawAngle - yawOffset, pitch);
            return;
        }

        yaw = ((yawBackwards2));
        if (yawOffset >= 0) yawOffset = 0;
        if (yawOffset <= -minOffset) yawOffset = -minOffset;
        yawWithOffset = yaw - yawAngle - yawOffset;
        neg = true;
        handleSmoothing();
        e.setRotations(yaw - yawAngle - yawOffset, pitch);
    }

    private void preciseRots(ClientRotationEvent e) {
        if (blockRotations != null) {
            yaw = blockRotations[0];
            pitch = blockRotations[1];
        }
        else {
            yaw = mc.thePlayer.rotationYaw - hardcodedYaw();
            pitch = 80F;
        }
        e.setRotations(yaw, pitch);
        theYaw = yaw;
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (!ModuleManager.scaffold.isEnabled) {
            return;
        }
        if (!fastScaffoldKeepY && !floatKeepY) {
            return;
        }
        mc.thePlayer.movementInput.jump = false;
    }

    @SubscribeEvent
    public void onSlotUpdate(SlotUpdateEvent e) {
        if (isEnabled && autoSwap.isToggled()) {
            lastSlot.set(e.slot);
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        stopUpdate = stopUpdate2 = false;
        if (!isEnabled) {
            stopUpdate2 = true;
        }
        if (LongJump.function) {
            startYPos = -1;
        }
        if (LongJump.stopModules) {
            stopUpdate2 = true;
        }
        if (!stopUpdate2) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            if (holdingBlocks() && setSlot()) {
                /*if (moduleEnabled && !finishProcedure) {
                    if (Utils.distanceToGround(mc.thePlayer) < 2) {
                        finishProcedure = true;
                    }
                    if (Utils.distanceToGround(mc.thePlayer) > 6) {
                        if (hasPlaced) {
                            finishProcedure = true;
                        }
                    } else if (!finishProcedure) {
                        stopUpdate = true;
                    }
                }*/
                if (!stopUpdate) {
                    hasSwapped = true;
                    int mode = (int) fastScaffold.getInput();
                    if (!ModuleManager.tower.placeExtraBlock) {
                        if (rotation.getInput() == 0 || rotationDelay == 0) {
                            placeBlock(0, 0);
                        }
                    } else if ((ModuleManager.tower.ebDelay == 0 || !ModuleManager.tower.firstVTP)) {
                        placeBlock(0, 0);
                        placedVP = true;
                    }
                    if (ModuleManager.tower.placeExtraBlock) {
                        placeBlock(0, -1);
                    }

                    if (fastScaffoldKeepY && !ModuleManager.tower.canTower()) {
                        ++keepYTicks;
                        if ((int) mc.thePlayer.posY > (int) startYPos) {
                            switch (mode) {
                                case 1:
                                    if (!firstKeepYPlace && keepYTicks == 3) {
                                        placeBlock(1, 0);
                                        firstKeepYPlace = true;
                                    }
                                    break;
                                case 2:
                                    if (!firstKeepYPlace && keepYTicks == 8 || keepYTicks == 11) {
                                        placeBlock(1, 0);
                                        firstKeepYPlace = true;
                                    }
                                    break;
                                case 3:
                                    if (!firstKeepYPlace && keepYTicks == 8 || firstKeepYPlace && keepYTicks == 7) {
                                        placeBlock(1, 0);
                                        firstKeepYPlace = true;
                                    }
                                    break;
                                case 4:
                                    if (!firstKeepYPlace && keepYTicks == 7) {
                                        placeBlock(1, 0);
                                        firstKeepYPlace = true;
                                    }
                                    break;
                            }
                        }
                        if (mc.thePlayer.onGround) keepYTicks = 0;
                        if ((int) mc.thePlayer.posY == (int) startYPos) firstKeepYPlace = false;
                    }
                    handleMotion();
                }
            }
        }

        if (disabledModule) {
            if (hasPlaced && (towerEdge || floatStarted && Utils.isMoving())) {
                dontDisable = true;
            }

            if (dontDisable && ++disableTicks >= 2) {
                isEnabled = false;
            }
            if (!dontDisable) {
                isEnabled = false;
            }


            if (!isEnabled) {
                disabledModule = dontDisable = false;
                disableTicks = 0;

                if (ModuleManager.tower.speed) {
                    Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer) / 1.6);
                }

                if (lastSlot.get() != -1) {
                    mc.thePlayer.inventory.currentItem = lastSlot.get();
                    lastSlot.set(-1);
                }
                blockSlot = -1;
                if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled()) {
                    ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(false);
                    ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(false);
                }
                if (offsetDelay > 0) {
                    ModuleManager.sprint.requireJump = false;
                }
                scaffoldBlockCount.beginFade();
                hasSwapped = hasPlaced = false;
                targetBlock = null;
                blockInfo = null;
                blockRotations = null;
                fastScaffoldKeepY = firstKeepYPlace = rotateForward = rotatingForward = floatStarted = floatJumped = floatWasEnabled = towerEdge =
                        was451 = was452 = enabledOffGround = finishProcedure = jump = blink = false;
                rotationDelay = keepYTicks = scaffoldTicks = floatTicks = rt = 0;
                firstStroke = vlS = 0;
                startYPos = -1;
                lookVec = null;
                lastPlacement = null;
            }
        }
    }

    @Override
    public String getInfo() {
        String info;
        if (fastOnRMB.isToggled()) {
            info = fastOnRMB() ? fastScaffoldModes[(int) fastScaffold.getInput()] : sprintModes[(int) sprint.getInput()];
        }
        else {
            info = fastScaffold.getInput() > 0 ? fastScaffoldModes[(int) fastScaffold.getInput()] : sprintModes[(int) sprint.getInput()];
        }
        return info;
    }

    public boolean stopFastPlace() {
        return this.isEnabled();
    }

    public void rotateForward(boolean delay) {
        if (jumpFacingForward.isToggled()) {
            if (rotation.getInput() > 0) {
                if (!rotatingForward) {
                    if (delay) {
                        rotationDelay = 2;
                    }
                }
                rotatingForward = rotateForward = true;
            }
        }
    }

    public boolean blockAbove() {
        return !(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)) instanceof BlockAir);
    }

    public boolean sprint() {
        if (isEnabled) {
            return handleFastScaffolds() > 0;
        }
        return false;
    }

    private int handleFastScaffolds() {
        if (fastOnRMB.isToggled()) {
            return fastScaffold.getInput() > 0 && fastOnRMB() ? (int) fastScaffold.getInput() : (int) sprint.getInput();
        }
        else {
            return fastScaffold.getInput() > 0 ? (int) fastScaffold.getInput() : (int) sprint.getInput();
        }
    }

    private boolean usingFloat() {
        return sprint.getInput() == 2 && Utils.isMoving() && !usingFastScaffold();
    }

    private boolean sprintScaf() {
        return sprint.getInput() > 0 && Utils.isMoving() && mc.thePlayer.onGround && !usingFastScaffold() && !ModuleManager.tower.canTower();
    }

    public boolean usingFastScaffold() {
        return fastScaffold.getInput() > 0 && (!fastOnRMB.isToggled() || fastOnRMB() && Utils.tabbedIn()) && !prioritizeSprint();
    }

    public boolean fastOnRMB() {
        return fastOnRMB.isToggled() && Utils.tabbedIn() && (Mouse.isButtonDown(1) || ModuleManager.bhop.isEnabled() || defPS());
    }

    private boolean defPS() {
        return prioritizeSprintWithSpeed.isToggled() && (sprint.getInput() == 0 || getSpeedLevel() == 0);
    }

    private boolean prioritizeSprint() {
        return prioritizeSprintWithSpeed.isToggled() && sprint.getInput() > 0 && getSpeedLevel() > 0 && !fastOnRMB();
    }

    public boolean safewalk() {
        return this.isEnabled() && safeWalk.isToggled();
    }

    public boolean stopRotation() {
        return this.isEnabled() && rotation.getInput() > 0;
    }

    private void place(PlaceData block) {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            return;
        }
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, block.blockPos, block.enumFacing, block.hitVec)) {
            if (silentSwing.isToggled()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            }
            else {
                mc.thePlayer.swingItem();
                if (holdingBlocks()) {
                    mc.getItemRenderer().resetEquippedProgress();
                }
            }
            if (ModuleManager.tower.placeExtraBlock) {
                ModuleManager.tower.firstVTP = true;
            }
            highlight.put(block.blockPos.offset(block.enumFacing), null);
            hasPlaced = true;
        }
    }

    public boolean canSafewalk() {
        if (!safeWalk.isToggled()) {
            return false;
        }
        if (usingFastScaffold()) {
            return false;
        }
        if (ModuleManager.tower.canTower()) {
            return false;
        }
        if (!isEnabled) {
            return false;
        }
        return true;
    }

    public int totalBlocks() {
        int totalBlocks = 0;
        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) stack.getItem()) && stack.stackSize > 0) {
                totalBlocks += stack.stackSize;
            }
        }
        return totalBlocks;
    }

    private void placeBlock(int yOffset, int xOffset) {
        locateAndPlaceBlock(yOffset, xOffset);
        int input = (int) multiPlace.getInput();
        if (input >= 1) {
            locateAndPlaceBlock(yOffset, xOffset);
            if (input >= 2) {
                locateAndPlaceBlock(yOffset, xOffset);
                if (input >= 3) {
                    locateAndPlaceBlock(yOffset, xOffset);
                    if (input >= 4) {
                        locateAndPlaceBlock(yOffset, xOffset);
                    }
                }
            }
        }
    }

    private void locateAndPlaceBlock(int yOffset, int xOffset) {
        locateBlocks(yOffset, xOffset);
        if (blockInfo == null) {
            return;
        }
        lastPlacement = blockInfo;
        place(blockInfo);
        blockInfo = null;
    }

    private void locateBlocks(int yOffset, int xOffset) {
        List<PlaceData> blocksInfo = findBlocks(yOffset, xOffset);

        if (blocksInfo == null) {
            return;
        }

        double sumX = 0, sumY = !mc.thePlayer.onGround ? 0 : blocksInfo.get(0).blockPos.getY(), sumZ = 0;
        int index = 0;
        for (PlaceData blockssInfo : blocksInfo) {
            if (index > 1 || (!Utils.isDiagonal(false) && index > 0 && mc.thePlayer.onGround)) {
                break;
            }
            sumX += blockssInfo.blockPos.getX();
            if (!mc.thePlayer.onGround) {
                sumY += blockssInfo.blockPos.getY();
            }
            sumZ += blockssInfo.blockPos.getZ();
            index++;
        }

        double avgX = sumX / index;
        double avgY = !mc.thePlayer.onGround ? sumY / index : blocksInfo.get(0).blockPos.getY();
        double avgZ = sumZ / index;

        targetBlock = new Vec3(avgX, avgY, avgZ);

        PlaceData blockInfo2 = blocksInfo.get(0);
        int blockX = blockInfo2.blockPos.getX();
        int blockY = blockInfo2.blockPos.getY();
        int blockZ = blockInfo2.blockPos.getZ();
        EnumFacing blockFacing = blockInfo2.enumFacing;
        blockInfo = blockInfo2;

        double hitX = (blockX + 0.5D) + getCoord(blockFacing.getOpposite(), "x") * 0.5D;
        double hitY = (blockY + 0.5D) + getCoord(blockFacing.getOpposite(), "y") * 0.5D;
        double hitZ = (blockZ + 0.5D) + getCoord(blockFacing.getOpposite(), "z") * 0.5D;
        lookVec = new Vec3(0.5D + getCoord(blockFacing.getOpposite(), "x") * 0.5D, 0.5D + getCoord(blockFacing.getOpposite(), "y") * 0.5D, 0.5D + getCoord(blockFacing.getOpposite(), "z") * 0.5D);
        hitVec = new Vec3(hitX, hitY, hitZ);
        blockInfo.hitVec = hitVec;
    }

    private double getCoord(EnumFacing facing, String axis) {
        switch (axis) {
            case "x": return (facing == EnumFacing.WEST) ? -0.5 : (facing == EnumFacing.EAST) ? 0.5 : 0;
            case "y": return (facing == EnumFacing.DOWN) ? -0.5 : (facing == EnumFacing.UP) ? 0.5 : 0;
            case "z": return (facing == EnumFacing.NORTH) ? -0.5 : (facing == EnumFacing.SOUTH) ? 0.5 : 0;
        }
        return 0;
    }

    private List<PlaceData> findBlocks(int yOffset, int xOffset) {
        int x = (int) Math.floor(mc.thePlayer.posX + xOffset);
        int y = (int) Math.floor(((startYPos != -1) ? startYPos : mc.thePlayer.posY) + yOffset);
        int z = (int) Math.floor(mc.thePlayer.posZ);

        BlockPos base = new BlockPos(x, y - 1, z);

        if (!BlockUtils.replaceable(base)) {
            return null;
        }

        EnumFacing[] allFacings = getFacingsSorted();
        List<EnumFacing> validFacings = new ArrayList<>(5);
        for (EnumFacing facing : allFacings) {
            if (facing != EnumFacing.UP && placeConditions(facing, yOffset, xOffset)) {
                validFacings.add(facing);
            }
        }
        int maxLayer = 1;
        List<PlaceData> possibleBlocks = new ArrayList<>();

        for (int dy = 1; dy <= maxLayer; dy++) {
            BlockPos layerBase = new BlockPos(x, y - dy, z);
            if (dy == 1) {
                for (EnumFacing facing : validFacings) {
                    BlockPos neighbor = layerBase.offset(facing);
                    if (!BlockUtils.replaceable(neighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(neighbor))) {
                        possibleBlocks.add(new PlaceData(neighbor, facing.getOpposite()));
                    }
                }
            }
            for (EnumFacing facing : validFacings) {
                BlockPos adjacent = layerBase.offset(facing);
                if (BlockUtils.replaceable(adjacent)) {
                    for (EnumFacing nestedFacing : validFacings) {
                        BlockPos nestedNeighbor = adjacent.offset(nestedFacing);
                        if (!BlockUtils.replaceable(nestedNeighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(nestedNeighbor))) {
                            possibleBlocks.add(new PlaceData(nestedNeighbor, nestedFacing.getOpposite()));
                        }
                    }
                }
            }
            for (EnumFacing facing : validFacings) {
                BlockPos adjacent = layerBase.offset(facing);
                if (BlockUtils.replaceable(adjacent)) {
                    for (EnumFacing nestedFacing : validFacings) {
                        BlockPos nestedNeighbor = adjacent.offset(nestedFacing);
                        if (BlockUtils.replaceable(nestedNeighbor)) {
                            for (EnumFacing thirdFacing : validFacings) {
                                BlockPos thirdNeighbor = nestedNeighbor.offset(thirdFacing);
                                if (!BlockUtils.replaceable(thirdNeighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(thirdNeighbor))) {
                                    possibleBlocks.add(new PlaceData(thirdNeighbor, thirdFacing.getOpposite()));
                                }
                            }
                        }
                    }
                }
            }
        }

        return possibleBlocks.isEmpty() ? null : possibleBlocks;
    }

    private EnumFacing[] getFacingsSorted() {
        EnumFacing lastFacing = EnumFacing.getHorizontal(MathHelper.floor_double((((IAccessorEntityPlayerSP)mc.thePlayer).getLastReportedYaw() * 4.0F / 360.0F) + 0.5D) & 3);

        EnumFacing perpClockwise = lastFacing.rotateY();
        EnumFacing perpCounterClockwise = lastFacing.rotateYCCW();

        EnumFacing opposite = lastFacing.getOpposite();

        float yaw = ((IAccessorEntityPlayerSP)mc.thePlayer).getLastReportedYaw() % 360;
        if (yaw > 180) {
            yaw -= 360;
        }
        else if (yaw < -180) {
            yaw += 360;
        }

        // Calculates the difference from the last placed angle and gets the closest one
        float diffClockwise = Math.abs(MathHelper.wrapAngleTo180_float(yaw - getFacingAngle(perpClockwise)));
        float diffCounterClockwise = Math.abs(MathHelper.wrapAngleTo180_float(yaw - getFacingAngle(perpCounterClockwise)));

        EnumFacing firstPerp, secondPerp;
        if (diffClockwise <= diffCounterClockwise) {
            firstPerp = perpClockwise;
            secondPerp = perpCounterClockwise;
        }
        else {
            firstPerp = perpCounterClockwise;
            secondPerp = perpClockwise;
        }

        return new EnumFacing[]{EnumFacing.UP, EnumFacing.DOWN, lastFacing, firstPerp, secondPerp, opposite};
    }

    private float getFacingAngle(EnumFacing facing) {
        switch (facing) {
            case WEST:
                return 90;
            case NORTH:
                return 180;
            case EAST:
                return -90;
            default:
                return 0;
        }
    }

    private boolean placeConditions(EnumFacing enumFacing, int yCondition, int xCondition) {
        if (xCondition == -1) {
            if (!ModuleManager.tower.placeExtraBlock) {
                return enumFacing == EnumFacing.EAST;
            }
            else {
                return enumFacing == EnumFacing.DOWN;
            }
        }
        else if (ModuleManager.tower.placeExtraBlock) {
            return enumFacing == EnumFacing.WEST;
        }
        if (yCondition == 1) {
            return enumFacing == EnumFacing.DOWN;
        }
        return true;
    }

    float applyGcd(float value) {
        float gcd = 0.2F * 0.2F * 0.2F * 8.0F;
        return (float) ((double) value - (double) value % ((double) gcd * 0.15D));
    }

    public float getMotionYaw() {
        return (float) Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0F;
    }

    private int getSpeedLevel() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            if (potionEffect.getEffectName().equals("potion.moveSpeed")) {
                return potionEffect.getAmplifier() + 1;
            }
            return 0;
        }
        return 0;
    }

    double[] speedLevels = {0.48, 0.5, 0.52, 0.58, 0.68};

    double getSpeed(int speedLevel) {
        if (speedLevel >= 0) {
            return speedLevels[speedLevel];
        }
        return speedLevels[0];
    }

    double[] floatSpeedLevels = {0.2, 0.22, 0.265, 0.29, 0.3};

    double getFloatSpeed(int speedLevel) {
        double min = 0;
        double value = 0;
        double input = (motion.getInput() / 100);
        if (mc.thePlayer.moveStrafing != 0 && mc.thePlayer.moveForward != 0) min = 0.003;
        value = floatSpeedLevels[0] - min;
        if (speedLevel >= 0) {
            value = (speedLevel == 2 ? (Utils.scaffoldDiagonal(false) ? 0.265 : 0.275) : floatSpeedLevels[speedLevel]) - min;
        }
        value *= input;
        return value;
    }

    private void handleMotion() {
        if (usingFastScaffold() || usingFloat() || ModuleManager.tower.canTower() || motion.getInput() == 100 || !mc.thePlayer.onGround) {
            return;
        }
        double input = (motion.getInput() / 100);
        mc.thePlayer.motionX *= input;
        mc.thePlayer.motionZ *= input;
    }

    public float hardcodedYaw() {
        float simpleYaw = 0F;

        boolean w = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        boolean s = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
        boolean a = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
        boolean d = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());

        boolean dupe = a & d;

        if (w) {
            simpleYaw -= 180;
            if (!dupe) {
                if (a) simpleYaw += 45;
                if (d) simpleYaw -= 45;
            }
        }
        else if (!s) {
            simpleYaw -= 180;
            if (!dupe) {
                if (a) simpleYaw += 90;
                if (d) simpleYaw -= 90;
            }
        }
        else if (!w) {
            if (!dupe) {
                if (a) simpleYaw -= 45;
                if (d) simpleYaw += 45;
            }
        }
        return simpleYaw;
    }

    public boolean holdingBlocks() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled() && lastSlot.get() != mc.thePlayer.inventory.currentItem && totalBlocks() > 0) {
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(true);
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(true);
        }
        if (!autoSwap.isToggled() || getSlot() == -1) {
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
                return false;
            }
        }
        return true;
    }

    private double getMovementAngle() {
        double angle = Math.toDegrees(Math.atan2(-mc.thePlayer.moveStrafing, mc.thePlayer.moveForward));
        return angle == -0 ? 0 : angle;
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
                if (itemStack != null && heldItem != null && (heldItem.getItem() instanceof ItemBlock) && Utils.canBePlaced((ItemBlock) heldItem.getItem()) && ModuleManager.autoSwap.sameType.isToggled() && !(itemStack.getItem().getClass().equals(heldItem.getItem().getClass()))) {
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

    public static boolean bypassRots() {
        return (ModuleManager.scaffold.rotation.getInput() == 2 || ModuleManager.scaffold.rotation.getInput() == 0);
    }

    public boolean setSlot() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        int slot = getSlot();
        if (slot == -1) {
            return false;
        }
        if (blockSlot == -1) {
            blockSlot = slot;
        }
        if (lastSlot.get() == -1) {
            lastSlot.set(mc.thePlayer.inventory.currentItem);
        }
        if (autoSwap.isToggled() && blockSlot != -1) {
            if (ModuleManager.autoSwap.swapToGreaterStack.isToggled()) {
                mc.thePlayer.inventory.currentItem = slot;
                spoofSlot = slot;
            }
            else {
                if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem()) || mc.thePlayer.getHeldItem().stackSize <= ModuleManager.autoSwap.swapAt.getInput()) {
                    mc.thePlayer.inventory.currentItem = slot;
                    spoofSlot = slot;
                }
            }
        }

        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            blockSlot = -1;
            return false;
        }
        return true;
    }


    static class PlaceData {
        EnumFacing enumFacing;
        BlockPos blockPos;
        Vec3 hitVec;

        PlaceData(BlockPos blockPos, EnumFacing enumFacing) {
            this.enumFacing = enumFacing;
            this.blockPos = blockPos;
        }

        public PlaceData(EnumFacing enumFacing, BlockPos blockPos) {
            this.enumFacing = enumFacing;
            this.blockPos = blockPos;
        }
    }
}