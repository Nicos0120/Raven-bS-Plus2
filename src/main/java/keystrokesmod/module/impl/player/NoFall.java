package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.*;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NoFall extends Module {
    public SliderSetting mode;
    public ButtonSetting initialPosition;
    private SliderSetting minFallDistance;
    private ButtonSetting disableAdventure;
    private ButtonSetting ignoreVoid, voidC;
    private ButtonSetting hideSound;
    public ButtonSetting renderTimer;
    private ButtonSetting disableTp;
    private String[] modes = new String[]{"Spoof", "NoGround", "A-Fall", "B-Fall", "CTW", "Prediction"};

    private int color = new Color(0, 187, 255, 255).getRGB();

    private double initialY;
    private double dynamic;
    public boolean isFalling;
    private double timerVal = 1;

    private int n;

    public boolean bnFalling, blink;

    private int y;

    private boolean tp;
    private double lastX, lastY, lastZ;

    public NoFall() {
        super("NoFall", category.player);
        this.registerSetting(mode = new SliderSetting("Mode", 2, modes));
        this.registerSetting(disableAdventure = new ButtonSetting("Disable adventure", false));
        this.registerSetting(minFallDistance = new SliderSetting("Minimum fall distance", 3, 0, 10, 0.1));
        this.registerSetting(ignoreVoid = new ButtonSetting("Ignore void", false));
        this.registerSetting(voidC = new ButtonSetting("Experimental void check", true));
        this.registerSetting(renderTimer = new ButtonSetting("Render Blink Timer", true));
        this.registerSetting(disableTp = new ButtonSetting("Disable on teleport", true));
        //this.registerSetting(hideSound = new ButtonSetting("Hide fall damage sound", false));
    }

    public void guiUpdate() {
        this.renderTimer.setVisible(mode.getInput() == 6, this);
    }

    public void onDisable() {
        Utils.resetTimer();
        blink = false;
        tp = false;
    }

    /*@SubscribeEvent
    public void onSoundEvent(SoundEvent.SoundSourceEvent e) {
        if (e.name.startsWith("game.player.hurt.fall")) {


        }
    }*/

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.getPacket() instanceof S08PacketPlayerPosLook) {
            if (n > 0) n = 34;
        }
    }


    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (mode.getInput() == 6) {
            if (Utils.fallDist() >= minFallDistance.getInput() && Utils.isEdgeOfBlock() && mc.thePlayer.onGround && !Utils.jumpDown() && !ModuleManager.scaffold.isEnabled && !ModuleManager.bhop.isEnabled() && !LongJump.function) {
                blink = true;
                y = (int) mc.thePlayer.posY;
            }
            else if (blink && !bnFalling && mc.thePlayer.posY > y) {
                blink = false;
            }
            if (mc.thePlayer.posY < y && !mc.thePlayer.onGround && blink) {
                bnFalling = true;
            }
            else if (bnFalling) {
                blink = false;
            }
            if (mc.thePlayer.posY <= y - 31 && blink) {
                blink = false;
            }
        }


        if (mc.thePlayer.posY >= lastY + 3.5D && !(mc.thePlayer.posY >= lastY + 10.0D) && ModuleUtils.hasTeleported && !ModuleUtils.worldChange) {
            if (disableTp.isToggled()) {
                Utils.modulePrint("§cMost likely staff checked, disabling NoFall until on ground");
                tp = true;
                Utils.ping();
            }
        }
        else if (mc.thePlayer.onGround && tp) {
            tp = false;
            Utils.modulePrint("§aNoFall re-enabled");
        }

        lastX = mc.thePlayer.posX;
        lastY = mc.thePlayer.posY;
        lastZ = mc.thePlayer.posZ;

        if (reset()) {
            Utils.resetTimer();
            initialY = mc.thePlayer.posY;
            isFalling = false;
            n = 0;
            timerVal = 1;
            return;
        }
        else if ((double) mc.thePlayer.fallDistance >= minFallDistance.getInput()) {
            isFalling = true;
        }


        double predictedY = mc.thePlayer.posY + mc.thePlayer.motionY;
        double distanceFallen = initialY - predictedY;
        if (isFalling && mode.getInput() == 2) {
            if (mc.thePlayer.motionY >= -1.0) {
                dynamic = 3.0;
            }
            if (mc.thePlayer.motionY < -1.0) {
                dynamic = 4.0;
            }
            if (mc.thePlayer.motionY < -2.0) {
                dynamic = 5.0;
            }
            if (distanceFallen >= dynamic) {
                if (mc.thePlayer.motionY < -0.01) {
                    timerVal = 0.8;
                }
                if (mc.thePlayer.motionY < -1.0) {
                    timerVal = 0.7;
                }
                if (mc.thePlayer.motionY < -1.6) {
                    timerVal = 0.6;
                }
                Utils.getTimer().timerSpeed = (float) timerVal;
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                initialY = mc.thePlayer.posY;
            }
        }
        if (isFalling && mode.getInput() == 3) {
            if (mc.thePlayer.motionY < -2.0) {
                dynamic = 4.0;
            }
            else {
                dynamic = 3.0;
            }
            Utils.resetTimer();
            if (mc.thePlayer.ticksExisted % 2 == 0) {
                if (mc.thePlayer.motionY < -0.01) {
                    timerVal = 0.64;
                }
                if (mc.thePlayer.motionY < -1.0) {
                    timerVal = 0.53;
                }
                if (mc.thePlayer.motionY < -1.6) {
                    timerVal = 0.46;
                }
                if (mc.thePlayer.motionY < -2.1) {
                    timerVal = 0.41;
                }
                Utils.getTimer().timerSpeed = (float) timerVal;
            }
            if (distanceFallen >= dynamic) {
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                initialY = mc.thePlayer.posY;
            }
        }
        if (isFalling && mode.getInput() == 4) {
            Utils.resetTimer();
            if (distanceFallen >= 7) {
                Utils.getTimer().timerSpeed = 0.7F;
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                initialY = mc.thePlayer.posY;
            }
        }
        if (isFalling && mode.getInput() == 5) {
            if (distanceFallen >= 3 && n <= 4) {
                mc.thePlayer.motionY = 0;
                n++;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent e) {
        switch ((int) mode.getInput()) {
            case 0:
                e.setOnGround(true);
                break;
            case 1:
                e.setOnGround(false);
                break;
            case 6:
                if (blink) {
                    e.setOnGround(true);
                }
                break;
        }
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }
    private boolean reset() {
        if (disableAdventure.isToggled() && mc.playerController.getCurrentGameType().isAdventure()) {
            return true;
        }
        if (ignoreVoid.isToggled() && Utils.overVoid()) {
            return true;
        }
        if (Utils.isBedwarsPractice()) {
            return true;
        }
        if (Utils.spectatorCheck()) {
            return true;
        }
        if (Utils.isReplay()) {
            return true;
        }
        if (mc.thePlayer.onGround) {
            return true;
        }
        if (BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ)) != Blocks.air) {
            return true;
        }
        if (mc.thePlayer.motionY > -0.0784) {
            return true;
        }
        if (mc.thePlayer.capabilities.isCreativeMode) {
            return true;
        }
        if (Utils.overVoid() && mc.thePlayer.posY <= 41) {
            return true;
        }
        if (mc.thePlayer.capabilities.isFlying) {
            return true;
        }
        if (voidC.isToggled() && Utils.overVoid() && !dist()) {
            return true;
        }
        if (tp) {
            return true;
        }
        return false;
    }





    public boolean dist() {
        double minMotion = 0.12;
        int dist1 = 4;
        int dist2 = 6;
        int dist3 = 7;
        int dist4 = 9;
        //1x1

        if (mc.thePlayer.isCollidedHorizontally) {
            return false;
        }

        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ) > dist1) {
            return true;
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ) > dist1) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ) > dist1) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ - 1) > dist1) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ + 1) > dist1) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ - 1) > dist1) {
            if (mc.thePlayer.motionX <= -minMotion && mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ + 1) > dist1) {
            if (mc.thePlayer.motionX >= minMotion && mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ + 1) > dist1) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ - 1) > dist1) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }

        // 2x2

        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ) > dist2) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ) > dist2) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ - 2) > dist2) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ + 2) > dist2) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ - 1) > dist2) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ - 2) > dist2) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ + 1) > dist2) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ + 2) > dist2) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ - 2) > dist2) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ - 2) > dist2) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ + 2) > dist2) {
            if (mc.thePlayer.motionX >= minMotion && mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ - 2) > dist2) {
            if (mc.thePlayer.motionX <= -minMotion && mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }

        // 3x3

        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ) > dist3) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionX >= minMotion && mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion && mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ + 1) > dist3) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ + 2) > dist3) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ - 1) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ - 2) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ - 1) > dist3) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ - 2) > dist3) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ + 1) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ + 2) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }

        // 4x4

        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ - 4) > dist4) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionX >= minMotion && mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ - 4) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion && mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ - 4) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ + 3) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ + 2) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ + 1) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ - 3) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ - 2) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ - 1) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ + 3) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ + 2) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ + 1) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ - 3) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ - 2) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ - 1) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ - 4) > dist4) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ - 4) > dist4) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ - 4) > dist4) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }





        return false;
    }

}