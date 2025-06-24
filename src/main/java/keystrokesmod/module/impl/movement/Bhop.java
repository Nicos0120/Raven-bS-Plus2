package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Bhop extends Module {
    public SliderSetting mode;
    public static SliderSetting friction;
    public static SliderSetting speedSetting;
    private ButtonSetting liquidDisable;
    public ButtonSetting disablerOnly;
    private ButtonSetting sneakDisable;
    private ButtonSetting jumpMoving;
    public ButtonSetting slowBackwards, damageBoost, strafe, damageBoostRequireKey;
    public GroupSetting damageBoostGroup, strafeGroup;
    private SliderSetting strafeDegrees;
    public KeySetting damageBoostKey;
    public String[] modes = new String[]{"Strafe", "Ground", "hyp-9t", "hyp-8t", "hyp-7t"};
    public boolean hopping, lowhop, didMove, setRotation;
    public boolean isNormalPos;
    public boolean running;

    public Bhop() {
        super("Bhop", Module.category.movement);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
        this.registerSetting(disablerOnly = new ButtonSetting("Require disabler", false));
        this.registerSetting(speedSetting = new SliderSetting("Speed", 2.0, 0.8, 1.2, 0.01));
        this.registerSetting(friction = new SliderSetting("Friction multiplier", 1, 1, 1.3, 0.01));
        this.registerSetting(liquidDisable = new ButtonSetting("Disable in liquid", true));
        this.registerSetting(sneakDisable = new ButtonSetting("Disable while sneaking", true));
        this.registerSetting(jumpMoving = new ButtonSetting("Only jump when moving", true));
        this.registerSetting(slowBackwards = new ButtonSetting("Slow backwards", false));

        this.registerSetting(damageBoostGroup = new GroupSetting("Damage boost"));
        this.registerSetting(damageBoost = new ButtonSetting(damageBoostGroup, "Enable Damage boost", false));
        this.registerSetting(damageBoostRequireKey = new ButtonSetting(damageBoostGroup,"Require key", false));
        this.registerSetting(damageBoostKey = new KeySetting(damageBoostGroup,"Enable key", 51));

        this.registerSetting(strafeGroup = new GroupSetting("Direction strafe"));
        this.registerSetting(strafe = new ButtonSetting(strafeGroup, "Enable Direction strafe", false));
        this.registerSetting(strafeDegrees = new SliderSetting(strafeGroup, "Degrees", 80, 50, 90, 5));
    }

    public void guiUpdate() {
        this.damageBoostKey.setVisible(damageBoostRequireKey.isToggled(), this);

        this.disablerOnly.setVisible(mode.getInput() >= 2, this);
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (!mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying) {
            return;
        }
        if (hopping) {
            mc.thePlayer.movementInput.jump = false;
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (((mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) && liquidDisable.isToggled()) || (mc.thePlayer.isSneaking() && sneakDisable.isToggled())) {
            return;
        }
        if (ModuleManager.scaffold.moduleEnabled || ModuleManager.scaffold.lowhop) {
            return;
        }
        if (LongJump.function) {
            return;
        }
        if (ModuleManager.invmove.active()) {
            return;
        }
        if (mode.getInput() >= 1) {
            if (mc.thePlayer.onGround && (!jumpMoving.isToggled() || Utils.isMoving())) {
                if (mc.thePlayer.moveForward <= -0.5 && !ModuleManager.killAura.rotating && !Utils.noSlowingBackWithBow() && !ModuleManager.scaffold.isEnabled) {
                    setRotation = true;
                }
                if (mode.getInput() != 3) {
                    mc.thePlayer.jump();
                }
                else {
                    mc.thePlayer.motionY = 0.41999998688698;
                }
                running = true;
                if (mc.thePlayer.posY % 1 == 0) {
                    isNormalPos = true;
                }
                else {
                    isNormalPos = false;
                }
                double speed = (speedSetting.getInput() - 0.52);
                double speedModifier = speed;
                final int speedAmplifier = Utils.getSpeedAmplifier();
                switch (speedAmplifier) {
                    case 1:
                        speedModifier = speed + 0.02;
                        break;
                    case 2:
                        speedModifier = speed + 0.04;
                        break;
                    case 3:
                        speedModifier = speed + 0.1;
                        break;
                }

                if (Utils.isMoving()) {
                    if (!Utils.noSlowingBackWithBow() && !ModuleManager.sprint.disableBackwards() && !slowBackwards()) {
                        Utils.setSpeed((speedModifier) * ModuleUtils.applyFrictionMulti());
                    }
                    else {
                        Utils.setSpeed((speedModifier) - 0.3);
                    }
                    didMove = true;
                }
                hopping = true;
            }
            if (mc.thePlayer.moveForward <= 0.5 && hopping) {
                ModuleUtils.handleSlow();
            }
            if (!mc.thePlayer.onGround) {
                hopping = false;
            }
        }
        switch ((int) mode.getInput()) {
            case 0:
                if (Utils.isMoving()) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                    mc.thePlayer.setSprinting(true);
                    Utils.setSpeed(Utils.getHorizontalSpeed() + 0.005 * speedSetting.getInput());
                    hopping = true;
                    break;
                }
                break;
            case 1:
                break;
            //lowhops
        }
        /*if (rotateYawOption.isToggled()) {
            if (!ModuleManager.killAura.isTargeting && !Utils.noSlowingBackWithBow() && !ModuleManager.scaffold.isEnabled && !mc.thePlayer.isCollidedHorizontally && mc.thePlayer.onGround) {
                float yaw = mc.thePlayer.rotationYaw;
                e.setYaw(yaw - hardcodedYaw());
            }
        }*/

        if (strafe.isToggled()) {
            airStrafe();
        }

    }

    private boolean slowBackwards() {
        return slowBackwards.isToggled() && mc.thePlayer.moveForward <= -0.5;
    }

    public float hardcodedYaw() {
        float simpleYaw = 0F;
        float f = 0.8F;

        if (mc.thePlayer.moveForward == 0) {
            if (mc.thePlayer.moveStrafing >= f) simpleYaw += 90;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw -= 90;
        }
        else if (mc.thePlayer.moveForward <= -f) {
            simpleYaw -= 180;
            if (mc.thePlayer.moveStrafing >= f) simpleYaw -= 45;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw += 45;
        }
        return simpleYaw;
    }

    private void airStrafe() {
        if (!mc.thePlayer.onGround && mc.thePlayer.hurtTime < 3 && (mc.thePlayer.motionX != 0 || mc.thePlayer.motionZ != 0)) {
            float moveDir = moveDirection(mc.thePlayer.rotationYaw);
            float currentMotionDir = strafeDirection();
            float diff = Math.abs(moveDir - currentMotionDir);
            int range = (int) strafeDegrees.getInput();

            if (diff > 180 - range && diff < 180 + range) {
                mc.thePlayer.motionX = -(mc.thePlayer.motionX * 0.85);
                mc.thePlayer.motionZ = -(mc.thePlayer.motionZ * 0.85);
            }
        }
    }

    private float moveDirection(float rawYaw) {
        float yaw = ((rawYaw % 360) + 360) % 360 > 180 ? ((rawYaw % 360) + 360) % 360 - 360 : ((rawYaw % 360) + 360) % 360;
        float forward = 1;

        if (mc.thePlayer.moveForward < 0) yaw += 180;
        if (mc.thePlayer.moveForward < 0) forward = -0.5F;
        if (mc.thePlayer.moveForward > 0) forward = 0.5F;

        if (mc.thePlayer.moveStrafing > 0) yaw -= 90 * forward;
        if (mc.thePlayer.moveStrafing  < 0) yaw += 90 * forward;

        return (float) (yaw);
    }

    private float strafeDirection() {
        float yaw = (float) Math.toDegrees(Math.atan2(-mc.thePlayer.motionX, mc.thePlayer.motionZ));
        if (yaw < 0) yaw += 360;
        return yaw;
    }

    public void onDisable() {
        hopping = false;
        running = false;
    }
}