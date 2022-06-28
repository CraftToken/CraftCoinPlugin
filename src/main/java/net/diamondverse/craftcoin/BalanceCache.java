package net.diamondverse.craftcoin;

import com.google.common.collect.ImmutableMap;
import org.craftcoin.walletconnectmc.api.WalletConnectMCApi;
import org.web3j.ierc20.IERC20;
import org.web3j.utils.Numeric;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BalanceCache {
  private final WalletConnectMCApi api = new WalletConnectMCApi();
  private final IERC20 token;
  private final Map<UUID, Double> balances = new HashMap<>();

  public BalanceCache(IERC20 token) {
    this.token = token;
  }

  public ImmutableMap<UUID, Double> getBalances() {
    return ImmutableMap.copyOf(balances);
  }

  public void requestBalanceUpdate(UUID player) {
    api.getAddress(player)
        .thenAccept(
            address ->
                token
                    .balanceOf(Numeric.toHexString(address))
                    .sendAsync()
                    .thenAccept(balance -> balances.put(player, Utils.toDouble(balance))));
  }

  public void add(UUID sender, double amount) {
    if (balances.containsKey(sender)) {
      balances.merge(sender, amount, Double::sum);
    } else {
      requestBalanceUpdate(sender);
    }
  }
}
