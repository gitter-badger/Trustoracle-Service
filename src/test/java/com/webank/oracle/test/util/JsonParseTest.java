package com.webank.oracle.test.util;

import com.webank.oracle.base.enums.ContractTypeEnum;
import com.webank.oracle.base.enums.ReqStatusEnum;
import com.webank.oracle.base.properties.ConstantProperties;
import com.webank.oracle.base.properties.EventRegister;
import com.webank.oracle.base.utils.HttpUtil;
import com.webank.oracle.contract.ContractDeploy;
import com.webank.oracle.test.base.BaseTest;
import com.webank.oracle.trial.contract.APISampleOracle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fisco.bcos.web3j.crypto.gm.GenCredential;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertNull;

/**
 *
 */

@Slf4j
public class JsonParseTest extends BaseTest {

    public static final String[] URL_ARRAY = new String[]{
         "plain(https://www.random.org/integers/?num=100&min=1&max=100&col=1&base=10&format=plain&rnd=new)",
         "plain(https://www.random.org/integers/?num=100&min=1&max=100&col=1&base=10&format=plain&rnd=new)[5]",
         "json(https://api.exchangerate-api.com/v4/latest/CNY).rates.JPY",
          "json(https://api.kraken.com/0/public/Ticker?pair=ETHXBT).result.XETHXXBT.c[0]"

    };

    @Test
    public void testApiConsumer() {
        credentials = GenCredential.create();

            EventRegister eventRegister = eventRegisterProperties.getEventRegisters().get(0);

            int chainId = eventRegister.getChainId();
            int groupId = eventRegister.getGroup();
            Web3j web3j = getWeb3j(chainId, groupId);

            Optional<ContractDeploy> deployOptional =
                    this.contractDeployRepository.findByChainIdAndGroupIdAndContractType(chainId, groupId, ContractTypeEnum.ORACLE_CORE.getId());
            if (!deployOptional.isPresent()) {
                Assertions.fail();
                return;
            }

            String oracleCoreAddress = deployOptional.get().getContractAddress();
            log.info("oracle core address " + oracleCoreAddress);

            try {
                APISampleOracle apiConsumer = APISampleOracle.deploy(web3j, credentials, ConstantProperties.GAS_PROVIDER, oracleCoreAddress).send();

                // asset
                for (String url : URL_ARRAY) {
                    TransactionReceipt t = apiConsumer.setUrl(url).send();

                    String apiConsumerAddress = apiConsumer.getContractAddress();
                    log.info("Deploy APIConsumer contract:[{}]", apiConsumerAddress);

                    TransactionReceipt t1 = apiConsumer.request().send();
                    log.info("Generate api receipt: [{}:{}]", t1.getStatus(), t1.getOutput());

                    Thread.sleep(3000);

                    BigInteger result = apiConsumer.result().send();
                    log.info("httpresult:[{}]", result);

                    Assertions.assertTrue(result.compareTo(BigInteger.ZERO) != 0);
                    Assertions.assertNull(reqHistoryRepository.findByReqId(t1.getOutput()).get().getError());
                    }


                String wrongUrl= "json(https://api.kraken.com/0/public/Ticker?pair=ETHXBT).result.XETHXXBT.c.0";
                apiConsumer.setUrl(wrongUrl).send();
                TransactionReceipt transactionReceipt = apiConsumer.request().send();
                Thread.sleep(3000);
                assertNotNull(reqHistoryRepository.findByReqId(transactionReceipt.getOutput()).get().getError());

                } catch (Exception e) {
                e.printStackTrace();
                Assertions.fail();
            }

    }



}

