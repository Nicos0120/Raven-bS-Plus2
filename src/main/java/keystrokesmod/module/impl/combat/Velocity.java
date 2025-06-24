package keystrokesmod.module.impl.combat;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Velocity extends Module {
    public SliderSetting mode;
    public static SliderSetting vertical, horizontal, reverseHorizontal, explosionsHorizontal, explosionsVertical, verticalM;
    public static SliderSetting minExtraSpeed, extraSpeedBoost;
    private SliderSetting chance;
    private ButtonSetting onlyWhileAttacking;
    private ButtonSetting onlyWhileTargeting;
    private ButtonSetting disableS;
    private ButtonSetting zzWhileNotTargeting, delayPacket;
    public ButtonSetting allowSelfFireball;
    public static ButtonSetting reverseDebug;
    private KeySetting switchToReverse, switchToPacket;
    private ButtonSetting requireMouseDown;
    private ButtonSetting requireMovingForward;
    private ButtonSetting requireAim;
    private ButtonSetting delay;
    private ButtonSetting disableLobby;
    private boolean stopFBvelo;
    public boolean disableVelo;
    private boolean buttonDown, pDown, rDown;
    private boolean setJump;
    private boolean ignoreNext;
    private boolean aiming;
    private int lastHurtTime;
    private double lastFallDistance;

    public boolean blink;
    private int delayTicks;

    private int db;

    private String[] modes = new String[] { "Normal", "Hypixel", "Reverse", "Jump Reset" };


    public Velocity() {
        super("Velocity", category.combat);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
        this.registerSetting(horizontal = new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(vertical = new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(verticalM = new SliderSetting("Vertical Motion Limit", 1.0, -1.0, 1, 0.1));

        this.registerSetting(reverseHorizontal = new SliderSetting("-Horizontal", 0.0, 0.0, 100.0, 1.0));

        this.registerSetting(explosionsHorizontal = new SliderSetting("Horizontal (Explosions)", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(explosionsVertical = new SliderSetting("Vertical (Explosions)", 0.0, 0.0, 100.0, 1.0));

        this.registerSetting(minExtraSpeed = new SliderSetting("Maximum speed for extra boost", 0, 0, 0.7, 0.01));
        this.registerSetting(extraSpeedBoost = new SliderSetting("Extra speed boost multiplier", "%", 0, 0, 100, 1));


        this.registerSetting(chance = new SliderSetting("Chance", "%", 100.0D, 0.0D, 100.0D, 1.0D));
        this.registerSetting(onlyWhileAttacking = new ButtonSetting("Only while attacking", false));
        this.registerSetting(onlyWhileTargeting = new ButtonSetting("Only while targeting", false));
        this.registerSetting(disableS = new ButtonSetting("Disable while holding S", false));
        this.registerSetting(zzWhileNotTargeting = new ButtonSetting("00 while not targeting", false));
        this.registerSetting(allowSelfFireball = new ButtonSetting("Allow self fireball", false));

        this.registerSetting(switchToReverse = new KeySetting("Switch to reverse", Keyboard.KEY_SPACE));
        this.registerSetting(switchToPacket = new KeySetting("Switch to packet", Keyboard.KEY_SPACE));

        this.registerSetting(reverseDebug = new ButtonSetting("Show reverse debug messages", false));

        this.registerSetting(requireMouseDown = new ButtonSetting("Require mouse down", false));
        this.registerSetting(requireMovingForward = new ButtonSetting("Require moving forward", false));
        this.registerSetting(requireAim = new ButtonSetting("Require aim", false));
        this.registerSetting(delay = new ButtonSetting("Delay", false));

        this.registerSetting(disableLobby = new ButtonSetting("Disable in lobby", false));
    }

    public void guiUpdate() {
        this.onlyWhileAttacking.setVisible(mode.getInput() == 0, this);
        this.onlyWhileTargeting.setVisible(mode.getInput() == 0, this);
        this.disableS.setVisible(mode.getInput() == 0, this);

        this.allowSelfFireball.setVisible(mode.getInput() == 1, this);
        this.zzWhileNotTargeting.setVisible(mode.getInput() == 1, this);

        this.switchToReverse.setVisible(mode.getInput() == 1, this);
        this.switchToPacket.setVisible(mode.getInput() == 2, this);



        this.horizontal.setVisible(mode.getInput() != 2 && mode.getInput() != 3, this);
        this.vertical.setVisible(mode.getInput() != 2 && mode.getInput() != 3, this);
        this.verticalM.setVisible(mode.getInput() == 1, this);
        this.chance.setVisible(mode.getInput() != 2, this);
        this.reverseHorizontal.setVisible(mode.getInput() == 2, this);

        this.explosionsHorizontal.setVisible(mode.getInput() != 0 && mode.getInput() != 3, this);
        this.explosionsVertical.setVisible(mode.getInput() != 0 && mode.getInput() != 3, this);

        this.minExtraSpeed.setVisible(mode.getInput() == 2, this);
        this.extraSpeedBoost.setVisible(mode.getInput() == 2, this);
        this.reverseDebug.setVisible(mode.getInput() == 2, this);

        this.requireMouseDown.setVisible(mode.getInput() == 3, this);
        this.requireMovingForward.setVisible(mode.getInput() == 3, this);
        this.requireAim.setVisible(mode.getInput() == 3, this);
        this.delay.setVisible(mode.getInput() == 3, this);
    }

    @Override
    public String getInfo() {
        return mode.getInput() == 2 ? "-" + ((int) reverseHorizontal.getInput()) + "%" : (mode.getInput() == 3 ? modes[(int) mode.getInput()] : ((int) horizontal.getInput() + "%" + " " + (int) vertical.getInput() + "%"));
    }

    @Override
    public void onDisable() {
        blink = false;
        delayTicks = 0;
        stopFBvelo = disableVelo = false;
        buttonDown = pDown = rDown = false;
        setJump = ignoreNext = aiming = false;
        lastHurtTime = 0;
        lastFallDistance = 0;
        db = 0;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (Utils.tabbedIn()) {
            if (switchToReverse.isPressed() && mode.getInput() == 1 && !buttonDown) {
                mode.setValue(2);
                buttonDown = true;
                Utils.modulePrint(Utils.formatColor("&7[&dR&7]&7 Switched to &bReverse&7 Velocity mode"));
            }
            if (switchToPacket.isPressed() && mode.getInput() == 2 && !buttonDown) {
                mode.setValue(1);
                buttonDown = true;
                Utils.modulePrint(Utils.formatColor("&7[&dR&7]&7 Switched to &bPacket&7 Velocity mode"));
            }
        }
        if (switchToReverse.isPressed() || switchToPacket.isPressed()) {
            buttonDown = true;
        }
        else {
            buttonDown = false;
        }

        if (delayTicks-- == 0 && delay.isToggled()) {
            blink = false;
        }

        if (db > 0) {
            db--;
        }

        int hurtTime = mc.thePlayer.hurtTime;
        boolean onGround = mc.thePlayer.onGround;

        if (onGround && lastFallDistance > 3 && !mc.thePlayer.capabilities.allowFlying) ignoreNext = true;
        if (hurtTime > lastHurtTime) {

            boolean mouseDown = Mouse.isButtonDown(0) || !requireMouseDown.isToggled();
            boolean aimingAt = aiming || !requireAim.isToggled();

            boolean forward = mc.gameSettings.keyBindForward.isKeyDown() || !requireMovingForward.isToggled();

            handlejr(onGround, aimingAt, forward, mouseDown);
            ignoreNext = false;
        }

        lastHurtTime = hurtTime;
        lastFallDistance = mc.thePlayer.fallDistance;
    }

    private void handlejr(boolean onGround, boolean aimingAt, boolean forward, boolean mouseDown) {
        if (mode.getInput() != 3) {
            return;
        }
        if (disableLobby.isToggled() && Utils.isLobby()) {
            return;
        }
        if (db > 0) {
            return;
        }
        if (!ignoreNext && onGround && aimingAt && forward && mouseDown && Utils.randomizeDouble(0, 100) < chance.getInput() && !hasBadEffect()) {
            if (delay.isToggled()) {
                blink = true;
                delayTicks = 3;
            }
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), setJump = true);
            KeyBinding.onTick(mc.gameSettings.keyBindJump.getKeyCode());
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7jumping enabled");
            }
        }
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        if (setJump && !Utils.jumpDown()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), setJump = false);
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7jumping disabled");
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacketAll(ReceiveAllPacketsEvent e) {
        if (e.getPacket() instanceof S27PacketExplosion) {
            db = 10;
        }
        if (mode.getInput() == 1) {
            if (!Utils.nullCheck() || LongJump.stopVelocity || e.isCanceled() || disableLobby.isToggled() && Utils.isLobby() || ModuleManager.bhop.isEnabled() && ModuleManager.bhop.damageBoost.isToggled() && ModuleUtils.firstDamage && (!ModuleManager.bhop.damageBoostRequireKey.isToggled() || ModuleManager.bhop.damageBoostKey.isPressed())) {
                return;
            }
            if (e.getPacket() instanceof S27PacketExplosion) {
                Packet packet = e.getPacket();
                S27PacketExplosion s27PacketExplosion = (S27PacketExplosion) e.getPacket();
                S27PacketExplosion s27 = (S27PacketExplosion) packet;

                if (allowSelfFireball.isToggled() && ModuleUtils.threwFireball) {
                    if ((mc.thePlayer.getPosition().distanceSq(s27.getX(), s27.getY(), s27.getZ()) <= ModuleUtils.MAX_EXPLOSION_DIST_SQ) || disableVelo) {
                        disableVelo = true;
                        ModuleUtils.threwFireball = false;
                        e.setCanceled(false);
                        return;
                    }
                }
                if (!dontEditMotion() && !disableVelo) {
                    if (explosionsHorizontal.getInput() == 0 && explosionsVertical.getInput() > 0) {
                        mc.thePlayer.motionY += s27PacketExplosion.func_149144_d() * explosionsVertical.getInput() / 100.0;
                    } else if (explosionsHorizontal.getInput() > 0 && explosionsVertical.getInput() == 0) {
                        mc.thePlayer.motionX += s27PacketExplosion.func_149149_c() * explosionsHorizontal.getInput() / 100.0;
                        mc.thePlayer.motionZ += s27PacketExplosion.func_149147_e() * explosionsHorizontal.getInput() / 100.0;
                    } else if (explosionsHorizontal.getInput() > 0 && explosionsVertical.getInput() > 0) {
                        mc.thePlayer.motionX += s27PacketExplosion.func_149149_c() * explosionsHorizontal.getInput() / 100.0;
                        mc.thePlayer.motionY += s27PacketExplosion.func_149144_d() * explosionsVertical.getInput() / 100.0;
                        mc.thePlayer.motionZ += s27PacketExplosion.func_149147_e() * explosionsHorizontal.getInput() / 100.0;
                    }
                }

                stopFBvelo = true;
                e.setCanceled(true);
                disableVelo = false;
            }
            if (e.getPacket() instanceof S12PacketEntityVelocity) {
                if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
                    S12PacketEntityVelocity s12PacketEntityVelocity = (S12PacketEntityVelocity) e.getPacket();

                    if (!stopFBvelo) {
                        if (!dontEditMotion() && !disableVelo) {
                            if (horizontal.getInput() == 0 && vertical.getInput() > 0) {
                                mc.thePlayer.motionY = ((double) s12PacketEntityVelocity.getMotionY() / 8000) * vertical.getInput() / 100.0;
                            } else if (horizontal.getInput() > 0 && vertical.getInput() == 0) {
                                mc.thePlayer.motionX = ((double) s12PacketEntityVelocity.getMotionX() / 8000) * horizontal.getInput() / 100.0;
                                mc.thePlayer.motionZ = ((double) s12PacketEntityVelocity.getMotionZ() / 8000) * horizontal.getInput() / 100.0;
                            } else if (horizontal.getInput() > 0 && vertical.getInput() > 0) {
                                mc.thePlayer.motionX = ((double) s12PacketEntityVelocity.getMotionX() / 8000) * horizontal.getInput() / 100.0;
                                mc.thePlayer.motionY = ((double) s12PacketEntityVelocity.getMotionY() / 8000) * vertical.getInput() / 100.0;
                                mc.thePlayer.motionZ = ((double) s12PacketEntityVelocity.getMotionZ() / 8000) * horizontal.getInput() / 100.0;
                            }
                        }
                    }
                    else {
                        if (!dontEditMotion() && !disableVelo) {
                            if (explosionsHorizontal.getInput() == 0 && explosionsVertical.getInput() > 0) {
                                mc.thePlayer.motionY = ((double) s12PacketEntityVelocity.getMotionY() / 8000) * explosionsVertical.getInput() / 100.0;
                            } else if (explosionsHorizontal.getInput() > 0 && explosionsVertical.getInput() == 0) {
                                mc.thePlayer.motionX = ((double) s12PacketEntityVelocity.getMotionX() / 8000) * explosionsHorizontal.getInput() / 100.0;
                                mc.thePlayer.motionZ = ((double) s12PacketEntityVelocity.getMotionZ() / 8000) * explosionsHorizontal.getInput() / 100.0;
                            } else if (explosionsHorizontal.getInput() > 0 && explosionsVertical.getInput() > 0) {
                                mc.thePlayer.motionX = ((double) s12PacketEntityVelocity.getMotionX() / 8000) * explosionsHorizontal.getInput() / 100.0;
                                mc.thePlayer.motionY = ((double) s12PacketEntityVelocity.getMotionY() / 8000) * explosionsVertical.getInput() / 100.0;
                                mc.thePlayer.motionZ = ((double) s12PacketEntityVelocity.getMotionZ() / 8000) * explosionsHorizontal.getInput() / 100.0;
                            }
                        }
                    }

                    stopFBvelo = false;
                    if (!disableVelo) {
                        e.setCanceled(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent ev) {
        if (mode.getInput() == 0) {
            if (Utils.nullCheck() && !LongJump.stopVelocity && !ModuleManager.bedAura.cancelKnockback()) {
                if (mc.thePlayer.maxHurtTime <= 0 || mc.thePlayer.hurtTime != mc.thePlayer.maxHurtTime) {
                    return;
                }
                if (onlyWhileAttacking.isToggled() && !ModuleUtils.isAttacking) {
                    return;
                }
                if (dontEditMotion()) {
                    return;
                }
                if (onlyWhileTargeting.isToggled() && (mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null)) {
                    return;
                }
                if (disableS.isToggled() && Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
                    return;
                }
                if (chance.getInput() == 0) {
                    return;
                }
                if (disableLobby.isToggled() && Utils.isLobby()) {
                    return;
                }
                if (chance.getInput() != 100) {
                    double ch = Math.random();
                    if (ch >= chance.getInput() / 100.0D) {
                        return;
                    }
                }
                if (horizontal.getInput() != 100.0D) {
                    mc.thePlayer.motionX *= horizontal.getInput() / 100;
                    mc.thePlayer.motionZ *= horizontal.getInput() / 100;
                }
                if (vertical.getInput() != 100.0D) {
                    mc.thePlayer.motionY *= vertical.getInput() / 100;
                }
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook) {
            checkAim(((C03PacketPlayer.C05PacketPlayerLook) e.getPacket()).getYaw(), ((C03PacketPlayer.C05PacketPlayerLook) e.getPacket()).getPitch());
        }
        else if (e.getPacket() instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
            checkAim(((C03PacketPlayer.C06PacketPlayerPosLook) e.getPacket()).getYaw(), ((C03PacketPlayer.C06PacketPlayerPosLook) e.getPacket()).getPitch());
        }
    }

    public boolean dontEditMotion() {
        if (mc.thePlayer.motionY >= verticalM.getInput() && !mc.thePlayer.onGround || mode.getInput() == 1 && zzWhileNotTargeting.isToggled() && KillAura.attackingEntity == null) {
            return true;
        }
        return false;
    }

    private boolean hasBadEffect() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            String name = potionEffect.getEffectName();
            return name.equals("potion.jump") || name.equals("potion.poison") || name.equals("potion.wither");
        }
        return false;
    }

    private void checkAim(float yaw, float pitch) {
        MovingObjectPosition result = RotationUtils.rayTrace(5, ((IAccessorMinecraft) mc).getTimer().renderPartialTicks, new float[] { yaw, pitch }, null);
        aiming = result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && result.entityHit instanceof EntityOtherPlayerMP;
    }
}