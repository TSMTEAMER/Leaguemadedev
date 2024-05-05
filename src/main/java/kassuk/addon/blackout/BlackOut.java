package kassuk.addon.blackout;

import kassuk.addon.blackout.commands.*;
import kassuk.addon.blackout.hud.ayo.ImageHUD;
import kassuk.addon.blackout.modules.Exploits.*;
import kassuk.addon.blackout.modules.Exploits.FastLatency.FastLatency;
import kassuk.addon.blackout.modules.Player.*;
import kassuk.addon.blackout.modules.chat.*;
import kassuk.addon.blackout.modules.chat.roast.Roast;
import kassuk.addon.blackout.modules.combat.*;
import kassuk.addon.blackout.hud.WalperTextHud;
import kassuk.addon.blackout.globalsettings.*;
import kassuk.addon.blackout.hud.*;
import com.mojang.logging.LogUtils;
import kassuk.addon.blackout.modules.misc.*;
import kassuk.addon.blackout.modules.movement.*;
import kassuk.addon.blackout.modules.render.*;
import kassuk.addon.blackout.themes.MercuryGuiTheme;
import kassuk.addon.blackout.utils.walper.SpotifyUtils;
import kassuk.addon.blackout.utils.walper.WalperStarScript;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import kassuk.addon.blackout.utils.vh.Stats;
import net.minecraft.item.Items;
import org.slf4j.Logger;

/**
 * @author OLEPOSSU
 * @author KassuK
 */

public class BlackOut extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    public static final Category CHATPLUS = new Category("chat+", Items.BLAZE_ROD.getDefaultStack());

    public static final Category SETTINGS = new Category("Settings", Items.BELL.getDefaultStack());
    public static final Category EXPLOITS = new Category("Exploits", Items.REDSTONE.getDefaultStack());
    public static final Category COMBATPLUS = new Category("Combat+", Items.END_CRYSTAL.getDefaultStack());
    public static final Category MISCPLUS = new Category("misc+", Items.BLUE_BED.getDefaultStack());
    public static final Category MOVEMENTPLUS = new Category("movement+", Items.POTION.getDefaultStack());
    public static final Category RENDERPLUS = new Category("render+", Items.AMETHYST_BLOCK.getDefaultStack());
    public static final Category PLAYERPLUS = new Category("Player+", Items.ANVIL.getDefaultStack());


    public static final HudGroup HUD_BLACKOUT = new HudGroup("League");
    public static final String BLACKOUT_NAME = "League";

    public static final String COLOR = "Color is the visual perception of different wavelengths of light as hue, saturation, and brightness";

    @Override
    public void onInitialize() {
        LOG.info("Initializing Leaguye");
        LOG.info(
            "\uD835\uDC11\uD835\uDC22\uD835\uDC1C\uD835\uDC22\uD835\uDC27.\uD835\uDC1C\uD835\uDC1C\n" +
                "\uD835\uDC2C\uD835\uDC2E\uD835\uDC1C\uD835\uDC1C\uD835\uDC1E\uD835\uDC2C\uD835\uDC2C\uD835\uDC1F\uD835\uDC2E\uD835\uDC25\uD835\uDC25\uD835\uDC32 \uD835\uDC0B\uD835\uDC28\uD835\uDC1A\uD835\uDC1D\uD835\uDC1E\uD835\uDC1D\n" +
                "\uD835\uDC0C\uD835\uDC1A\uD835\uDC1D\uD835\uDC1E \uD835\uDC1B\uD835\uDC32 \uD835\uDFCF\uD835\uDC27\uD835\uDC23\uD835\uDC1E\uD835\uDC1C\uD835\uDC2D\n" +
                "\uD835\uDC05\uD835\uDC2E\uD835\uDC1C\uD835\uDC24 \uD835\uDC2C\uD835\uDC24\uD835\uDC22\uD835\uDC1D\uD835\uDC2C");

        initializeModules(Modules.get());

        initializeSettings(Modules.get());

        initializeSettings(Modules.get());

        initializeCommands();

        initializeHud(Hud.get());


    }

    private void initializeModules(Modules modules) {
        modules.add(new VehicleOneHit());
        modules.add(new GamemodeNotifier());
        modules.add(new PistonBurrow());
        modules.add(new TPFly());
        modules.add(new MinecartAura());
        modules.add(new BurrowEsp());
        modules.add(new GradientOverlay());
        modules.add(new AutoSand());
        modules.add(new AutoChunkBan());
        modules.add(new AntiPlacement());
        modules.add(new AutoGold());
        modules.add(new BlockClap());
        modules.add(new BurrowAlert());
        modules.add(new SoundLocator());
        modules.add(new FlightAntikick());
        modules.add(new AnchorAura());
        modules.add(new DubCounter());
        modules.add(new ChestExplorer());
        modules.add(new BedBomb());
        modules.add(new AnchorBomb());
        modules.add(new SkeletonESP());
        modules.add(new AutoBedTrap());
        modules.add(new AutoWither());
        modules.add(new HitboxDesync());
        modules.add(new AutoSandTwo());
        modules.add(new CevBreaker());
        modules.add(new BedBombV2());
        modules.add(new ChorusPredict());
        modules.add(new ChatConfig());
        modules.add(new DmSpam());
        modules.add(new BTPistonPush());
        modules.add(new tntaura());
        modules.add(new AutoCityPlus());
        modules.add(new PistonAura());
        modules.add(new AutoCrystalPlus());
        modules.add(new AntiPiston());
        modules.add(new FastXP());
        modules.add(new AntiSurroundBlocks());
        modules.add(new FastLatency());
        modules.add(new OldAnvil());
        modules.add(new CityBreaker());
        modules.add(new Strafe());
        modules.add(new AntiRegear());
        modules.add(new QQuiver());
        modules.add(new HeadProtect());
        modules.add(new GroundSpeed());
        modules.add(new AntiLay());
        modules.add(new Phase());
        modules.add(new Sevila());
        modules.add(new SpamBypass());
        modules.add(new EFly());
        modules.add(new OffHando());
        modules.add(new FloRida());
        modules.add(new Spama());
        modules.add(new BedDisabler());
        modules.add(new DiscordNotifier());
        modules.add(new Auto32K());
        modules.add(new OpenAnarchyAutoDupe());
        modules.add(new ItemFrameDupe());
        modules.add(new XsDupe());
        modules.add(new ArmorNotify());
        modules.add(new AnteroTaateli());
        modules.add(new AntiCrystal());
        modules.add(new AutoGriffer());
        modules.add(new AntiAim());
        modules.add(new AntiCrawl());
        modules.add(new Scan());
        modules.add(new AutoCraftingTable());
        modules.add(new AutoCrystal());
        modules.add(new AutoEz());
        modules.add(new Twerk());
        modules.add(new Automation());
        modules.add(new Surround());
        modules.add(new CityBreaker());
        modules.add(new AutoTraprewrite());
        modules.add(new PacketLogger());
        modules.add(new Holefillrewrite());
        modules.add(new AutoMend());
        modules.add(new AutoMine());
        modules.add(new AfkLogout());
        modules.add(new AutoMoan());
        modules.add(new BowBomb());
        modules.add(new ArmorMessages());
        modules.add(new AutoPearl());
        modules.add(new AutoTrap());
        modules.add(new BedAura());
        modules.add(new ChatCommands());
        modules.add(new ChatColor());
        modules.add(new XPThrower());
        modules.add(new Blocker());
        modules.add(new AimAssist());
        modules.add(new VenomCrystal());
        modules.add(new QuartzFarmer());
        modules.add(new AntiBot());
        modules.add(new MultiTasks());
        modules.add(new TriggerBot());
        modules.add(new AutoFarm());
        modules.add(new LecternCrash());
        modules.add(new CustomFOV());
        modules.add(new ElytraFlyPlus());
        modules.add(new Roast());
        modules.add(new poplag());
        modules.add(new FeetESP());
        modules.add(new FlightPlus());
        modules.add(new Fog());
        modules.add(new ForceSneak());
        modules.add(new HoleFillPlus());
        modules.add(new HoleFill());
        modules.add(new HoleSnap());
        modules.add(new Jesus());
        modules.add(new killAura());
        modules.add(new TntRange());
        modules.add(new LightsOut());
        modules.add(new WalpuhThighHighlighter());
        modules.add(new BedSaver());
        modules.add(new MineESP());
        modules.add(new OffHandPlus());
        modules.add(new DiscordRPC());
        modules.add(new PacketFly());
        modules.add(new BuildPoop());
        modules.add(new PingSpoof());
        modules.add(new PistonCrystal());
        modules.add(new PistonPush());
        modules.add(new BanEvasion());
        modules.add(new PortalGodMode());
        modules.add(new ScaffoldPlus());
        modules.add(new SelfTrapPlus());
        modules.add(new SoundModifier());
        modules.add(new SpeedPlus());
        modules.add(new SprintPlus());
        modules.add(new StepPlus());
        modules.add(new StrictNoSlow());
        modules.add(new Suicide());
        modules.add(new SurroundPlus());
        modules.add(new SwingModifier());
        modules.add(new TickShift());
    }

    private void initializeSettings(Modules modules) {
        modules.add(new FacingSettings());
        modules.add(new RangeSettings());
        modules.add(new RaytraceSettings());
        modules.add(new RotationSettings());
        modules.add(new ServerSettings());
        modules.add(new SwingSettings());
    }

    private void initializeCombat(Modules modules) {
        modules.add(new AntiCrystal());
    }

    private void initializeCommands() {
        Commands.add(new AddWaypoint());
        Commands.add(new ReconnectCommand());
        Commands.add(new VelocityTeleportCMD());
        Commands.add(new GitCommand());
        Commands.add(new TerrainExport());
        Commands.add(new Coords());
    }

    private void initializeHud(Hud hud) {
        hud.register(ArmorHudPlus.INFO);
        hud.register(BlackoutArray.INFO);
        hud.register(SpotifyHud.INFO);
        hud.register(primordial.INFO);
        hud.register(TextRadarHud.INFO);
        hud.register(BindsHud.INFO);
        hud.register(TextPresets.INFO);
        hud.register(GearHud.INFO);
        hud.register(ImageHUD.INFO);
        hud.register(HudWaterMark.INFO);
        hud.register(Bigrat.INFO);
        hud.register(RadarHud.INFO);
        hud.register(WalperTextHud.INFO);
        hud.register(Freevirus.INFO);
        hud.register(Keys.INFO);
        hud.register(Grass.INFO);
        hud.register(TargetHud.INFO);
        hud.register(Welcomer.INFO);
        hud.register(OnTope.INFO);
        hud.register(CatGirl.INFO);
        GuiThemes.add(new MercuryGuiTheme());
        WalperTextHud.create("Spotify Track and Artist", "Current Track - #1{walper.spotifyTrack} by #1{walper.spotifyArtist} ");
    }


    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(COMBATPLUS);
        Modules.registerCategory(MISCPLUS);
        Modules.registerCategory(RENDERPLUS);
        Modules.registerCategory(MOVEMENTPLUS);
        Modules.registerCategory(PLAYERPLUS);
        Modules.registerCategory(EXPLOITS);
        Modules.registerCategory(CHATPLUS);
        Modules.registerCategory(SETTINGS);
    }


    @Override
    public String getPackage() {
        return "kassuk.addon.blackout";
    }


}
