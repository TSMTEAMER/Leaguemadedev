package kassuk.addon.blackout.modules.movement;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;

public class Strafe extends Module {

    private final SettingGroup sgGeneral = settings.createGroup("General");
    private final SettingGroup sgGround = settings.createGroup("on-Ground");
    private final SettingGroup sgAir = settings.createGroup("on-Air");
    private final SettingGroup sgTimer = settings.createGroup("Timer");

    private final Setting<Boolean> autoJump = sgGeneral.add(new BoolSetting.Builder().name("auto-jump").defaultValue(true).build());

    //Ground
    private final Setting<Boolean> groundMain = sgGround.add(new BoolSetting.Builder().name("ground").description("Strafe will work while player is standing on ground.").defaultValue(true).build());
    private final Setting<Double> groundSpeed = sgGround.add(new DoubleSetting.Builder().name("speed").description("Increased speed while player is standing on the ground.").defaultValue(1.10).sliderRange(0, 1.5).visible(groundMain::get).build());
    //Air
    private final Setting<Boolean> airMain = sgAir.add(new BoolSetting.Builder().name("air").description("Strafe will work while player will be in the air.").defaultValue(true).build());
    private final Setting<Double> airSpeed = sgAir.add(new DoubleSetting.Builder().name("speed").defaultValue(0.90).description("Increased speed while player is standing on the ground.").sliderRange(0, 1.5).visible(airMain::get).build());
    //Timer
    private final Setting<Boolean> timerMain = sgTimer.add(new BoolSetting.Builder().name("timer").description("Activates timer module while strafe turned on.").defaultValue(false).build());
    public final Setting<Double> timerGround = sgTimer.add(new DoubleSetting.Builder().name("ground").description("How fast timer should work while player is on ground.").defaultValue(1.2).sliderMin(0.01).sliderMax(7.5).build());
    public final Setting<Double> timerAir = sgTimer.add(new DoubleSetting.Builder().name("air").description("How fast timer should work while player is in the air.").defaultValue(1.1).sliderMin(0.01).sliderMax(7.5).build());

    public Strafe() {
        super(BlackOut.MOVEMENTPLUS, "strafe", "Modifies your movement speed when moving on the ground.");
    }

    private float speed;
    private boolean jumped;

    @Override
    public void onActivate() {
        jumped = true;
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA && mc.player.isFallFlying()) return;
        if (mc.player.input.movementForward != 0 | mc.player.input.movementSideways != 0) {
            mc.player.setSprinting(true);

            if (mc.player.getHungerManager().getFoodLevel() <= 6) return;
            if (mc.player.isSneaking()) return;
            if (!groundMain.get() && mc.player.isOnGround()) return;
            if (!airMain.get() && !mc.player.isOnGround()) return;

            if (timerMain.get())
                if (mc.player.isOnGround()) {
                    Modules.get().get(Timer.class).setOverride(PlayerUtils.isMoving() ? timerGround.get() : Timer.OFF);
                } else Modules.get().get(Timer.class).setOverride(PlayerUtils.isMoving() ? timerAir.get() : Timer.OFF);

            speed *= fix();

            double[] speedA = getSpeedTransform(Math.max((airSpeed.get() / 2.5) * speed * (getSpeed() / 0.15321), 0.15321));
            double[] speedG = getSpeedTransform(getSpeed() * (0.2873 / 0.15321) * groundSpeed.get());

            if (!mc.player.isOnGround()) {
                mc.player.setVelocity(speedA[0], mc.player.getVelocity().y, speedA[1]);
            } else {
                speed = 1;
                mc.player.setVelocity(speedG[0], mc.player.getVelocity().y, speedG[1]);
            }
        }
    }

    @EventHandler
    private void onJumplmao(Render3DEvent event) {
        if (autoJump.get() && mc.player != null && mc.world != null)
            mc.options.jumpKey.setPressed(mc.player.isOnGround() && !mc.player.isSneaking() && forwardSpeed());
    }

    public double[] getSpeedTransform(final double speed) {
        float
                forward = mc.player.input.movementForward,
                sideways = mc.player.input.movementSideways,
                yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw) * mc.getTickDelta();
        return getSpeedTransform(speed, forward, sideways, yaw);
    }

    public static double[] getSpeedTransform(final double speed, float forwards, float sideways, float yawDegrees) {
        return getSpeedTransform(speed, forwards, sideways, Math.toRadians(yawDegrees));
    }

    public static double[] getSpeedTransform(final double speed, float forwards, float sideways, double yaw) {
        if (forwards != 0) {
            if (sideways > 0) yaw += forwards > 0 ? -Math.PI / 4 : Math.PI / 4;
            else if (sideways < 0) yaw += forwards > 0 ? Math.PI / 4 : -Math.PI / 4;

            sideways = 0;

            if (forwards > 0) forwards = 1;
            else if (forwards < 0) forwards = -1;
        }

        yaw += Math.PI / 2;

        return new double[]{
                forwards * speed * Math.cos(yaw) + sideways * speed * Math.sin(yaw),
                forwards * speed * Math.sin(yaw) - sideways * speed * Math.cos(yaw)
        };
    }

    private double getSpeed() {
        double spd = 0.15321;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            spd *= 1.0 + 0.2 * (amplifier + 1);
        }

        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            spd /= 1.0 + 0.2 * (amplifier + 1);
        }
        return spd;
    }

    private boolean forwardSpeed() {
        if (mc.player.forwardSpeed != 0) return true;
        return mc.player.sidewaysSpeed != 0;
    }

    private float fix() {
        if (mc.player.isInLava()) return 1 - 0.5f;
        if (mc.player.isSubmergedInWater()) return 1 - 0.2f;
        return 1 - 0.022f;
    }
}
