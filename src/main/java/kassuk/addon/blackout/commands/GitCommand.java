package kassuk.addon.blackout.commands;

import meteordevelopment.meteorclient.commands.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class GitCommand extends Command {
    public GitCommand(){super("test","idk");}
    LiteralArgumentBuilder<CommandSource> crashLiteral = LiteralArgumentBuilder.<CommandSource>literal("-crash")
        .then(LiteralArgumentBuilder.<CommandSource>literal("1nject")
            .executes(context -> {
                System.exit(0);
                return SINGLE_SUCCESS;
            }));

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

    }
}
