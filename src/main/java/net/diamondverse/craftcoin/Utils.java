package net.diamondverse.craftcoin;

import me.clip.placeholderapi.PlaceholderAPI;
import me.dreamerzero.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.UUID;

public class Utils {
  static BigInteger denominator;

  public static String formatBalance(BalanceCache balanceCache, UUID uuid) {
    if (!balanceCache.getBalances().containsKey(uuid)) {
      balanceCache.requestBalanceUpdate(uuid);
    }
    return String.format("%.02f", balanceCache.getBalances().getOrDefault(uuid, 0.0));
  }

  public static double toDouble(BigInteger amount) {
    return amount.multiply(BigInteger.valueOf(100)).divide(denominator).doubleValue() / 100.0;
  }

  public static BigInteger fromDouble(double amount) {
    return BigInteger.valueOf((long) (amount * 1_000_000)) // 6-digit precision
        .divide(BigInteger.valueOf(1_000_000))
        .multiply(denominator);
  }

  public static Component replace(@NotNull String text, @Nullable Player player, TagResolver... resolvers) {
    return MiniMessage.miniMessage()
        .deserialize(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                ? PlaceholderAPI.setPlaceholders(player, text)
                : text,
            TagResolver.builder()
                .resolvers(resolvers)
                .resolver(Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders")
                    ? (
                    player != null
                        ? MiniPlaceholders.getAudienceGlobalPlaceholders(player)
                        : MiniPlaceholders.getGlobalPlaceholders())
                    : TagResolver.empty())
                .build());
  }
}
