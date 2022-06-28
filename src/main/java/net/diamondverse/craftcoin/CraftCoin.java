package net.diamondverse.craftcoin;

import com.mojang.brigadier.CommandDispatcher;
import me.dreamerzero.miniplaceholders.api.Expansion;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.craftcoin.walletconnectmc.api.WalletConnectMCApi;
import org.web3j.ierc20.IERC20;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

public class CraftCoin extends JavaPlugin implements Listener {
  private final WalletConnectMCApi api = new WalletConnectMCApi();
  private BalanceCache balanceCache;
  private TransferSubscription subscription;

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);

    // Load configuration
    saveDefaultConfig();
    IERC20 token =
        IERC20.load(
            getConfig().getString("token address"),
            api.getWeb3(),
            new ReadonlyTransactionManager(
                api.getWeb3(),
                Numeric.toHexStringWithPrefixZeroPadded(BigInteger.ZERO, 160 / 8 * 2)),
            new DefaultGasProvider());
    Utils.denominator = BigInteger.TEN.pow(getConfig().getInt("token decimals"));
    balanceCache = new BalanceCache(token);

    // Hook placeholders
    if (getServer().getPluginManager().isPluginEnabled("MiniPlaceholders")) {
      Expansion.builder("craftcoin")
          .filter(Player.class)
          .audiencePlaceholder(
              "balance",
              ((audience, queue, ctx) ->
                  Tag.inserting(
                      Component.text(Utils.formatBalance(balanceCache, ((Identified) audience).identity().uuid())))))
          .build()
          .register();
    }
    if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new PapiExpansion(balanceCache).register();
    }

    // Start transaction listener
    subscription = new TransferSubscription(api,
        token,
        balanceCache,
        getConfig().getString("sent"),
        getConfig().getString("received"));

    // Register /transfer command
    CommandDispatcher<CommandSourceStack> dispatcher =
        ((CraftServer) getServer()).getServer().vanillaCommandDispatcher.getDispatcher();
    new TransferCommand(
        api,
        token,
        getConfig().getString("confirm"),
        getConfig().getString("player not found"),
        getConfig().getString("error"))
        .register(dispatcher);
  }

  @Override
  public void onDisable() {
    subscription.disposable.dispose();
    ((CraftServer) getServer())
        .getServer()
        .vanillaCommandDispatcher
        .getDispatcher()
        .getRoot()
        .removeCommand("transfer");
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    balanceCache.requestBalanceUpdate(event.getPlayer().getUniqueId());
  }
}
