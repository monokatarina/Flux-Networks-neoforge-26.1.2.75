package sonar.fluxnetworks.common.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.common.capability.FluxPlayer;
import sonar.fluxnetworks.register.Messages;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;

public class FluxCommands {

    public static void register(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(FluxNetworks.MODID)
                .then(Commands.literal("superadmin")
                        .requires(s -> true) // Permite qualquer um (singleplayer)
                        .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                .then(Commands.argument("enable", BoolArgumentType.bool())
                                        .executes(s -> superAdmin(s.getSource(),
                                                GameProfileArgument.getGameProfiles(s, "targets"),
                                                BoolArgumentType.getBool(s, "enable"))
                                        )
                                )
                        )
                )
        );
    }

    private static int superAdmin(@Nonnull CommandSourceStack source,
                                  @Nonnull Collection<NameAndId> profiles, boolean enable) {
        PlayerList playerList = source.getServer().getPlayerList();
        int success = 0;

        for (NameAndId nameAndId : profiles) {
            // NameAndId tem Id() que retorna UUID
            UUID playerId = nameAndId.id();
            ServerPlayer player = playerList.getPlayer(playerId);
            if (player != null) {
                final FluxPlayer fp = FluxPlayer.get(player);
                if (fp != null && fp.setSuperAdmin(enable)) {
                    Messages.syncCapability(player);
                    player.sendSystemMessage(Component.translatable(enable ?
                            "gui.fluxnetworks.superadmin.on" : "gui.fluxnetworks.superadmin.off"));
                    success++;
                }
            }
        }

        return success;
    }
}