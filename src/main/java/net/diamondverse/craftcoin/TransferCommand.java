package net.diamondverse.craftcoin;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.dreamerzero.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.craftcoin.walletconnectmc.api.WalletConnectMCApi;
import org.web3j.ierc20.IERC20;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static com.mojang.brigadier.arguments.DoubleArgumentType.*;
import static net.minecraft.commands.Commands.*;

public class TransferCommand {
  private final WalletConnectMCApi api;
  private final IERC20 token;
  private final String confirm;
  private final String playerNotFound;
  private final String error;

  public TransferCommand(
      WalletConnectMCApi api, IERC20 token, String confirm, String playerNotFound, String error) {
    this.api = api;
    this.token = token;
    this.confirm = confirm;
    this.playerNotFound = playerNotFound;
    this.error = error;
  }

  public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        literal("transfer")
            .requires(s -> s.hasPermission(Integer.MAX_VALUE, "craftcoin.transfer"))
            .requires(
                s ->
                    s.getBukkitEntity() != null
                        && s.getBukkitEntity().getType() == EntityType.PLAYER)
            .then(
                argument("name/address", StringArgumentType.word())
                    .then(
                        argument("amount", doubleArg())
                            .executes(
                                c -> {
                                  String addressString = c.getArgument("name/address", String.class);
                                  double amount = c.getArgument("amount", Double.class);
                                  try {
                                    byte[] address = Numeric.hexStringToByteArray(addressString);
                                    if (address.length != 20) {
                                      throw new IllegalArgumentException("Invalid address");
                                    }
                                    api.getTransactionManager((Player) c.getSource().getBukkitEntity())
                                        .thenAccept(
                                            manager -> {
                                              if (manager == null) return;
                                              IERC20 playerToken =
                                                  IERC20.load(
                                                      token.getContractAddress(),
                                                      api.getWeb3(),
                                                      manager,
                                                      new DefaultGasProvider());
                                              playerToken
                                                  .transfer(
                                                      Numeric.toHexString(address),
                                                      Utils.fromDouble(amount))
                                                  .sendAsync()
                                                  .thenAccept(
                                                      receipt -> {
                                                        if (!receipt.isStatusOK()) {
                                                          c.getSource()
                                                              .getBukkitSender()
                                                              .sendMessage(
                                                                  MiniMessage.miniMessage()
                                                                      .deserialize(error));
                                                        }
                                                      })
                                                  .exceptionally(
                                                      exception -> {
                                                        c.getSource()
                                                            .getBukkitSender()
                                                            .sendMessage(
                                                                MiniMessage.miniMessage()
                                                                    .deserialize(error));
                                                        return null;
                                                      });
                                            })
                                        .exceptionally(
                                            exception -> {
                                              c.getSource()
                                                  .getBukkitSender()
                                                  .sendMessage(
                                                      MiniMessage.miniMessage().deserialize(error));
                                              return null;
                                            });
                                    return 0;
                                  } catch (Exception exception) {
                                    // maybe addressString is name
                                    Bukkit.getScheduler().runTaskAsynchronously(CraftCoin.getPlugin(CraftCoin.class),
                                        () -> {
                                      OfflinePlayer player = Bukkit.getOfflinePlayer(addressString);
                                      sendConfirmationMessage(c.getSource().getBukkitSender(),
                                          amount,
                                          player.getUniqueId(),
                                          player.getName());
                                    });
                                    return 0;
                                  }
                                }))));
  }

  private void sendConfirmationMessage(CommandSender sender, double amount, UUID id, String name) {
    api.getAddress(id)
        .thenAccept(
            address ->
                sender.sendMessage(
                    Utils.replace(confirm
                            .replaceAll(
                                "<amount>",
                                String.valueOf(amount))
                            .replaceAll(
                                "<receiver>",
                                Numeric.toHexString(address)),
                        // TODO fix minimessage to parse
                        // tags in commands
                        null,
                        Placeholder.unparsed(
                            "name", name),
                        Placeholder.unparsed(
                            "amount",
                            String.valueOf(amount)),
                        Placeholder.unparsed(
                            "receiver",
                            Numeric.toHexString(
                                address)))))
        .exceptionally(
            exception2 -> {
              sender.sendMessage(
                  MiniMessage.miniMessage()
                      .deserialize(
                          playerNotFound,
                          TagResolver.builder()
                              .resolver(
                                  Placeholder.unparsed(
                                      "name", name))
                              .resolver(
                                  Placeholder.unparsed(
                                      "amount",
                                      String.valueOf(amount)))
                              .resolver(MiniPlaceholders.getGlobalPlaceholders())
                              .build()));
              return null;
            });
  }
}
