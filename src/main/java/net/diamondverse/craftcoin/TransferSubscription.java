package net.diamondverse.craftcoin;

import io.reactivex.disposables.Disposable;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.craftcoin.walletconnectmc.api.WalletConnectMCApi;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Type;
import org.web3j.ierc20.IERC20;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TransferSubscription {
  Disposable disposable;

  public TransferSubscription(WalletConnectMCApi api,
                              IERC20 token,
                              BalanceCache balanceCache,
                              String sent,
                              String received) {
    disposable = api.getWeb3()
        .ethLogFlowable(
            new EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST,
                token.getContractAddress())
                .addSingleTopic(EventEncoder.encode(IERC20.TRANSFER_EVENT)))
        .subscribe(
            log -> {
              List<Type> values =
                  FunctionReturnDecoder.decode(
                      log.getData(), IERC20.TRANSFER_EVENT.getNonIndexedParameters());
              if (values.size() != 1
                  || !values.get(0).getTypeAsString().equals("uint256")
                  || log.getTopics().size() != 3) {
                throw new RuntimeException("Invalid transfer event received");
              }
              double amount = Utils.toDouble((BigInteger) values.get(0).getValue());
              String senderAddress =
                  log.getTopics().get(1).substring("0x".length() + (256 - 160) / 8 * 2);
              String receiverAddress =
                  log.getTopics().get(2).substring("0x".length() + (256 - 160) / 8 * 2);
              api.getPlayerAccountsByAddress(Numeric.hexStringToByteArray(senderAddress))
                  .thenAccept(
                      senders ->
                          api.getPlayerAccountsByAddress(Numeric.hexStringToByteArray(receiverAddress))
                              .thenAccept(
                                  receivers -> {
                                    TagResolver resolver =
                                        TagResolver.builder()
                                            .resolver(
                                                Placeholder.unparsed(
                                                    "amount", String.valueOf(amount)))
                                            .resolver(Placeholder.unparsed("sender", senderAddress))
                                            .resolver(
                                                Placeholder.unparsed("receiver", receiverAddress))
                                            .resolver(
                                                Placeholder.unparsed(
                                                    "tx_hash", log.getTransactionHash()))
                                            .build();
                                    for (UUID sender : senders) {
                                      if (Bukkit.getPlayer(sender) != null) {
                                        Player p = Objects.requireNonNull(Bukkit.getPlayer(sender));
                                        p.sendMessage(Utils.replace(sent, p, resolver));
                                        balanceCache.add(sender, -amount);
                                      }
                                    }
                                    for (UUID receiver : receivers) {
                                      if (Bukkit.getPlayer(receiver) != null) {
                                        Player p = Objects.requireNonNull(Bukkit.getPlayer(receiver));
                                        p.sendMessage(Utils.replace(received, p, resolver));
                                        balanceCache.add(receiver, amount);
                                      }
                                    }
                                  }));
            });
  }
}
