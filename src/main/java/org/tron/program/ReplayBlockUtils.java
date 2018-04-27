package org.tron.program;

import java.nio.file.Paths;
import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.ValidateSignatureException;


@Slf4j
public class ReplayBlockUtils {

  public static void cleanDb(String dataBaseDir) {
    dataBaseDir += "/database";
    String[] dbs = new String[]{
        "account",
        "asset-issue",
        "block-index",
        "block_KDB",
        "peers",
        "trans",
        "utxo",
        "witness",
        "witness_schedule",
        "nodeId.properties"
    };

    for (String db : dbs) {
      System.out.println(Paths.get(dataBaseDir, db).toString());
      FileUtil.recursiveDelete(Paths.get(dataBaseDir, db).toString());
    }
  }

  public static void main(String[] args) throws BadBlockException {
    Args.setParam(args, Constant.TESTNET_CONF);

    String dataBaseDir = Args.getInstance().getLocalDBDirectory();
    cleanDb(dataBaseDir);

    ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);

    Manager dbManager = context.getBean(Manager.class);
    replayBlock(dbManager);
  }

  public static void replayBlock(Manager dbManager) throws BadBlockException {
    long latestSolidifiedBlockNum = dbManager.getDynamicPropertiesStore()
        .getLatestSolidifiedBlockNum();
    BlockStore localBlockStore = dbManager.getBlockStore();

    logger.info("local db latestSolidifiedBlockNum:" + latestSolidifiedBlockNum);

    dbManager.resetDynamicProperties();
    Iterator iterator = localBlockStore.iterator();
    long replayIndex = 0;

    logger.info("replay solidified block start");
    while (iterator.hasNext() && replayIndex <= latestSolidifiedBlockNum) {
      BlockCapsule blockCapsule = (BlockCapsule) iterator.next();
      if (replayIndex == 0) {
        // skip Genesis Block
        replayIndex++;
        continue;
      }
      try {
        dbManager.replayBlock(blockCapsule);
        dbManager.getDynamicPropertiesStore()
            .saveLatestSolidifiedBlockNum(replayIndex);
        logger.info(String.format("replay block %d", replayIndex));
      } catch (ValidateSignatureException e) {
        throw new BadBlockException("validate signature exception");
      } catch (ContractValidateException e) {
        throw new BadBlockException("validate contract exception");
      } catch (UnLinkedBlockException e) {
        throw new BadBlockException("validate unlink exception");
      } catch (ContractExeException e) {
        throw new BadBlockException("validate contract exe exception");
      }
      replayIndex++;
    }

    logger.info("delete non-solidified block start");
    if (replayIndex != 1L || (replayIndex - 1 == latestSolidifiedBlockNum)) {
      while (iterator.hasNext()) {
        BlockCapsule blockCapsule = (BlockCapsule) iterator.next();
        logger.info("delete :" + blockCapsule.toString());
        dbManager.getBlockStore().delete(blockCapsule.getBlockId().getBytes());
      }
    }
    logger.info("delete non-solidified block complete");
    logger.info("replay solidified block complete");
    logger.info("LatestSolidifiedBlockNum:" + dbManager.getDynamicPropertiesStore()
        .getLatestSolidifiedBlockNum());

  }

}
