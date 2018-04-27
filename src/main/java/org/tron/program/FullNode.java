package org.tron.program;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadBlockException;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;

@Slf4j
public class FullNode {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("Full node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    if (cfgArgs.isNeedReplay()) {
      String dataBaseDir = Args.getInstance().getLocalDBDirectory();
      ReplayBlockUtils.backupAndCleanDb(dataBaseDir);
    }

    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }

    ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);

    if (cfgArgs.isNeedReplay()) {
      try {
        String dataBaseDir = Args.getInstance().getLocalDBDirectory();
        Manager dbManager = context.getBean(Manager.class);
        logger.error(dbManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum() + "lastestSloid");
        ReplayBlockUtils.backupAndCleanDb(dataBaseDir);
        ReplayBlockUtils.replayBlock(dbManager, dataBaseDir);
      } catch (BadBlockException e) {
        logger.error("bad block", e.getMessage());
        logger.error("delete local db", e.getMessage());
      }
    }

    Application appT = ApplicationFactory.create(context);
    shutdown(appT);
    //appT.init(cfgArgs);
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    if (cfgArgs.isWitness()) {
      appT.addService(new WitnessService(appT));
    }
    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();
    rpcApiService.blockUntilShutdown();
  }

  private static void shutdown(final Application app) {
    logger.info("******** application shutdown ********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }
}
