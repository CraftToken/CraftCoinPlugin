package net.diamondverse.craftcoin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PapiExpansion extends PlaceholderExpansion {
  private final BalanceCache balanceCache;

  public PapiExpansion(BalanceCache balanceCache) {
    this.balanceCache = balanceCache;
  }

  @NotNull
  @Override
  public String getIdentifier() {
    return "craftcoin";
  }

  @NotNull
  @Override
  public String getAuthor() {
    return "Sliman4";
  }

  @NotNull
  @Override
  public String getVersion() {
    return "1.0.0";
  }

  @Override
  public String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
    if (params.equals("balance") && player != null) {
      return Utils.formatBalance(balanceCache, player.getUniqueId());
    } else {
      return null;
    }
  }

  @Override
  public boolean persist() {
    return true;
  }
}
